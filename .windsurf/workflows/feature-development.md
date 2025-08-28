---
description: Simple Feature Development - Prepare repo for new feature development
category: while-coding
---

# Simple Feature Development Workflow

## Quick Summary
**What it does**: Prepares the repository for new feature development by creating a feature branch, verifying the build, and understanding the codebase structure.

**Time**: ~5-10 minutes
**Prerequisites**: Java 17+, Maven 3.6+, Git

## Workflow Steps

### Step 1: Create Feature Branch
```bash
# Sync with latest
git checkout master
git pull origin master

# Create feature branch (use descriptive name)
git checkout -b feature/your-feature-name

# Verify clean state
git status
```

### Step 2: Verify Build Baseline
```bash
# Clean build to ensure starting point is solid
mvn clean compile

# Run all tests to verify baseline
mvn test

# Check architecture compliance
mvn test -Dtest="*ArchitectureTest"
```

**Expected Output**: 
- Clean compilation (~85 source files)
- 89+ unit tests passing
- Architecture tests passing

### Step 3: Explore Domain Structure
```bash
# Examine existing domain packages
find src/main/java/io/pillopl/library -type d | sort

# Look at core domain models
ls src/main/java/io/pillopl/library/lending/*/model/
ls src/main/java/io/pillopl/library/catalogue/

# Check existing test patterns
find src/test/groovy -name "*Test.groovy" | head -5
```

**Key Packages**:
- `catalogue/` - Book catalog management
- `lending/book/model/` - Book domain objects  
- `lending/patron/model/` - Patron domain objects
- `lending/*/application/` - Use cases and services

### Step 4: Understand Test Structure
```bash
# Review test categories available
mvn test -Dtest="*ArchitectureTest" | grep -A 3 "Architecture"
mvn test -Dtest="*IT" | grep "Tests run" || echo "No integration tests with IT suffix"

# Check coverage baseline
mvn test jacoco:report
open target/site/jacoco/index.html
```

### Step 5: Ready for Development
```bash
# Confirm feature branch is active
git branch --show-current

# Confirm clean working directory
git status

# Ready to start feature development
echo "✅ Repository prepared for feature development"
echo "📁 Working on branch: $(git branch --show-current)"
echo "🏗️ Domain packages identified"
echo "🧪 Test baseline verified"
```

## Next Steps for Agent
- Identify which domain package your feature belongs to
- Follow existing test patterns (Spock for unit tests, Spring for integration)
- Use TDD approach: write tests first, then implementation
- Run `mvn test` frequently during development
- Keep commits atomic and descriptive

## Domain Guidelines
- Follow hexagonal architecture (domain/application/infrastructure)
- Business logic goes in domain models (Book, Patron, etc.)
- Use Value Objects for concepts like ISBN, PatronId
- Application services orchestrate use cases
- Keep Spring annotations out of domain classes
