from __future__ import annotations

import urllib.request
from email.message import Message

import pytest

import app.security as security
from app.ws_request_handler import _ValidatingRedirectHandler


def _handler():
    return _ValidatingRedirectHandler()


def test_redirect_to_private_is_blocked(monkeypatch):
    def fake_validate(url: str) -> str:
        if "169.254.169.254" in url or "127.0.0.1" in url:
            raise security.UnsafeUrlError("non-public")
        return url

    monkeypatch.setattr(security, "validate_public_url", fake_validate)

    req = urllib.request.Request("https://public.example.com/start")
    with pytest.raises(security.UnsafeUrlError):
        _handler().redirect_request(
            req, None, 302, "Found", Message(),
            "http://169.254.169.254/latest/meta-data/",
        )


def test_redirect_to_loopback_is_blocked(monkeypatch):
    def fake_validate(url: str) -> str:
        if "127.0.0.1" in url:
            raise security.UnsafeUrlError("loopback")
        return url

    monkeypatch.setattr(security, "validate_public_url", fake_validate)

    req = urllib.request.Request("https://public.example.com/start")
    with pytest.raises(security.UnsafeUrlError):
        _handler().redirect_request(
            req, None, 302, "Found", Message(), "http://127.0.0.1:8000/admin",
        )


def test_redirect_to_public_is_allowed(monkeypatch):
    monkeypatch.setattr(security, "validate_public_url", lambda url: url)

    req = urllib.request.Request("https://public.example.com/start")
    new_req = _handler().redirect_request(
        req, None, 302, "Found", Message(), "https://cdn.example.com/video.mp4",
    )
    assert new_req is not None
    assert new_req.get_full_url() == "https://cdn.example.com/video.mp4"
