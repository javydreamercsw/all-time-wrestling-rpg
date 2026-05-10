# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->

## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:

```bash
git pull --rebase
bd dolt push
git push
git status  # MUST show "up to date with origin"
```

5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

<!-- END BEADS INTEGRATION -->

## Build & Test

```bash
# Start in dev mode (default goal: spring-boot:run)
./mvnw

# Run unit tests
mvn test

# Run a single test class or method
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#testMethod

# Run integration tests (IT suffix)
mvn -Pintegration-test verify

# Run end-to-end tests (E2ETest suffix, requires browser)
mvn -Pe2e verify

# Full test run with coverage report (output: target/site/jacoco/)
mvn clean verify

# Fix code formatting (must pass before commits)
mvn spotless:apply

# Build production JAR
./mvnw -Pproduction package
```

Other packaging profiles: `-Pwar` (Tomcat), `-Pdesktop` (native installers), `-Pportable` (ZIP), `-Pnative` (GraalVM).

## Architecture Overview

Spring Boot 4 monolith with a Vaadin 25 frontend, persisted to H2 (dev/test) or MySQL (production) via Flyway migrations.

**Main packages** under `com.github.javydreamercsw`:
- `base/` — cross-cutting framework: AI providers, Spring config, security (RBAC), account/ranking services, Vaadin UI utilities
- `management/` — game logic: domain entities, REST controllers, services, Vaadin views, DTOs

**Domain model highlights:**
- `Wrestler` — core entity; has tier (Rookie→Icon), fans, health, stamina, contracts, injuries, rivalries
- `Show` → `Segment` → wrestlers: a show is an ordered list of segments; each segment has a type (match, promo), participants, rules, outcome, and AI-generated narration
- `Title` / `TitleReign` — championships and lineage
- `Faction` — wrestler groups
- `Rivalry` — dynamic feuds with heat events; `DramaEvent` for random incidents
- `Campaign` — story-driven content with player choices

**Persistence:** `AbstractEntity<ID>` → `AbstractSyncableEntity<ID>` (adds Notion sync metadata). Repositories extend `JpaRepository`. Flyway migrations live in `src/main/resources/db/migration/h2/` and `.../mysql/`.

**AI narration:** pluggable providers (Claude, Gemini, OpenAI, Mock) in `base/ai/`. Provider selection is configurable; async processing inherits the Spring Security context.

**Security:** Spring Security with four roles — ADMIN, BOOKER, PLAYER, VIEWER. Account lockout after 5 failed logins (15-minute lockout).

## Conventions & Patterns

- **Code style:** Google Java Format via Spotless. Run `mvn spotless:apply` before committing; `mvn spotless:check` runs automatically at compile phase.
- **Lombok:** `@Getter`/`@Setter`/`@Builder` etc. are standard. `lombok.config` adds `@Generated` to skip coverage for generated code.
- **Tests:** unit (`*Test.java`) run with `mvn test`; integration (`*IT.java`) run with `-Pintegration-test`; E2E (`*E2ETest.java`) run with `-Pe2e`. Parallel test execution is disabled to avoid database conflicts.
- **Test base classes:** extend `AbstractMockUserIntegrationTest`, `ManagementIntegrationTest`, or `AbstractRestControllerIT` as appropriate.
- **DTOs:** use `*ResponseDTO` / `*DTO` classes for REST responses; map via MapStruct.
- **ArchUnit:** architecture rules are enforced by tests in `*ArchTests.java`; violations fail the build.
- **Flyway:** add new migrations as versioned SQL files in both `h2/` and `mysql/` directories when changing the schema.
- **Notion sync:** entities that sync from Notion extend `AbstractSyncableEntity`. See `docs/SYNC_TROUBLESHOOTING.md` for debugging.

