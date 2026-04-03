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
        logger.info("WS proxy _send %s: %s %s headers=%s", request_id, request.method, request.url, dict(self._get_headers(request).items()))

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
        logger.info("WS proxy %s: got response type=%s status=%s", request_id, result.get("type"), result.get("status"))

        if result.get("type") == "http_error":
            error_msg = result.get("error", "")
            logger.info("WS proxy %s: error=%s", request_id, error_msg)
            # iOS NSURLSessionDataTask -1103 bug — fallback to server-side request
            if "-1103" in error_msg:
                logger.info("WS proxy %s: iOS -1103, falling back to direct request", request_id)
                return self._direct_request(request)
            raise TransportError(error_msg or "Unknown WS proxy error")

        response_body = base64.b64decode(result.get("body") or "")
        response_url = result.get("url", request.url)
        raw_headers = result.get("headers") or []

        # Parse headers: support both legacy dict and new array-of-pairs format
        if isinstance(raw_headers, dict):
            header_pairs = list(raw_headers.items())
        else:
            header_pairs = [(pair[0], pair[1]) for pair in raw_headers]

        response_headers = {}
        for k, v in header_pairs:
            response_headers.setdefault(k, v)

        # Extract Set-Cookie headers from response into the cookie jar
        msg_headers = Message()
        for k, v in header_pairs:
            msg_headers[k] = v
        mock_resp = addinfourl(
            BytesIO(response_body), msg_headers, response_url,
            code=result.get("status", 200),
        )
        cookiejar.extract_cookies(mock_resp, urllib_req)

        return Response(
            fp=BytesIO(response_body),
            url=response_url,
            headers=response_headers,
            status=result.get("status", 200),
        )


    def _direct_request(self, request: Request) -> Response:
        """Fallback: make the request directly from the server using urllib."""
        cookiejar = self._get_cookiejar(request)
        urllib_req = urllib.request.Request(
            request.url,
            data=request.data,
            headers=dict(self._get_headers(request).items()),
            method=request.method,
        )
        cookiejar.add_cookie_header(urllib_req)

        resp = urllib.request.urlopen(urllib_req, timeout=30)
        response_body = resp.read()

        # Extract cookies from the direct response
        msg_headers = Message()
        for k, v in resp.headers.items():
            msg_headers[k] = v
        mock_resp = addinfourl(
            BytesIO(response_body), msg_headers, resp.url,
            code=resp.status,
        )
        cookiejar.extract_cookies(mock_resp, urllib_req)

        return Response(
            fp=BytesIO(response_body),
            url=resp.url,
            headers=dict(resp.headers),
            status=resp.status,
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
