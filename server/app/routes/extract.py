from __future__ import annotations

import asyncio
from typing import Optional

import yt_dlp
from fastapi import APIRouter, Header, HTTPException
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
    request: ExtractRequest,
    x_api_key: Optional[str] = Header(default=None),
) -> ExtractResponse:
    if settings.EXTRACT_API_KEY and x_api_key != settings.EXTRACT_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")

    url = request.url.strip() if request.url else ""
    if not url:
        raise HTTPException(status_code=400, detail="URL must not be empty")

    def _extract():
        with yt_dlp.YoutubeDL(get_ydl_opts()) as ydl:
            return ydl.extract_info(url, download=False)

    try:
        info = await asyncio.to_thread(_extract)
    except yt_dlp.utils.DownloadError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except yt_dlp.utils.ExtractorError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="An unexpected server error occurred") from exc

    if info is None:
        raise HTTPException(status_code=422, detail="Could not extract video info")

    return ExtractResponse(
        title=info.get("title", ""),
        thumbnail=info.get("thumbnail"),
        duration=info.get("duration"),
        formats=filter_formats(info),
    )
