from __future__ import annotations

import asyncio
from typing import Optional

import yt_dlp
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel

from app.config import settings

router = APIRouter(tags=["extract"])


class ExtractRequest(BaseModel):
    url: str


class FormatInfo(BaseModel):
    format_id: str
    ext: str
    resolution: Optional[str]
    filesize: Optional[int]
    url: str
    vcodec: Optional[str] = None
    acodec: Optional[str] = None


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

    ydl_opts = {
        "extract_flat": False,
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 30,
    }

    def _extract():
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            return ydl.extract_info(url, download=False)

    try:
        info = await asyncio.to_thread(_extract)
    except yt_dlp.utils.DownloadError as exc:
        raise HTTPException(status_code=422, detail="Extraction failed for the provided URL") from exc
    except yt_dlp.utils.ExtractorError as exc:
        raise HTTPException(status_code=422, detail="Extraction failed for the provided URL") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="An unexpected server error occurred") from exc

    if info is None:
        raise HTTPException(status_code=422, detail="Could not extract video info")

    raw_formats: list[dict] = info.get("formats") or []
    formats: list[FormatInfo] = []
    for fmt in raw_formats:
        ext = fmt.get("ext") or ""
        format_note = fmt.get("format_note") or ""
        fmt_url = fmt.get("url") or ""

        if ext == "mhtml":
            continue
        if "storyboard" in format_note.lower():
            continue
        if not fmt_url:
            continue

        resolution: Optional[str] = fmt.get("resolution")
        if not resolution:
            width = fmt.get("width")
            height = fmt.get("height")
            if width and height:
                resolution = f"{width}x{height}"

        formats.append(
            FormatInfo(
                format_id=fmt.get("format_id", ""),
                ext=ext,
                resolution=resolution,
                filesize=fmt.get("filesize"),
                url=fmt_url,
                vcodec=fmt.get("vcodec"),
                acodec=fmt.get("acodec"),
            )
        )

    return ExtractResponse(
        title=info.get("title", ""),
        thumbnail=info.get("thumbnail"),
        duration=info.get("duration"),
        formats=formats,
    )
