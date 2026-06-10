from __future__ import annotations

import socket

import pytest

from app.security import UnsafeUrlError, validate_public_url


def _fake_getaddrinfo(ip: str):
    """Return a getaddrinfo replacement that always resolves to ``ip``."""

    def _impl(host, port, *args, **kwargs):
        family = socket.AF_INET6 if ":" in ip else socket.AF_INET
        return [(family, socket.SOCK_STREAM, socket.IPPROTO_TCP, "", (ip, port or 0))]

    return _impl


# --- Scheme validation -----------------------------------------------------

@pytest.mark.parametrize(
    "url",
    [
        "ftp://example.com/file",
        "file:///etc/passwd",
        "gopher://example.com/",
        "ws://example.com/socket",
        "example.com/no-scheme",
    ],
)
def test_rejects_non_http_schemes(url):
    with pytest.raises(UnsafeUrlError):
        validate_public_url(url)


def test_rejects_empty_url():
    with pytest.raises(UnsafeUrlError):
        validate_public_url("")
    with pytest.raises(UnsafeUrlError):
        validate_public_url("   ")


def test_rejects_url_without_host():
    with pytest.raises(UnsafeUrlError):
        validate_public_url("https://")


# --- IP literal validation (no DNS needed) ---------------------------------

@pytest.mark.parametrize(
    "url",
    [
        "http://127.0.0.1/",
        "http://127.0.0.1:5000/x",
        "http://10.0.0.5/",
        "http://192.168.1.1/router",
        "http://172.16.0.1/",
        "http://172.31.255.255/",
        "http://0.0.0.0/",
        "http://169.254.169.254/latest/meta-data/",
        "http://[::1]/",
        "http://[fc00::1]/",
        "http://[fe80::1]/",
    ],
)
def test_rejects_private_and_loopback_ip_literals(url):
    with pytest.raises(UnsafeUrlError):
        validate_public_url(url)


@pytest.mark.parametrize(
    "url",
    [
        "https://8.8.8.8/resource",
        "http://172.15.0.1/",  # outside 172.16.0.0/12
        "http://172.32.0.1/",  # outside 172.16.0.0/12
    ],
)
def test_allows_public_ip_literals(url):
    assert validate_public_url(url) == url


# --- Local hostnames -------------------------------------------------------

@pytest.mark.parametrize(
    "url",
    [
        "http://localhost/admin",
        "http://localhost:8080/",
        "http://service.local/",
        "http://api.localhost/",
    ],
)
def test_rejects_local_hostnames(url):
    with pytest.raises(UnsafeUrlError):
        validate_public_url(url)


# --- Hostname resolution (mocked DNS) --------------------------------------

def test_allows_hostname_resolving_to_public_ip(monkeypatch):
    monkeypatch.setattr(socket, "getaddrinfo", _fake_getaddrinfo("93.184.216.34"))
    url = "https://example.com/video.mp4"
    assert validate_public_url(url) == url


def test_rejects_hostname_resolving_to_private_ip(monkeypatch):
    # DNS-rebinding style: a public-looking host that resolves to a private IP.
    monkeypatch.setattr(socket, "getaddrinfo", _fake_getaddrinfo("10.1.2.3"))
    with pytest.raises(UnsafeUrlError):
        validate_public_url("https://evil.example.com/")


def test_rejects_hostname_resolving_to_metadata_ip(monkeypatch):
    monkeypatch.setattr(socket, "getaddrinfo", _fake_getaddrinfo("169.254.169.254"))
    with pytest.raises(UnsafeUrlError):
        validate_public_url("https://metadata.attacker.test/")


def test_rejects_unresolvable_hostname(monkeypatch):
    def _boom(*args, **kwargs):
        raise socket.gaierror("name resolution failed")

    monkeypatch.setattr(socket, "getaddrinfo", _boom)
    with pytest.raises(UnsafeUrlError):
        validate_public_url("https://does-not-exist.invalid/")


def test_strips_whitespace_and_returns_cleaned_url(monkeypatch):
    monkeypatch.setattr(socket, "getaddrinfo", _fake_getaddrinfo("93.184.216.34"))
    assert validate_public_url("  https://example.com/x  ") == "https://example.com/x"
