# All Time Wrestling RPG - Developer Security Guide

This guide provides instructions for developers on how to work with the security features of the All Time Wrestling RPG application.

## Table of Contents

- [Security Architecture Overview](#security-architecture-overview)
- [Securing Views](#securing-views)
- [Securing Service Methods](#securing-service-methods)
- [Ownership-Based Security for Players](#ownership-based-security-for-players)
- [Role-Based UI Components](#role-based-ui-components)
- [Campaign System Scripting](#campaign-system-scripting)

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

## Campaign System Scripting

The solo campaign uses dynamic scripting for ability cards (Ally, Valet, Face/Heel cards). For a detailed guide on available methods and script syntax, refer to the [Campaign Scripting Guide](CAMPAIGN_SCRIPTING.md).

