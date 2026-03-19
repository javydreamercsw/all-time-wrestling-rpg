# AI-Generated "Wrestling World" News Feed - Plan

## Phase 1: Analysis & Infrastructure

- [x] Review existing `NewsItem` and `NewsGenerationService`.
- [x] Implement `EventAggregationService` to gather monthly data (match results, title changes, alignment shifts).
- [x] Define `MonthlyWrapUp` logic in `NewsGenerationService`.

## Phase 2: Monthly PLE Synthesis

- [x] Create a trigger for Monthly News generation (manual debug button + automatic post-PLE logic).
- [x] Design AI prompt for holistic monthly analysis.
- [x] Store large-form monthly reports as a special `NewsItem` category.

## Phase 3: Social Media & UI

- [x] Enhance `NewsTickerComponent` with smoother transitions.
- [x] Implement `SocialMediaView` (`/news/feed`) with a "X/Twitter" style aesthetic.
- [x] Add "Wrestler Reactions" (mock posts) to the feed based on match outcomes.

## Phase 4: Export & Polish

- [x] Add JSON/Text export for Monthly Wrap-Ups.
- [x] Integration tests for monthly aggregation.
- [x] Documentation E2E test for the News Feed.

