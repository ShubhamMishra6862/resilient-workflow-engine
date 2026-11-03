#!/bin/bash
# ==============================================================================
# FlowLab Crash & Recovery Demonstration Script
# 1. Launches an order workflow with simulated failure on UpdateInventory
# 2. Waits 1.5 seconds for ValidateOrder & ProcessPayment to succeed
# 3. Simulates a worker crash (kill -9) while UpdateInventory is running
# 4. Observes the RecoveryScheduler rescuing the execution via DB leases
# ==============================================================================

set -e

BASE_URL="http://localhost:8080/api/v1"

echo "=========================================================="
echo "FlowLab: Crash-Proof Workflow Demonstration"
echo "=========================================================="

echo "Step 1: Starting new order-processing workflow..."
EXECUTION_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows/order-processing/executions" \
  -H "Content-Type: application/json" \
  -d '{
    "inputData": {
      "orderId": "ORD-LIVE-DEMO",
      "amount": 299.99,
      "simulateFailure": true,
      "failAttempts": 1
    }
  }')

EXECUTION_ID=$(echo "$EXECUTION_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$EXECUTION_ID" ]; then
  echo "Error: Could not retrieve execution ID. Is the backend running on $BASE_URL?"
  exit 1
fi

echo "Execution started! ID: $EXECUTION_ID"
echo "Waiting 1.8s for ValidateOrder & ProcessPayment to complete..."
sleep 1.8

echo "Step 2: Simulating sudden worker crash (kill -9) during UpdateInventory..."
CRASH_RES=$(curl -s -X POST "$BASE_URL/executions/$EXECUTION_ID/simulate-crash")
echo "Crash response: $CRASH_RES"

echo ""
echo "Step 3: Watching RecoveryScheduler rescue the execution..."
for i in {1..5}; do
  sleep 1.5
  STATUS_RES=$(curl -s "$BASE_URL/executions/$EXECUTION_ID")
  CURRENT_STATUS=$(echo "$STATUS_RES" | grep -o '"status":"[^"]*' | head -1 | cut -d'"' -f4)
  echo "Polling (t+${i}x1.5s): Overall Status = $CURRENT_STATUS"
  if [ "$CURRENT_STATUS" = "COMPLETED" ]; then
    echo "SUCCESS! Workflow automatically resumed from exact point of failure and finished COMPLETED!"
    break
  fi
done

echo ""
echo "Step 4: Fetching Audit Event Log:"
curl -s "$BASE_URL/executions/$EXECUTION_ID/events" | grep -o '"eventType":"[^"]*' || true
echo ""
echo "=========================================================="
