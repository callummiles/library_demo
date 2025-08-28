# Local Development Setup

## Prerequisites

- Java 17
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Lombok Setup (Required)

This project uses Lombok for code generation. **Compilation will fail without proper Lombok configuration.**

### IntelliJ IDEA
1. Install Lombok plugin: `File` → `Settings` → `Plugins` → Search "Lombok" → Install
2. Enable annotation processing: `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors` → Check "Enable annotation processing"
3. Restart IntelliJ

### Eclipse
1. Download lombok.jar from https://projectlombok.org/download
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation directory
4. Click "Install/Update"
5. Restart Eclipse

### VS Code
1. Install "Lombok Annotations Support for VS Code" extension
2. Install "Extension Pack for Java" if not already installed
3. Reload VS Code

## Build and Run

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run application
mvn spring-boot:run
```

## Common Issues

### Compilation Errors (getters/setters not found)
- **Cause**: Lombok not properly configured
- **Solution**: Follow Lombok setup instructions above
- **If still failing**: The Maven compiler plugin has been updated to include Lombok annotation processing. Run `mvn clean compile` to rebuild.

### "cannot find symbol: class __"
- **Cause**: Incomplete Lombok processing
- **Solution**: Clean rebuild after Lombok setup: `mvn clean compile`

### Maven Build Still Failing After Lombok Setup
- **Cause**: Maven compiler plugin not configured for Lombok
- **Solution**: The pom.xml has been updated with `annotationProcessorPaths` for Lombok. This should resolve all compilation issues.

## Database

The application uses H2 in-memory database by default. No additional setup required for development.
