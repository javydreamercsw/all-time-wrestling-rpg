# In-Game Documentation Reorganization Plan

## Objective

Improve the organization and completeness of the in-game documentation (VitePress site). This involves restructuring the sidebar to include logical categories (such as a dedicated "Game Modes" section), integrating several unused screenshots, and creating a new page for GM Mode features.

## Key Files & Context

- **Sidebar Configuration:** `docs/site/.vitepress/config.mjs`
- **Existing Content:** `docs/site/guide/*.md`
- **Screenshots:** `docs/screenshots/*.png` (specifically identifying unused ones like `admin-gm-dashboard.png`, `mechanic-expansion-packs.png`, etc.)

## Implementation Steps

1. **Restructure Sidebar (`docs/site/.vitepress/config.mjs`)**
   * Update the `sidebar` configuration to group links logically:
     * **Overview:** Game Mechanics (`game-mechanics`), AI Features (`ai-features`)
     * **Game Modes:** Campaign Mode (`campaign`), Booker Guide (`booker`), GM Mode (`gm-mode`), Leagues (`leagues`)
     * **Dashboards & Features:** Player Dashboard (`player-dashboard`), Dashboards (`dashboards`), News (`news`), Wrestler Profile (`wrestler`), NPCs (`npc`)
     * **Management:** Entities (`entities`), Admin Tools (`admin`), User Settings (`user-settings`)
2. **Create New Documentation Page**
   * **File:** `docs/site/guide/gm-mode.md`
   * **Content:** Create a new page dedicated to General Manager features.
   * **Screenshots to Include:**
     * `admin-gm-dashboard.png` (GM Dashboard)
     * `admin-contract-management.png` (Contract Management)
     * `admin-expansion-management.png` (Expansion Management)
3. **Update Existing Documentation Pages**
   * **`docs/site/guide/game-mechanics.md`:** Add new sections for missing mechanics.
     * Include `mechanic-expansion-packs.png` (Expansion Packs)
     * Include `mechanic-wear-and-tear.png` (Wear and Tear)
     * Include `mechanic-ringside-actions.png` (Ringside Actions)
   * **`docs/site/guide/wrestler.md`:** Expand the profile documentation.
     * Include `wrestler-profile-redesign.png` or `profile-drawer.png` (Profile Details)
     * Include `wrestler-profile-relationships.png` (Relationships)
   * **`docs/site/guide/admin.md`:** Expand admin tools coverage.
     * Include `admin-wrestler-relationships.png` (Wrestler Relationships Management)

## Verification & Testing

- Ensure the syntax in `config.mjs` is valid JavaScript.
- Verify all newly referenced `.png` files exist in the public screenshots directory.
- Build the VitePress site locally (e.g., `npm run docs:build` in the `docs/site` folder) to confirm no broken links or missing assets occur during the build process.

