#!/bin/bash

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

export GATLING_CLIENT_ID
GATLING_CLIENT_ID=$(echo "$GATLING_ID_JSON" | jq -r '.data.id')
export GATLING_CLIENT_SECRET
GATLING_CLIENT_SECRET=$(echo "$GATLING_SECRET_JSON" | jq -r '.data.secret')
echo "Gatling client credentials loaded"

echo "Running Gatling..."
CLIENT_ID=$GATLING_CLIENT_ID CLIENT_SECRET=$GATLING_CLIENT_SECRET ./gradlew --no-daemon gatlingRun -Denv=dev -DgetPrisonNumber=1 -DgetCrnNumber=1 -DgetDefendantId=1 -Dduration=60

echo "Open Gatling Report"
open "build/reports/gatling/$(ls -1t build/reports/gatling | head -1)/index.html"


