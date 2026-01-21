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

## Troubleshooting

### Port 8080 Already in Use

```bash
# Find the process
lsof -i :8080

# Kill it
kill -9 <PID>
```

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

**Start:** `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
**Stop:** `pkill -f "spring-boot:run"`
**URL:** http://localhost:8080/atw-rpg
**Login:** admin / admin123

## Configuration

The application can be configured using environment variables. These variables will populate the database settings on every startup.

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
| `AI_LOCALAI_ENABLED`      | Enable LocalAI                                    | false                                      |
| `AI_LOCALAI_BASE_URL`     | LocalAI Base URL                                  | http://localhost:8088                      |
| `AI_LOCALAI_MODEL`        | LocalAI Model Name                                | llama-3.2-1b-instruct:q4_k_m               |

