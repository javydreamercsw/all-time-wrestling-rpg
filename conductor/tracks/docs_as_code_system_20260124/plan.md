# Implementation Plan - Docs-as-Code System

## Phase 1: VitePress scaffolding

- [ ] Task: Initialize VitePress Project
  - [ ] Create `docs/site` directory.
  - [ ] Initialize `package.json` for the docs site.
  - [ ] Install VitePress: `npm add -D vitepress`.
  - [ ] Create basic `index.md` and config files.
  - [ ] Add `docs:dev` and `docs:build` scripts to the root `package.json` (or a dedicated one).
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Java Manifest Infrastructure

- [ ] Task: Create `DocumentationManifest` Class
  - [ ] Create a POJO `DocEntry` (category, title, description, imagePath).
  - [ ] Create a singleton/manager `DocumentationManifest` to hold the list of entries.
  - [ ] Implement `write(Path path)` to serialize the list to JSON (using Jackson).
- [ ] Task: Enhance `AbstractE2ETest`
  - [ ] Add `documentFeature(category, title, description, name)` method.
  - [ ] Ensure it calls `takeDocScreenshot`.
  - [ ] Ensure it registers the entry in `DocumentationManifest`.
  - [ ] Add an `@AfterAll` or shutdown hook (or listener) to ensure `manifest.json` is flushed to disk at the end of the run.
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Update Tests & Verify

- [ ] Task: Update `CampaignDocsE2ETest`
  - [ ] Replace `takeDocScreenshot` calls with `documentFeature`.
  - [ ] Add rich descriptions for the screenshots.
- [ ] Task: Verification Run
  - [ ] Run `mvn verify -Pgenerate-docs`.
  - [ ] Verify `docs/manifest.json` is created and contains the correct data.
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: The Generator (JSON -> Markdown)

- [ ] Task: Implement Generator Logic
  - [ ] Create a simple script (Node.js preferred as we have the runtime) `scripts/generate-docs.js`.
  - [ ] Logic: Read `docs/manifest.json`, generate `docs/site/guide/*.md`.
  - [ ] Configure the sidebar in VitePress config to auto-discover these new pages (or update the config dynamically).
- [ ] Task: Integrate & Final Polish
  - [ ] Run the full pipeline: Test -> Manifest -> Markdown -> Site Build.
  - [ ] Review the generated site.
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
