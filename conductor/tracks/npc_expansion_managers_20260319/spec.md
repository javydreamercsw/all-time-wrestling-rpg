# Specification: NPC Expansion and Manager Support

## Overview

Expand the Expansion Pack system to include NPCs and allow managers to be assigned to Teams and Factions. This ensures that NPCs like MVP and Colonel Mustafa are correctly filtered and that managerial roles are properly integrated into the game's structures.

## Functional Requirements

1. **NPC Expansion Support:**
   * Add a `set` field to the JSON objects in `npcs.json` (consistent with `wrestlers.json`).
   * Implement expansion-aware filtering in `NPCService` using the "Hard Disable" logic.
   * Assign **MVP** to the `HURT_BUSINESS` expansion and **Colonel Mustafa** to the `RUMBLE` expansion.
2. **Manager Assignment for Teams and Factions:**
   * Add an optional `manager` field to `teams.json` and `factions.json` (referencing the NPC by name).
   * Support one assigned manager per Tag Team and one primary manager per Faction.
   * **Optional Component Logic:** If a manager's expansion is disabled, the associated Team or Faction remains enabled (provided its wrestlers are enabled), but the manager is hidden from the presentation.
3. **Database Persistence:**
   * Add an `expansion_code` column to the `npc` table.
   * Add a `manager_id` foreign key (referencing `npc_id`) to the `team` and `faction` tables.
4. **UI Enhancements:**
   * Update Team and Faction management dialogs to include a manager selection dropdown (filtered by expansion).
   * Display the assigned manager on the Roster, Team list, and Faction views.
   * Enhance the Faction Management view to allow assigning/editing the primary manager.

## Acceptance Criteria

* [ ] NPCs are filtered across all views based on expansion enablement.
* [ ] MVP is hidden when `HURT_BUSINESS` is disabled; Colonel Mustafa is hidden when `RUMBLE` is disabled.
* [ ] Managers can be assigned/changed for Teams and Factions through the administrative UI.
* [ ] Disabling a manager's expansion correctly removes them from the Team/Faction display without disabling the Team/Faction itself.
* [ ] Database migrations successfully add the required columns and relationships.

## Out of Scope

* Support for multiple managers in a single Team or Faction.
* Manager-specific mechanical bonuses (to be handled in a future "Manager Impact" track).

