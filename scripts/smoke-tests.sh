#!/bin/bash

##############################################
# Smoke Tests for Darum Microservices
# Tests basic functionality after deployment
##############################################

set -e

ENVIRONMENT=$1
BASE_URL=""

if [ "$ENVIRONMENT" == "staging" ]; then
    BASE_URL="https://staging.darum.com"
elif [ "$ENVIRONMENT" == "production" ]; then
    BASE_URL="https://darum.com"
else
    echo "Usage: $0 <staging|production>"
    exit 1
fi

echo "🚀 Running smoke tests for $ENVIRONMENT environment..."
echo "Base URL: $BASE_URL"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

# Function to test endpoint
test_endpoint() {
    local endpoint=$1
    local expected_status=$2
    local description=$3
    
    echo -n "Testing: $description... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint" || echo "000")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (Status: $response)"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC} (Expected: $expected_status, Got: $response)"
        ((FAILED++))
    fi
}

# Function to test authenticated endpoint
test_auth_endpoint() {
    local endpoint=$1
    local token=$2
    local expected_status=$3
    local description=$4
    
    echo -n "Testing: $description... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $token" \
        "$BASE_URL$endpoint" || echo "000")
    
    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (Status: $response)"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC} (Expected: $expected_status, Got: $response)"
        ((FAILED++))
    fi
}

echo ""
echo "===== Health Check Tests ====="
test_endpoint "/actuator/health" 200 "API Gateway Health"
test_endpoint "/auth/actuator/health" 200 "Auth Service Health"
test_endpoint "/employee/actuator/health" 200 "Employee Service Health"

echo ""
echo "===== Discovery Service Tests ====="
test_endpoint "/eureka/apps" 200 "Eureka Registry"

echo ""
echo "===== Authentication Tests ====="
# Try to register a test user
TIMESTAMP=$(date +%s)
TEST_EMAIL="smoketest-$TIMESTAMP@test.com"

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"firstName\": \"Smoke\",
        \"lastName\": \"Test\",
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"Test@123\",
        \"role\": \"EMPLOYEE\"
    }")

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ PASSED${NC} User registration successful"
    ((PASSED++))
    
    # Extract token for further tests
    TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    echo ""
    echo "===== Authenticated Endpoint Tests ====="
    test_auth_endpoint "/api/v1/employees/me" "$TOKEN" 200 "Get My Profile"
    
else
    echo -e "${RED}✗ FAILED${NC} User registration failed"
    ((FAILED++))
fi

# Test login
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"Test@123\"
    }")

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ PASSED${NC} User login successful"
    ((PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} User login failed"
    ((FAILED++))
fi

echo ""
echo "===== OpenAPI Documentation Tests ====="
test_endpoint "/api/v1/auth/docs" 200 "Auth Service API Docs"
test_endpoint "/api/v1/employee/docs" 200 "Employee Service API Docs"

echo ""
echo "===== Summary ====="
echo -e "Total Tests: $((PASSED + FAILED))"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -gt 0 ]; then
    echo ""
    echo -e "${RED}❌ Smoke tests FAILED${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All smoke tests PASSED${NC}"
    exit 0
fi
