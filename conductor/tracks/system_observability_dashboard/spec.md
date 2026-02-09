# Specification: Technical: System Observability Dashboard

## Goal

Expose existing backend performance and health metrics through a user-friendly UI for administrators to ensure system stability and optimize AI usage.

## Requirements

1. **Performance Visualization:**
   - Display real-time charts for AI response times (per provider).
   - Show token usage trends to monitor costs/limits.
   - **Constraint:** Use only MIT-licensed or free open-source charting libraries (e.g., ApexCharts, Chart.js). No paid commercial licenses.
2. **Cache Management:**
   - UI to view hit/miss rates for all Hibernate and custom caches.
   - Action to "Purge" specific caches without restarting the app.
3. **External Service Health:**
   - Connectivity status for Gemini, Claude, OpenAI, and Notion.
4. **Database Observability:**
   - View slow query counts and index usage (leveraging `DatabaseOptimizationConfig`).

## Success Criteria

- Admins can identify the slowest AI provider via the UI.
- Cache hit rates are visible and actionable.
- External service outages are immediately apparent on the "System Pulse" view.

