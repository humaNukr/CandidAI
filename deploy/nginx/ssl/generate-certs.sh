#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "${SCRIPT_DIR}/candidai.key" \
  -out "${SCRIPT_DIR}/candidai.crt" \
  -subj "/CN=localhost/O=CandidAI/C=UA" \
  -addext "subjectAltName=DNS:localhost,DNS:candidai.local,IP:127.0.0.1"
