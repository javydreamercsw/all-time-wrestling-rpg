# Plan: Technical: System Observability Dashboard

## Phase 1: Metric Exposure

- [ ] Enhance `PerformanceMonitoringService` to export historical data (not just current state).
- [ ] Expose `CacheManager` statistics via a secured internal service.

## Phase 2: Observability UI

- [ ] Create `SystemObservabilityView` (Admin only).
- [ ] Integrate a free open-source charting library (e.g., ApexCharts or Chart.js) for performance trends.
- [ ] Build the "System Pulse" component for service health.

## Phase 3: Administrative Actions

- [ ] Implement "Purge Cache" buttons for each major cache.
- [ ] Add "Database Analysis" action to trigger index optimization manually.

## Phase 4: Verification

- [ ] Integration tests for metric gathering.
- [ ] E2E tests verifying only Admins can access the dashboard.

