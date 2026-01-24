# Implementation Plan - Advanced Campaign Chapters

## Phase 1: Core Systems & Triggers [checkpoint: 602f368]

- [x] Task: Create campaign chapter definitions b453c55
  - [x] Create `campaign_chapters.json` entries for "The Fighting Champion", "Gang Warfare", and "Corporate Power Trip".
  - [x] Define entry criteria (triggers) for each chapter in the JSON structure.
- [x] Task: Implement Chapter Trigger Logic 09f37c7
  - [x] Update `CampaignService` to evaluate player state (IsChampion, FactionMembership, Heat) against chapter criteria.
  - [x] Create unit tests for trigger evaluation logic.
- [x] Task: Conductor - User Manual Verification 'Core Systems & Triggers' (Protocol in workflow.md)

## Phase 2: "The Fighting Champion" (Open Challenge)

- [x] Task: Implement Open Challenge Logic 10ef0e9
    - [x] Update `CampaignEncounterService` prompt logic to handle "The Fighting Champion" context.
    - [x] Ensure AI is instructed to provide surprise opponents for Open Challenges.

- [ ] Task: Integrate with Match Engine

  - [ ] Ensure Open Challenge matches correctly affect title history and fan calculations.
- [ ] Task: Conductor - User Manual Verification 'The Fighting Champion' (Protocol in workflow.md)

## Phase 3: "Gang Warfare" (Faction Integration)

- [ ] Task: Implement Faction Logic for Campaign
  - [ ] Create service methods to retrieve and manipulate Faction data for campaign contexts.
  - [ ] Implement "Recruitment" and "Betrayal" event logic.
- [ ] Task: Create Faction-based Segments
  - [ ] Implement specific segment types for Faction Warfare (e.g., "Faction Beatdown", "Group Promo").
- [ ] Task: Conductor - User Manual Verification 'Gang Warfare' (Protocol in workflow.md)

## Phase 4: "Corporate Power Trip" (Authority Logic)

- [ ] Task: Implement Authority Logic
  - [ ] Create logic to identify active Authority NPCs.
  - [ ] Implement "Unfair Match" modifiers (e.g., Pre-match damage, Fast count for opponent).
- [ ] Task: Create Authority Segments
  - [ ] Implement specific segments: "GM Office Confrontation", "Performance Review".
- [ ] Task: Conductor - User Manual Verification 'Corporate Power Trip' (Protocol in workflow.md)

## Phase 5: Testing & Integration

- [ ] Task: Integration Testing
  - [ ] Create E2E tests simulating a full run-through of each new chapter.
  - [ ] Verify state persistence between chapters.
- [ ] Task: Polish & Balancing
  - [ ] Adjust difficulty curves and fan rewards for new chapters.
- [ ] Task: Conductor - User Manual Verification 'Testing & Integration' (Protocol in workflow.md)

