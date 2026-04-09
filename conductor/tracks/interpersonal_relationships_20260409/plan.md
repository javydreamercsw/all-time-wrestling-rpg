# Interpersonal Relationships Enhancement - Implementation Plan

## Phase 1: Foundation (Data Model)

- [x] Create `RelationshipType` enum.
- [x] Create `WrestlerRelationship` entity and repository.
- [x] Add `relationships` list to `Wrestler` entity.
- [x] Create Flyway migration for new tables.

## Phase 2: Core Logic

- [x] Implement `RelationshipService` for managing bonds.
- [x] Update `SegmentAdjudicationService` to apply Chemistry Bonuses.
- [x] Update `SegmentNarrationService` to inject relationship context into AI prompts.
- [x] Implement JSON import support via `relationships.json`.

## Phase 3: UI & Drama

- [x] Add "Relationships" section to `WrestlerProfileView`.
- [x] Implement `WrestlerRelationshipManagementView` in Admin view.
- [x] Add relationship-specific templates to `DramaEventService`.

## Phase 4: Validation

- [x] Unit tests for relationship logic (`WrestlerRelationshipServiceTest`).
- [x] Unit tests for Wrestler entity consolidation (`WrestlerTest`).
- [x] Integration tests for JSON sync (`DataInitializerTest`).
- [x] DocsE2E tests for UI features (`WrestlerRelationshipDocsE2ETest`).

