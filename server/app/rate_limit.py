from __future__ import annotations

import ipaddress
import threading
import time
from collections import defaultdict, deque

from fastapi import HTTPException, Request, WebSocket

from app.config import settings

# Sliding-window counters keyed by client identifier. In-memory and per-process;
# good enough for a single-instance personal deployment. Behind a load balancer
# this would need a shared store (e.g. Redis).
_WINDOW_SECONDS = 60.0
_hits: dict[str, deque[float]] = defaultdict(deque)
_lock = threading.Lock()
# Sweep idle keys periodically so a flood of distinct (e.g. spoofed) IPs cannot
# grow _hits without bound. Counted under _lock.
_SWEEP_EVERY_CALLS = 256
_calls_since_sweep = 0


def _trusted_networks() -> list[ipaddress._BaseNetwork]:
    nets: list[ipaddress._BaseNetwork] = []
    for entry in settings.TRUSTED_PROXIES:
        try:
            nets.append(ipaddress.ip_network(entry, strict=False))
        except ValueError:
            # Ignore malformed entries rather than failing closed on every request.
            continue
    return nets


def _peer_is_trusted(peer: str | None) -> bool:
    if not peer:
        return False
    nets = _trusted_networks()
    if not nets:
        return False
    try:
        addr = ipaddress.ip_address(peer)
    except ValueError:
        return False
    return any(addr in net for net in nets)


def client_ip(request: Request | WebSocket) -> str:
    """Resolve the rate-limit key for a caller.

    X-Forwarded-For / X-Real-IP are only honoured when the direct TCP peer is a
    configured trusted proxy (``TRUSTED_PROXIES``). Otherwise the headers are
    client-controlled and would let anyone spoof a fresh identity per request to
    bypass the per-IP limit, so we fall back to the real peer address.
    """
    client = request.client
    peer = client.host if client else None

    if _peer_is_trusted(peer):
        forwarded = request.headers.get("x-forwarded-for")
        if forwarded:
            # First hop is the original client (proxy appends downstream).
            return forwarded.split(",")[0].strip()
        real_ip = request.headers.get("x-real-ip")
        if real_ip:
            return real_ip.strip()

    return peer or "unknown"


def _sweep_locked(cutoff: float) -> None:
    """Drop keys whose most recent hit is older than the window. Caller holds _lock."""
    stale = [k for k, bucket in _hits.items() if not bucket or bucket[-1] < cutoff]
    for k in stale:
        del _hits[k]


def _allow(key: str) -> bool:
    global _calls_since_sweep
    limit = settings.RATE_LIMIT_PER_MINUTE
    if limit <= 0:
        return True
    now = time.monotonic()
    cutoff = now - _WINDOW_SECONDS
    with _lock:
        _calls_since_sweep += 1
        if _calls_since_sweep >= _SWEEP_EVERY_CALLS:
            _calls_since_sweep = 0
            _sweep_locked(cutoff)

        bucket = _hits[key]
        while bucket and bucket[0] < cutoff:
            bucket.popleft()
        if len(bucket) >= limit:
            return False
        bucket.append(now)
        return True


def enforce_rate_limit(request: Request) -> None:
    """FastAPI dependency: raise HTTP 429 when the per-IP minute budget is spent."""
    if not _allow(client_ip(request)):
        raise HTTPException(
            status_code=429,
            detail=f"Rate limit exceeded ({settings.RATE_LIMIT_PER_MINUTE}/min)",
        )


def check_rate_limit_ws(websocket: WebSocket) -> bool:
    """Return True if the WebSocket caller is within budget."""
    return _allow(client_ip(websocket))
