# Global Tour & Dynamic Arenas - Specification

This document details the requirements and functionality for the "Global Tour & Dynamic Arenas" system in the All Time Wrestling RPG. This system aims to add strategic depth by introducing diverse locations and arenas, each with unique characteristics that influence gameplay and narrative.

## 1. Core Concepts

### 1.1. Locations

Represents distinct geographical or thematic areas in the ATW future world (e.g., Megacities, Orbital Stations, Historical Zones).
- **Attributes:**
- `name`: String, unique identifier (e.g., "Neo-Tokyo", "Lunar Colony Alpha").
- `description`: Text, flavor text describing the location.
- `culturalTags`: Optional list of strings (e.g., "High-Tech", "Industrial", "Historic"). These can influence AI narration or future event generation.

### 1.2. Arenas

Specific venues where shows take place, linked to a Location. Arenas are central to show planning and impact match outcomes and fan reactions.
- **Attributes:**
- `name`: String, unique (e.g., "The Grand Colosseum", "Cyber-Basement Brawl").
- `description`: Text, flavor text.
- `locationId`: Foreign key to `Location`.
- `capacity`: Integer, maximum number of fans a show can attract in this arena.
- `alignmentBias`: Enum (`FACE_FAVORABLE`, `HEEL_FAVORABLE`, `ANARCHIC`, `NEUTRAL`). This dictates how the crowd reacts to different wrestler alignments.
- `imageUrl`: String, URL to an image representing the arena. Supports AI generation and direct upload.
- `environmentalTraits`: Optional list of strings (e.g., "Low-G", "Digital-Only Crowd"). These can provide specific mechanical modifiers or AI narration hooks.

### 1.3. Wrestler Heritage

A historical or cultural identifier for a wrestler, influencing their reception in certain locations.
- **Attribute:**
- `heritageTag`: String, a reference to a historical place or cultural identity (e.g., "Old Chicago," "Andean Highlands"). This is NOT directly linked to a `Location` entity but can be used for contextual bonuses.

## 2. Relationships

- **Show <-> Arena:** A `Show` must be linked to one `Arena`.
- **Arena <-> Location:** An `Arena` must be linked to one `Location`.

## 3. Key Functionality

### 3.1. CRUD Operations

- **Locations:** Full CRUD views (List, Add, Edit, Delete).
- **Arenas:** Full CRUD views (List, Add, Edit, Delete).
  - **Image Management:** For `Arena.imageUrl`, provide functionality to:
    - Upload an image URL.
    - Trigger AI image generation based on arena name/description/location context.

### 3.2. Data Initialization

- `DataInitializer` will read `locations.json` and `arenas.json` files from resources and populate the database.
- These JSON files will contain initial, future-themed data.

### 3.3. Game Logic Integration

- **SegmentAdjudicationService:**
  - **Fan Gain Modification:** Based on `Arena.alignmentBias`:
    - `FACE_FAVORABLE`: Faces receive +X% fan gain, Heels -Y% fan gain.
    - `HEEL_FAVORABLE`: Heels receive +X% fan gain, Faces -Y% fan gain.
    - `ANARCHIC`: Crowd reacts based on momentum; winners get bonus momentum.
    - `NEUTRAL`: No specific bias.
  - **Capacity Capping:** Total fan gain for a show in an `Arena` cannot exceed the `Arena.capacity`.
  - **Heritage Bonus:** If a wrestler's `heritageTag` aligns with the `Arena.location` (e.g., by matching a keyword or culturalTag), they may receive:
    - Small `PhysicalCondition` recovery bonus after the match.
    - Temporary `Fan` multiplier.
- **AI Narration:**
  - Arena context (name, location, alignment bias) will be integrated into the AI Narration prompts for match and segment summaries.
  - AI should generate descriptions of crowd reactions fitting the arena's bias (e.g., "the partisan crowd booed every move by the Face wrestler").

### 3.4. UI Presentation

- **ShowPlanningView:** Allow Bookers to select an `Arena` for a show via a ComboBox.
- **ShowDetailView:** Display `Arena` details (name, location, capacity, alignment bias, image) prominently.
- **WrestlerProfileView:** Display the wrestler's `heritageTag` if available.

## 4. Testing Requirements

- **Unit Tests:** For `LocationService`, `ArenaService`, `SegmentAdjudicationService` (new logic), `ShowService` (Arena linking).
- **Integration Tests:** Verify Flyway migrations, `DataInitializer` loading, and proper data relationships.
- **UI Tests (E2E):** Cover:
  - CRUD functionality for `Location` and `Arena` views.
  - Arena selection in `ShowPlanningView`.
  - Arena details display in `ShowDetailView`.
- **DocsE2E Tests:** Capture screenshots and descriptions for `Location` and `Arena` CRUD views, and an updated `ShowDetailView` demonstrating an arena.

