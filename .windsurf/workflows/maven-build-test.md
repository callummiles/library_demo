---
description: How to perform a Maven Build and Test
---

# Library Management System - Build & Test Workflow

## Overview
This workflow provides a standardized, repeatable process for building, testing, and optionally running the UI for the Library Management System. This DDD (Domain-Driven Design) uses Spring Boot, Spock/Groovy testing, and H2 database.

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- Git

## Workflow Steps

### Step 1: Environment Verification
Verify the development environment is properly configured:

```bash
# Check Java version (should be 17+)
java -version

# Check Maven version (should be 3.6+)
mvn -version

# Verify repository structure
ls -la src/main/java/io/pillopl/library/
```

**Expected Output**: Should show Java 17+, Maven 3.6+, and library domain packages (catalogue, lending, commons)

### Step 2: Clean and Compile
Perform a clean build to ensure no stale artifacts:

```bash
mvn clean compile
```

**Expected Output**: `BUILD SUCCESS` with ~85 source files compiled
**Note**: UI code (`**/ui/**`) and duplicate models (`**/new_model/**`) are excluded via compiler configuration

### Step 3: Run Unit Tests
Execute the core unit test suite:

```bash
mvn test
```

**Expected Output**: 
- 89 unit tests passing
- 0 failures, 0 errors
- Test categories: domain model, application services, infrastructure mapping
- Some expected ERROR logs in `PlacingBookOnHoldTest` (intentional negative test cases)

### Step 4: Run Integration Tests
Execute integration tests with Spring Boot context:

```bash
mvn verify
```

**Expected Output**:
- All unit tests (89) + integration tests (22) = 111 total tests passing
- Spring Boot contexts started for database integration tests
- JaCoCo code coverage report generated in `target/site/jacoco/`

### Step 5: Generate Reports
View test and coverage reports:

```bash
# View JaCoCo coverage report
open target/site/jacoco/index.html

# Check surefire test reports
ls target/surefire-reports/
```

### Step 6: Package Application
Create executable JAR:

```bash
mvn package
```

**Expected Output**: Creates `library-0.0.1-SNAPSHOT.jar` in `target/` directory

### Step 7: Run Application (Optional UI)
Start the Spring Boot application to access REST APIs and potential web UI:

```bash
# Run the application
java -jar target/library-0.0.1-SNAPSHOT.jar

# Alternative: Run via Maven
mvn spring-boot:run
```

**Expected Output**: 
- Spring Boot starts on default port 8080
- Available endpoints:
  - Health check: `http://localhost:8080/actuator/health`
  - Metrics: `http://localhost:8080/actuator/prometheus`
  - Patron profiles: `http://localhost:8080/profiles/{patronId}` (if implemented)

### Step 8: Validation Checks
Verify the deployment:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check available endpoints
curl http://localhost:8080/actuator

# Stop application (Ctrl+C)
```

## Enterprise Integration

### CI/CD Pipeline Integration
Add these Maven goals to your pipeline:

```yaml
# Example Jenkins/GitHub Actions steps
- mvn clean compile  # Build verification
- mvn test          # Unit tests
- mvn verify        # Integration tests + packaging
- mvn spring-boot:run & # Start application (background)
- # Run API tests, UI tests, etc.
- kill $!           # Stop application
```

### Quality Gates
Recommended quality thresholds:
- Test Coverage: >80% (check JaCoCo reports)
- Unit Tests: 100% pass rate
- Integration Tests: 100% pass rate
- Build Time: <30 seconds
- Test Execution: <60 seconds

### Monitoring
Key metrics to track:
- Build success rate
- Test execution time trends
- Code coverage trends
- Application startup time

## Troubleshooting

### Common Issues

1. **Maven Repository Issues**
   ```bash
   mvn clean -U  # Force update dependencies
   ```

2. **Java Version Mismatch**
   ```bash
   export JAVA_HOME=/path/to/java17
   ```

3. **Port Conflicts (8080 in use)**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```

4. **UI Dependencies Missing**
   - UI code is intentionally excluded from compilation
   - Core library functionality works without UI dependencies
   - To restore UI: remove exclusions in `pom.xml` and add missing dependencies

### Log Files
- Maven logs: Console output
- Application logs: Console output or `logs/` directory
- Test reports: `target/surefire-reports/`

## Architecture Notes

This is a **hexagonal architecture** implementation with:
- **Domain Layer**: Core business logic (book, patron, holds)
- **Application Layer**: Use cases and command handlers
- **Infrastructure Layer**: Database repositories and event publishing
- **API Layer**: REST controllers (patron profiles)

The excluded UI code appears to be from a different project (IGV - Integrative Genomics Viewer) and requires additional dependencies not relevant to the library domain.
