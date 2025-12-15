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

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| booker | booker123 | BOOKER |
| player | player123 | PLAYER |
| viewer | viewer123 | VIEWER |

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
