from __future__ import annotations

import asyncio
import base64
from concurrent.futures import Future
from dataclasses import dataclass, field
from io import BytesIO
from typing import TYPE_CHECKING

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

        body_bytes: bytes | None = None
        if request.data is not None:
            data = request.data
            if isinstance(data, bytes):
                body_bytes = data
            else:
                body_bytes = b"".join(data)

        msg = {
            "type": "http_request",
            "id": request_id,
            "url": request.url,
            "method": request.method,
            "headers": self._get_headers(request),
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
        return Response(
            fp=BytesIO(response_body),
            url=result.get("url", request.url),
            headers=result.get("headers") or {},
            status=result.get("status", 200),
        )


def inject_ws_handler(ydl, ctx: WSContext) -> None:
    """Shadow ydl._request_director with one that only uses WebSocketProxyRH."""
    from yt_dlp.utils._utils import _YDLLogger

    logger = _YDLLogger(ydl)
    director = RequestDirector(logger=logger)
    handler = WebSocketProxyRH(ctx=ctx, logger=logger)
    director.add_handler(handler)

    # Shadow the functools.cached_property by writing to instance __dict__
    ydl.__dict__["_request_director"] = director
