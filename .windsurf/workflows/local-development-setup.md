---
description: Local Development Setup - Configure environment, dependencies, and debug tools
---

# Local Development Setup Workflow

## Quick Summary
**What it does**: Sets up local development environment with all dependencies, debug tools, and optimal configuration for the library management system.

**Time**: ~10-15 minutes
**Prerequisites**: Java 11+, Maven 3.6+, IDE, jq (JSON processor, install via 'brew install jq')

## Workflow Steps

### Step 1: Environment Verification
```bash
# Verify required versions
java -version | grep "11\|17\|21"
mvn -version | grep "Apache Maven"
git config --get user.name && git config --get user.email

# Verify repository structure
ls src/main/java/io/pillopl/library/
```

### Step 2: Install Security Dependencies
```bash
# Add OWASP Dependency Check plugin to pom.xml
# Insert this in the <plugins> section:
cat >> temp_plugin.xml << 'EOF'
            <plugin>
                <groupId>org.owasp</groupId>
                <artifactId>dependency-check-maven</artifactId>
                <version>8.4.2</version>
                <configuration>
                    <format>ALL</format>
                    <failBuildOnCVSS>7</failBuildOnCVSS>
                </configuration>
            </plugin>
EOF

echo "Add the above plugin to pom.xml <plugins> section"
```

### Step 3: Create Development Configuration
```bash
# Create development application properties
cat > src/main/resources/application-dev.yml << 'EOF'
spring:
  profiles:
    active: dev
  
  # H2 Database for development
  datasource:
    url: jdbc:h2:mem:library-dev;MODE=LEGACY;DB_CLOSE_ON_EXIT=FALSE
    username: dev
    password: dev
  
  # Enable H2 console
  h2:
    console:
      enabled: true
      path: /h2-console
  
  # Development logging
  logging:
    level:
      io.pillopl.library: DEBUG
      
  # Actuator endpoints
  management:
    endpoints:
      web:
        exposure:
          include: "*"
    endpoint:
      health:
        show-details: always

server:
  port: 8080
EOF
```

### Step 4: Create OWASP Suppression File
```bash
# Create suppression file for security scanning
cat > owasp-suppressions.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Add false positive suppressions here as needed -->
</suppressions>
EOF
```

### Step 5: Test Development Setup
```bash
# Build and verify everything works
mvn clean compile

# Test application startup
mvn spring-boot:run -Dspring-boot.run.profiles=dev &
APP_PID=$!
sleep 15

# Verify endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/h2-console

# Stop application
kill $APP_PID
```

### Step 6: Configure Debug Tools
```bash
# Update vulnerability database (first time)
mvn org.owasp:dependency-check-maven:update-only

# Test coverage report generation
mvn test jacoco:report
```

### Step 7: Verify Complete Setup
```bash
# Final verification
echo "=== Development Environment Ready ==="
echo "✅ Java version: $(java -version 2>&1 | head -1)"
echo "✅ Maven version: $(mvn -version | head -1)"
echo "✅ Application config: application-dev.yml created"
echo "✅ Security scanning: OWASP plugin ready"
echo "✅ Debug tools: H2 console, actuator endpoints"
echo ""
echo "🚀 Start development server: mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "📊 H2 Console: http://localhost:8080/h2-console"
echo "❤️ Health Check: http://localhost:8080/actuator/health"
echo "🔒 Security Scan: mvn org.owasp:dependency-check-maven:check"
```

## Development Tools Available
- **H2 Console**: Database inspection at `/h2-console`
- **Actuator Endpoints**: Health, metrics, info at `/actuator/*`
- **Security Scanning**: OWASP dependency check
- **Code Coverage**: JaCoCo reports in `target/site/jacoco/`
- **Hot Reload**: Spring Boot DevTools (if enabled)

## Quick Commands
```bash
# Start development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests with coverage
mvn test jacoco:report

# Security scan
mvn org.owasp:dependency-check-maven:check

# Architecture check
mvn test -Dtest="*ArchitectureTest"
```
