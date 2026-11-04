#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ACTIVE_URL_FILE="${ROOT_DIR}/deploy/tunnel/active-tunnel.url"

TARGET_URL="${1:-}"

if [[ -z "${TARGET_URL}" ]]; then
    if [[ -f "${ACTIVE_URL_FILE}" ]]; then
        TARGET_URL=$(tr -d '[:space:]' < "${ACTIVE_URL_FILE}")
    fi
fi

if [[ -z "${TARGET_URL}" ]]; then
    TARGET_URL="http://localhost:8080"
fi

TARGET_URL="${TARGET_URL%/}"

CURL_TLS_FLAG=""
if [[ "${TARGET_URL}" =~ ^https://(localhost|127\.0\.0\.1|\[::1\])(:[0-9]+)?$ ]]; then
    CURL_TLS_FLAG="-k"
fi

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}   CandidAI External Network Access Verification                      ${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "Target URL: ${BOLD}${YELLOW}${TARGET_URL}${NC}"
echo -e "Started at: $(date -u +"%Y-%m-%dT%H:%M:%SZ")\n"

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local expected_code="$4"
    local headers="$5"
    local data="$6"
    local check_body_substring="${7:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "Test ${TOTAL_TESTS}: ${test_name}... "

    local start_time=$(python3 -c "import time; print(int(time.time() * 1000))" 2>/dev/null || date +%s000)
    local response_file=$(mktemp)
    local http_code=""

    local curl_cmd=(
        curl -s -S -L --post301
        -H "ngrok-skip-browser-warning: true"
        -o "${response_file}"
        -w "%{http_code}"
        -X "${method}"
        "${TARGET_URL}${endpoint}"
    )
    if [[ -n "${CURL_TLS_FLAG}" ]]; then
        curl_cmd+=("${CURL_TLS_FLAG}")
    fi
    if [[ -n "${headers}" ]]; then
        while IFS= read -r header_line; do
            if [[ -n "${header_line}" ]]; then
                curl_cmd+=(-H "${header_line}")
            fi
        done <<< "${headers}"
    fi

    if [[ -n "${data}" ]]; then
        curl_cmd+=(-d "${data}")
    fi

    http_code=$("${curl_cmd[@]}" 2>/dev/null || true)
    if [[ -z "${http_code}" ]]; then
        http_code="000"
    fi
    local end_time=$(python3 -c "import time; print(int(time.time() * 1000))" 2>/dev/null || date +%s000)
    local latency=$((end_time - start_time))

    local body_match=true
    if [[ -n "${check_body_substring}" ]]; then
        if ! grep -q "${check_body_substring}" "${response_file}" 2>/dev/null; then
            body_match=false
        fi
    fi

    if [[ "${http_code}" == "${expected_code}" && "${body_match}" == "true" ]]; then
        echo -e "${GREEN}✓ PASS${NC} [HTTP ${http_code}, ${latency}ms]"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAIL${NC} [HTTP ${http_code} (expected ${expected_code}), ${latency}ms]"
        if [[ "${body_match}" == "false" ]]; then
            echo -e "  ${YELLOW}Body missing substring: '${check_body_substring}'${NC}"
        fi
        echo -e "  Response preview: $(head -n 3 "${response_file}" | tr -d '\r\n')"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    rm -f "${response_file}"
}

run_test "Public Web UI (Thymeleaf Vacancies Catalog)" \
         "GET" \
         "/vacancies" \
         "200" \
         "Accept: text/html" \
         "" \
         "CandidAI"

run_test "Session Web UI (Login Form & CSRF Token)" \
         "GET" \
         "/login" \
         "200" \
         "Accept: text/html" \
         "" \
         "_csrf"

RECRUITER_LOGIN_PAYLOAD='{"email":"recruiter@candidai.com","password":"RecruiterPass123!"}'
TEMP_AUTH_RESPONSE=$(mktemp)
AUTH_CURL=(curl -s -L --post301
    -H "ngrok-skip-browser-warning: true"
    -o "${TEMP_AUTH_RESPONSE}" -w "%{http_code}"
    -X POST "${TARGET_URL}/api/v1/auth/login"
    -H "Content-Type: application/json"
    -d "${RECRUITER_LOGIN_PAYLOAD}")
if [[ -n "${CURL_TLS_FLAG}" ]]; then
    AUTH_CURL+=("${CURL_TLS_FLAG}")
fi
RECRUITER_HTTP_CODE=$("${AUTH_CURL[@]}" 2>/dev/null || true)

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -n "Test ${TOTAL_TESTS}: REST API Authentication (JWT Token Generation)... "

RECRUITER_TOKEN=""
if [[ "${RECRUITER_HTTP_CODE}" == "200" ]]; then
    RECRUITER_TOKEN=$(jq -r '.accessToken // .token // empty' "${TEMP_AUTH_RESPONSE}" 2>/dev/null || true)
    if [[ -n "${RECRUITER_TOKEN}" ]]; then
        echo -e "${GREEN}✓ PASS${NC} [HTTP 200, JWT token acquired]"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAIL${NC} [HTTP 200, but JWT token missing in response body]"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
else
    echo -e "${RED}✗ FAIL${NC} [HTTP ${RECRUITER_HTTP_CODE:-000} (expected 200)]"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
rm -f "${TEMP_AUTH_RESPONSE}"

if [[ -n "${RECRUITER_TOKEN}" ]]; then
    run_test "Authorized REST API Call with JWT Bearer" \
             "GET" \
             "/api/v1/vacancies" \
             "200" \
             "Authorization: Bearer ${RECRUITER_TOKEN}
Accept: application/json" \
             "" \
             ""
fi

run_test "Security Boundary (Unauthorized Request rejection)" \
         "GET" \
         "/api/v1/applications" \
         "401" \
         "Accept: application/problem+json" \
         "" \
         ""

CANDIDATE_LOGIN_PAYLOAD='{"email":"candidate@candidai.com","password":"CandidatePass123!"}'
TEMP_CANDIDATE_RESPONSE=$(mktemp)
CANDIDATE_CURL=(curl -s -L --post301
    -H "ngrok-skip-browser-warning: true"
    -o "${TEMP_CANDIDATE_RESPONSE}" -w "%{http_code}"
    -X POST "${TARGET_URL}/api/v1/auth/login"
    -H "Content-Type: application/json"
    -d "${CANDIDATE_LOGIN_PAYLOAD}")
if [[ -n "${CURL_TLS_FLAG}" ]]; then
    CANDIDATE_CURL+=("${CURL_TLS_FLAG}")
fi
CANDIDATE_HTTP_CODE=$("${CANDIDATE_CURL[@]}" 2>/dev/null || true)

CANDIDATE_TOKEN=""
if [[ "${CANDIDATE_HTTP_CODE}" == "200" ]]; then
    CANDIDATE_TOKEN=$(jq -r '.accessToken // .token // empty' "${TEMP_CANDIDATE_RESPONSE}" 2>/dev/null || true)
fi
rm -f "${TEMP_CANDIDATE_RESPONSE}"

if [[ -n "${CANDIDATE_TOKEN}" ]]; then
    VACANCY_CREATE_PAYLOAD='{
        "authorId":"00000000-0000-0000-0000-000000000001",
        "title":"Unauthorized Vacancy Creation Attempt",
        "category":"ENGINEERING",
        "specialization":"Java / Spring Boot",
        "seniorityLevel":"Senior",
        "minYearsOfExperience":5,
        "description":"Attempt to create a vacancy without RECRUITER or ADMIN privileges.",
        "requiredSkills":["Java 25","Spring Boot"],
        "minEnglishLevel":"B2",
        "salaryMin":3000,
        "salaryMax":5000,
        "currency":"USD",
        "employmentType":"FULL_TIME",
        "locationType":"REMOTE"
    }'
    run_test "Role-Based Access Control (Candidate denied creating vacancy)" \
             "POST" \
             "/api/v1/vacancies" \
             "403" \
             "Authorization: Bearer ${CANDIDATE_TOKEN}
Content-Type: application/json
Accept: application/problem+json" \
             "${VACANCY_CREATE_PAYLOAD}" \
             ""
fi

echo -e "\n${BLUE}======================================================================${NC}"
echo -e "${BLUE}                           TEST RESULTS SUMMARY                       ${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "Total Executed: ${BOLD}${TOTAL_TESTS}${NC}"
echo -e "Passed:         ${GREEN}${BOLD}${PASSED_TESTS}${NC}"
echo -e "Failed:         ${RED}${BOLD}${FAILED_TESTS}${NC}"

if [[ "${FAILED_TESTS}" -eq 0 ]]; then
    echo -e "\n${GREEN}${BOLD}✓ ALL EXTERNAL NETWORK ACCESS TESTS PASSED SUCCESSFULLY!${NC}"
    exit 0
else
    echo -e "\n${RED}${BOLD}✗ SOME EXTERNAL ACCESS TESTS FAILED! Check application logs.${NC}"
    exit 1
fi
