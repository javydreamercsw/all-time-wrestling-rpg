# Implementation Plan - AI Image Gen for Show Templates

- [x] **Data Model & Migration**

  - [x] Add `imageUrl` String field to `ShowTemplate` entity.

  - [x] Create Flyway migration script (e.g., `V42__Add_ImageUrl_To_ShowTemplate.sql`).

  - [x] Update `ShowTemplateRepository` if needed (likely not).

- [x] **UI Implementation**

  - [x] Update `ShowTemplateListView` to display the image (Avatar or Image component).

  - [x] Add "Generate Image" button to the actions in `ShowTemplateListView` (or inside the edit dialog).

  - [x] Create `ShowTemplateImageGenerationDialog` (extending `GenericImageGenerationDialog`) or instantiate `GenericImageGenerationDialog` directly with a custom prompt supplier.

  - [x] Prompt Strategy: "A professional wrestling TV show logo for '{name}'. Style: {style_description}. High contrast, exciting, TV graphic."

- [x] **Integration**

  - [x] Ensure `ImageStorageService` works for these new images (it should, generic).

  - [x] Update `ImageCleanupService` to check `ShowTemplate` images so they aren't deleted.

- [x] **Testing**

  - [x] `ShowTemplateImageGenerationE2ETest`: Verify the flow from list -> generate -> save -> view update.

  - [x] Verify `ImageCleanupService` doesn't delete referenced show template images.

