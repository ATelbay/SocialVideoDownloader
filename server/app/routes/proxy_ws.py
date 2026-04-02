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


def _extract(url: str, ctx: WSContext) -> dict:
    with yt_dlp.YoutubeDL(get_ydl_opts()) as ydl:
        inject_ws_handler(ydl, ctx)
        rd = ydl._request_director
        logger.debug("Injected handler. Director handlers: %s", list(rd.handlers.keys()))
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
    loop = asyncio.get_event_loop()
    ctx = WSContext(websocket=websocket, loop=loop)

    extract_task = asyncio.create_task(asyncio.to_thread(_extract, url, ctx))
    dispatch_task = asyncio.create_task(_dispatch_loop(ctx, websocket))

    try:
        result = await extract_task
        dispatch_task.cancel()
        await websocket.send_json({"type": "extract_result", "data": result})
    except Exception as e:
        dispatch_task.cancel()
        await websocket.send_json({"type": "extract_error", "detail": str(e)})
    finally:
        await websocket.close()
