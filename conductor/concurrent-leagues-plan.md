# Concurrent Leagues Refactoring & Migration Plan

## Objective
Decouple dynamic wrestler state (health, fans, injuries) from the static global `Wrestler` entity to allow the same wrestler to participate in multiple leagues concurrently without state interference. 

**Critical Constraint:** The migration must preserve all existing user data (current fans, health, injuries, alignment, etc.) by mapping the current "global" state into a "default" league context.

---

## 1. Schema Changes & The "Default League" Strategy

Currently, `wrestler` holds both identity and state. We will introduce a new entity, `wrestler_league_state`, to hold the dynamic, league-specific state for a wrestler.

### The New `wrestler_league_state` Table
This table will map a `wrestler_id` to a `league_id` and store the dynamic attributes.
*   `id` (PK)
*   `wrestler_id` (FK)
*   `league_id` (FK) - *Nullable if we want a "Global Exhibition" state, or we explicitly create a "Base Universe" league.*
*   `fans`
*   `tier`
*   `bumps`
*   `current_health`
*   `physical_condition`
*   `morale`
*   `management_stamina`
*   `alignment` (Face/Heel)
*   `faction_id` (Wrestlers can be in different factions in different leagues)
*   `manager_id`

### Updates to Related Tables
*   **`injury` table:** Add a `league_id` (FK). An injury must be scoped to the league where it occurred.
*   **`title` table:** Add a `league_id` (FK). Each league should have its own set of championships.
*   **`wrestler` table:** Once data is safely migrated, the dynamic columns listed above will be dropped.

---

## 2. Safe Data Migration Steps (Flyway)

To prevent data loss, the migration must be done in sequential phases using Flyway scripts.

### Phase 1: Schema Creation (VXXX__Create_League_State.sql)
1. Create the `wrestler_league_state` table.
2. Add `league_id` to the `injury` table.
3. Ensure a "Global Universe" or "Default League" exists in the `league` table (ID 1) to act as the receptacle for all current global state.

### Phase 2: Data Migration (VXXX__Migrate_Wrestler_State.sql)
This is the critical step. We copy the data from the `wrestler` table *before* dropping the columns.

```sql
-- 1. Create a default state for every wrestler in the "Default League" (e.g., League ID 1)
INSERT INTO wrestler_league_state 
(wrestler_id, league_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id)
SELECT 
wrestler_id, 1, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id
FROM wrestler;

-- 2. If a wrestler is currently in a LeagueRoster, copy their state to that specific league as well
INSERT INTO wrestler_league_state 
(wrestler_id, league_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id)
SELECT 
w.wrestler_id, lr.league_id, w.fans, w.tier, w.bumps, w.current_health, w.physical_condition, w.morale, w.management_stamina, w.faction_id, w.manager_id
FROM wrestler w
JOIN league_roster lr ON w.wrestler_id = lr.wrestler_id
WHERE lr.league_id != 1; -- Avoid duplicating the default league insert

-- 3. Link existing injuries to the Default League (or the league of their current roster)
UPDATE injury 
SET league_id = 1 
WHERE league_id IS NULL;
```

### Phase 3: Cleanup (VXXX__Drop_Global_State_Columns.sql)
Only after Phase 2 is verified, a subsequent script will drop the migrated columns (`fans`, `bumps`, `current_health`, etc.) from the `wrestler` table.

---

## 3. Java Entity & Service Refactoring

### Entity Updates
1.  **`Wrestler.java`**: Remove dynamic fields. Add a `@OneToMany` mapping to `LeagueWrestlerState`.
2.  **`LeagueWrestlerState.java`**: Create this new entity mapping to `wrestler_league_state`.
3.  **`Injury.java`**: Add a `@ManyToOne` mapping to `League`.

### Service Layer Updates
The most extensive code changes will be in the service layer. Currently, services fetch a `Wrestler` and modify it. 

*   `WrestlerService`: Methods like `awardFans(Long wrestlerId, int amount)` must become `awardFans(Long wrestlerId, Long leagueId, int amount)`.
*   `InjuryService`: Methods to create or heal injuries must operate within a specific `leagueId` context.
*   `SegmentService` & `ShowService`: When booking a match and applying post-match effects (wear and tear, momentum shifts), the service must pull the correct `LeagueWrestlerState` using the `Show`'s associated league.

---

## 4. UI Context Awareness

The frontend (Vaadin/React) currently assumes a single global state.

*   **Global Context Switcher:** The UI needs a persistent state (e.g., in a session or context provider) indicating the "Current Active League." 
*   **Wrestler Profile:** When viewing a wrestler profile, the UI must fetch the stats for the *Current Active League*. If no league is selected, it should default to the "Global Universe" state.
*   **Dashboards:** News, rankings, and show histories must all be filtered by the currently active league context.