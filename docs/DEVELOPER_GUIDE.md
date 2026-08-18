# All Time Wrestling RPG - Developer Security Guide

This guide provides instructions for developers on how to work with the security features of the All Time Wrestling RPG application.

## Table of Contents

- [Security Architecture Overview](#security-architecture-overview)
- [Securing Views](#securing-views)
- [Securing Service Methods](#securing-service-methods)
- [Ownership-Based Security for Players](#ownership-based-security-for-players)
- [Role-Based UI Components](#role-based-ui-components)
- [Dynamic Scripting (Campaign & Achievements)](#dynamic-scripting-campaign--achievements)

## Security Architecture Overview

The application's security is built on **Spring Security**. It integrates with Vaadin to provide a secure user experience. The main components of the security architecture are:

- `SecurityConfig.java`: The central configuration class for Spring Security. It defines the login page, security policies (like HTTPS enforcement), and enables method-level security.
- `CustomUserDetailsService.java`: Loads user-specific data from the `AccountRepository` and converts it into a `UserDetails` object that Spring Security can use for authentication and authorization.
- `SecurityUtils.java`: A utility class with helper methods to get the current user and check their roles. This class is the primary way to interact with security information from the UI layer.
- `PermissionService.java`: A service used in `@PreAuthorize` annotations to perform complex permission checks, such as ownership.

## Securing Views

All views (classes that extend a Vaadin `Component` and are annotated with `@Route`) must be secured to prevent unauthorized access. This is done using the `jakarta.annotation.security.RolesAllowed` annotation.

To secure a new view, add the `@RolesAllowed` annotation at the class level with the roles that are permitted to access it. You can use the `RoleName` enum constants for consistency.

**Example: Securing an Admin-only view**

```java
import com.github.javydreamercsw.base.domain.account.RoleName;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed(RoleName.ADMIN)
public class AdminView extends VerticalLayout {
    // ... view content ...
}
```

You can allow multiple roles to access a view:

```java
@RolesAllowed({RoleName.ADMIN, RoleName.BOOKER})
```

## Securing Service Methods

In addition to securing views, it is crucial to secure the service layer methods to protect the application's business logic. This is achieved using Spring's method-level security with the `@PreAuthorize` annotation.

`@EnableMethodSecurity` is enabled in `SecurityConfig.java`, so you can immediately use `@PreAuthorize` in your services.

**Example: Securing a delete operation**
This method can only be executed by users with the `ADMIN` or `BOOKER` role.

```java
import org.springframework.security.access.prepost.PreAuthorize;

@Service
public class WrestlerService {

    @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
    public void delete(@NonNull Wrestler wrestler) {
        // ... deletion logic ...
    }
}
```

The expression inside `@PreAuthorize` is a SpEL (Spring Expression Language) expression. You can use `hasRole('ROLE_NAME')`, `hasAnyRole('ROLE_1', 'ROLE_2')`, and other built-in functions. Note that role names in `hasRole` expressions are automatically prefixed with `ROLE_` by Spring Security (e.g., `ADMIN` becomes `ROLE_ADMIN`). It is recommended to use the role names directly as the framework handles the prefixing.

## Ownership-Based Security for Players

For the `PLAYER` role, access is often restricted to entities they "own". For example, a player can only edit their own wrestler. This is handled by the `PermissionService`.

The `PermissionService` has an `isOwner()` method that checks if the current user is the owner of a given entity. You can use this service in `@PreAuthorize` annotations.

**Example: Securing a save operation with an ownership check**

```java
@Service
public class WrestlerService {

    @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#wrestler)")
    public Wrestler save(@NonNull Wrestler wrestler) {
        // ... save logic ...
    }
}
```

In this example:
-   `hasAnyRole('ADMIN', 'BOOKER')`: Allows admins and bookers to save any wrestler.
-   `or @permissionService.isOwner(#wrestler)`: Additionally allows the operation if the current user is the owner of the `wrestler` object being passed as an argument. The `#wrestler` syntax refers to the method parameter named `wrestler`.

## Role-Based UI Components

To provide a good user experience, UI components (like buttons and menu items) for actions a user is not authorized to perform should be hidden. The `SecurityUtils` class provides helper methods for this purpose.

**Example: Hiding a "Create" button**

```java
import com.github.javydreamercsw.base.security.SecurityUtils;

// In a Vaadin View
Button createButton = new Button("Create Wrestler");
createButton.setVisible(SecurityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER));
```

The `SecurityUtils` class provides the following convenient methods:
-   `hasAnyRole(RoleName... roles)`: Checks if the current user has any of the specified roles.
-   `canCreate()`: Checks if the user has `ADMIN`, `BOOKER`, or `PLAYER` role.
-   `canEdit(Object entity)`: Checks if the user can edit a specific entity (including ownership checks).
-   `canDelete()`: Checks if the user has `ADMIN` or `BOOKER` role.
-   `isAdmin()`, `isBooker()`, `isPlayer()`, `isViewer()`: Convenient booleans to check for a specific role.

Always use these `SecurityUtils` methods to control the visibility of UI components based on the current user's permissions. This ensures that the UI accurately reflects what the user is allowed to do, as enforced by the backend's method-level security.

## Dynamic Scripting (Campaign & Achievements)

The solo campaign uses dynamic scripting for ability cards (Ally, Valet, Face/Heel cards), chapter progression criteria, and status card trigger conditions. For a detailed guide on available methods and script syntax, refer to the [Campaign Scripting Guide](CAMPAIGN_SCRIPTING.md).

Achievement unlock conditions use the same trusted-content-only Groovy pattern: an optional `unlockCondition` script on each `achievements.json` entry, evaluated by `AchievementScriptService` (fail-closed — a broken script is logged and treated as "not unlocked", never propagated). As with campaign scripts, this executes arbitrary Groovy against shipped JSON content, not user-supplied input — treat new script sources the same way when reviewing for security implications. See [Content Management Guide § Scripted unlock conditions](CONTENT_GUIDE.md#scripted-unlock-conditions) for the field reference and available variables.

## Tournament Format System

The tournament engine is built around the **Strategy + Adapter** pattern. This section explains how it works and how to add a new format.

### Architecture

```
TournamentFormat (interface)          ← strategy: one @Component per format
  ├── SingleEliminationFormat         ← TREE render mode (default)
  └── RoundRobinFormat                ← ROUND_ROBIN_GRID render mode

TournamentBracketModel (interface)    ← view-layer contract
  ├── TournamentDTOAdapter            ← wraps campaign TournamentDTO
  └── TournamentEntityAdapter         ← wraps domain Tournament entity

TournamentBracketComponent            ← renders from TournamentBracketModel,
                                         dispatches on renderMode()
```

`TournamentService` autowires `List<TournamentFormat>` — any `@Component` that implements the interface is auto-registered with no additional wiring.

### Adding a New Format

1. **Create the class** in `management.service.tournament`:

```java
@Component
public class DoubleEliminationFormat implements TournamentFormat {

    public static final String FORMAT_ID = "DOUBLE_ELIMINATION";

    @Override public String getFormatId()    { return FORMAT_ID; }
    @Override public String getDisplayName() { return "Double Elimination"; }
    @Override public int getMinEntrants()    { return 4; }
    @Override public int getMaxEntrants()    { return 32; }

    @Override
    public List<TournamentRound> generateBracket(Tournament t, TournamentFormatContext ctx) {
        // persist rounds and matches via ctx.getRoundRepository() / ctx.getMatchRepository()
    }

    @Override
    public List<TournamentMatch> advanceRound(Tournament t, TournamentFormatContext ctx) {
        // called after each round completes; return new matches or empty list when done
    }

    @Override
    public boolean isComplete(Tournament t) {
        // return true when a champion is determined
    }

    // Optional: override renderMode() if TREE doesn't fit your format
    // @Override public RenderMode renderMode() { return RenderMode.ROUND_ROBIN_GRID; }
}
```

2. **No other wiring needed.** Spring picks up the `@Component`, `TournamentService` includes it in `getAvailableFormats()`, and the creation wizard offers it automatically.

3. **If your format needs a new visual layout**, add a new `RenderMode` constant to `TournamentFormat.RenderMode` and add the corresponding `build*` method to `TournamentBracketComponent`.

### Render Modes

|        Mode        |               Used by               |                                Layout                                 |
|--------------------|-------------------------------------|-----------------------------------------------------------------------|
| `TREE`             | `SingleEliminationFormat` (default) | Column per round; matches narrow toward the Final                     |
| `ROUND_ROBIN_GRID` | `RoundRobinFormat`                  | Rounds listed vertically; each round's matches displayed horizontally |

### Campaign vs Booker Context

`TournamentBracketComponent` accepts any `TournamentBracketModel`:

```java
// Campaign (TournamentDTO from CampaignTournamentService)
new TournamentBracketComponent(dto)                        // convenience overload
new TournamentBracketComponent(new TournamentDTOAdapter(dto))

// Booker detail view (domain entity, must be graph-initialized)
Tournament t = tournamentService.findByIdWithDetails(id).orElseThrow();
new TournamentBracketComponent(new TournamentEntityAdapter(t, tournamentService.getAvailableFormats()))
```

`TournamentEntityAdapter` resolves `renderMode()` at construction time by looking up the format by ID from the provided list. `TournamentDTOAdapter` always returns `TREE` because the campaign subsystem currently only generates single-elimination brackets.

### Lazy Loading

All `Tournament` associations (`entries`, `rounds`, rounds' `matches`, matches' `entrant1`/`entrant2`/`winner`, entries' `wrestler`) are `FetchType.LAZY`. The application sets `spring.jpa.open-in-view=false`.

**Always use `TournamentService.findByIdWithDetails(id)`** when loading a tournament for display outside a transaction (views, tests). This method initializes the full object graph within a `@Transactional` boundary. Using `findById` for display will throw `LazyInitializationException`.

