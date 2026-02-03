# Image Generation Specification

## Overview

Allow players and bookers to generate visual portraits for wrestlers based on their physical description, attire, and gimmick.

## User Stories

1. **As a User**, I want to click a "Generate Portrait" button on a wrestler profile to create a new image.
2. **As a User**, I want to be able to save the generated image as the wrestler's profile picture.

## Functional Requirements

1. **UI Updates:**
   * Add "Generate Image" button to `WrestlerView`.
   * Display generated image preview.
   * "Save" and "Retry" options.
2. **Backend Updates:**
   * Integration with Image Gen API (DALL-E, Stable Diffusion via LocalAI).
   * Prompt engineering based on wrestler attributes.
   * Image storage (local file system or DB blob).

## Technical Constraints

* Ensure prompt safety.
* Handle API timeouts/failures gracefully.

