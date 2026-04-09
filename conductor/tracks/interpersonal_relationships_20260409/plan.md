# Interpersonal Relationships Enhancement - Implementation Plan

## Phase 1: Foundation (Data Model)
- [ ] Create `RelationshipType` enum.
- [ ] Create `WrestlerRelationship` entity and repository.
- [ ] Add `relationships` list to `Wrestler` entity.
- [ ] Create Flyway migration for new tables.

## Phase 2: Core Logic
- [ ] Implement `RelationshipService` for managing bonds.
- [ ] Update `SegmentAdjudicationService` to apply Chemistry Bonuses.
- [ ] Update `SegmentNarrationService` to inject relationship context into AI prompts.

## Phase 3: UI & Drama
- [ ] Add "Personal" tab to `WrestlerProfileView`.
- [ ] Implement relationship editor in Admin view.
- [ ] Add relationship-specific templates to `DramaEventService`.

## Phase 4: Validation
- [ ] Unit tests for relationship logic.
- [ ] E2E tests for profile display.
