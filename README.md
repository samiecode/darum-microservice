# Darum Microservices Architecture

## 🏗️ Architecture Overview

This is a **Spring Cloud-based microservices architecture** for employee and authentication management.

### 📦 Services

| Service               | Port    | Description                          | Dependencies                                       |
| --------------------- | ------- | ------------------------------------ | -------------------------------------------------- |
| **Discovery Service** | 8761    | Eureka Server for service discovery  | None                                               |
| **Config Server**     | 8888    | Centralized configuration management | Discovery Service                                  |
| **API Gateway**       | 8080    | Single entry point for all services  | Discovery Service                                  |
| **Auth Service**      | Dynamic | Authentication & JWT management      | Config Server, Discovery, PostgreSQL               |
| **Employee Service**  | Dynamic | Employee & department management     | Config Server, Discovery, PostgreSQL, Auth Service |

### 🗄️ Databases

-   **auth_db**: PostgreSQL database for authentication service
-   **employee_db**: PostgreSQL database for employee service

### 🔑 Key Technologies

-   **Java 21**
-   **Spring Boot 3.5.7**
-   **Spring Cloud 2025.0.0**
-   **PostgreSQL 16**
-   **JWT (io.jsonwebtoken)**
-   **Flyway** (Database migrations)
-   **Docker**
-   **GitHub Actions** (CI/CD)

## 🚀 Getting Started

### Prerequisites

-   Java 21
-   Maven 3.9+
-   Docker & Docker Compose
-   PostgreSQL 16 (if running locally)

### Local Development

1. **Clone the repository**

    ```bash
    git clone https://github.com/your-org/darum-microservice.git
    cd darum-microservice
    ```

2. **Build all services**

    ```bash
    mvn clean install -
    ```

3. **Start services with Docker Compose**

    ```bash
    docker-compose up -d
    ```

4. **Verify services are running**
    - Eureka Dashboard: http://localhost:8761
    - Config Server: http://localhost:8888
    - API Gateway: http://localhost:8080
    - Auth Service: http://localhost:8081 (or check Eureka)
    - Employee Service: http://localhost:8082 (or check Eureka)

### Running Individual Services

```bash
# Start Discovery Service first
cd discovery-service
mvn spring-boot:run

# Start Config Server
cd config-server
mvn spring-boot:run

# Start databases (or use Docker)
docker-compose up -d postgres-auth postgres-employee

# Start Auth Service
cd auth-service
mvn spring-boot:run

# Start Employee Service
cd employee-service
mvn spring-boot:run

# Start API Gateway
cd api-gateway
mvn spring-boot:run
```

## 📊 Testing

### Run All Tests

```bash
mvn test
```

## 🔐 Authentication Flow

1. **Register**: POST `/api/v1/auth/register`

    ```json
    {
    	"name": "John Doe",
    	"email": "john@example.com",
    	"password": "SecurePass@123",
    }
    ```

2. **Login**: POST `/api/v1/auth/login`

    ```json
    {
    	"email": "john@example.com",
    	"password": "SecurePass@123"
    }
    ```

3. **Response**:

    ```json
    {
    	"status": "success",
    	"message": "Authentication successful",
    	"data": {
    		"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    		"user": {
    			"id": 1,
    			"email": "john@example.com",
    			"role": "EMPLOYEE"
    		}
    	}
    }
    ```

4. **Use Token**: Add to all subsequent requests
    ```
    Authorization: Bearer <token>
    ```

## 🛠️ API Endpoints

### Auth Service (`/api/v1/auth`)

| Method | Endpoint    | Description       | Auth Required |
| ------ | ----------- | ----------------- | ------------- |
| POST   | `/register` | Register new user | No            |
| POST   | `/login`    | Authenticate user | No            |

### Employee Service (`/api/v1/employees`)

| Method | Endpoint             | Description                 | Auth Required  |
| ------ | -------------------- | --------------------------- | -------------- |
| GET    | `/`                  | Get all employees           | ADMIN          |
| GET    | `/{id}`              | Get employee by ID          | ADMIN          |
| POST   | `/`                  | Create employee             | ADMIN          |
| PUT    | `/{id}`              | Update employee             | ADMIN          |
| DELETE | `/{id}`              | Delete employee             | ADMIN          |
| GET    | `/me`                | Get current user profile    | ALL            |
| GET    | `/department/{name}` | Get employees by department | ADMIN, MANAGER |

### Department Service (`/api/v1/departments`)

| Method | Endpoint       | Description            | Auth Required |
| ------ | -------------- | ---------------------- | ------------- |
| GET    | `/`            | Get all departments    | ADMIN         |
| GET    | `/{id}`        | Get department by ID   | ADMIN         |
| POST   | `/`            | Create department      | ADMIN         |
| PUT    | `/{id}`        | Update department      | ADMIN         |
| DELETE | `/{id}`        | Delete department      | ADMIN         |
| GET    | `/name/{name}` | Get department by name | ADMIN         |

## 🐳 Docker

### Build Images

```bash
# Build all images
docker-compose build

# Build specific service
docker build -t auth-service ./auth-service
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Remove volumes
docker-compose down -v
```

## 📁 Project Structure

```
darum-microservice/
├── .github/
│   └── workflows/           # GitHub Actions CI/CD pipelines
├── api-gateway/             # API Gateway service
├── auth-service/            # Authentication service
│   ├── src/
│   │   ├── main/java/
│   │   │   └── com/darum/auth/
│   │   └── test/java/       # tests
│   ├── Dockerfile
│   └── pom.xml
├── employee-service/        # Employee management service
│   ├── src/
│   │   ├── main/java/
│   │   │   └── com/darum/employee/
│   │   └── test/java/       # tests
│   ├── Dockerfile
│   └── pom.xml
├── config-server/           # Spring Cloud Config Server
├── discovery-service/       # Eureka Discovery Service
├── shared-domain/           # Shared DTOs and utilities
├── k8s/                     # Kubernetes manifests
│   ├── namespaces.yml
│   ├── postgres.yml
│   └── deployments.yml
├── scripts/                 # Deployment scripts
│   ├── smoke-tests.sh
│   └── health-check.sh
├── docker-compose.yml       # Docker Compose configuration
├── pom.xml                  # Parent POM
└── .env.example             # Environment Variable
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file (see `.env.example`):

```bash
DB_USER=darum
DB_PASSWORD=darum123
REGISTRY=ghcr.io
IMAGE_TAG=latest
```

### Application Profiles

-   **default**: Local development
-   **docker**: Docker Compose environment
-   **k8s**: Kubernetes environment
-   **test**: Testing with H2 database

## 📈 Monitoring

### Actuator Endpoints

All services expose Spring Boot Actuator endpoints:

-   `/actuator/health` - Health check
-   `/actuator/info` - Application info
-   `/actuator/metrics` - Metrics
-   `/actuator/prometheus` - Prometheus metrics

### OpenAPI Documentation

-   Auth Service: http://localhost:8081/docs/swagger-ui.html
-   Employee Service: http://localhost:8082/docs/swagger-ui.html

## 🐛 Troubleshooting

### Service won't start

1. Check if all dependencies are running (Eureka, Config Server, Database)
2. Verify port availability
3. Check logs: `docker-compose logs <service-name>`

### Database connection issues

1. Ensure PostgreSQL is running
2. Verify connection details in application.yml
3. Check Flyway migrations

### Authentication failures

1. Verify JWT secret key configuration
2. Check token expiration
3. Ensure user exists in database


## 📄 License

This project is licensed under the MIT License.