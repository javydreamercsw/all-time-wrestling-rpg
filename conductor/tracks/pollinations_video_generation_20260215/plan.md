# Implementation Plan: Pollinations AI Video Generation

This plan outlines the steps to integrate Pollinations AI for dynamic video generation, storage, and distribution via player newsletters.

## Phase 1: Infrastructure & Service Setup
- [ ] Task: Define `MatchVideo` entity and repository to store video metadata and paths.
    - [ ] Create `MatchVideo` JPA entity.
    - [ ] Create `MatchVideoRepository`.
- [ ] Task: Implement `PollinationsVideoService` for API interaction.
    - [ ] Write failing unit tests for `PollinationsVideoService`.
    - [ ] Implement service logic to call Pollinations AI API.
    - [ ] Implement error handling and timeout logic.
- [ ] Task: Extend Storage System for Video Support.
    - [ ] Write failing unit tests for video storage.
    - [ ] Update existing storage service to handle `.mp4` (or relevant) file types.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Infrastructure & Service Setup' (Protocol in workflow.md)

## Phase 2: Core Logic & Asynchronous Processing
- [ ] Task: Implement Video Generation Logic for Matches.
    - [ ] Write failing unit tests for prompt construction (using character images).
    - [ ] Implement logic to build visual prompts from match context and participant data.
    - [ ] Set up `@Async` processing for video generation.
- [ ] Task: Implement Newsletter & Inbox Integration.
    - [ ] Write failing unit tests for Newsletter delivery.
    - [ ] Update `InboxService` to support embedding video links/components in newsletters.
    - [ ] Implement logic to trigger newsletter delivery after show processing.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Core Logic & Asynchronous Processing' (Protocol in workflow.md)

## Phase 3: UI Implementation
- [ ] Task: Add Video Generation Trigger to Booker Dashboard.
    - [ ] Update Booker Dashboard UI to include a "Generate Video" button for matches.
    - [ ] Implement progress indicator/notifications for background generation.
- [ ] Task: Create and Integrate Video Player Component.
    - [ ] Implement a reusable Vaadin Video Player component.
    - [ ] Integrate player into the **Inbox** (Newsletter view).
    - [ ] Integrate player into the **Match Report** and **Match History** views.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI Implementation' (Protocol in workflow.md)

## Phase 4: Final Integration & E2E Testing
- [ ] Task: End-to-End Verification.
    - [ ] Write E2E tests for the full flow: Generate Video -> Deliver Newsletter -> Play Video.
    - [ ] Verify character image inclusion in prompts via integration tests.
- [ ] Task: Quality Gate Check.
    - [ ] Verify coverage (>80% for new code).
    - [ ] Perform mobile layout check for the video player.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration & E2E Testing' (Protocol in workflow.md)
