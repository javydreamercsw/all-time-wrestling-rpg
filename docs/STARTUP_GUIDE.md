# 🚀 Application Startup Guide

## Standard Development Startup

To run the application in **development mode** with the correct profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Why Use Dev Profile?

- Uses local database: `./data/management-db-dev.mv.db`
- Avoids conflicts with production database location
- Proper development configuration

## Alternative: Update Default Profile

If you want to avoid specifying the profile every time, you can change `application.properties`:

```properties
# Change this line:
spring.profiles.active=prod

# To this:
spring.profiles.active=dev
```

## Accessing the Application

Once started, navigate to:

```
http://localhost:8080/atw-rpg
```

## Default Login Credentials

| Username | Password  |  Role  |
|----------|-----------|--------|
| admin    | admin123  | ADMIN  |
| booker   | booker123 | BOOKER |
| player   | player123 | PLAYER |
| viewer   | viewer123 | VIEWER |

## Stopping the Application

```bash
# Stop all Spring Boot instances
pkill -f "spring-boot:run"

# Or press Ctrl+C in the terminal where it's running
```

## Desktop Integration Mode

For a more seamless experience on personal computers, you can enable **Desktop Mode**. This provides:
- **Auto-launch**: The application automatically opens your default browser to the correct URL.
- **System Tray**: An icon appears in your System Tray (Windows) or Menu Bar (macOS) for background management.
- **Dock Branding**: The application uses the custom ATW logo in your Dock/Taskbar.
- **Dynamic Port**: The app automatically finds a free port, avoiding conflicts.

### Running in Desktop Mode

```bash
./mvnw spring-boot:run -Pdesktop
```

---

## Alternative Distribution Formats

If you are not a developer, you likely want one of these pre-packaged options:

### 1. Native Installers (Recommended for Users)

Native installers bundle everything you need, including a minimal Java runtime.
- **macOS**: `.dmg` file
- **Windows**: `.msi` file
- **Linux**: `.deb` file

**How to build:**

```bash
./mvnw clean verify -Pproduction,desktop -DskipTests
```

The installer will be in `target/dist/`.

### 2. Portable ZIP (Zero-Install)

A simple ZIP file you can extract and run anywhere without administrative privileges.

**How to build:**

```bash
./mvnw clean package -Pportable -DskipTests
```

**How to run:**
1. Unzip the file in `target/`.
2. Run `start-windows.bat`, `start-macos.sh`, or `start-linux.sh`.

### 3. GraalVM Native Image

A single, high-performance binary with near-instant startup.

**How to build:**

```bash
./mvnw clean package -Pproduction,native -DskipTests
```

The binary will be available in `target/all-time-wrestling-rpg`.

---

## Troubleshooting

### Port Already in Use

In standard mode, the app uses port 8080. If it's busy, the app will fail.
**Solution**: Use **Desktop Mode** (`-Pdesktop`), which uses port 0 (dynamic allocation) to automatically find an available port.

### Database Locked

```bash
# Stop the app
pkill -f "spring-boot:run"

# Wait a moment
sleep 2

# Restart
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Clean Start

```bash
# Stop the app
pkill -f "spring-boot:run"

# Delete dev database
rm -f data/management-db-dev.mv.db data/management-db-dev.trace.db

# Restart (Flyway will recreate everything)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Quick Reference

**Start (Dev):** `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
**Start (JAR):** `java -jar target/all-time-wrestling-rpg-*.jar`
**Build Installer:** `mvn package -Pproduction,desktop -DskipTests`
**Stop:** `pkill -f "spring-boot:run"` or `Ctrl+C`
**URL:** http://localhost:8080/atw-rpg
**Login:** admin / admin123

## Configuration

The application can be configured using environment variables. These variables will populate the database settings on every startup.

> ⚠️ **Security Note:** Sensitive values (API keys, tokens, passwords) are not stored in the Docker image for security reasons. When using Docker, you **must** provide these values at runtime using the `-e` flag or an `--env-file`.

### Notion Configuration

|    Variable    |            Description            |
|----------------|-----------------------------------|
| `NOTION_TOKEN` | Your Notion API integration token |

### AI Configuration

|         Variable          |                    Description                    |                  Default                   |
|---------------------------|---------------------------------------------------|--------------------------------------------|
| `AI_TIMEOUT`              | AI request timeout in seconds                     | 300                                        |
| `AI_PROVIDER_AUTO`        | Automatically select the first available provider | true                                       |
| `AI_OPENAI_ENABLED`       | Enable OpenAI                                     | false                                      |
| `AI_OPENAI_API_KEY`       | OpenAI API Key                                    |                                            |
| `AI_OPENAI_API_URL`       | OpenAI API URL                                    | https://api.openai.com/v1/chat/completions |
| `AI_OPENAI_DEFAULT_MODEL` | Default OpenAI Model                              | gpt-3.5-turbo                              |
| `AI_OPENAI_PREMIUM_MODEL` | Premium OpenAI Model                              | gpt-4                                      |
| `AI_CLAUDE_ENABLED`       | Enable Claude                                     | false                                      |
| `AI_CLAUDE_API_KEY`       | Claude API Key                                    |                                            |
| `AI_CLAUDE_MODEL_NAME`    | Claude Model Name                                 | claude-3-haiku-20240307                    |
| `AI_GEMINI_ENABLED`       | Enable Gemini                                     | false                                      |
| `AI_GEMINI_API_KEY`       | Gemini API Key                                    |                                            |
| `AI_GEMINI_MODEL_NAME`    | Gemini Model Name                                 | gemini-2.5-flash                           |

