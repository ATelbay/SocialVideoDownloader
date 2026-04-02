from __future__ import annotations

from typing import Optional

from pydantic import BaseModel


class FormatInfo(BaseModel):
    format_id: str
    ext: str
    resolution: Optional[str]
    filesize: Optional[int]
    url: str
    vcodec: Optional[str] = None
    acodec: Optional[str] = None


def get_ydl_opts() -> dict:
    return {
        "extract_flat": False,
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 30,
        "http_headers": {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/131.0.0.0 Safari/537.36"
            ),
        },
        "extractor_args": {
            "youtube": ["player_client=web", "po_token=web+bgutil"],
        },
        "js_runtimes": {
            "node": {},
        },
    }


def filter_formats(info_dict: dict) -> list[FormatInfo]:
    raw_formats: list[dict] = info_dict.get("formats") or []
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
    return formats
