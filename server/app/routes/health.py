from __future__ import annotations

import asyncio
import os
import sys
import time
from typing import Optional

from fastapi import APIRouter, Header, HTTPException

_venv_bin = os.path.dirname(sys.executable)
_start_time = time.time()

router = APIRouter(tags=["health"])


@router.get("/health")
async def health():
    proc = await asyncio.create_subprocess_exec(
        os.path.join(_venv_bin, "yt-dlp"), "--version",
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.DEVNULL,
    )
    stdout, _ = await proc.communicate()
    ytdlp_version = stdout.decode().strip() if proc.returncode == 0 else "unknown"
    return {
        "status": "ok",
        "ytdlp_version": ytdlp_version,
        "uptime_seconds": time.time() - _start_time,
    }


@router.post("/update-ytdlp")
async def update_ytdlp(x_api_key: Optional[str] = Header(default=None)):
    update_api_key = os.environ.get("UPDATE_API_KEY", "")
    if not update_api_key:
        raise HTTPException(status_code=403, detail="Endpoint disabled")
    if x_api_key != update_api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")

    process = await asyncio.create_subprocess_exec(
        os.path.join(_venv_bin, "pip"), "install", "-U", "yt-dlp",
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.STDOUT,
    )
    stdout, _ = await process.communicate()
    output = stdout.decode() if stdout else ""
    success = process.returncode == 0

    new_version = None
    if success:
        version_proc = await asyncio.create_subprocess_exec(
            os.path.join(_venv_bin, "yt-dlp"), "--version",
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.DEVNULL,
        )
        version_stdout, _ = await version_proc.communicate()
        if version_proc.returncode == 0:
            new_version = version_stdout.decode().strip()

    return {
        "success": success,
        "new_version": new_version,
        "output": output,
    }
