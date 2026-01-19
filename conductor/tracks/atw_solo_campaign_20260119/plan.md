# Plan: ATW Solo Campaign "All or Nothing"

## Phase 1: Domain Refinement & Persistence [checkpoint: c5891ff]

- [x] Task 1.1: Create database migrations for `Campaign`, `CampaignState`, and `CampaignAction` tables. [8126093]

- [x] Task 1.2: Extend existing `Wrestler` or create `WrestlerAlignment` to support the Face/Heel track. [e96e096]

- [x] Task 1.3: Refactor the existing `Injury` system to support persistent "Bumps" and campaign-specific penalties (e.g., hand size reduction). [e96e096]

- [x] Task 1.4: Implement JPA repositories and basic CRUD services for the new entities. [e96e096]

- [x] Task: Conductor - User Manual Verification 'Phase 1: Domain Refinement & Persistence' (Protocol in workflow.md) [c5891ff]

## Phase 2: Scriptable Campaign Engine [checkpoint: bef7857]

- [x] Task 2.1: Integrate Groovy scripting engine into the Spring Boot backend. [4df3e9d]

- [x] Task 2.2: Implement `CampaignScriptService` to load and execute scripts for alignment progression and ability unlocks. [d4659e5]

- [x] Task 2.3: Create initial Groovy script templates for Chapter 1 progression rules. [d4659e5]

- [x] Task 2.4: Write unit tests verifying that Groovy scripts correctly update `CampaignState`. [d4659e5]

- [x] Task: Conductor - User Manual Verification 'Phase 2: Scriptable Campaign Engine' (Protocol in workflow.md) [bef7857]

## Phase 3: Backstage Actions & Chapter Logic [checkpoint: 44fb9e0]

- [x] Task 3.1: Implement the dice-roll logic (1d6, success on 4+) for backstage actions. [cc75ffe]

- [x] Task 3.2: Implement `BackstageActionService` handling Training, Recovery, Promo, and Attack outcomes. [ccc7f9f]

- [x] Task 3.3: Implement Chapter management logic (VP rewards/penalties, difficulty scaling, Chapter transitions). [ccc7f9f]

- [x] Task 3.4: Integrate with existing `DramaEvent` system to trigger campaign story beats. [86fd530]

- [x] Task: Conductor - User Manual Verification 'Phase 3: Backstage Actions & Chapter Logic' (Protocol in workflow.md) [44fb9e0]

## Phase 4: AI Storyteller Integration

- [ ] Task 4.1: Extend AI services to accept `CampaignState` as context for narration and promo generation.
- [ ] Task 4.2: Implement "Story Branching" logic where the AI suggests the next `Rival` or `Outsider` event based on alignment.
- [ ] Task 4.3: Verify AI-generated content correctly reflects current wrestler injuries and momentum.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: AI Storyteller Integration' (Protocol in workflow.md)

## Phase 5: Campaign User Interface

- [ ] Task 5.1: Create the `CampaignDashboardView` (Vaadin) showing overview stats and current chapter.
- [ ] Task 5.2: Create the `BackstageActionView` with interactive dice-roll visualizations.
- [ ] Task 5.3: Implement the `AbilityTreeView` for skill token exchanges and card unlocks.
- [ ] Task 5.4: Implement the `NarrativeStoryView` for immersive chapter intros and match summaries.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Campaign User Interface' (Protocol in workflow.md)

## Phase 6: E2E Verification & Polish

- [ ] Task 6.1: Write E2E tests for a full Chapter 1 run (Login -> Action -> Match -> Result).
- [ ] Task 6.2: Perform a UI/UX polish pass to ensure the "Gritty & Realistic" wrestling theme is consistent.
- [ ] Task 6.3: Final verification of the Medical Limit and VP cost logic.
- [ ] Task: Conductor - User Manual Verification 'Phase 6: E2E Verification & Polish' (Protocol in workflow.md)

