#!/bin/bash
set -euo pipefail

echo "=== yt-dlp API Server Bootstrap ==="

# Update system and install dependencies
apt-get update -y
apt-get upgrade -y
apt-get install -y python3 python3-venv python3-pip

# Create system user with no login shell
useradd --system --no-create-home --shell /usr/sbin/nologin ytdlp || echo "User ytdlp already exists"

# Create app directory (owned by ubuntu for deploy access)
mkdir -p /opt/ytdlp-api

# Create default .env
cat > /opt/ytdlp-api/.env << 'ENVEOF'
HOST=0.0.0.0
PORT=8000
ALLOWED_ORIGINS=["*"]
UPDATE_API_KEY=
ENVEOF
chmod 644 /opt/ytdlp-api/.env

# Create Python virtual environment
python3 -m venv /opt/ytdlp-api/venv

# Install dependencies (requirements.txt placed by deploy.sh rsync)
/opt/ytdlp-api/venv/bin/pip install --upgrade pip
/opt/ytdlp-api/venv/bin/pip install -r /opt/ytdlp-api/requirements.txt

# Install systemd service
cp /opt/ytdlp-api/deploy/ytdlp-api.service /etc/systemd/system/ytdlp-api.service
systemctl daemon-reload
systemctl enable ytdlp-api
systemctl start ytdlp-api

# Configure firewall
ufw allow OpenSSH
ufw allow 8000/tcp
ufw --force enable

echo "=== Bootstrap complete. yt-dlp API service is running. ==="
