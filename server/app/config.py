from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    ALLOWED_ORIGINS: list[str] = ["*"]
    RATE_LIMIT_PER_MINUTE: int = 10
    # IPs/CIDRs of reverse proxies we trust to set X-Forwarded-For / X-Real-IP.
    # Empty (default) => never trust those headers; rate-limit by the real peer.
    # Set this only when the app sits behind a known proxy (e.g. nginx) so a
    # direct caller cannot spoof its identity and bypass per-IP limits.
    TRUSTED_PROXIES: list[str] = []
    UPDATE_API_KEY: str = ""
    EXTRACT_API_KEY: str = ""
    # Hard ceiling on a single extraction (HTTP or WS) to bound resource use.
    EXTRACT_TIMEOUT_SECONDS: float = 120.0
    # Cap concurrent WS proxy sessions to avoid thread-pool exhaustion.
    WS_MAX_CONCURRENT_SESSIONS: int = 8

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
