#!/bin/bash

##############################################
# Quick Start Script for Darum Microservices
##############################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}"
echo "╔═══════════════════════════════════════════╗"
echo "║   Darum Microservices - Quick Start      ║"
echo "╔═══════════════════════════════════════════╗"
echo -e "${NC}"

# Check prerequisites
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

command -v java >/dev/null 2>&1 || { echo -e "${RED}✗ Java is not installed${NC}"; exit 1; }
echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

command -v mvn >/dev/null 2>&1 || { echo -e "${RED}✗ Maven is not installed${NC}"; exit 1; }
echo -e "${GREEN}✓ Maven found: $(mvn -version | head -n 1)${NC}"

command -v docker >/dev/null 2>&1 || { echo -e "${RED}✗ Docker is not installed${NC}"; exit 1; }
echo -e "${GREEN}✓ Docker found: $(docker --version)${NC}"

command -v docker-compose >/dev/null 2>&1 || { echo -e "${RED}✗ Docker Compose is not installed${NC}"; exit 1; }
echo -e "${GREEN}✓ Docker Compose found: $(docker-compose --version)${NC}"

# Build services
echo -e "\n${YELLOW}Building all services...${NC}"
mvn clean install -DskipTests

echo -e "${GREEN}✓ Build completed successfully${NC}"

# Start services with Docker Compose
echo -e "\n${YELLOW}Starting services with Docker Compose...${NC}"
docker-compose up -d

echo -e "\n${YELLOW}Waiting for services to be healthy...${NC}"
sleep 30

# Check service status
echo -e "\n${YELLOW}Checking service status...${NC}"
docker-compose ps

# Display service URLs
echo -e "\n${GREEN}╔═══════════════════════════════════════════╗"
echo "║          Services are now running!         ║"
echo "╚═══════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Service URLs:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "🔍 Eureka Dashboard:    ${GREEN}http://localhost:8761${NC}"
echo -e "⚙️  Config Server:       ${GREEN}http://localhost:8888${NC}"
echo -e "🚪 API Gateway:          ${GREEN}http://localhost:8080${NC}"
echo -e "🔐 Auth Service:         ${GREEN}http://localhost:8081${NC}"
echo -e "👥 Employee Service:     ${GREEN}http://localhost:8082${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${YELLOW}API Documentation:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "📚 Auth API:             ${GREEN}http://localhost:8081/docs/swagger-ui.html${NC}"
echo -e "📚 Employee API:         ${GREEN}http://localhost:8082/docs/swagger-ui.html${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${YELLOW}Useful commands:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  View logs:           docker-compose logs -f"
echo "  Stop services:       docker-compose down"
echo "  Restart services:    docker-compose restart"
echo "  Run tests:           mvn test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${GREEN}✅ Setup complete! Happy coding! 🚀${NC}"
