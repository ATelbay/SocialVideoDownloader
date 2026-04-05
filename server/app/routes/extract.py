from __future__ import annotations

import asyncio
from typing import Optional

import yt_dlp
from fastapi import APIRouter, Header, HTTPException, Request
from pydantic import BaseModel

from app.config import settings
from app.ytdlp_opts import FormatInfo, filter_formats, get_ydl_opts

router = APIRouter(tags=["extract"])


class ExtractRequest(BaseModel):
    url: str


class ExtractResponse(BaseModel):
    title: str
    thumbnail: Optional[str]
    duration: Optional[float]
    formats: list[FormatInfo]


@router.post("", response_model=ExtractResponse)
async def extract_video_info(
    request_body: ExtractRequest,
    request: Request,
    x_api_key: Optional[str] = Header(default=None),
) -> ExtractResponse:
    if settings.EXTRACT_API_KEY and x_api_key != settings.EXTRACT_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")

    url = request_body.url.strip() if request_body.url else ""
    if not url:
        raise HTTPException(status_code=400, detail="URL must not be empty")

    # Handle optional platform cookies
    cookies_b64 = request.headers.get("X-Platform-Cookies")
    cookie_file_path = None
    if cookies_b64:
        import base64
        import tempfile
        import os
        cookie_bytes = base64.b64decode(cookies_b64)
        fd, cookie_file_path = tempfile.mkstemp(suffix=".txt", prefix="ytdlp_cookies_")
        os.write(fd, cookie_bytes)
        os.close(fd)

    def _extract():
        with yt_dlp.YoutubeDL(get_ydl_opts(cookiefile=cookie_file_path)) as ydl:
            return ydl.extract_info(url, download=False)

    try:
        try:
            info = await asyncio.to_thread(_extract)
        except yt_dlp.utils.DownloadError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except yt_dlp.utils.ExtractorError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        except Exception as exc:
            raise HTTPException(status_code=500, detail="An unexpected server error occurred") from exc
    finally:
        # Clean up cookie tempfile
        if cookie_file_path:
            import os
            try:
                os.unlink(cookie_file_path)
            except OSError:
                pass

    if info is None:
        raise HTTPException(status_code=422, detail="Could not extract video info")

    return ExtractResponse(
        title=info.get("title", ""),
        thumbnail=info.get("thumbnail"),
        duration=info.get("duration"),
        formats=filter_formats(info),
    )
