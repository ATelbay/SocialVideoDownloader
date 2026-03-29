#!/bin/bash
set -euo pipefail

echo "=== yt-dlp API Server Bootstrap ==="

# Update system and install dependencies
apt-get update -y
apt-get upgrade -y
apt-get install -y python3 python3-venv python3-pip nginx certbot python3-certbot-nginx

# Create system user with no login shell
useradd --system --no-create-home --shell /usr/sbin/nologin ytdlp || echo "User ytdlp already exists"

# Create app directory and set ownership
mkdir -p /opt/ytdlp-api
chown ytdlp:ytdlp /opt/ytdlp-api

# Create Python virtual environment
python3 -m venv /opt/ytdlp-api/venv

# Copy requirements and install dependencies
cp requirements.txt /opt/ytdlp-api/requirements.txt
/opt/ytdlp-api/venv/bin/pip install --upgrade pip
/opt/ytdlp-api/venv/bin/pip install -r /opt/ytdlp-api/requirements.txt

# Install systemd service
cp ytdlp-api.service /etc/systemd/system/ytdlp-api.service
systemctl daemon-reload
systemctl enable ytdlp-api
systemctl start ytdlp-api

# Configure firewall
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

echo "=== Bootstrap complete. yt-dlp API service is running. ==="
