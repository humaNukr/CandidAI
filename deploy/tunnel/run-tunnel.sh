#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ACTIVE_URL_FILE="${SCRIPT_DIR}/active-tunnel.url"
LOG_FILE="${SCRIPT_DIR}/tunnel.log"

PORT="${1:-443}"
TOOL="${2:-auto}"

if [[ "${PORT}" =~ ^https?:// ]]; then
    TARGET_ORIGIN="${PORT}"
elif [[ "${PORT}" == "443" ]]; then
    TARGET_ORIGIN="https://localhost:443"
elif [[ "${PORT}" =~ ^[0-9]+$ ]]; then
    TARGET_ORIGIN="http://localhost:${PORT}"
else
    TARGET_ORIGIN="http://${PORT}"
fi

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   CandidAI Public Access Tunnel Launcher             ${NC}"
echo -e "${BLUE}======================================================${NC}"

if [[ "${TOOL}" == "auto" ]]; then
    if command -v ngrok &>/dev/null; then
        TOOL="ngrok"
    elif command -v cloudflared &>/dev/null; then
        TOOL="cloudflared"
    else
        echo -e "${RED}[ERROR] Neither ngrok nor cloudflared is installed!${NC}"
        echo -e "Please install one of them:"
        echo -e "  - ngrok:       brew install ngrok"
        echo -e "  - cloudflared: brew install cloudflared"
        exit 1
    fi
fi

TUNNEL_PID=""

cleanup() {
    echo -e "\n${YELLOW}[SHUTDOWN] Stopping tunnel process...${NC}"
    if [[ -n "${TUNNEL_PID}" ]] && kill -0 "${TUNNEL_PID}" 2>/dev/null; then
        kill "${TUNNEL_PID}" 2>/dev/null || true
        wait "${TUNNEL_PID}" 2>/dev/null || true
    fi
    rm -f "${ACTIVE_URL_FILE}"
    echo -e "${GREEN}[SHUTDOWN] Tunnel closed cleanly.${NC}"
}
trap cleanup EXIT INT TERM

echo -e "${BLUE}[INFO]${NC} Selected tunnel tool: ${GREEN}${TOOL}${NC}"
echo -e "${BLUE}[INFO]${NC} Target local origin:  ${GREEN}${TARGET_ORIGIN}${NC}"

PUBLIC_URL=""

if [[ "${TOOL}" == "ngrok" ]]; then
    echo -e "${BLUE}[INFO]${NC} Starting Ngrok tunnel for ${TARGET_ORIGIN}..."
    ngrok http "${TARGET_ORIGIN}" --log=stdout > "${LOG_FILE}" 2>&1 &
    TUNNEL_PID=$!

    echo -e "${BLUE}[INFO]${NC} Waiting for Ngrok API at http://127.0.0.1:4040..."
    for i in {1..30}; do
        if curl -s http://127.0.0.1:4040/api/tunnels > /dev/null 2>&1; then
            PUBLIC_URL=$(curl -s http://127.0.0.1:4040/api/tunnels | jq -r '.tunnels[] | select(.proto=="https") | .public_url' | head -n 1)
            if [[ -n "${PUBLIC_URL}" && "${PUBLIC_URL}" != "null" ]]; then
                break
            fi
        fi
        sleep 1
    done

elif [[ "${TOOL}" == "cloudflared" ]]; then
    echo -e "${BLUE}[INFO]${NC} Starting Cloudflare Tunnel for ${TARGET_ORIGIN}..."
    cloudflared tunnel --url "${TARGET_ORIGIN}" --no-tls-verify > "${LOG_FILE}" 2>&1 &
    TUNNEL_PID=$!

    echo -e "${BLUE}[INFO]${NC} Waiting for Cloudflare tunnel URL in logs..."
    for i in {1..30}; do
        if [[ -f "${LOG_FILE}" ]]; then
            PUBLIC_URL=$(grep -oE 'https://[a-zA-Z0-9-]+\.trycloudflare\.com' "${LOG_FILE}" | head -n 1 || true)
            if [[ -n "${PUBLIC_URL}" ]]; then
                break
            fi
        fi
        sleep 1
    done
fi

if [[ -z "${PUBLIC_URL}" || "${PUBLIC_URL}" == "null" ]]; then
    echo -e "${RED}[ERROR] Failed to obtain public tunnel URL within 30 seconds!${NC}"
    echo -e "${YELLOW}Check ${LOG_FILE} for details:${NC}"
    tail -n 20 "${LOG_FILE}" || true
    exit 1
fi

echo "${PUBLIC_URL}" > "${ACTIVE_URL_FILE}"

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}  ✓ Tunnel is LIVE and accessible from the Internet!  ${NC}"
echo -e "${GREEN}  Public URL: ${YELLOW}${PUBLIC_URL}${NC}"
echo -e "${GREEN}======================================================${NC}"
echo -e "Saved URL to: ${ACTIVE_URL_FILE}"
echo -e "You can now test external access from another terminal:"
echo -e "  ${BLUE}./deploy/scripts/test-external-access.sh ${PUBLIC_URL}${NC}"
echo -e "\nPress Ctrl+C to stop the tunnel."

wait "${TUNNEL_PID}"
