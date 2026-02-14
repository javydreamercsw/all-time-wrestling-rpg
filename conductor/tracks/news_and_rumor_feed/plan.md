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

## Phase 4: Verification

- [ ] Integration test for news generation based on mock show results.
- [ ] E2E test verifying news appears on the dashboard.

