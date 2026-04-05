from __future__ import annotations

import asyncio
import logging

import yt_dlp
from fastapi import APIRouter, WebSocket
from starlette.websockets import WebSocketDisconnect

from app.ws_request_handler import WSContext, inject_ws_handler
from app.ytdlp_opts import filter_formats, get_ydl_opts

logger = logging.getLogger(__name__)

router = APIRouter(tags=["proxy"])


def _extract(url: str, ctx: WSContext, cookiefile: str | None = None) -> dict:
    with yt_dlp.YoutubeDL(get_ydl_opts(cookiefile=cookiefile)) as ydl:
        inject_ws_handler(ydl, ctx)
        rd = ydl._request_director
        logger.info("Injected handler. Director handlers: %s", list(rd.handlers.keys()))
        info = ydl.extract_info(url, download=False)
    if info is None:
        raise ValueError("Could not extract video info")
    return {
        "title": info.get("title", ""),
        "thumbnail": info.get("thumbnail"),
        "duration": info.get("duration"),
        "formats": [f.model_dump() for f in filter_formats(info)],
    }


async def _dispatch_loop(ctx: WSContext, websocket: WebSocket) -> None:
    try:
        while True:
            msg = await websocket.receive_json()
            msg_type = msg.get("type")
            msg_id = msg.get("id")
            if msg_type in ("http_response", "http_error") and msg_id in ctx.pending:
                future = ctx.pending.pop(msg_id)
                if not future.done():
                    future.set_result(msg)
    except WebSocketDisconnect:
        for future in ctx.pending.values():
            if not future.done():
                future.cancel()
        ctx.pending.clear()


@router.websocket("/extract")
async def ws_extract(websocket: WebSocket) -> None:
    await websocket.accept()

    msg = await websocket.receive_json()
    if msg.get("type") != "extract_request" or not msg.get("url"):
        await websocket.send_json({"type": "extract_error", "detail": "Invalid request"})
        await websocket.close()
        return

    url: str = msg["url"]

    # Handle optional cookies for platform authentication
    cookies_b64 = msg.get("cookies")
    cookie_file_path = None
    if cookies_b64:
        import base64
        import tempfile
        import os
        cookie_bytes = base64.b64decode(cookies_b64)
        fd, cookie_file_path = tempfile.mkstemp(suffix=".txt", prefix="ytdlp_cookies_")
        os.write(fd, cookie_bytes)
        os.close(fd)

    loop = asyncio.get_event_loop()
    ctx = WSContext(websocket=websocket, loop=loop)

    extract_task = asyncio.create_task(
        asyncio.to_thread(_extract, url, ctx, cookiefile=cookie_file_path)
    )
    dispatch_task = asyncio.create_task(_dispatch_loop(ctx, websocket))

    try:
        result = await extract_task
        dispatch_task.cancel()
        await websocket.send_json({"type": "extract_result", "data": result})
    except Exception as e:
        dispatch_task.cancel()
        await websocket.send_json({"type": "extract_error", "detail": str(e)})
    finally:
        # Clean up cookie tempfile
        if cookie_file_path:
            import os
            try:
                os.unlink(cookie_file_path)
            except OSError:
                pass
        await websocket.close()
