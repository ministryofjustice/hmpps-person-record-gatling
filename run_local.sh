#!/bin/bash

set -euo pipefail

echo "Fetch DB details"
DB_JSON=$(cloud-platform decode-secret --secret=hmpps-person-record-rds-instance-output --namespace=hmpps-person-record-dev)

export DB_NAME
DB_NAME=$(echo "$DB_JSON" | jq -r '.data.database_name')
export DB_URL="jdbc:postgresql://localhost:5432/$DB_NAME"
export DB_USER
DB_USER=$(echo "$DB_JSON" | jq -r '.data.database_username')
export DB_PASS
DB_PASS=$(echo "$DB_JSON" | jq -r '.data.database_password')

sleep 5
echo "DB variables loaded"

echo "Generate test data"
export OUT_FILE="src/main/resources/testdata/data.csv"
./gradlew --no-daemon generateTestData --args="'$DB_URL' '$DB_USER' '$DB_PASS' '$OUT_FILE'"

echo "Fetch Gatling client credentials"
GATLING_ID_JSON=$(cloud-platform decode-secret --secret=hmpps-person-record-gatling-client-id --namespace=hmpps-person-record-dev)
GATLING_SECRET_JSON=$(cloud-platform decode-secret --secret=hmpps-person-record-gatling-client-secret --namespace=hmpps-person-record-dev)

export CLIENT_ID
CLIENT_ID=$(echo "$GATLING_ID_JSON" | jq -r '.data.id')
export CLIENT_SECRET
CLIENT_SECRET=$(echo "$GATLING_SECRET_JSON" | jq -r '.data.secret')
echo "Gatling client credentials loaded"

echo "Running Gatling..."
./gradlew --no-daemon gatlingRun -Denv=dev -DgetPrisonNumber=1 -DgetCrnNumber=1 -DgetDefendantId=1 -Dduration=60

echo "Open Gatling Report"
REPORTS_DIR="build/reports/gatling"

if [[ ! -d "$REPORTS_DIR" ]]; then
  echo "Gatling completed but no report directory was generated at $REPORTS_DIR" >&2
  exit 1
fi

LATEST_RUN_DIR=$(find "$REPORTS_DIR" -mindepth 1 -maxdepth 1 -type d | sort | tail -1)

if [[ -z "$LATEST_RUN_DIR" ]]; then
  echo "Gatling completed but no run report directory was found in $REPORTS_DIR" >&2
  exit 1
fi

REPORT_INDEX="$LATEST_RUN_DIR/index.html"

if [[ ! -f "$REPORT_INDEX" ]]; then
  echo "Gatling completed but the report file was not found at $REPORT_INDEX" >&2
  exit 1
fi

open "$REPORT_INDEX"
