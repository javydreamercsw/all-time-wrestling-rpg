# Plan: AI-Driven News & Rumor Feed

## Phase 1: Foundation [checkpoint: f3eda212]

- [x] Create `NewsItem` entity (title, content, date, category, isRumor).
- [x] Implement `NewsRepository` and `NewsService`.
- [x] Create a "News" tab/view in the application.

## Phase 2: AI Integration [checkpoint: 8f3cf619]

- [x] Implement `NewsGenerationService` that gathers "Events of the Day" (Show results, etc.).
- [x] Create AI prompt templates for "Sports Journalism" style headlines.
- [x] Implement scheduled job or trigger to generate news after each show.

## Phase 3: Dashboard UI [checkpoint: f3eda212]

- [x] Create `NewsTickerComponent` or `HeadlinesWidget`.
- [x] Integrate the widget into the `DashboardView`.
- [x] Add icons/styling to distinguish between "Breaking News" and "Backstage Rumors."

## Phase 4: Verification [checkpoint: 8f3cf619]

- [x] Integration test for news generation based on mock show results.
- [x] E2E test verifying news appears on the dashboard.
- [x] DocsE2ETest to capture news feature screenshots.

## Phase 5: Refinement & Optimization

- [ ] Implement "News-Worthy" filtering (Titles, Injuries, Rivalry endings).
- [ ] Add Show-level synthesis strategy (batch segments into one AI call).
- [ ] Implement probability-based Rumor Engine ("Daily Roll").
- [ ] Add advanced configurations to Game Settings (Rumor chance, Strategy toggle).
- [ ] Update DocsE2ETest to verify refined triggers.

