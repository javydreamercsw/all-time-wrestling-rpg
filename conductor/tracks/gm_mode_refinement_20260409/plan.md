# General Manager (GM) Mode Refinement - Implementation Plan

## Phase 1: Financial & Logistics Model
- [ ] Update `Season` and `League` entities with budget and duration fields.
- [ ] Create `WrestlerContract` entity with `durationWeeks` and `isInitialDraft` flag.
- [ ] Add `morale` and `managementStamina` (1-100) fields to `Wrestler` entity.
- [ ] Implement `SalaryCalculator` to tie contract costs to `fans` count.
- [ ] Create Flyway migration for new fields and tables.

## Phase 2: GM Mode Lifecycle & Draft
- [ ] Implement `GmDraftService` to handle the initial roster selection.
- [ ] Create `DraftRoomView` for the interactive draft experience.
- [ ] Logic to auto-assign "Season-Long" contracts to drafted wrestlers.
- [ ] Implement revenue calculation logic (Ticket sales, PPV).
- [ ] Implement expense deduction logic (Salaries based on Fans, Production).
- [ ] Implement Stamina depletion and recovery logic.
- [ ] Link Stamina < 40 to increased injury rolls in `SegmentAdjudicationService`.
- [ ] Add morale decay/gain logic based on booking history.

## Phase 3: GM Interface & Atmosphere
- [ ] Create `GmDashboardView` with financial charts and morale alerts.
- [ ] Create `ContractManagementView` for roster decisions.
- [ ] Add "Production Options" to `ShowPlanningView` (e.g., Pyro levels).
- [ ] Implement visual "Crowd Noise Meter" for `MatchView`.
- [ ] Add "Locker Room Morale" tracker to the side menu.

## Phase 4: Validation
- [ ] Integration tests for monthly financial cycles.
- [ ] Unit tests for morale influence factors.
