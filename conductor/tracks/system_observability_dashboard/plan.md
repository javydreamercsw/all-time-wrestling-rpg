# Plan: Technical: System Observability Dashboard

## Phase 1: Metric Exposure

- [x] Enhance `PerformanceMonitoringService` to export historical data (not just current state).
- [x] Expose `CacheManager` statistics via a secured internal service.
- [x] Implement AI performance tracking (response times and token usage).

## Phase 2: Observability UI

- [x] Create `SystemObservabilityView` (Admin only).
- [x] Integrate a free open-source charting library (e.g., ApexCharts or Chart.js) for performance trends.
- [x] Build the "System Pulse" component for service health.
- [x] Add AI Performance charts to the dashboard.

## Phase 3: Administrative Actions

- [x] Implement "Purge Cache" buttons for each major cache.
- [x] Add "Database Analysis" action to trigger index optimization manually.
- [x] Add "Optimize Indexes" action to the database tab.

## Phase 4: Verification

- [x] Integration tests for metric gathering.
- [x] E2E tests verifying only Admins can access the dashboard.

