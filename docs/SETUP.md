# Development Setup Guide

This guide covers setting up the 5GLab Booking System for local development.

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 21+ | Runtime & compilation |
| Docker Desktop | Latest | Database & containerization |
| Git | Latest | Version control |
| IDE | IntelliJ IDEA / VS Code | Development |

### Optional Software

| Software | Purpose |
|----------|---------|
| PostgreSQL client | Direct database access |
| Postman / Insomnia | API testing |
| OpenSSL | Key generation |

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/5GLab-booking-system.git
cd 5GLab-booking-system
```

### 2. Generate JWT Keys

The application uses RSA-256 for JWT signing. Generate the key pair:

```bash
# Create keys directory if it doesn't exist
mkdir -p src/main/resources/keys

# Generate private key (PKCS#8 format)
openssl genrsa -out src/main/resources/keys/private.pem 2048

# Extract public key
openssl rsa -in src/main/resources/keys/private.pem -pubout -out src/main/resources/keys/public.pem
```

> ⚠️ **Important**: Never commit these keys to version control. They are in `.gitignore`.

### 3. Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env and set your database password
# POSTGRES_PASSWORD=your_secure_password
```

### 4. Start the Database

```bash
# Start only the database container
docker compose up -d booking_db

# Verify it's running
docker compose ps
```

### 5. Run the Application

#### Option A: Using Gradle (recommended for development)

```bash
# Windows
./gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

#### Option B: Using Docker Compose (full stack)

```bash
docker compose up -d
```

### 6. Verify Setup

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Open Swagger UI
# Navigate to: http://localhost:8080/swagger-ui.html
```

## Environment Configuration

### Development Environment Variables

Create a `.env` file in the project root:

```env
# Database (use your own secure credentials)
DB_URL=jdbc:postgresql://localhost:5432/<database_name>
DB_USERNAME=<your_db_username>
DB_PASSWORD=<your_secure_password>
DB_DRIVER=org.postgresql.Driver

# JWT Configuration
JWT_PRIVATE_KEY_PATH=classpath:keys/private.pem
JWT_PUBLIC_KEY_PATH=classpath:keys/public.pem
JWT_ACCESS_TOKEN_EXPIRY=15m
JWT_REFRESH_TOKEN_EXPIRY=7d
JWT_ISSUER=booking-system
```

> ⚠️ **Security Note**: Never commit `.env` files or credentials to version control. The `.env` file is in `.gitignore`.

### Configuration Files

| File | Purpose |
|------|---------|
| `application.yml` | Main application configuration |
| `application-test.yml` | Test profile configuration |
| `docker-compose.yml` | Development Docker setup |
| `docker-compose.prod.yml` | Production Docker setup |

## Running Tests

### Unit Tests

Unit tests run without external dependencies:

```bash
./gradlew test --tests "com._glab.booking_system.auth.controller.LoginControllerTest" --tests "com._glab.booking_system.auth.service.PasswordSetupTokenServiceTest"
```

### Integration Tests

Integration tests use Testcontainers and require Docker:

```bash
# Run all tests (integration tests run in CI)
./gradlew test
```

> ⚠️ **Note for Windows users**: Testcontainers may have issues with Docker Desktop + WSL2. Integration tests are designed to run in CI (Linux environment). Run unit tests locally.

### Test Reports

After running tests, view the report at:
```
build/reports/tests/test/index.html
```

## Database Management

### Access PostgreSQL

```bash
# Using Docker (replace with your container name and credentials)
docker exec -it <container_name> psql -U <username> -d <database>

# Common commands
\dt                    # List tables
\d account             # Describe account table
SELECT * FROM account; # Query users
```

### Reset Database

```bash
# Stop and remove database container (data will be lost)
docker compose down -v

# Restart
docker compose up -d booking_db
```

### Database Schema

The application uses Hibernate's `ddl-auto: update` for development. Tables are created automatically on first run.

## IDE Setup

### IntelliJ IDEA

1. **Import Project**: File → Open → Select `build.gradle`
2. **Enable Annotation Processing**: Settings → Build → Compiler → Annotation Processors → Enable
3. **Install Lombok Plugin**: Settings → Plugins → Search "Lombok"
4. **Set Java SDK**: Project Structure → Project → SDK → 21

### VS Code

1. **Install Extensions**:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Configure settings.json**:
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic"
}
```

## Common Issues

### Port 5432 Already in Use

```bash
# Find process using the port
netstat -ano | findstr :5432  # Windows
lsof -i :5432                 # Linux/Mac

# Stop conflicting PostgreSQL service
net stop postgresql-x64-16    # Windows
sudo systemctl stop postgresql # Linux
```

### JWT Key Loading Errors

```
Failed to load JWT keys
```

**Solution**: Ensure keys exist and are in correct format:
```bash
# Verify keys
cat src/main/resources/keys/private.pem
cat src/main/resources/keys/public.pem

# Regenerate if needed
openssl genrsa -out src/main/resources/keys/private.pem 2048
openssl rsa -in src/main/resources/keys/private.pem -pubout -out src/main/resources/keys/public.pem
```

### Docker Desktop Not Starting

**Windows**: Ensure WSL2 is installed and updated:
```powershell
wsl --update
wsl --set-default-version 2
```

### Gradle Build Failures

```bash
# Clean build
./gradlew clean build

# Clear Gradle cache
rm -rf ~/.gradle/caches
```

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Write code following existing patterns
- Add unit tests for new functionality
- Update documentation if needed

### 3. Run Tests Locally

```bash
# Unit tests
./gradlew test --tests "*Test"
```

### 4. Commit & Push

```bash
git add .
git commit -m "feat: description of changes"
git push origin feature/your-feature-name
```

### 5. Create Pull Request

- CI will run all tests including integration tests
- Request review from team members

## Useful Commands

| Command | Description |
|---------|-------------|
| `./gradlew bootRun` | Run application |
| `./gradlew test` | Run all tests |
| `./gradlew clean build` | Clean and build |
| `./gradlew dependencies` | Show dependencies |
| `docker compose up -d` | Start all containers |
| `docker compose down` | Stop all containers |
| `docker compose logs -f` | View container logs |

## Next Steps

- Read the [API Documentation](API.md) to understand available endpoints
- Review the [Architecture Guide](ARCHITECTURE.md) for system design

