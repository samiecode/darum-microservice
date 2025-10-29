#!/bin/bash

##############################################
# Health Check Script for Kubernetes Deployments
##############################################

set -e

ENVIRONMENT=$1
VERSION=$2  # blue or green

if [ -z "$ENVIRONMENT" ] || [ -z "$VERSION" ]; then
    echo "Usage: $0 <staging|production> <blue|green>"
    exit 1
fi

NAMESPACE=$ENVIRONMENT
LABEL="version=$VERSION"

echo "🏥 Running health checks for $ENVIRONMENT environment ($VERSION deployment)..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

HEALTHY=0
UNHEALTHY=0

# Function to check pod health
check_pod_health() {
    local deployment=$1
    
    echo -n "Checking $deployment... "
    
    # Get pods for deployment
    PODS=$(kubectl get pods -n $NAMESPACE -l app=$deployment,$LABEL -o json)
    
    # Count ready pods
    READY_COUNT=$(echo $PODS | jq '[.items[] | select(.status.conditions[] | select(.type=="Ready" and .status=="True"))] | length')
    TOTAL_COUNT=$(echo $PODS | jq '.items | length')
    
    if [ "$READY_COUNT" -eq "$TOTAL_COUNT" ] && [ "$TOTAL_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✓ HEALTHY${NC} ($READY_COUNT/$TOTAL_COUNT pods ready)"
        ((HEALTHY++))
    else
        echo -e "${RED}✗ UNHEALTHY${NC} ($READY_COUNT/$TOTAL_COUNT pods ready)"
        ((UNHEALTHY++))
        
        # Show pod details for unhealthy pods
        echo "Pod details:"
        kubectl get pods -n $NAMESPACE -l app=$deployment,$LABEL
    fi
}

# Function to check service endpoint
check_service_endpoint() {
    local service=$1
    local port=$2
    
    echo -n "Checking $service endpoint... "
    
    # Port forward to service
    kubectl port-forward -n $NAMESPACE svc/$service-$VERSION $port:$port &
    PF_PID=$!
    
    sleep 2
    
    # Check health endpoint
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health || echo "000")
    
    # Kill port forward
    kill $PF_PID 2>/dev/null || true
    
    if [ "$response" -eq "200" ]; then
        echo -e "${GREEN}✓ HEALTHY${NC} (Status: $response)"
        ((HEALTHY++))
    else
        echo -e "${RED}✗ UNHEALTHY${NC} (Status: $response)"
        ((UNHEALTHY++))
    fi
}

echo ""
echo "===== Pod Health Checks ====="
check_pod_health "discovery-service"
check_pod_health "config-server"
check_pod_health "api-gateway"
check_pod_health "auth-service"
check_pod_health "employee-service"

echo ""
echo "===== Service Endpoint Checks ====="
check_service_endpoint "discovery-service" "8761"
check_service_endpoint "config-server" "8888"
check_service_endpoint "api-gateway" "8080"
check_service_endpoint "auth-service" "8080"
check_service_endpoint "employee-service" "8080"

echo ""
echo "===== Database Connectivity ====="
# Check if database pods are healthy
echo -n "Checking PostgreSQL databases... "
DB_HEALTHY=$(kubectl get pods -n $NAMESPACE -l app=postgres -o json | \
    jq '[.items[] | select(.status.conditions[] | select(.type=="Ready" and .status=="True"))] | length')
DB_TOTAL=$(kubectl get pods -n $NAMESPACE -l app=postgres -o json | jq '.items | length')

if [ "$DB_HEALTHY" -eq "$DB_TOTAL" ] && [ "$DB_TOTAL" -gt 0 ]; then
    echo -e "${GREEN}✓ HEALTHY${NC} ($DB_HEALTHY/$DB_TOTAL databases ready)"
    ((HEALTHY++))
else
    echo -e "${RED}✗ UNHEALTHY${NC} ($DB_HEALTHY/$DB_TOTAL databases ready)"
    ((UNHEALTHY++))
fi

echo ""
echo "===== Resource Usage ====="
echo "CPU and Memory usage by pods:"
kubectl top pods -n $NAMESPACE -l version=$VERSION 2>/dev/null || echo -e "${YELLOW}⚠ Metrics not available${NC}"

echo ""
echo "===== Summary ====="
echo -e "Total Checks: $((HEALTHY + UNHEALTHY))"
echo -e "${GREEN}Healthy: $HEALTHY${NC}"
echo -e "${RED}Unhealthy: $UNHEALTHY${NC}"

if [ $UNHEALTHY -gt 0 ]; then
    echo ""
    echo -e "${RED}❌ Health checks FAILED${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All health checks PASSED${NC}"
    exit 0
fi
