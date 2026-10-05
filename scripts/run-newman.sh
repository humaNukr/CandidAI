#!/bin/bash
set -e

COLLECTION="postman/candidai_collection.json"
ENVIRONMENT="postman/candidai_env.json"
REPORT_DIR="build/reports/newman"
REPORT_XML="$REPORT_DIR/report.xml"

mkdir -p "$REPORT_DIR"

echo "================================================="
echo "  Running CandidAI E2E API Tests via Newman CLI  "
echo "================================================="

if ! command -v npx >/dev/null 2>&1; then
    echo "Error: npx is not installed or not in PATH."
    exit 1
fi

npx newman run "$COLLECTION" \
    -e "$ENVIRONMENT" \
    --reporters cli,junit \
    --reporter-junit-export "$REPORT_XML"

echo ""
echo "Tests completed successfully. Report generated at: $REPORT_XML"
