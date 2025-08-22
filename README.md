# All Time Wrestling Rpg README

- [ ] TODO Replace or update this README with instructions relevant to your application

To start the application in development mode, import it into your IDE and run the `Application` class.
You can also start the application from the command line by running:

```bash
./mvnw
```

To build the application in production mode, run:

```bash
./mvnw -Pproduction package
```

## Security

This project includes several security measures:

### Dependency Security
- **OWASP Dependency Check**: Automated vulnerability scanning of dependencies
- **GitHub Dependency Review**: PR-based dependency vulnerability detection
- **Regular Security Scans**: Weekly automated security scans via GitHub Actions

### Running Security Scans Locally
```bash
# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Check for dependency updates
mvn versions:display-dependency-updates
```

### Security Policies
- Dependencies with CVSS score ≥ 7.0 will fail the build
- Only approved open source licenses are allowed
- Regular dependency updates are encouraged

## Getting Started

The [Getting Started](https://vaadin.com/docs/latest/getting-started) guide will quickly familiarize you with your new
All Time Wrestling Rpg implementation. You'll learn how to set up your development environment, understand the project
structure, and find resources to help you add muscles to your skeleton — transforming it into a fully-featured
application.
