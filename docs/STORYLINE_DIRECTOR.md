# AI Storyline Director Design

## Overview

The AI Storyline Director is responsible for planning and guiding long-term narrative arcs within the Campaign mode. It moves beyond isolated encounters by creating "Storyline Arcs" with "Milestones" that branch based on player performance and choices.

## Domain Model

### CampaignStoryline

Represents a specific narrative thread (e.g., "The Underdog's Redemption", "The Faction Betrayal").
- `id`: Long
- `campaign`: Campaign (ManyToOne)
- `title`: String
- `description`: String
- `status`: Enum (ACTIVE, COMPLETED, ABANDONED)
- `currentMilestone`: StorylineMilestone (OneToOne)
- `startedAt`: LocalDateTime
- `endedAt`: LocalDateTime

### StorylineMilestone

A specific beat within a storyline.
- `id`: Long
- `storyline`: CampaignStoryline (ManyToOne)
- `title`: String
- `description`: String
- `narrativeGoal`: String (Instruction for the AI Director during encounters)
- `status`: Enum (PENDING, ACTIVE, COMPLETED, FAILED)
- `order`: Integer
- `nextMilestoneOnSuccess`: StorylineMilestone (ManyToOne)
- `nextMilestoneOnFailure`: StorylineMilestone (ManyToOne)

## Workflow

1. **Arc Initialization:**
   - When a new chapter starts, `CampaignService` calls `StorylineDirectorService.initializeStoryline(campaign)`.
   - `StorylineDirectorService` uses AI to generate a `CampaignStoryline` with 3-5 milestones based on the chapter theme and player alignment.
2. **Encounter Generation:**
   - `CampaignEncounterService` queries the active `StorylineMilestone`.
   - The `narrativeGoal` of the milestone is injected into the AI prompt for generating the next encounter.
3. **Outcome Processing:**
   - After a match or promo, `StorylineDirectorService.evaluateProgress(campaign, outcome)` is called.
   - If the outcome matches a milestone's success/failure condition, the milestone is marked as COMPLETED/FAILED and the `currentMilestone` is updated to the next branch.
4. **UI Integration:**
   - `CampaignDashboardView` will display the current Storyline title and description.
   - A "Storyline Progress" widget will show the completed milestones and the current goal.

## AI Prompts

### Storyline Generation Prompt

"You are the Lead Creative Writer for ATW. Based on the current chapter [CHAPTER_NAME] and the player's alignment [ALIGNMENT], generate a storyline arc with 3 milestones. Each milestone should have a narrative goal and clear success/failure branches."

### Encounter Prompt Injection

"CURRENT STORYLINE: [TITLE]
CURRENT GOAL: [MILESTONE_GOAL]
Ensure this encounter advances the current goal."
