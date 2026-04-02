from __future__ import annotations

import asyncio
import base64
import logging
import urllib.request
from concurrent.futures import Future
from dataclasses import dataclass, field
from email.message import Message
from io import BytesIO
from typing import TYPE_CHECKING
from urllib.response import addinfourl

logger = logging.getLogger(__name__)

from yt_dlp.networking.common import (
    Request,
    RequestDirector,
    RequestHandler,
    Response,
)
from yt_dlp.networking.exceptions import TransportError

if TYPE_CHECKING:
    from fastapi import WebSocket


@dataclass
class WSContext:
    websocket: "WebSocket"
    loop: asyncio.AbstractEventLoop
    pending: dict[str, Future] = field(default_factory=dict)
    request_counter: int = 0


class WebSocketProxyRH(RequestHandler):
    """Routes HTTP requests through a WebSocket-connected iOS client."""

    RH_KEY = "WebSocketProxy"
    RH_NAME = "WebSocketProxy"

    _SUPPORTED_URL_SCHEMES = ("http", "https")
    _SUPPORTED_PROXY_SCHEMES = ()

    def __init__(self, *, ctx: WSContext, logger, **kwargs):
        super().__init__(logger=logger, **kwargs)
        self._ctx = ctx

    def _check_extensions(self, extensions):
        # Accept any extensions — we don't validate them ourselves.
        pass

    def _send(self, request: Request) -> Response:
        ctx = self._ctx

        request_id = f"req-{ctx.request_counter:03d}"
        ctx.request_counter += 1
        logger.debug("WS proxy _send %s: %s %s", request_id, request.method, request.url)

        body_bytes: bytes | None = None
        if request.data is not None:
            data = request.data
            if isinstance(data, bytes):
                body_bytes = data
            else:
                body_bytes = b"".join(data)

        # Inject cookies from the cookie jar into headers
        cookiejar = self._get_cookiejar(request)
        urllib_req = urllib.request.Request(request.url)
        for k, v in self._get_headers(request).items():
            urllib_req.add_unredirected_header(k, v)
        cookiejar.add_cookie_header(urllib_req)
        headers = dict(urllib_req.header_items())

        msg = {
            "type": "http_request",
            "id": request_id,
            "url": request.url,
            "method": request.method,
            "headers": headers,
            "body": base64.b64encode(body_bytes).decode() if body_bytes else None,
        }

        future: Future = Future()
        ctx.pending[request_id] = future

        asyncio.run_coroutine_threadsafe(
            ctx.websocket.send_json(msg), ctx.loop
        ).result(timeout=30)

        result = future.result(timeout=90)

        if result.get("type") == "http_error":
            raise TransportError(result.get("error", "Unknown WS proxy error"))

        response_body = base64.b64decode(result.get("body") or "")
        response_url = result.get("url", request.url)
        response_headers = result.get("headers") or {}

        # Extract Set-Cookie headers from response into the cookie jar
        msg_headers = Message()
        for k, v in response_headers.items():
            msg_headers[k] = v
        mock_resp = addinfourl(
            BytesIO(response_body), msg_headers, response_url
        )
        mock_resp.status = result.get("status", 200)
        cookiejar.extract_cookies(mock_resp, urllib_req)

        return Response(
            fp=BytesIO(response_body),
            url=response_url,
            headers=response_headers,
            status=result.get("status", 200),
        )


def inject_ws_handler(ydl, ctx: WSContext) -> None:
    """Shadow ydl._request_director with one that only uses WebSocketProxyRH."""
    from yt_dlp.utils._utils import _YDLLogger

    logger = _YDLLogger(ydl)
    director = RequestDirector(logger=logger)
    handler = WebSocketProxyRH(
        ctx=ctx,
        logger=logger,
        cookiejar=ydl.cookiejar,
        headers=ydl.params.get("http_headers", {}),
    )
    director.add_handler(handler)

    # Shadow the functools.cached_property by writing to instance __dict__
    ydl.__dict__["_request_director"] = director
