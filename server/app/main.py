import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routes import extract, health

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting ytdlp-api on %s:%d", settings.HOST, settings.PORT)
    logger.info("Allowed origins: %s", settings.ALLOWED_ORIGINS)
    yield
    logger.info("Shutting down ytdlp-api")


app = FastAPI(title="ytdlp-api", version="0.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(extract.router, prefix="/extract")
app.include_router(health.router, prefix="")


@app.get("/")
async def root():
    return {"service": "ytdlp-api", "version": "0.1.0"}
