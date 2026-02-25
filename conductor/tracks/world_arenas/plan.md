# Global Tour & Dynamic Arenas - Plan

## Phase 1: Entity & Data Modeling

- [x] Create `Location` entity (name, description, future-themed traits like "environmentTags").
- [x] Create `Arena` entity (name, description, `capacity`, `alignmentBias`, `imageUrl`, link to `Location`).
- [x] Add `hometown` or `heritageTag` (String) to the `Wrestler` entity.
- [x] Link `Show` to `Arena` (ManyToOne relationship).

## Phase 2: JSON Data Integration & Migrations

- [x] Create `locations.json` and `arenas.json` with initial future-themed data.
- [x] Update `DataInitializer` to sync these files into the database.
- [x] Create Flyway migrations for `Location` and `Arena` tables, and for adding `arena_id` to `Show` and `heritage_tag` to `Wrestler`.

## Phase 3: Service Layer & Business Logic

- [x] Create `LocationService` (CRUD).
- [x] Create `ArenaService` (CRUD, including image generation/upload logic integration).
- [x] Update `SegmentAdjudicationService` to apply:
  - [x] **Alignment Bias:** Modify Fan Gain based on the winner's alignment vs. Arena bias (Face-Favorable, Heel-Favorable, Anarchic).
  - [x] **Capacity Capping:** Ensure Fan Gain doesn't exceed the venue's potential.
  - [x] **Heritage Bonus:** Apply bonuses if a wrestler performs in their `heritageTag` region/location.
- [x] Update AI Narration prompts to include Arena Alignment Bias for dynamic crowd descriptions.

## Phase 4: UI Enhancements & CRUD Views

- [x] Create **CRUD views** for `Location` (simple list/form).
- [x] Create **CRUD views** for `Arena` (list/form, including image upload/AI generation component).
- [x] Update `ShowPlanningView` to allow Bookers to select an Arena (ComboBox).
- [x] Display Arena details (Capacity/Bias/Image) in `ShowDetailView`.
- [x] Display wrestler `heritageTag` in `WrestlerProfileView`.

## Phase 5: Testing

- [~] **Unit Tests:** Cover `LocationService`, `ArenaService`, `SegmentAdjudicationService` (new logic), `ShowService` (Arena linking).
- [ ] **Integration Tests:** Verify data persistence, JSON initialization, and complex service interactions.
- [ ] **UI Tests:** Cover `Location` and `Arena` CRUD forms, `ShowPlanningView` changes, `ShowDetailView` display.
- [ ] **DocsE2E Tests:** Add documentation captures for `Location` and `Arena` views, and potentially an updated `ShowDetailView` showing an Arena.

