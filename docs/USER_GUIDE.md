# All Time Wrestling RPG - User Guide

This guide provides an overview of the account management features in the All Time Wrestling RPG application.

## Table of Contents

- [Roles and Permissions](#roles-and-permissions)
- [Logging In and Out](#logging-in-and-out)
- [Managing Your Profile](#managing-your-profile)
- [Account Security](#account-security)
- [Account Management (for Admins)](#account-management-for-admins)

## Roles and Permissions

The application uses a role-based access control system to manage what users can see and do. There are four roles available:

- **ADMIN:** Administrators have full access to the entire system. They can manage all aspects of the game, including user accounts, system settings, and all game data.
- **BOOKER:** Bookers are responsible for the creative aspects of the game. They can create and manage shows, storylines, wrestlers, and other game content. They do not have access to system administration features like user management.
- **PLAYER:** Players are the participants in the game. They have limited access to manage their own wrestler and associated content (like decks). They can view most game data but cannot change it.
- **VIEWER:** Viewers have read-only access to the application. They can see public information like show results and wrestler profiles but cannot make any changes.

## Logging In and Out

You can log in to the application using the username and password provided by the administrator. The login form is the first screen you will see.

To log out, click on your username in the top right corner of the application and select "Log out".

## Managing Your Profile

All users can manage their own profile information and personalize their experience. To access your profile:

1. Click on the **Profile** button in the top navigation bar.
2. A drawer will slide out from the right side of the screen.

From the Profile Drawer, you can:

- **Update your email address.**
- **Change your password:** Click "Change Password" to open a dialog where you must enter your current password and your new password twice.
- **Select a Theme:** Choose your preferred visual theme (Light, Dark, Retro, Neon, High Contrast) from the dropdown. This setting is saved to your account and persists across sessions.
- **Save Changes:** Click "Save Changes" to apply your updates. The application will reload to apply the new theme.

## Account Security

### Password Requirements

- Minimum length: 8 characters
- Must contain at least one letter
- Must contain at least one number

### Account Lockout

To protect against brute-force attacks, your account will be automatically locked for 15 minutes after 5 failed login attempts. If you are locked out, you can either wait for the lockout period to expire or contact an administrator to unlock your account.

### Remember Me

The login form includes a "Remember Me" option. If you check this box, you will remain logged in for 7 days, even if you close your browser. Use this option only on trusted devices.

## Account Management (for Admins)

Users with the **ADMIN** role have access to the Account Management view, which allows them to perform the following actions:

- **View all user accounts:** See a list of all registered users, their roles, and their status.
- **Create new accounts:** Create new user accounts and assign them a role.
- **Edit existing accounts:** Change a user's username, email, roles, and enabled status.
- **Delete accounts:** Permanently remove a user account from the system.
- **Unlock accounts:** Manually unlock an account that has been locked due to failed login attempts.

