#!/bin/bash
set -euo pipefail

EC2_HOST="13.50.106.77"
EC2_USER="ubuntu"
KEY_PATH="~/Downloads/aws-per-ytdl.pem"
REMOTE_DIR="/opt/ytdlp-api"

echo "=== Deploying to EC2 $EC2_HOST ==="

# Ensure remote directory exists
ssh -i "$KEY_PATH" "$EC2_USER@$EC2_HOST" \
  "sudo mkdir -p $REMOTE_DIR && sudo chown $EC2_USER:$EC2_USER $REMOTE_DIR"

# Sync app code and requirements to remote
rsync -avz --delete \
  -e "ssh -i $KEY_PATH" \
  server/app/ "$EC2_USER@$EC2_HOST:$REMOTE_DIR/app/"

rsync -avz \
  -e "ssh -i $KEY_PATH" \
  server/requirements.txt "$EC2_USER@$EC2_HOST:$REMOTE_DIR/requirements.txt"

rsync -avz \
  -e "ssh -i $KEY_PATH" \
  server/deploy/ "$EC2_USER@$EC2_HOST:$REMOTE_DIR/deploy/"

# Install dependencies and restart service (skip on first deploy when venv doesn't exist)
ssh -i "$KEY_PATH" "$EC2_USER@$EC2_HOST" \
  "if [ -d $REMOTE_DIR/venv ]; then \
     source $REMOTE_DIR/venv/bin/activate && \
     pip install -r $REMOTE_DIR/requirements.txt && \
     (sudo systemctl restart ytdlp-api 2>/dev/null || echo 'Service not installed yet — run setup.sh first'); \
   else \
     echo 'No venv found — run setup.sh on the server first'; \
   fi"

echo "=== Deploy complete. ==="
