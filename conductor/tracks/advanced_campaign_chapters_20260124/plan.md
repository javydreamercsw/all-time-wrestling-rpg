# Implementation Plan - Advanced Campaign Chapters

## Phase 1: Core Systems & Triggers [checkpoint: 602f368]

- [x] Task: Create campaign chapter definitions b453c55
  - [x] Create `campaign_chapters.json` entries for "The Fighting Champion", "Gang Warfare", and "Corporate Power Trip".
  - [x] Define entry criteria (triggers) for each chapter in the JSON structure.
- [x] Task: Implement Chapter Trigger Logic 09f37c7
  - [x] Update `CampaignService` to evaluate player state (IsChampion, FactionMembership, Heat) against chapter criteria.
  - [x] Create unit tests for trigger evaluation logic.
- [x] Task: Conductor - User Manual Verification 'Core Systems & Triggers' (Protocol in workflow.md)

## Phase 2: "The Fighting Champion" (Open Challenge) [checkpoint: 9c415ff]

- [x] Task: Implement Open Challenge Logic 10ef0e9

  - [x] Update `CampaignEncounterService` prompt logic to handle "The Fighting Champion" context.

  - [x] Ensure AI is instructed to provide surprise opponents for Open Challenges.

- [x] Task: Integrate with Match Engine 7f87846

  - [x] Ensure Open Challenge matches correctly affect title history and fan calculations.
- [x] Task: Conductor - User Manual Verification 'The Fighting Champion' (Protocol in workflow.md)

## Phase 3: "Gang Warfare" (Faction Integration) [checkpoint: c93d1fc]

- [x] Task: Implement Faction Logic for Campaign ee4e8c7

    - [x] Create service methods to retrieve and manipulate Faction data for campaign contexts.

    - [x] Implement "Recruitment" and "Betrayal" event logic.

- [x] Task: Create Faction-based Segments 4929a22

    - [x] Implement specific segment types for Faction Warfare (e.g., "Faction Beatdown", "Group Promo").

- [x] Task: Conductor - User Manual Verification 'Gang Warfare' (Protocol in workflow.md)

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

