from __future__ import annotations

from collections import namedtuple

import pytest

import app.rate_limit as rl
from app.config import settings

_Client = namedtuple("_Client", ["host"])


class _FakeRequest:
    """Minimal stand-in for a Starlette Request/WebSocket for client_ip()."""

    def __init__(self, peer: str | None, headers: dict[str, str] | None = None):
        self.client = _Client(peer) if peer is not None else None
        # Starlette Headers are case-insensitive; emulate with lowercased keys.
        self.headers = {k.lower(): v for k, v in (headers or {}).items()}


@pytest.fixture(autouse=True)
def _reset_state():
    original = list(settings.TRUSTED_PROXIES)
    rl._hits.clear()
    rl._calls_since_sweep = 0
    yield
    settings.TRUSTED_PROXIES = original
    rl._hits.clear()
    rl._calls_since_sweep = 0


# --- X-Forwarded-For trust -------------------------------------------------

def test_xff_ignored_when_no_trusted_proxy():
    settings.TRUSTED_PROXIES = []
    req = _FakeRequest(peer="203.0.113.9", headers={"X-Forwarded-For": "1.2.3.4"})
    assert rl.client_ip(req) == "203.0.113.9"


def test_xff_ignored_when_peer_not_trusted():
    settings.TRUSTED_PROXIES = ["10.0.0.0/8"]
    req = _FakeRequest(peer="203.0.113.9", headers={"X-Forwarded-For": "1.2.3.4"})
    assert rl.client_ip(req) == "203.0.113.9"


def test_xff_honoured_when_peer_trusted():
    settings.TRUSTED_PROXIES = ["127.0.0.1"]
    req = _FakeRequest(
        peer="127.0.0.1",
        headers={"X-Forwarded-For": "1.2.3.4, 127.0.0.1"},
    )
    assert rl.client_ip(req) == "1.2.3.4"


def test_x_real_ip_fallback_when_trusted():
    settings.TRUSTED_PROXIES = ["127.0.0.1"]
    req = _FakeRequest(peer="127.0.0.1", headers={"X-Real-IP": "5.6.7.8"})
    assert rl.client_ip(req) == "5.6.7.8"


def test_spoofed_xff_cannot_bypass_limit():
    """Each request carries a unique spoofed XFF but the real peer is untrusted,
    so they all share one bucket and the limit still trips."""
    settings.TRUSTED_PROXIES = []
    limit = settings.RATE_LIMIT_PER_MINUTE
    allowed = 0
    for i in range(limit + 5):
        req = _FakeRequest(peer="203.0.113.9", headers={"X-Forwarded-For": f"1.2.3.{i}"})
        if rl._allow(rl.client_ip(req)):
            allowed += 1
    assert allowed == limit


# --- Bounded growth --------------------------------------------------------

def test_hits_swept_after_window(monkeypatch):
    fake_now = {"t": 1000.0}
    monkeypatch.setattr(rl.time, "monotonic", lambda: fake_now["t"])
    monkeypatch.setattr(rl, "_SWEEP_EVERY_CALLS", 4)

    # Seed several distinct idle keys.
    for i in range(50):
        rl._allow(f"ip-{i}")
    assert len(rl._hits) == 50

    # Advance well past the window so all seeded keys are stale, then make a few
    # calls to trigger the sweep.
    fake_now["t"] += rl._WINDOW_SECONDS + 1
    for _ in range(rl._SWEEP_EVERY_CALLS + 1):
        rl._allow("active")

    # Only the still-active key should remain.
    assert "active" in rl._hits
    assert len(rl._hits) == 1
