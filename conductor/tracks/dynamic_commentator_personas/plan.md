# Plan: Dynamic Commentator Personas (AI)

## Phase 1: Data Model & Personas

- [x] Create `Commentator` entity and repository. fcaf78ce
- [x] Populate database with initial "Commentary Teams" (Pairs of Commentators). 94c7776a
- [x] Add `CommentaryTeam` selection to `Show` and `ShowTemplate`. 0c90774c

## Phase 2: AI Prompt Engineering [checkpoint: aa1a37f]

- [x] Refactor `SegmentNarrationService` to include commentator profiles and wrestler alignment in the AI prompt context. aa1a37f8
- [x] Update AI system instructions to enforce dialogue-style output with specific tags. e60e190d
- [x] Implement a parser to split the AI response into a list of `NarrationLine` objects. e60e190d

## Phase 3: UI Enhancement [checkpoint: aa1a37f]

- [x] Create a `CommentaryComponent` for `MatchView`. 80f76f8d
- [x] Style dialogue lines based on the commentator's alignment (e.g., blue for Face, red for Heel). 81db07bd
- [x] Update `MatchView` to iterate through parsed dialogue lines. 80f76f8d
- [x] Add E2E documentation test to capture commentator personas in action. 57762e3f
- [x] Update Game Mechanics guide with AI Commentary section. 57762e3f

## Phase 4: Verification [checkpoint: 586393a]

- [x] Unit tests for dialogue parsing logic. 8bf3483d
- [x] Fix `AbstractMatchNarrationServiceTest` for new prompt format. 586393ae
- [x] E2E tests verifying that narration displays with multiple personas. 57762e3f
- [x] Documentation screenshots updated. 28ccc38f

