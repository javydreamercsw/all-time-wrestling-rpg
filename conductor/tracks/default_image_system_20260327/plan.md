# Implementation Plan: Default Image System for Game Entities

## Phase 1: Foundation & Core Service [checkpoint: 19cddf7]
- [x] Task: Create `ImageCategory` enum (Wrestler, NPC, Show, Venue, Title, Team, Faction)
    - [x] Define the enum with category-specific default filenames.
- [x] Task: Create `ImageSource` interface and `ImageResolution` result object
    - [x] `ImageSource` should have a method to find an image by name and category.
- [x] Task: Implement `ClasspathImageSource`
    - [x] Create TDD unit tests to verify finding images in `src/main/resources/images/`.
    - [x] Implement the source to search the classpath.
- [x] Task: Implement `DefaultImageService`
    - [x] Create TDD unit tests for the resolution logic (specific match -> fallback).
    - [x] Implement the service to orchestrate multiple `ImageSource`s.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Foundation & Core Service' (Protocol in workflow.md)

## Phase 2: Configuration & External Sources
- [ ] Task: Implement `FileSystemImageSource`
  - [ ] Create TDD unit tests for resolving images from a local directory.
  - [ ] Implement the source with configurable path support.
- [ ] Task: Implement `PollinationsImageSource`
  - [ ] Create TDD unit tests for dynamic image generation using `PollinationsImageGenerationService`.
  - [ ] Implement the source to generate images based on entity name and category.
- [ ] Task: Add Spring Configuration for Image System
  - [ ] Define properties for external image paths and source priority.
  - [ ] Ensure `DefaultImageService` is properly initialized with configured sources.
- [ ] Task: Implement `RemoteUrlImageSource` (Basic)
  - [ ] Add support for resolving images via absolute URLs.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Configuration & External Sources' (Protocol in workflow.md)

## Phase 3: Integration with Entities

- [ ] Task: Update Entity Models/Services to use `DefaultImageService`
  - [ ] Update `WrestlerService` to resolve images via the new system.
  - [ ] Update `NPCService`, `ShowTemplateService`, etc.
- [ ] Task: Create Integration Tests for Entity Image Resolution
  - [ ] Verify that requesting a wrestler's image through the service returns the expected path/URL.
- [ ] Task: Add Default Generic Images to Resources
  - [ ] Ensure `generic-wrestler.png`, `generic-npc.png`, etc., exist in `src/main/resources/images/`.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Integration with Entities' (Protocol in workflow.md)

## Phase 4: UI Integration & Final Verification

- [ ] Task: Update Vaadin UI components
  - [ ] Replace hardcoded image paths in the frontend with calls to the `DefaultImageService`.
- [ ] Task: Implement E2E tests for Image Rendering
  - [ ] Create E2E tests to verify that images (both specific and default) are correctly rendered in the browser.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: UI Integration & Final Verification' (Protocol in workflow.md)

