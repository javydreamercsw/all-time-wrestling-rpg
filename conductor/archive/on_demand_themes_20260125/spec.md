# Specification: On-demand Theme Changes

## Overview

This feature allows users to customize their visual experience by selecting from various color themes (Light, Dark, and predefined palettes) directly from their user profile settings. These preferences are saved to the user's account in the database and applied upon a page reload.

## Functional Requirements

- **Theme Selection UI:** Add a theme selection interface within the User Profile settings.
- **Predefined Themes:** Support a variety of themes, including:
  - Light Mode (Default)
  - Dark Mode
  - Predefined Palettes: Retro, High Contrast, Neon.
- **Persistence:** User theme preferences must be stored in the database and linked to the user's account.
- **Default Configuration:** Administrators can configure the default application theme for users who have not set a personal preference.
- **Application of Theme:** When a user saves their selection, the application will perform a full reload to apply the new theme.

## Non-Functional Requirements

- **Stability:** The theme switching mechanism should not interfere with existing UI components or state management.
- **Maintainability:** Themes should be managed through standard Vaadin/CSS variables where possible to facilitate adding new themes in the future.

## Acceptance Criteria

- [ ] Users can see a list of available themes in their profile settings.
- [ ] Selecting a theme and saving triggers a page reload.
- [ ] After reload, the selected theme is correctly applied to the entire application.
- [ ] Theme preferences persist across sessions and devices for the logged-in user.
- [ ] Administrators can successfully change the global default theme.

## Out of Scope

- Real-time (instant) theme switching without page reload.
- User-created custom color palettes (only predefined themes are supported).

