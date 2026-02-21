# Track Specification: Add Season Summary to Player Dashboard

## Overview

This feature introduces a "Season Summary" component to the Player Dashboard. It provides players with a quick overview of their performance in the current and past seasons, focusing on key metrics like win/loss records and fan growth. This data will be accessible via a sidebar element on the dashboard, with the ability to toggle between different seasons.

## Functional Requirements

- **Sidebar Integration:** A new "Season Summary" section will be added to the Player Dashboard's sidebar or drawer.
- **Metric Display:**
  - **Season Record:** Display the player's current win/loss/draw record for the selected season.
  - **Fan Growth:** Visualize fan acquisition/loss during the season using progress bars or similar visual indicators.
- **Historical Toggle:**
  - A dropdown menu to select the season (defaulting to the current active season).
  - Updating the displayed metrics dynamically based on the selected season.
- **Visual Enhancements:**
  - Use **Progress Bars** to represent fan growth relative to tier boundaries or season goals.
  - Use **Icons/Badges** to represent specific season-based achievements or milestones.

## Non-Functional Requirements

- **Performance:** Switching between seasons should be near-instant, leveraging efficient database queries or caching.
- **Responsive Design:** The sidebar element should scale correctly for different screen sizes, especially on mobile.

## Acceptance Criteria

- [ ] A "Season Summary" widget is visible in the Player Dashboard sidebar.
- [ ] The widget correctly displays the Win/Loss/Draw record for the current season by default.
- [ ] Fan growth is visually represented with a progress bar.
- [ ] A dropdown allows the player to select past seasons and view their respective summaries.
- [ ] UI remains responsive and follows the Lumo theme guidelines.

## Out of Scope

- Detailed match-by-match breakdowns (available in other views).
- Complex analytics or multi-season trend charts.
- Booker-specific views or administrative season management tools.

