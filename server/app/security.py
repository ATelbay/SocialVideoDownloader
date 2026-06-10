from __future__ import annotations

import ipaddress
import socket
from urllib.parse import urlsplit

# Only these URL schemes may ever be fetched server-side.
_ALLOWED_SCHEMES = ("http", "https")


class UnsafeUrlError(ValueError):
    """Raised when a URL is rejected by the SSRF guard."""


def _is_blocked_ip(ip: str) -> bool:
    try:
        addr = ipaddress.ip_address(ip)
    except ValueError:
        # Not an IP literal — caller resolves the hostname separately.
        return False
    return (
        addr.is_private
        or addr.is_loopback
        or addr.is_link_local
        or addr.is_reserved
        or addr.is_multicast
        or addr.is_unspecified
    )


def validate_public_url(url: str) -> str:
    """
    Validate that ``url`` is an http(s) URL that resolves only to public
    addresses. Blocks SSRF to loopback, private, link-local (cloud metadata
    169.254.169.254), reserved and multicast ranges.

    Returns the stripped URL or raises :class:`UnsafeUrlError`.
    """
    cleaned = (url or "").strip()
    if not cleaned:
        raise UnsafeUrlError("URL must not be empty")

    parts = urlsplit(cleaned)
    if parts.scheme.lower() not in _ALLOWED_SCHEMES:
        raise UnsafeUrlError(f"Unsupported URL scheme: {parts.scheme or '(none)'}")

    host = parts.hostname
    if not host:
        raise UnsafeUrlError("URL has no host")

    # Reject obvious local hostnames before any DNS work.
    lowered = host.lower()
    if lowered == "localhost" or lowered.endswith(".localhost") or lowered.endswith(".local"):
        raise UnsafeUrlError(f"Host not allowed: {host}")

    # If the host is an IP literal, check it directly.
    if _is_blocked_ip(host):
        raise UnsafeUrlError(f"Host resolves to a non-public address: {host}")

    # Otherwise resolve the hostname and ensure every address is public.
    try:
        infos = socket.getaddrinfo(host, parts.port or None, proto=socket.IPPROTO_TCP)
    except socket.gaierror as exc:
        raise UnsafeUrlError(f"Could not resolve host: {host}") from exc

    for info in infos:
        ip = info[4][0]
        if _is_blocked_ip(ip):
            raise UnsafeUrlError(f"Host resolves to a non-public address: {host} -> {ip}")

    return cleaned
