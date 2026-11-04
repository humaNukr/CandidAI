#!/bin/sh
set -eu

SSL_DIR="/etc/nginx/ssl"
CERT_FILE="${SSL_DIR}/candidai.crt"
KEY_FILE="${SSL_DIR}/candidai.key"

if [ -f "${CERT_FILE}" ] && [ -f "${KEY_FILE}" ]; then
    exit 0
fi

mkdir -p "${SSL_DIR}"

if ! command -v openssl >/dev/null 2>&1; then
    apk add --no-cache openssl >/dev/null
fi

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "${KEY_FILE}" \
    -out "${CERT_FILE}" \
    -subj "/CN=localhost/O=CandidAI/C=UA" \
    -addext "subjectAltName=DNS:localhost,DNS:candidai.local,IP:127.0.0.1" >/dev/null 2>&1

chmod 600 "${KEY_FILE}"
chmod 644 "${CERT_FILE}"
