# Universe-Based State Isolation Plan

## Objective

Decouple dynamic wrestler state (health, fans, injuries) from the static global `Wrestler` template. Introduce the **Universe** as a self-contained "playthrough instance" that can host different game modes (Leagues, Campaigns) without state interference.

**Critical Constraint:** Preserve all existing user data by migrating current global stats into a "Default Universe".

---

## 1. Core Architecture: The Universe Concept

### The `Universe` Entity

A container representing a specific timeline or game instance.
*   `id` (PK)
*   `name` (e.g., "Default Universe", "Monday Night League")
*   `type` (GLOBAL, LEAGUE, CAMPAIGN)
*   `creation_date`

### The `WrestlerState` Entity (Isolated Progress)

Stores dynamic attributes for a wrestler *within* a specific `Universe`.
*   `id` (PK)
*   `wrestler_id` (FK to template)
*   `universe_id` (FK)
*   `fans`, `tier`, `bumps`, `current_health`, `physical_condition`, `morale`, `management_stamina`, `alignment`, `faction_id`, `manager_id`

### Entity Scoping (Universe-Specific)

The following entities are now scoped to a `Universe` instead of being global or linked to a `League`:
*   `Injury`: Scoped via `universe_id`.
*   `Title`: Scoped via `universe_id`.
*   `Faction`: Scoped via `universe_id`.
*   `Team`: Scoped via `universe_id`.
*   `DramaEvent`: Scoped via `universe_id`.
*   `League`: Links to a `Universe`.
*   `Campaign`: Links to a `Universe`.

---

## 2. Implementation Phases

### Phase 1: Database & Schema

1. **Flyway Script:** Create `universe` and `wrestler_state` tables.
2. **Migration:**
   * Create a "Default Universe" (ID 1).
   * Insert `WrestlerState` records for all wrestlers using their current global stats.
   * Update all existing Injuries, Titles, Factions, Teams, and DramaEvents to link to Universe 1.
3. **Cleanup:** Drop dynamic columns from the `wrestler` table after verification.

### Phase 2: Java Refactoring

1. **Domain:** Create `Universe.java` and `WrestlerState.java`.
2. **Repositories:** Create `UniverseRepository` and `WrestlerStateRepository`.
3. **Services:**
   * Rename `LeagueContextService` $\rightarrow$ `UniverseContextService`.
   * Refactor `WrestlerService` to operate on `universeId`.
   * Refactor `InjuryService`, `TitleService`, `SegmentAdjudicationService` to be universe-aware.
4. **Controllers:** Update REST endpoints to accept `universeId` (defaulting to 1).

### Phase 3: UI & Documentation

1. **UI Layout:** Replace "League Selector" with "Universe Selector" in the main drawer.
2. **Context:** Ensure all views (Profile, Rankings, Dashboards) pull data from the active `Universe`.
3. **Docs:** Update VitePress guides and `README.md` to reflect the Universe/Wrestler Template architecture.

---

## 3. Verification Strategy

- **Isolation Test:** Modify a wrestler's fans in Universe A and verify they remain unchanged in Universe B.
- **Migration Test:** Verify that after the update, all wrestlers in the "Global Universe" (ID 1) have the same stats they had before the refactor.
- **Build Check:** Ensure no lingering references to old `WrestlerState` or removed `Wrestler` fields.

