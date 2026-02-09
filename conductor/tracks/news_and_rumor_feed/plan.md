# Plan: AI-Driven News & Rumor Feed

## Phase 1: Foundation

- [ ] Create `NewsItem` entity (title, content, date, category, isRumor).
- [ ] Implement `NewsRepository` and `NewsService`.
- [ ] Create a "News" tab/view in the application.

## Phase 2: AI Integration

- [ ] Implement `NewsGenerationService` that gathers "Events of the Day" (Show results, etc.).
- [ ] Create AI prompt templates for "Sports Journalism" style headlines.
- [ ] Implement scheduled job or trigger to generate news after each show.

## Phase 3: Dashboard UI

- [ ] Create `NewsTickerComponent` or `HeadlinesWidget`.
- [ ] Integrate the widget into the `DashboardView`.
- [ ] Add icons/styling to distinguish between "Breaking News" and "Backstage Rumors."

## Phase 4: Verification

- [ ] Integration test for news generation based on mock show results.
- [ ] E2E test verifying news appears on the dashboard.

