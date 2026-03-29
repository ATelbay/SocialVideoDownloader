#!/bin/bash
set -euo pipefail

EC2_HOST="13.50.106.77"
EC2_USER="ubuntu"
KEY_PATH="~/Downloads/aws-per-ytdl.pem"
REMOTE_DIR="/opt/ytdlp-api"

echo "=== Deploying to EC2 $EC2_HOST ==="

# Sync app code and requirements to remote
rsync -avz --delete \
  -e "ssh -i $KEY_PATH" \
  server/app/ "$EC2_USER@$EC2_HOST:$REMOTE_DIR/app/"

rsync -avz \
  -e "ssh -i $KEY_PATH" \
  server/requirements.txt "$EC2_USER@$EC2_HOST:$REMOTE_DIR/requirements.txt"

# Install dependencies and restart service
ssh -i "$KEY_PATH" "$EC2_USER@$EC2_HOST" \
  "source $REMOTE_DIR/venv/bin/activate && \
   pip install -r $REMOTE_DIR/requirements.txt && \
   sudo systemctl restart ytdlp-api"

echo "=== Deploy complete. ==="
