#!/bin/bash
set -euo pipefail

SG_ID="sg-0b378f79afe399f05"
REGION="eu-north-1"
MY_IP="$(curl -4 -s ifconfig.me)/32"

echo "Current IP: $MY_IP"

# Remove all existing SSH rules
OLD_IPS=$(aws ec2 describe-security-groups --group-ids "$SG_ID" --region "$REGION" \
  --query "SecurityGroups[0].IpPermissions[?FromPort==\`22\`].IpRanges[].CidrIp" --output text)

for ip in $OLD_IPS; do
  echo "Removing old SSH rule: $ip"
  aws ec2 revoke-security-group-ingress --group-id "$SG_ID" --region "$REGION" \
    --protocol tcp --port 22 --cidr "$ip"
done

# Add current IP
echo "Adding SSH rule: $MY_IP"
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --region "$REGION" \
  --protocol tcp --port 22 --cidr "$MY_IP"

echo "SSH allowed from $MY_IP"
