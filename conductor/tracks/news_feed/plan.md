# AI-Generated "Wrestling World" News Feed - Plan

## Phase 1: Analysis & Infrastructure

- [x] Review existing `NewsItem` and `NewsGenerationService`.
- [ ] Implement `EventAggregationService` to gather monthly data (match results, title changes, alignment shifts).
- [ ] Define `MonthlyWrapUp` logic in `NewsGenerationService`.

## Phase 2: Monthly PLE Synthesis

- [ ] Create a trigger for Monthly News generation (manual debug button + automatic post-PLE logic).
- [ ] Design AI prompt for holistic monthly analysis.
- [ ] Store large-form monthly reports as a special `NewsItem` category.

## Phase 3: Social Media & UI

- [ ] Enhance `NewsTickerComponent` with smoother transitions.
- [ ] Implement `SocialMediaView` (`/news/feed`) with a "X/Twitter" style aesthetic.
- [ ] Add "Wrestler Reactions" (mock posts) to the feed based on match outcomes.

## Phase 4: Export & Polish

- [ ] Add JSON/Text export for Monthly Wrap-Ups.
- [ ] Integration tests for monthly aggregation.
- [ ] Documentation E2E test for the News Feed.

