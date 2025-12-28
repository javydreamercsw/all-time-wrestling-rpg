# Spring Security Implementation Plan

**Date:** December 14, 2025
**Status:** In Progress

## Decisions Made

### Core Decisions
- ✅ **Entity Name:** `Account` (avoids Spring Security naming conflicts)
- ✅ **Roles:** ADMIN, BOOKER, PLAYER, VIEWER
- ✅ **Role Assignment:** Single role per user (with Many-to-Many support for future flexibility)
- ✅ **Session Timeout:** 30 minutes
- ✅ **Remember Me:** Enabled
- ✅ **HTTPS:** Forced in production
- ✅ **Password Requirements:** Basic (length minimum)
- ✅ **Login Branding:** Placeholder logo for now
- ✅ **Forgot Password:** Include if not too complicated
- ✅ **Account Lockout:** Use Spring Security's built-in features
- ✅ **Button Visibility:** Hide (not disable) for unauthorized actions
- ✅ **Security Utils:** Create helper class
- ✅ **Method Security:** Enabled with `@EnableMethodSecurity`

### Role Access Matrix

| Role | Access Level | Description |
|------|-------------|-------------|
| ADMIN | Full System Access | All views, all operations, account management |
| BOOKER | Management Access | All views except Admin/Sync, can create/edit/delete content |
| PLAYER | Limited Management | Can manage own content, view most data, no planning |
| VIEWER | Read-Only | View-only access, no Admin/Sync/Planning views, no create/edit/delete |

### View Access Control

| View | ADMIN | BOOKER | PLAYER | VIEWER | Notes |
|------|-------|--------|--------|--------|-------|
| AdminView | ✅ | ❌ | ❌ | ❌ | Admin only |
| NotionSyncView | ✅ | ❌ | ❌ | ❌ | Admin only |
| ShowPlanningView | ✅ | ✅ | ❌ | ❌ | No VIEWER/PLAYER |
| AccountListView | ✅ | ❌ | ❌ | ❌ | Admin only |
| WrestlerListView | ✅ | ✅ | ✅ | ✅ | All roles |
| WrestlerProfileView | ✅ | ✅ | ✅ | ✅ | All roles |
| WrestlerRankingsView | ✅ | ✅ | ✅ | ✅ | All roles |
| ShowListView | ✅ | ✅ | ✅ | ✅ | All roles |
| ShowDetailView | ✅ | ✅ | ✅ | ✅ | All roles |
| ShowCalendarView | ✅ | ✅ | ✅ | ✅ | All roles |
| ShowTemplateListView | ✅ | ✅ | ✅ | ✅ | All roles |
| TeamsView | ✅ | ✅ | ✅ | ✅ | All roles |
| FactionListView | ✅ | ✅ | ✅ | ✅ | All roles |
| FactionRivalryListView | ✅ | ✅ | ✅ | ✅ | All roles |
| TitleListView | ✅ | ✅ | ✅ | ✅ | All roles |
| SeasonListView | ✅ | ✅ | ✅ | ✅ | All roles |
| CardListView | ✅ | ✅ | ✅ | ✅ | All roles |
| DeckListView | ✅ | ✅ | ✅ | ✅ | All roles |
| NpcListView | ✅ | ✅ | ✅ | ✅ | All roles |
| SegmentTypeListView | ✅ | ✅ | ✅ | ✅ | All roles |
| InboxView | ✅ | ✅ | ✅ | ✅ | All roles |
| ProfileView | ✅ | ✅ | ✅ | ✅ | Own profile |

### Button-Level Permissions

| Action | ADMIN | BOOKER | PLAYER | VIEWER |
|--------|-------|--------|--------|--------|
| Create | ✅ | ✅ | ✅* | ❌ |
| Edit | ✅ | ✅ | ✅* | ❌ |
| Delete | ✅ | ✅ | ❌ | ❌ |
| Export | ✅ | ✅ | ✅ | ✅ |
| Sync | ✅ | ❌ | ❌ | ❌ |
| Admin Functions | ✅ | ❌ | ❌ | ❌ |

*PLAYER can only edit their own content

## Implementation Phases

### Phase 1: Foundation & Security Setup ✅ (COMMITTED - December 14, 2025)
**Goal:** Get basic authentication working

1. ✅ Add Spring Security dependency to pom.xml
2. ✅ Create domain entities:
- `Account.java` entity
- `Role.java` entity (enum-based)
- `AccountRepository.java`
- `RoleRepository.java`
3. ✅ Create Flyway migration:
- `account` table
- `role` table
- `account_roles` join table
- Seed data: roles and default admin
4. ✅ Create security infrastructure:
- `SecurityConfig.java` (extends VaadinWebSecurity)
- `CustomUserDetailsService.java`
- `SecurityUtils.java` helper class
5. ✅ Create LoginView:
- Username/password form
- Remember me checkbox (via Spring Security config)
- Placeholder logo (emoji 🤼)
- Error handling
6. ✅ Update MainLayout with user info and logout button
7. ✅ Add security annotations to all views
8. ✅ Fix MainView final modifier issue
9. ✅ Generate correct BCrypt password hashes
10. ✅ Test basic login/logout functionality

**Deliverable:** ✅ Can log in with default admin account - COMPLETE!
**Status:** Committed to feature branch

---

### Phase 2: View-Level Access Control ✅ (COMMITTED - December 14, 2025)
**Goal:** Restrict view access based on roles and filter menu items

7. ✅ Add role annotations to all views using RoleName constants
8. ✅ Enhanced MenuItem class with role requirements
9. ✅ Updated MenuService with filtering logic
10. ✅ Implemented recursive menu filtering
11. ✅ Created BookerView and PlayerView (placeholders)
12. ✅ Restricted Entities menu to ADMIN only
13. ✅ Test role-based menu visibility

**Deliverable:** ✅ Different roles see different menu items - COMMITTED!

---

### Phase 3: Method-Level Security ✅ (COMPLETED - December 21, 2025)
**Goal:** Secure backend services

11. ✅ Enable method security in SecurityConfig
12. ✅ Add `@PreAuthorize` to service layer methods:
	- Create operations: ADMIN, BOOKER, PLAYER (own content)
	- Update operations: ADMIN, BOOKER, PLAYER (own content)
	- Delete operations: ADMIN, BOOKER
	- Read operations: All authenticated
13. ✅ Implement ownership checks for PLAYER role
14. ✅ Test method security with different roles

**Deliverable:** Backend enforces role permissions - COMPLETE!
---

### Phase 4: UI Component Security ✅ (COMPLETED - December 21, 2025)
**Goal:** Hide/show UI elements based on permissions

15. ✅ Update all list views to hide create/edit/delete buttons:
	- Use `SecurityUtils.hasAnyRole()` checks
	- VIEWER sees no action buttons
	- PLAYER sees limited buttons
16. ✅ Update all form views to check permissions before save
17. ✅ Add helper methods to SecurityUtils:
	- `canCreate()`
	- `canEdit()`
	- `canDelete()`
	- `isAdmin()`
	- `isBooker()`
	- `isPlayer()`
	- `isViewer()`
18. ✅ Test UI adapts to user role

**Deliverable:** UI shows only permitted actions - COMPLETE!
---

### Phase 5: Account Management 👥 ✅ COMPLETE!

**Deliverable:** Admins can manage accounts, users can update profile, and core account management components are implemented.

---

### Phase 6: Booker and Player View Design 🎨 ✅ COMPLETE!

**Deliverable:** A clear design and implementation plan for the Booker and Player views.

---

### Phase 7: Password Management 🔐 ✅ COMPLETE!
**Goal:** Password reset and security features

28. ✅ Implement password strength validation
29. ✅ Add "Change Password" functionality
30. ✅ Implement "Forgot Password" flow:
	- ✅ Password reset token generation
	- ✅ Token storage (in database)
	- ✅ Reset password view
	- (Email integration can come later)
31. ✅ Configure account lockout after failed login attempts
	- ✅ Updated `Account` entity (fields `failedLoginAttempts`, `lockedUntil`, `accountNonLocked` already present in `V20__Create_Account_Tables.sql`)
	- ✅ Updated `AccountService` (password validation logic for update, successful)
	- ✅ Updated `CustomUserDetailsService` (implemented `recordFailedLoginAttempt`, `recordSuccessfulLogin`, and lockout check in `loadUserByUsername`)
	- ✅ Updated `SecurityConfig` (configured to use the handlers for login/logout)
	- ✅ Flyway migration confirmed to be in place (`V20__Create_Account_Tables.sql` has the necessary columns)
32. ✅ Test password features

**Deliverable:** Password management and security features working

---

### Phase 8: Testing & Documentation 🧪
**Goal:** Comprehensive test coverage

33. ✅ Create security test fixtures:
	- Test accounts for each role
	- `@WithMockUser` test utilities
34. ✅ Write security tests:
	- View access tests (✅)
	- Method security tests (✅)
	- Login/logout tests (✅)
	- Account management tests (✅)
35. ✅ Update documentation:
	- README security section
	- User guide for account management
	- Developer guide for adding secured views
36. ✅ Test complete flows with all roles

**Deliverable:** Fully tested and documented security system

---

### Phase 9: Production Hardening 🛡️
**Goal:** Production-ready security

37. 🔨 Configure HTTPS enforcement for production profile
38. 🔨 Set secure session cookies
39. 🔨 Configure CORS if needed
40. 🔨 Review and test security headers

**Deliverable:** Production-ready security configuration

---

## Technical Specifications

### Ownership Model

For the `PLAYER` role, a concept of "ownership" is used to restrict access to specific data. This ensures that a player can only manage the entities that belong to them.

-   **Account-Wrestler Link:** The core of the ownership model is the link between an `Account` and a `Wrestler`. Each `Account` with the `PLAYER` role is associated with a single `Wrestler` record. This is established via the `account_id` foreign key in the `wrestler` table (see migration `V22__Add_Account_To_Wrestler.sql`).
-   **Ownership Check:** When a `PLAYER` attempts to modify an entity (like a `Wrestler`, `Deck`, or `DeckCard`), the `PermissionService` checks if the currently authenticated user's account is the one linked to the `Wrestler` associated with that entity.
-   **Implementation:** This check is performed in the `PermissionService.isOwner()` method, which is called from `@PreAuthorize` annotations in the service layer. For example, in `WrestlerService`:
	```java
	@PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#wrestler)")
	public Wrestler save(@NonNull Wrestler wrestler) {
		// ...
	}
	```
-   **Scope:** Ownership rules currently apply to `Wrestler`, `Deck`, `DeckCard`, and `InboxItem` entities.

This model allows `PLAYER`s to have limited control over their own game assets without being able to interfere with other players' or the main game state managed by `BOOKER`s and `ADMIN`s.

### Account Entity Structure
```java
@Entity
@Table(name = "account")
class Account {
	@Id @GeneratedValue Long id;
	@Column(unique = true, nullable = false) String username;
	@Column(nullable = false) String password; // BCrypt encoded
	@Column(unique = true, nullable = false) String email;
	@ManyToMany Set<Role> roles;
	boolean enabled;
	boolean accountNonExpired;
	boolean accountNonLocked;
	boolean credentialsNonExpired;
	int failedLoginAttempts;
	LocalDateTime lastLogin;
	LocalDateTime createdDate;
	LocalDateTime updatedDate;
}
```

### Role Entity Structure
```java
@Entity
@Table(name = "role")
class Role {
	@Id @GeneratedValue Long id;
	@Enumerated(EnumType.STRING)
	@Column(unique = true, nullable = false)
	RoleName name; // ADMIN, BOOKER, PLAYER, VIEWER
	String description;
}

enum RoleName {
	ADMIN, BOOKER, PLAYER, VIEWER
}
```

### Password Requirements
- Minimum length: 8 characters
- Must contain at least one letter
- Must contain at least one number
- (Can enhance later with special characters, etc.)

### Session Configuration
- Timeout: 30 minutes of inactivity
- Remember Me: 7 days
- Concurrent Sessions: 1 per user (configurable)

### Account Lockout Policy
- Lock after: 5 failed attempts
- Lockout duration: 15 minutes
- Admin can unlock accounts

---

## Default Accounts (Created via Migration)

| Username | Password | Role | Email |
|----------|----------|------|-------|
| admin | admin123 | ADMIN | admin@atwrpg.local |
| booker | booker123 | BOOKER | booker@atwrpg.local |
| player | player123 | PLAYER | player@atwrpg.local |
| viewer | viewer123 | VIEWER | viewer@atwrpg.local |

**Note:** These should be changed on first login in production!

---

## Files to Create

### Domain Layer
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/Account.java`
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/AccountRepository.java`
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/Role.java`
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/RoleRepository.java`
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/RoleName.java`
- [X] `src/main/java/com/github/javydreamercsw/management/domain/account/PasswordResetToken.java`

### Security Layer
- [X] `src/main/java/com/github/javydreamercsw/base/security/SecurityConfig.java`
- [X] `src/main/java/com/github/javydreamercsw/base/security/CustomUserDetailsService.java`
- [X] `src/main/java/com/github/javydreamercsw/base/security/SecurityUtils.java`
- [X] `src/main/java/com/github/javydreamercsw/base/security/CustomUserDetails.java`
- [X] `src/main/java/com/github/javydreamercsw/base/security/PasswordValidator.java`

### Service Layer
- [X] `src/main/java/com/github/javydreamercsw/management/service/PasswordResetService.java`

### UI Layer
- [X] `src/main/java/com/github/javydreamercsw/base/ui/view/LoginView.java`
- [X] `src/main/java/com/github/javydreamercsw/base/ui/view/AccessDeniedView.java`
- [X] `src/main/java/com/github/javydreamercsw/management/ui/view/account/ChangePasswordDialog.java`
- [X] `src/main/java/com/github/javydreamercsw/management/ui/view/ForgotPasswordView.java`
- [X] `src/main/java/com/github/javydreamercsw/management/ui/view/ResetPasswordView.java`

### Database Migrations
- [X] `src/main/resources/db/migration/V{next}_create_account_tables.sql`
- [X] `src/main/resources/db/migration/V{next}_insert_default_roles.sql`
- [X] `src/main/resources/db/migration/V{next}_insert_default_accounts.sql`
- [X] `src/main/resources/db/migration/V{next}_create_password_reset_token_table.sql`

### Test Layer
- [ ] `src/test/java/com/github/javydreamercsw/base/security/SecurityConfigTest.java`
- [ ] `src/test/java/com/github/javydreamercsw/base/security/SecurityUtilsTest.java`
- [X] `src/test/java/com/github/javydreamercsw/management/service/AccountServiceTest.java`
- [X] `src/test/java/com/github/javydreamercsw/management/ui/view/LoginViewTest.java`
- [X] `src/test/java/com/github/javydreamercsw/base/security/TestSecurityConfig.java`
- [ ] `src/test/resources/test-accounts.sql`

---

## Progress Tracking

**Current Phase:** Phase 8 - Testing & Documentation
**Started:** December 27, 2025
**Phase 7 Completed:** December 26, 2025
**Phase 6 Completed:** December 26, 2025
**Phase 5 Completed:** December 26, 2025
**Phase 4 Completed:** December 21, 2025
**Phase 3 Completed:** December 21, 2025
**Phase 2 Completed:** December 14, 2025
**Phase 1 Committed:** December 14, 2025
**Target Completion:** TBD

### Completed Tasks
- ✅ Requirements gathering and planning
- ✅ Architecture decisions
- ✅ Spring Security dependencies added
- ✅ Account and Role domain entities created
- ✅ Flyway migrations created (V20, V21)
- ✅ Security infrastructure (SecurityConfig, CustomUserDetailsService, SecurityUtils, CustomUserDetails)
- ✅ LoginView with placeholder logo
- ✅ AccessDeniedView
- ✅ MainLayout updated with user info and logout
- ✅ All views annotated with security annotations
- ✅ MainView final modifier removed
- ✅ BCrypt password hashes corrected
- ✅ Phase 1 tested and working
- ✅ **Phase 1 committed to feature branch**
- ✅ MenuItem enhanced with role requirements
- ✅ MenuService updated with filtering logic
- ✅ Menu filtering tested with all 4 roles
- ✅ **Phase 2 complete - menu filtering working**
- ✅ Enable method security in SecurityConfig
- ✅ Add `@PreAuthorize` to service layer methods (CardService: ✅; CardSetService: ✅; DeckService: ✅; DeckCardService: ✅; DramaEventService: ✅; FactionService: ✅; FactionRivalryService: ✅; FeudResolutionService: ✅; InboxService: ✅; InjuryService: ✅; InjuryTypeService: ✅; SegmentAdjudicationService: ✅; NpcService: ✅; PerformanceMonitoringService: ✅; RankingService: ✅; TierBoundaryService: ✅; TierRecalculationService: ✅; TierRecalculationScheduler: ✅; SeasonService: ✅; SegmentService: ✅; SegmentOutcomeService: ✅; SegmentRuleService: ✅; SegmentTypeService: ✅; ShowService: ✅; PromoBookingService: ✅; ShowBookingService: ✅; ShowPlanningService: ✅; ShowPlanningAiService: ✅; ShowTemplateService: ✅; ShowTypeService: ✅; NotionSyncService: ✅; NotionSyncScheduler: ✅; BackupService: ✅; TeamService: ✅; TitleService: ✅; WrestlerService: ✅)
- ✅ Implement ownership checks for PLAYER role (WrestlerService: ✅, DeckService: ✅, DeckCardService: ✅, InboxService: ✅)
- ✅ Test method-level security
- ✅ Add helper methods to SecurityUtils: canCreate(), canEdit(), canDelete(), isAdmin(), isBooker(), isPlayer(), isViewer().
- ✅ Update all list views to hide create/edit/delete buttons:
	- Use `SecurityUtils.hasAnyRole()` checks
	- VIEWER sees no action buttons
	- PLAYER sees limited buttons
- ✅ Update all form views to check permissions before save
- ✅ Test UI adapts to user role
- ✅ Created `AccountService.java`
- ✅ Created `AccountListView.java` (admin only)
- ✅ Created `AccountFormDialog.java`
- ✅ Created `ProfileView.java` (all users)
- ✅ Added account management to AdminView
- ✅ Tested account CRUD operations

---

## Notes & Considerations

- **Development Mode:** Consider adding `spring.security.enabled=false` property for easier development
- **Testing:** Use H2 in-memory database for security tests
- **Vaadin Integration:** Use Vaadin's `VaadinWebSecurity` for better integration
- **CSRF:** Vaadin handles CSRF automatically for most cases
- **Remember Me:** Store tokens in database for better security
- **Future Enhancements:**
- OAuth2/LDAP integration
- Two-factor authentication
- Email notifications for account events
- Audit logging (who did what when)
- Session management UI
- IP-based restrictions

---

## Success Criteria

- ✅ Users must log in to access the application
- ✅ Different roles see different views and capabilities
- ✅ VIEWER role is truly read-only
- ✅ Backend validates all operations regardless of UI
- ✅ Admins can manage user accounts
- ✅ Users can update their own profiles and passwords
- ✅ Account lockout works after failed attempts
- ✅ Remember me functionality works
- ✅ All security features are tested
- ✅ Production uses HTTPS and secure configuration
