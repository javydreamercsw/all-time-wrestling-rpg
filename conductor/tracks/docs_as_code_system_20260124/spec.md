# Specification: Manifest-Driven Docs-as-Code

## 1. Overview
The goal is to automate the creation of the **Game Guide** by tightly coupling documentation with E2E testing. As tests verify features, they will "author" the documentation by capturing screenshots and emitting descriptive metadata (manifests). A static site generator (VitePress) will then consume these manifests to render a modern, searchable web guide.

## 2. Architecture

### 2.1 The "Author" (E2E Tests)
- **Role:** Execute scenarios, verify logic, and capture visual evidence.
- **Output:**
    - High-res screenshots in `docs/screenshots/*.png`.
    - A `docs/manifest.json` file.
- **Mechanism:** A new `documentFeature()` method in `AbstractE2ETest` that acts as a wrapper around `takeDocScreenshot()`, accepting additional metadata (title, description, category).

### 2.2 The "Exchange" (Manifest Format)
The `manifest.json` will follow this structure:
```json
{
  "features": [
    {
      "id": "campaign-tournament-bracket",
      "category": "Campaign",
      "title": "Tournament Bracket",
      "description": "Players progress through a seeded bracket...",
      "imagePath": "screenshots/campaign-tournament-bracket.png",
      "order": 10
    }
  ]
}
```

### 2.3 The "Builder" (Generator Script)
- **Role:** Convert the JSON manifest into Markdown content.
- **Logic:**
    - Read `manifest.json`.
    - Group entries by `category`.
    - For each category, generate (or inject into) a Markdown file (e.g., `docs/site/guide/campaign.md`).
    - Entries are rendered as sections with the title, screenshot, and description.

### 2.4 The "Viewer" (VitePress)
- **Role:** Host the static site.
- **Location:** `docs/site/`
- **Stack:** VitePress (Vue-based static site generator).
- **Features:** Dark mode, search, responsive layout, easy deployment.

## 3. Implementation Details

### 3.1 Java Infrastructure
- **`DocumentationManifest` class:** A singleton/static utility to accumulate entries during the test run and write the JSON file at the end of the suite execution.
- **`AbstractE2ETest` enhancements:**
    - `documentFeature(String category, String title, String description, String screenshotName)`

### 3.2 VitePress Setup
- Initialize a new VitePress project in `docs/site`.
- Configure `vite.config.ts` (or `docs/.vitepress/config.js`) to set up the sidebar navigation based on the categories.

## 4. Workflows

### Generation Workflow
1. Developer runs `mvn verify -Pgenerate-docs`.
2. Tests execute, screenshots are saved, `manifest.json` is written.
3. Maven (via `exec-maven-plugin`) or a simple script runs the "Builder" logic to update the Markdown files.
4. Developer runs `npm run docs:dev` to preview the guide.

### CI/CD (Future)
- The build pipeline runs the tests.
- The site is built (`npm run docs:build`).
- The artifacts are deployed (e.g., GitHub Pages).
