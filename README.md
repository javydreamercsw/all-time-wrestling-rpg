# All Time Wrestling RPG

All Time Wrestling (ATW) RPG is a web-based wrestling RPG simulator that allows you to book shows, manage wrestlers, and generate AI-narrated matches.

## Table of Contents
<!-- spotless:off -->
- [Getting Started](#getting-started)
- [Features](#features)
- [Show Management](#show-management)
	- [Segment Ordering and Main Event](#segment-ordering-and-main-event)
	- [AI Narration](#ai-narration)
- [Segment Rules](#segment-rules)
	- [Bump Addition](#bump-addition)
- [Notion Synchronization](#notion-synchronization)
- [Development](#development)
- [Running the Application](#running-the-application)
- [Docker](#docker)
	- [Prerequisites](#prerequisites)
	- [Building the Docker Image](#building-the-docker-image)
	- [Running the Application with Docker](#running-the-application-with-docker)
- [Running with a Standalone Tomcat Server](#running-with-a-standalone-tomcat-server)
- [Code Quality](#code-quality)
- [Code Coverage](#code-coverage)
- [Code Formatting](#code-formatting)
- [Security](#security)
- [Dependency Security](#dependency-security)
- [NVD API Key Configuration](#nvd-api-key-configuration)
- [Security Workflows](#security-workflows)
- [Running Security Scans Locally](#running-security-scans-locally)
- [Security Policies](#security-policies)
- [Development Workflow](#development-workflow)
- [Pull Request Labeling](#pull-request-labeling)
<!-- spotless:on -->

## Getting Started

To start the application in development mode, import it into your IDE and run the `Application` class.

You can also start the application from the command line by running:

```bash
./mvnw
```

The application will be available at `http://localhost:8080/atw-rpg`.

For more detailed instructions, see the [Vaadin Getting Started Guide](https://vaadin.com/docs/latest/getting-started).

## Features

### Show Management

#### Segment Ordering and Main Event

You can now reorder segments within a show and designate one segment as the main event. This provides greater control over the flow and narrative of your shows.

**How to use:**

1.  **Navigate to the Show Details page:** From the "Show List" view, click on a show to open its details.
2.  **Drag and drop to reorder:** In the "Segments" grid, you can now drag and drop segment rows to change their order.
3.  **Set the main event:** Each segment row has a "Main Event" checkbox. Select the checkbox for the segment you want to mark as the main event.

**AI Narration:**

The AI narration service is aware of the segment order and which segment is the main event. It also has access to the results and narration of previous segments. This allows the AI to generate more context-aware and compelling narrations that build on events from earlier in the show.

### Ranking System

The ranking system has been updated to provide a more dynamic and inclusive contender pool. Wrestlers are primarily ranked based on their fan count, with higher fan counts generally indicating higher ranks.

Crucially, wrestlers who have surpassed the typical fan cap for a given title's tier are no longer excluded from its rankings. This means a top-tier wrestler (e.g., a Main Eventer) can still appear in the contender list for a mid-card title if they meet the minimum fan requirements for that title, allowing for more diverse and strategic championship pursuits.

#### Fan Adjudication

Fan acquisition and loss mechanics have been refined to ensure a more balanced and realistic progression for wrestlers:
*   **Gaining Fans:** Wrestlers gain fans by winning matches and delivering successful promos. The amount of fans gained can be influenced by the wrestler's current tier, with higher tiers potentially experiencing diminishing returns to prevent runaway fan inflation.
*   **Losing Fans:** A significant change is the introduction of fan loss for match losers. Previously, all participants would gain fans. Now, wrestlers who lose a match may experience a decrease in their fan count, or at best a minimal gain, depending on the match quality and their performance. This adds a layer of consequence to match outcomes and helps to naturally regulate fan numbers across the roster.

### Segment Rules

#### Bump Addition

You can now configure segment rules to automatically add bumps to participants. This allows for more granular control over the physical toll that different types of matches have on wrestlers.

**How to use:**

1.  **Navigate to the Segment Rule List page:** From the main menu, select "Segment Rules".
2.  **Create or Edit a Segment Rule:** Click the "Create Segment Rule" button or the edit icon on an existing rule.
3.  **Select the Bump Addition option:** In the edit dialog, you will see a "Bump Addition" dropdown. You can select one of the following options:
	*   **NONE:** No bumps are added to any participants.
	*   **LOSERS:** Bumps are added to all losers of the segment.
	*   **WINNERS:** Bumps are added to all winners of the segment.
	*   **ALL:** Bumps are added to all participants of the segment.
4.  **Save the Segment Rule:** Click the "Save" button to save your changes.

When a segment with the configured rule is adjudicated, bumps will be added to the participants according to the selected option.

### Notion Synchronization

The application can synchronize data from Notion databases to local JSON files and the application's database. This feature is highly configurable and includes a user interface for managing and monitoring the synchronization process.

**Features:**

- **High Performance:** Optimized for performance with parallel processing and batch operations.
- **Configurable:** Enable or disable synchronization, select which entities to sync, and configure the sync interval.
- **REST API:** A complete REST API for triggering and monitoring synchronization.
- **UI for Sync Management:** An interactive UI to manage and monitor Notion synchronization with real-time progress tracking.

**Configuration:**

The Notion synchronization feature is configured in the `application.properties` file.

```properties
# Notion Sync Configuration
notion.sync.enabled=false
notion.sync.scheduler.enabled=false
notion.sync.scheduler.interval=3600000
notion.sync.entities=shows,wrestlers,teams,matches
notion.sync.backup.enabled=true
```

To enable the feature, you need to provide a Notion API token via the `NOTION_TOKEN` environment variable.

## Development

This section contains information for developers contributing to the project.

## Running the Application

To build the application in production mode, run:

```bash
./mvnw -Pproduction package
```

### Docker

This project can be built and run using Docker.

#### Prerequisites

*   [Docker](https://docs.docker.com/get-docker/) installed on your machine.

#### Building the Docker Image

1.  Build the project using Maven:

	```bash
	./mvnw clean install -DskipTests
	```

2.  Build the Docker image:

	```bash
	docker build -t all-time-wrestling-rpg .
	```

#### Running the Application with Docker

To run the application using Docker, you need to provide the required environment variables and mount a volume to persist the database.

1.  Run the Docker container:

	```bash
	docker run -p 9090:9090 \
	-v /path/to/your/data:/data \
	-e GEMINI_API_KEY="<your_gemini_key>" \
	-e OPENAI_API_KEY="<your_openai_key>" \
	-e NOTION_TOKEN="<your_notion_token>" \
	all-time-wrestling-rpg
	```

	*   `-p 9090:9090`: Maps the container's port 9090 to the host's port 9090.
	*   `-v /path/to/your/data:/data`: Mounts a directory from your host machine to the `/data` directory inside the container. This is where the H2 database file will be stored, ensuring data persistence. Replace `/path/to/your/data` with the absolute path on your host machine.
	*   `-e`: Sets the environment variables required for the AI and Notion integrations.

	The application will be accessible at `http://localhost:9090/atw-rpg`.

### Running with a Standalone Tomcat Server

You can also deploy the application to a standalone Tomcat server.

1.  **Build the .war file**:

	```bash
	./mvnw -Pproduction package
	```

2.  **Deploy the .war file**:

	You have two options to get the `.war` file:
	*   **Build from source**: Copy the generated `target/all-time-wrestling-rpg-*.war` file to the `webapps` directory of your Tomcat installation.
	*   **Download from GitHub Releases**: Download the latest `all-time-wrestling-rpg-*.war` from [GitHub Releases](http://github.com/javydreamercsw/all-time-wrestling-rpg/releases) and copy it to the `webapps` directory of your Tomcat installation.

	In either case, rename the `.war` file to `atw-rpg.war`.

3.  **Configure Environment Variables**:

	Create a `setenv.sh` (for Linux/macOS) or `setenv.bat` (for Windows) file in the `bin` directory of your Tomcat installation and add the required environment variables.

	**Example `setenv.sh`:**

	```bash
	#!/bin/bash
	export GEMINI_API_KEY="<your_gemini_key>"
	export OPENAI_API_KEY="<your_openai_key>"
	export NOTION_TOKEN="<your_notion_token>"
	# Use a file-based database for persistence
	export SPRING_DATASOURCE_URL="jdbc:h2:file:/path/to/your/database/atwrpg;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
	export SPRING_DATASOURCE_USERNAME="sa"
	export SPRING_DATASOURCE_PASSWORD=""
	export SPRING_H2_CONSOLE_ENABLED="true"
	```
	Replace the placeholder values with your actual keys and desired database path.

4.  **Start Tomcat**:

	Run `startup.sh` or `startup.bat` from the `bin` directory of your Tomcat installation.

The application will be accessible at `http://localhost:8080/atw-rpg` (assuming Tomcat is running on the default port 8080).

### Code Quality

#### Code Coverage
This project maintains a **90% code coverage goal** using JaCoCo:

```bash
# Run tests with coverage (requires Java 17)
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

For detailed coverage guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md#code-coverage).

#### Code Formatting
```bash
# Apply code formatting
mvn spotless:apply
```

### Security

This project includes several security measures:

#### Dependency Security
- **OWASP Dependency Check**: Automated vulnerability scanning of dependencies
- **GitHub Dependency Review**: PR-based dependency vulnerability detection (public repositories only)
- **Regular Security Scans**: Weekly automated security scans via GitHub Actions

#### NVD API Key Configuration
To improve OWASP Dependency Check performance and avoid long update times, configure an NVD API key:

1. **Get a free API key**: Visit [https://nvd.nist.gov/developers/request-an-api-key](https://nvd.nist.gov/developers/request-an-api-key)
2. **Set environment variable**: `export NVD_API_KEY=your-api-key-here`
3. **Or use Maven property**: `mvn dependency-check:check -Denv.NVD_API_KEY=your-api-key-here`

Without an API key, dependency checks may take significantly longer due to rate limiting.

#### Security Workflows

This is a **public repository** with full GitHub Advanced Security features:

- **Complete dependency review**: License compliance + vulnerability scanning
- **GitHub Advanced Security**: Full security feature set available
- **SARIF integration**: Vulnerabilities appear in Security tab
- **Automated license compliance**: Only approved licenses allowed
- **Weekly security scans**: Automated vulnerability monitoring

#### Running Security Scans Locally
```bash
# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Check for dependency updates
mvn versions:display-dependency-updates

# Generate detailed security report
mvn org.owasp:dependency-check-maven:check -Dformats=HTML,JSON,SARIF
```

#### Security Policies
- Dependencies with CVSS score ≥ 7.0 will fail the build
- Only approved open source licenses are allowed (enforced on public repos)
- Regular dependency updates are encouraged
- Weekly automated vulnerability scanning

### Development Workflow

#### Pull Request Labeling
This repository uses automated pull request labeling based on changed files:

- **Automatic labeling**: PRs are automatically labeled based on the files you modify
- **Smart categorization**: Labels help identify the type and scope of changes
- **Review efficiency**: Makes it easier for reviewers to understand PR content

##### Label Categories
- **Code Areas**: `frontend`, `backend`, `database`, `configuration`
- **Content**: `documentation`, `tests`, `security`
- **Domain**: `wrestling`, `ai`, `dependencies`
- **Size**: `major`, `minor`, `patch`

##### Setting Up Labels
Run the label setup script to create all necessary labels:
```bash
./scripts/setup-labels.sh
```
