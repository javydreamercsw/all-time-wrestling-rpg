# Contributing to All Time Wrestling RPG

Thank you for your interest in contributing to the All Time Wrestling RPG project! This guide will help you get started with development, testing, and code quality standards.

## Development Setup

### Prerequisites
- Java 25 or higher
- Maven 3.8+
- Git

### Getting Started
1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/all-time-wrestling-rpg.git`
3. Navigate to the project: `cd all-time-wrestling-rpg`
4. Run the application: `./mvnw`

## Code Quality Standards

### Code Coverage
This project maintains a **90% code coverage goal** using JaCoCo. All contributions must maintain or improve coverage.

#### Running Coverage Analysis
**Important**: This project requires Java 25. If you have multiple Java versions installed, set JAVA_HOME:

```bash
# Set Java 25 (adjust path as needed)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home

# Run all tests with coverage
mvn clean verify

# Run with coverage verification (fails if below 90%)
mvn clean verify -Pcoverage

# Generate coverage report only
mvn jacoco:report

# View HTML coverage report
open target/site/jacoco/index.html
```

**Alternative**: Run with Java 25 inline:
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home mvn clean verify
```

#### Coverage Thresholds
- **Overall Coverage**: 90% minimum (instruction and branch coverage)
- **Class Coverage**: 80% minimum per class
- **Integration Tests**: 80% minimum coverage

#### Coverage Reports
- **HTML Report**: `target/site/jacoco/index.html` (interactive, open in browser)
- **XML Report**: `target/site/jacoco/jacoco.xml` (for CI/CD integration)
- **CSV Report**: `target/site/jacoco/jacoco.csv` (for analysis)

### Code Formatting
The project uses Spotless for code formatting:
```bash
# Apply code formatting
mvn spotless:apply

# Check formatting (fails if not formatted)
mvn spotless:check
```

### Static Analysis
PMD is used for static code analysis:
```bash
# Run PMD analysis
mvn pmd:check

# Generate PMD report
mvn pmd:pmd
```

## Testing Guidelines

### Test Types
1. **Unit Tests**: Test individual classes and methods
2. **Integration Tests**: Test component interactions and database operations
3. **End-to-End Tests**: Test complete user workflows

### Test Naming Conventions
- Unit tests: `*Test.java`
- Integration tests: `*IT.java` or `*IntegrationTest.java`
- Test methods: `should[ExpectedBehavior]When[StateUnderTest]`

### Writing Tests
```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests
mvn test -Dtest="*IT,*IntegrationTest"

# Run specific test class
mvn test -Dtest="WrestlerServiceTest"
```

### Test Coverage Best Practices
1. **Focus on business logic**: Prioritize testing service classes and domain logic
2. **Test edge cases**: Include boundary conditions and error scenarios
3. **Use integration tests**: Test API endpoints and database interactions
4. **Mock external dependencies**: Use Mockito for unit tests
5. **Maintain coverage**: New code should have ≥90% coverage

## Build and Verification

### Full Build Process
```bash
# Complete build with all checks
mvn clean verify -Pcoverage

# Production build
mvn clean package -Pproduction
```

### Pre-commit Checklist
Before submitting a pull request, ensure:
- [ ] Code is formatted: `mvn spotless:apply`
- [ ] All tests pass: `mvn test`
- [ ] Coverage is ≥90%: `mvn verify -Pcoverage`
- [ ] No PMD violations: `mvn pmd:check`
- [ ] No security vulnerabilities: `mvn org.owasp:dependency-check-maven:check`

## Pull Request Guidelines

### Before Submitting
1. **Create a feature branch**: `git checkout -b feature/your-feature-name`
2. **Write tests first**: Follow TDD practices when possible
3. **Maintain coverage**: Ensure your changes don't reduce overall coverage
4. **Update documentation**: Update relevant documentation for new features
5. **Run full verification**: `mvn clean verify -Pcoverage`

### PR Requirements
- [ ] All tests pass
- [ ] Code coverage ≥90%
- [ ] Code is properly formatted
- [ ] No static analysis violations
- [ ] Documentation updated (if applicable)
- [ ] Commit messages are descriptive

### Automated Checks
Pull requests automatically run:
- Unit and integration tests
- Code coverage analysis
- Security vulnerability scanning
- Code formatting verification
- Static analysis (PMD)

## Development Workflow

### Branch Naming
- Features: `feature/description`
- Bug fixes: `fix/description`
- Documentation: `docs/description`
- Refactoring: `refactor/description`

### Commit Messages
Use conventional commit format:
```
type(scope): description

Examples:
feat(wrestler): add injury system with bump tracking
fix(match): resolve NPC match resolution deadlock
test(injury): add comprehensive integration tests
docs(coverage): update JaCoCo configuration guide
```

## Code Architecture

### Package Structure
- `domain/`: Entity classes and repositories
- `service/`: Business logic and services
- `controller/`: REST API endpoints
- `ui/`: Vaadin UI components
- `config/`: Configuration classes

### Testing Structure
- `src/test/java/`: Unit tests
- Integration tests are mixed with unit tests but use `*IT.java` naming
- Test utilities in `src/test/java/com/github/javydreamercsw/testutil/`

## Getting Help

- **Issues**: Create GitHub issues for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions
- **Code Review**: All PRs require review before merging

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
