# Track Spec: Data-Driven Achievement System

## Overview

Refactor the existing hardcoded Achievement enum into a flexible, data-driven system where achievement definitions are loaded from a JSON file and stored in the database. This allows for rapid addition of new milestones (match types, specific victories, main events) without code changes.

## Objectives

- Replace `AchievementType` enum with a database-backed entity model.
- Implement a JSON importer for achievement definitions.
- Expand the achievement set to include participation and wins for all special match types.
- Add achievements for Main Event positioning (Weekly vs PLE).
- Implement a generic trigger system in `LegacyService`.

## User Experience

- Players can see a vast array of potential medals in their dashboard.
- Unlocking rare achievements (like winning a Rumble) provides significantly more XP and prestigious visual indicators.

## Technical Requirements

- New database fields for `Achievement`: `achievement_key` (slug), `category`.
- `achievements.json` containing definitions for existing and new milestones.
- Integration points in `SegmentAdjudicationService` and `ShowService`.

