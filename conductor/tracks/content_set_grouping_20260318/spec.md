# Specification: Content Set Grouping

## Overview
Implement a system for grouping and toggling game content (wrestlers, staff, teams, and factions) based on their "set" (e.g., base game vs. specific extensions). This will allow users to customize their game experience by enabling or disabling entire collections of content.

## Functional Requirements
1.  **Set Property Injection:** Add a `set` field to the JSON objects for wrestlers, managers, and referees (in `wrestlers.json`) and teams/factions (in `teams.json`).
2.  **Official vs. Extension Logic:**
    *   Content belonging to the "Base Game" (official set) will be identified.
    *   Content belonging to "Extensions" will be treated as additional sets.
3.  **Hard Disable Mechanism:** When a set is disabled, all associated content MUST be completely hidden from match selection, rosters, and team lists.
4.  **Dedicated Set Management UI:** Create a dedicated "Set Management" or "Library" page where users can toggle the enablement of each identified set.
5.  **Persistence:** Use the existing `GameSetting` table to store the enabled/disabled state for each set.
6.  **Pre-defined Extension Support:** Support the inclusion of pre-defined extension packs that can be toggled on or off by the user.

## Non-Functional Requirements
*   **Performance:** Toggling a set should update the filtered view of content without noticeable delay.
*   **Scalability:** The system should handle an increasing number of sets and content objects efficiently.

## Acceptance Criteria
*   [ ] Each wrestler and team has a `set` property in their JSON definition.
*   [ ] A "Set Management" page exists and lists all unique sets found in the data files.
*   [ ] Toggling a set off immediately removes its content from the Roster and Team lists.
*   [ ] Set preferences persist across user sessions using the `GameSetting` table.
*   [ ] The system differentiates between "Base Game" and "Extension" content.

## Out of Scope
*   User-uploaded custom JSON files (to be considered in a future track).
*   Dynamic creation of new sets through the UI.