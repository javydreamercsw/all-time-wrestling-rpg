# Implementation Plan - Docs-as-Code System

## Phase 1: VitePress scaffolding

- [x] Task: Initialize VitePress Project
  - [x] Create `docs/site` directory.
  - [x] Initialize `package.json` for the docs site.
  - [x] Install VitePress: `npm add -D vitepress`.
  - [x] Create basic `index.md` and config files.
  - [x] Add `docs:dev` and `docs:build` scripts to the root `package.json` (or a dedicated one).
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Java Manifest Infrastructure

- [x] Task: Create `DocumentationManifest` Class
  - [x] Create a POJO `DocEntry` (category, title, description, imagePath).
  - [x] Create a singleton/manager `DocumentationManifest` to hold the list of entries.
  - [x] Implement `write(Path path)` to serialize the list to JSON (using Jackson).
- [x] Task: Enhance `AbstractE2ETest`
  - [x] Add `documentFeature(category, title, description, name)` method.
  - [x] Ensure it calls `takeDocScreenshot`.
  - [x] Ensure it registers the entry in `DocumentationManifest`.
  - [x] Add an `@AfterAll` or shutdown hook (or listener) to ensure `manifest.json` is flushed to disk at the end of the run.
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Update Tests & Verify

- [x] Task: Update `CampaignDocsE2ETest`
  - [x] Replace `takeDocScreenshot` calls with `documentFeature`.
  - [x] Add rich descriptions for the screenshots.
- [x] Task: Verification Run
  - [x] Run `mvn verify -Pgenerate-docs`.
  - [x] Verify `docs/manifest.json` is created and contains the correct data.
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: The Generator (JSON -> Markdown)

- [x] Task: Implement Generator Logic
  - [x] Create a simple script `scripts/generate-docs.cjs`.
  - [x] Logic: Read `docs/manifest.json`, generate `docs/site/guide/*.md`.
- [x] Task: Integrate & Embed in App
  - [x] Update `generate-docs.cjs` to also perform `vitepress build` and copy output to `src/main/resources/META-INF/resources/docs`.
  - [x] Update `MainLayout.java` to add a "Game Guide" menu item (via `MenuService.java`).
  - [x] Configure security to allow access to `/docs/**`.
- [x] Task: Final Build Verification
  - [x] Run the full pipeline.
  - [x] Verify docs are accessible via the app menu.
- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

