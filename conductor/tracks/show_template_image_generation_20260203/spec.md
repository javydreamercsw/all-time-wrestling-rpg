# Support AI Image Generation for Show Templates

## Overview

Allow creating AI-generated show art for `ShowTemplate` entities. This adds visual flair to the show templates, which define the branding for events.

## Goals

1. Add `imageUrl` field to `ShowTemplate` entity.
2. Enable AI image generation for Show Templates via the UI.
3. Display the generated image in the `ShowTemplate` list or details.

## Requirements

### Data Model

* **Entity:** `ShowTemplate`
* **Field:** `imageUrl` (String, max length 512 or similar to others)
* **Database:** Update schema (Flyway migration).

### UI/UX

* **View:** `ShowTemplateListView` (and `ShowTemplateDialog` if images are editable manually).
* **Action:** "Generate Show Art" button/menu item.
* **Dialog:** Reuse `GenericImageGenerationDialog` with a prompt tailored for wrestling show posters/logos.
* **Display:** Show a thumbnail or icon in the list view.

### AI Prompting

* Construct a prompt based on:
  * Show Template Name (e.g., "Monday Night Raw", "WrestleMania").
  * Show Type (e.g., "Weekly", "PLE").
  * Keywords: "Pro wrestling event poster", "Logo", "High energy", "Crowd", "Pyrotechnics".

## Testing

* **Unit:** Verify `imageUrl` persistence.
* **E2E:** Test the generation flow (using Mock AI or real if configured) and ensuring the image is saved and displayed.

