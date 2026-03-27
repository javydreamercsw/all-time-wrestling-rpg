# Specification: Default Image System for Game Entities

## 1. Overview
Implement a centralized system for resolving default images for various game entities (Wrestlers, NPCs, Show Templates, Venues, Titles, Teams, and Factions). This system will prioritize finding an image by exact name match and fall back to a generic category image if no specific image is found. It should support multiple image sources, including bundled classpath resources and external storage.

## 2. Functional Requirements

### 2.1 Entity-to-Image Resolution
- **Resolution Strategy:** The system must attempt to find an image matching the exact name of the entity (e.g., "Princess Aussie" -> `Princess Aussie.png`).
- **Fallback Logic:** If a specific image is not found, the system must return a generic category image (e.g., `generic-wrestler.png`, `generic-npc.png`, `generic-show.png`).
- **Case Sensitivity:** Matching should be consistent (e.g., case-insensitive or strictly following naming conventions).
- **Naming Conventions:** Special characters in entity names must be handled (e.g., converting to filesystem-friendly names if necessary).

### 2.2 Supported Entity Types
- Wrestlers
- NPCs (Managers, Referees, etc.)
- Show Templates
- Venues
- Titles (Championship Belts)
- Teams
- Factions

### 2.3 Image Sources
- **Classpath-based:** Bundled within the application resources (e.g., `src/main/resources/images/`).
- **External Storage:** Support for a configurable directory on the server's filesystem.
- **Remote Storage:** Support for images served from a remote URL or cloud storage (e.g., S3).

### 2.4 Service & API
- Provide a Java service/component that allows other parts of the application to request an image for a given entity.
- The service should return either a resource path, a URL, or an input stream.

## 3. Non-Functional Requirements
- **Performance:** Cache the results of image lookups to avoid frequent filesystem/remote checks.
- **Extensibility:** The system should be designed to easily add new entity categories in the future.
- **Reliability:** Gracefully handle missing files or connection issues with remote storage.

## 4. Acceptance Criteria
1. Wrestlers with matching images in the designated folder show those images.
2. Wrestlers without specific images show a generic wrestler placeholder.
3. NPCs, Show Templates, Venues, Titles, Teams, and Factions follow the same resolution logic.
4. The system can resolve images from at least two different sources (e.g., Classpath and Local Filesystem).
5. All images are rendered correctly in the UI wherever entity images are displayed.

## 5. Out of Scope
- AI dynamic image generation.
- Direct image uploading via the UI (this system handles *default* images provided by the system).
- Image editing or cropping functionality.
