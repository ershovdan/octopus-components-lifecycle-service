#!/bin/bash
# Script to check pod logs during Helm deployment
# Add this as a parallel step in TeamCity to capture logs while deployment is happening

set -e

NAMESPACE="${OKD_PROJECT_NAME:-f1}"
RELEASE="${HELM_RELEASE:-components-lifecycle-service-test}"
TIMEOUT="${1:-300}"  # Default 5 minutes

echo "=== Waiting for pod to be created in namespace: $NAMESPACE ==="
echo "Release: $RELEASE"
echo ""

# Wait for pod to exist (max 60 seconds)
for i in {1..30}; do
  POD_NAME=$(kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/instance=$RELEASE" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

  if [ -n "$POD_NAME" ]; then
    echo "✓ Pod found: $POD_NAME"
    break
  fi

  echo "Waiting for pod to be created... ($i/30)"
  sleep 2
done

if [ -z "$POD_NAME" ]; then
  echo "❌ ERROR: Pod not found after 60 seconds"
  echo ""
  echo "=== Checking recent events ==="
  kubectl get events -n "$NAMESPACE" --sort-by='.lastTimestamp' | tail -20
  exit 1
fi

echo ""
echo "=== Pod Status ==="
kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o wide
echo ""

echo "=== Pod Description ==="
kubectl describe pod -n "$NAMESPACE" "$POD_NAME"
echo ""

echo "=== Tailing Pod Logs ==="
echo "Press Ctrl+C to stop following logs"
echo ""

# Follow logs with timeout
timeout "$TIMEOUT" kubectl logs -n "$NAMESPACE" "$POD_NAME" --follow 2>&1 || {
  EXIT_CODE=$?

  echo ""
  echo "=== Log streaming ended (exit code: $EXIT_CODE) ==="

  if [ $EXIT_CODE -eq 124 ]; then
    echo "⏰ Timeout reached after ${TIMEOUT}s"
  fi

  echo ""
  echo "=== Final Pod Status ==="
  kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o wide

  echo ""
  echo "=== Recent Events ==="
  kubectl get events -n "$NAMESPACE" --field-selector involvedObject.name="$POD_NAME" --sort-by='.lastTimestamp'

  echo ""
  echo "=== Pod Container Status ==="
  kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o jsonpath='{.status.containerStatuses[0]}' | jq '.'

  exit 0
}
