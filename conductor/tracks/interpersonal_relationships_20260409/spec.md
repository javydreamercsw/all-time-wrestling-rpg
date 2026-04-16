# Interpersonal Relationships Enhancement - Specification

## Overview

This enhancement introduces a social layer to the All Time Wrestling RPG, allowing wrestlers to have deep, non-competitive connections such as family ties, marriages, and friendships. These relationships will influence AI narration, segment performance, and random events.

## 1. Core Concepts

### 1.1. Relationship Types

- **SPOUSE:** Married or long-term romantic partners (e.g., Johnny All Time & Taya).
- **SIBLING:** Biological or storyline siblings.
- **BEST_FRIEND:** Deep platonic bond.
- **MENTOR / PROTEGE:** Veteran/Rookie dynamic.
- **ROMANCE:** Developing romantic storyline.

### 1.2. Relationship Attributes

- `type`: Enum of Relationship Types.
- `level`: Integer (0-100) representing the strength of the bond.
- `isStoryline`: Boolean (Storyline vs. "Real Life" heritage).
- `startedDate`: When the relationship began.

## 2. Gameplay Impact

### 2.1. Chemistry Bonus

Segments involving wrestlers with high-level positive relationships (e.g., Tag Team matches between Siblings or Spouses) receive a **Chemistry Bonus** to the final segment rating.

### 2.2. Automatic Support

If a wrestler is in a high-stakes match (Title match or Main Event), their Spouse or Best Friend has a high probability of appearing at ringside or in a backstage segment, even if not explicitly booked.

### 2.3. AI Narration

The `SegmentNarrationService` will be updated to include relationship context in the prompt.
*Example prompt injection:* "Wrestler A and Wrestler B are real-life spouses. Mention how B's ringside presence affects A's performance."

### 2.4. Drama Integration

`DramaEventService` will include new event templates for relationship milestones (e.g., Anniversaries, Betrayals, Family Feuds).

## 3. Technical Requirements

### 3.1. Persistence

- New entity `WrestlerRelationship` with a join table or self-referential many-to-many.
- Update `Wrestler` entity to have a list of `relationships`.

### 3.2. JSON Data Import

Relationships can be bulk-imported or defined in expansion packs via `src/main/resources/relationships.json`.
- **Format:** Array of objects containing `wrestler1` (name), `wrestler2` (name), `type` (enum), `level` (0-100), and `isStoryline` (boolean).
- **Automation:** `DataInitializer` automatically syncs this file during application startup, linking wrestlers by their unique names.

### 3.3. UI

- **Wrestler Profile:** Add a "Personal" tab showing active relationships.
- **Relationship Editor:** Admin tool to define "Real Life" relationships for expansions.

## 4. Testing Plan

- **Unit Tests:** Relationship creation and level modification.
- **Integration Tests:** Chemistry bonus calculation in `SegmentAdjudicationService`.
- **E2E Tests:** Verification of relationship display on the Wrestler Profile.

