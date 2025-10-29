# Darum Microservices CI/CD

This document describes the CI/CD pipeline setup for the Darum Microservices project.

## 📋 Overview

The project uses **GitHub Actions** for continuous integration and continuous deployment with the following workflows:

### 1. CI - Pull Request (`ci-pull-request.yml`)

Runs on every pull request to `main` or `develop` branches.

**Features:**

-   ✅ Builds all services in parallel
-   ✅ Runs unit tests for all services
-   ✅ Runs integration tests (auth-service, employee-service)
-   ✅ Generates test coverage reports (JaCoCo)
-   ✅ Uploads coverage to Codecov
-   ✅ Code quality analysis (Checkstyle, SpotBugs, SonarCloud)
-   ✅ Security scanning (OWASP Dependency Check)
-   ✅ Posts PR summary comment with results

**Matrix Strategy:**

-   Java Version: 21
-   Services: discovery-service, config-server, api-gateway, auth-service, employee-service, shared-domain

### 2. CD - Deploy to Production (`cd-main.yml`)

Runs on push to `main` branch or manual workflow dispatch.

**Features:**

-   🐳 Builds Docker images for all services
-   📦 Pushes images to GitHub Container Registry (GHCR)
-   🚀 Deploys to Staging environment automatically
-   🚀 Deploys to Production with approval (Blue-Green deployment)
-   🔍 Runs smoke tests after deployment
-   🏥 Health checks before traffic switch
-   🔄 Automatic rollback on failure
-   📧 Notifications (Slack, Email)

**Environments:**

-   **Staging**: Auto-deployed on every push to main
-   **Production**: Manual approval required, Blue-Green deployment

### 3. Test Coverage Report (`test-coverage.yml`)

Runs on push, pull request, or weekly schedule.

**Features:**

-   📊 Generates detailed coverage reports
-   🎯 Creates coverage badges
-   📈 Uploads to Codecov
-   💬 Comments on PR with coverage details
-   📋 Aggregates coverage across services

### 4. Dependency Update Check (`dependency-update.yml`)

Runs weekly on Monday or manual trigger.

**Features:**

-   🔍 Checks for dependency updates
-   ⬆️ Updates dependencies automatically
-   🧪 Tests with updated dependencies
-   🔀 Creates PR for review
-   📦 Generates dependency report

## 🚀 Setup Instructions

### 1. Prerequisites

-   GitHub repository
-   Docker Hub or GitHub Container Registry account
-   Kubernetes cluster (for deployment)
-   Codecov account (optional, for coverage reports)
-   SonarCloud account (optional, for code quality)

### 2. Required Secrets

Add these secrets to your GitHub repository:

**For CI/CD:**

```
GITHUB_TOKEN              # Auto-provided by GitHub
PAT_TOKEN                 # Personal Access Token for creating PRs
```

**For Container Registry:**

```
DOCKER_USERNAME           # Docker Hub username (if using Docker Hub)
DOCKER_PASSWORD           # Docker Hub password (if using Docker Hub)
```

**For Kubernetes Deployment:**

```
KUBE_CONFIG_STAGING       # Kubernetes config for staging
KUBE_CONFIG_PRODUCTION    # Kubernetes config for production
```

**For Code Quality (Optional):**

```
SONAR_TOKEN               # SonarCloud token
CODECOV_TOKEN             # Codecov token
```

**For Notifications:**

```
SLACK_WEBHOOK             # Slack webhook URL
EMAIL_USERNAME            # Email username for notifications
EMAIL_PASSWORD            # Email password for notifications
```

### 3. Enable GitHub Actions

1. Go to your repository on GitHub
2. Click on **Actions** tab
3. Enable workflows if not already enabled
4. Workflows will trigger based on their defined events

### 4. Configure Branch Protection

Protect your `main` and `develop` branches:

1. Go to **Settings** → **Branches**
2. Add branch protection rule for `main`:
    - ✅ Require status checks to pass before merging
    - ✅ Require branches to be up to date before merging
    - ✅ Select required status checks:
        - Build and Test All Services
        - Code Quality Analysis
        - Security Vulnerability Scan

### 5. Setup Kubernetes Cluster

Apply Kubernetes manifests:

```bash
# Create namespaces
kubectl apply -f k8s/namespaces.yml

# Deploy PostgreSQL
kubectl apply -f k8s/postgres.yml

# Deploy services
kubectl apply -f k8s/deployments.yml
```

### 6. Local Development with Docker

Build and run all services locally:

```bash
# Build services
mvn clean install

# Build Docker images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## 🔄 Workflow Triggers

| Workflow          | Trigger                      | Purpose                      |
| ----------------- | ---------------------------- | ---------------------------- |
| CI - Pull Request | Pull request to main/develop | Run tests and quality checks |
| CD - Deploy       | Push to main or manual       | Deploy to staging/production |
| Test Coverage     | Push, PR, weekly             | Generate coverage reports    |
| Dependency Update | Weekly or manual             | Update dependencies          |

## 📊 Test Reports

Test reports are automatically generated and available:

1. **GitHub Actions Summary**: View in Actions tab after workflow completion
2. **Codecov**: https://codecov.io/gh/your-org/darum-microservice
3. **SonarCloud**: https://sonarcloud.io/dashboard?id=darum-microservice
4. **Artifacts**: Download from workflow run (surefire-reports, jacoco reports)

## 🐳 Docker Images

Images are pushed to GitHub Container Registry:

-   `ghcr.io/your-org/discovery-service:latest`
-   `ghcr.io/your-org/config-server:latest`
-   `ghcr.io/your-org/api-gateway:latest`
-   `ghcr.io/your-org/auth-service:latest`
-   `ghcr.io/your-org/employee-service:latest`

## 🚢 Deployment Strategy

### Staging Deployment

-   **Trigger**: Automatic on push to main
-   **Strategy**: Rolling update
-   **Replicas**: 2-3 per service
-   **Health Checks**: Before marking as ready
-   **Smoke Tests**: After deployment

### Production Deployment

-   **Trigger**: Manual approval required
-   **Strategy**: Blue-Green deployment
-   **Process**:
    1. Deploy to green environment
    2. Run health checks on green
    3. Switch traffic to green
    4. Monitor for 60 seconds
    5. Automatic rollback on failure

## 🔧 Smoke Tests

Smoke tests are automatically run after deployment:

```bash
# Manual smoke test
./scripts/smoke-tests.sh staging
./scripts/smoke-tests.sh production
```

Tests include:

-   ✅ Service health checks
-   ✅ Discovery service registration
-   ✅ Authentication flow
-   ✅ API endpoint availability
-   ✅ Database connectivity

## 🏥 Health Checks

Health checks validate deployment success:

```bash
# Manual health check
./scripts/health-check.sh staging blue
./scripts/health-check.sh production green
```

Checks include:

-   ✅ Pod readiness status
-   ✅ Service endpoint availability
-   ✅ Database connectivity
-   ✅ Resource usage (CPU, Memory)

## 📝 Monitoring and Notifications

### Slack Notifications

Configure webhook URL in secrets to receive:

-   ✅ Deployment status
-   ✅ Test failures
-   ✅ Security vulnerabilities

### Email Notifications

Configure email credentials to receive:

-   ✅ Deployment summaries
-   ✅ Critical failures
-   ✅ Weekly reports

## 🔐 Security

Security measures implemented:

-   ✅ OWASP Dependency Check on every PR
-   ✅ Container vulnerability scanning
-   ✅ Secret scanning (GitHub native)
-   ✅ Branch protection rules
-   ✅ Required code reviews
-   ✅ Signed commits (recommended)

## 🐛 Troubleshooting

### Build Failures

1. Check build logs in Actions tab
2. Verify Java version (should be 21)
3. Clear Maven cache and retry
4. Check for dependency conflicts

### Deployment Failures

1. Check pod logs: `kubectl logs -n darum-staging <pod-name>`
2. Verify database connectivity
3. Check Eureka registration
4. Review environment variables

### Test Failures

1. Download test reports from artifacts
2. Check test logs for specific failures
3. Run tests locally: `mvn test`
4. Verify test data and mocks

## 📚 Additional Resources

-   [GitHub Actions Documentation](https://docs.github.com/en/actions)
-   [Docker Documentation](https://docs.docker.com/)
-   [Kubernetes Documentation](https://kubernetes.io/docs/)
-   [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)

## 🤝 Contributing

1. Create a feature branch from `develop`
2. Make your changes
3. Ensure all tests pass locally
4. Create a pull request to `develop`
5. Wait for CI checks to pass
6. Request code review
7. Merge after approval

## 📞 Support

For issues or questions:

-   Create an issue in GitHub
-   Contact DevOps team
-   Check workflow logs in Actions tab
