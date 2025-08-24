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
- **GitHub Dependency Review**: PR-based dependency vulnerability detection (public repositories only)
- **Regular Security Scans**: Weekly automated security scans via GitHub Actions

### Security Workflows

This is a **public repository** with full GitHub Advanced Security features:

- **Complete dependency review**: License compliance + vulnerability scanning
- **GitHub Advanced Security**: Full security feature set available
- **SARIF integration**: Vulnerabilities appear in Security tab
- **Automated license compliance**: Only approved licenses allowed
- **Weekly security scans**: Automated vulnerability monitoring

### Running Security Scans Locally
```bash
# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Check for dependency updates
mvn versions:display-dependency-updates

# Generate detailed security report
mvn org.owasp:dependency-check-maven:check -Dformats=HTML,JSON,SARIF
```

### Security Policies
- Dependencies with CVSS score ≥ 7.0 will fail the build
- Only approved open source licenses are allowed (enforced on public repos)
- Regular dependency updates are encouraged
- Weekly automated vulnerability scanning

## Development Workflow

### Pull Request Labeling
This repository uses automated pull request labeling based on changed files:

- **Automatic labeling**: PRs are automatically labeled based on the files you modify
- **Smart categorization**: Labels help identify the type and scope of changes
- **Review efficiency**: Makes it easier for reviewers to understand PR content

#### Label Categories
- **Code Areas**: `frontend`, `backend`, `database`, `configuration`
- **Content**: `documentation`, `tests`, `security`
- **Domain**: `wrestling`, `ai`, `dependencies`
- **Size**: `major`, `minor`, `patch`

#### Setting Up Labels
Run the label setup script to create all necessary labels:
```bash
./scripts/setup-labels.sh
```

## Getting Started

The [Getting Started](https://vaadin.com/docs/latest/getting-started) guide will quickly familiarize you with your new
All Time Wrestling Rpg implementation. You'll learn how to set up your development environment, understand the project
structure, and find resources to help you add muscles to your skeleton — transforming it into a fully-featured
application.
