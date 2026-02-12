# Plan: Dynamic Commentator Personas (AI)

## Phase 1: Data Model & Personas

- [x] Create `Commentator` entity and repository. fcaf78ce
- [x] Populate database with initial "Commentary Teams" (Pairs of Commentators). 94c7776a
- [x] Add `CommentaryTeam` selection to `Show` and `ShowTemplate`. 0c90774c

## Phase 2: AI Prompt Engineering [checkpoint: e60e190]

- [x] Refactor `SegmentNarrationService` to include commentator profiles in the AI prompt context. 0ec7d045
- [x] Update AI system instructions to enforce dialogue-style output with specific tags. e60e190d
- [x] Implement a parser to split the AI response into a list of `NarrationLine` objects. e60e190d

## Phase 3: UI Enhancement

- [ ] Create a `CommentaryComponent` for `MatchView`.
- [ ] Style dialogue lines based on the commentator's alignment (e.g., blue for Face, red for Heel).
- [ ] Update `MatchView` to iterate through parsed dialogue lines.

## Phase 4: Verification

- [ ] Unit tests for dialogue parsing logic.
- [ ] E2E tests verifying that narration displays with multiple personas.

