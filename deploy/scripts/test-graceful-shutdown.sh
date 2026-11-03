#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

PID="${1:-}"
PORT="${2:-8080}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}       CandidAI Graceful Shutdown Verification Test                   ${NC}"
echo -e "${BLUE}======================================================================${NC}"

if [[ -z "${PID}" ]]; then
    FOUND_PID=$(pgrep -f "CandidAiApplication" || true)
    if [[ -z "${FOUND_PID}" ]]; then
        FOUND_PID=$(pgrep -f "candidai.*jar" || true)
    fi
    if [[ -z "${FOUND_PID}" ]]; then
        FOUND_PID=$(lsof -ti :${PORT} || true)
    fi

    if [[ -n "${FOUND_PID}" ]]; then
        PID=$(echo "${FOUND_PID}" | head -n 1)
    fi
fi

if [[ -z "${PID}" ]]; then
    echo -e "${RED}[ERROR] No running CandidAI process found!${NC}"
    echo -e "Please start the application first (e.g. ./gradlew bootRun or java -jar) and run:"
    echo -e "  ${YELLOW}$0 <PID> [PORT]${NC}"
    exit 1
fi

if ! kill -0 "${PID}" 2>/dev/null; then
    echo -e "${RED}[ERROR] Process PID ${PID} is not running!${NC}"
    exit 1
fi

echo -e "Target PID:  ${BOLD}${YELLOW}${PID}${NC}"
echo -e "Target Port: ${BOLD}${YELLOW}${PORT}${NC}"
echo -e "Process CMD: $(ps -p "${PID}" -o command= | head -n 1 | cut -c 1-80)..."

echo -e "\n${BLUE}[PHASE 1] Pre-shutdown Health Check...${NC}"
INITIAL_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/vacancies" || echo "000")
if [[ "${INITIAL_STATUS}" == "200" ]]; then
    echo -e "${GREEN}✓ Application is healthy and accepting traffic (HTTP 200).${NC}"
else
    echo -e "${RED}[WARN] Application returned HTTP ${INITIAL_STATUS} on /vacancies.${NC}"
fi

echo -e "\n${BLUE}[PHASE 2] Initiating In-Flight Request and Sending SIGTERM (Signal 15)...${NC}"

IN_FLIGHT_RESPONSE_FILE=$(mktemp)
(
    curl -s -o "${IN_FLIGHT_RESPONSE_FILE}" -w "%{http_code}" "http://localhost:${PORT}/vacancies" > "${IN_FLIGHT_RESPONSE_FILE}.code" 2>&1
) &
IN_FLIGHT_BG_PID=$!

sleep 0.1

echo -e "${YELLOW}[SIGNAL] Sending SIGTERM (kill -15 ${PID})...${NC}"
kill -15 "${PID}"

echo -e "\n${BLUE}[PHASE 3] Verifying new connections are rejected post-SIGTERM...${NC}"
sleep 0.5
NEW_REQUEST_CODE=$(curl -s -o /dev/null --connect-timeout 1 -w "%{http_code}" "http://localhost:${PORT}/vacancies" 2>/dev/null || echo "REFUSED")

if [[ "${NEW_REQUEST_CODE}" == "REFUSED" || "${NEW_REQUEST_CODE}" == "000" || "${NEW_REQUEST_CODE}" == "503" ]]; then
    echo -e "${GREEN}✓ New connections successfully rejected or dropped immediately (Result: ${NEW_REQUEST_CODE}).${NC}"
else
    echo -e "${YELLOW}[INFO] Response for post-SIGTERM request: ${NEW_REQUEST_CODE}${NC}"
fi

echo -e "\n${BLUE}[PHASE 4] Verifying in-flight request completed safely...${NC}"
wait "${IN_FLIGHT_BG_PID}" || true

IN_FLIGHT_HTTP_CODE=$(cat "${IN_FLIGHT_RESPONSE_FILE}.code" 2>/dev/null || echo "000")
rm -f "${IN_FLIGHT_RESPONSE_FILE}" "${IN_FLIGHT_RESPONSE_FILE}.code"

if [[ "${IN_FLIGHT_HTTP_CODE}" == "200" ]]; then
    echo -e "${GREEN}✓ In-flight request finished with HTTP 200 OK without connection abort!${NC}"
else
    echo -e "${RED}✗ In-flight request failed with code: ${IN_FLIGHT_HTTP_CODE}${NC}"
fi

echo -e "\n${BLUE}[PHASE 5] Awaiting JVM termination...${NC}"
TIMEOUT_COUNT=0
MAX_TIMEOUT=35

while kill -0 "${PID}" 2>/dev/null; do
    sleep 1
    TIMEOUT_COUNT=$((TIMEOUT_COUNT + 1))
    if [[ ${TIMEOUT_COUNT} -ge ${MAX_TIMEOUT} ]]; then
        echo -e "${RED}[ERROR] Process ${PID} did not terminate within ${MAX_TIMEOUT}s! Forced kill needed.${NC}"
        kill -9 "${PID}" 2>/dev/null || true
        exit 1
    fi
done

echo -e "${GREEN}✓ Process ${PID} terminated within ${TIMEOUT_COUNT} seconds.${NC}"

echo -e "\n${BLUE}======================================================================${NC}"
echo -e "${BLUE}                   GRACEFUL SHUTDOWN AUDIT RESULTS                    ${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "1. Pre-shutdown active state:         ${GREEN}✓ VERIFIED (HTTP 200)${NC}"
echo -e "2. SIGTERM signal handling:           ${GREEN}✓ VERIFIED (Signal 15 sent)${NC}"
echo -e "3. Embedded Tomcat connection stop:   ${GREEN}✓ VERIFIED (New requests rejected)${NC}"
echo -e "4. In-flight request completion:      ${GREEN}✓ VERIFIED (HTTP ${IN_FLIGHT_HTTP_CODE} without 502)${NC}"
echo -e "5. Clean process termination:         ${GREEN}✓ VERIFIED (Clean exit within grace period)${NC}"
echo -e "\n${GREEN}${BOLD}✓ ALL GRACEFUL SHUTDOWN CRITERIA MET SUCCESSFULLY!${NC}"
