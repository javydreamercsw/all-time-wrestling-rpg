/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.ui.view.admin;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.MarkersBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.stroke.Curve;
import com.github.appreciated.apexcharts.helper.Series;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import com.github.javydreamercsw.management.config.DatabaseOptimizationConfig;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService.PerformanceSnapshot;
import com.github.javydreamercsw.management.service.performance.SystemPulseService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Route(value = "admin/observability")
@PageTitle("System Observability")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
@NpmPackage(value = "@webcomponents/shadycss", version = "1.11.2")
@NpmPackage(value = "onecolor", version = "4.1.0")
@NpmPackage(value = "apexcharts", version = "3.49.0")
public class SystemObservabilityView extends VerticalLayout {

  private final PerformanceMonitoringService performanceService;
  private final CacheMonitor cacheMonitor;
  private final DatabaseOptimizationConfig databaseConfig;
  private final SystemPulseService pulseService;
  private final Div content = new Div();
  private final Tabs tabs;

  public SystemObservabilityView(
      @NonNull PerformanceMonitoringService performanceService,
      @NonNull CacheMonitor cacheMonitor,
      @NonNull DatabaseOptimizationConfig databaseConfig,
      @NonNull SystemPulseService pulseService) {
    this.performanceService = performanceService;
    this.cacheMonitor = cacheMonitor;
    this.databaseConfig = databaseConfig;
    this.pulseService = pulseService;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    setSizeFull();

    add(new ViewToolbar("System Observability"));

    tabs = createTabs();
    content.setSizeFull();
    add(tabs, content);
    setFlexGrow(1.0, content);

    updateContent(tabs.getSelectedTab());
    tabs.addSelectedChangeListener(event -> updateContent(event.getSelectedTab()));
  }

  private Tabs createTabs() {
    Tab performanceTab = new Tab("Performance");
    performanceTab.setId("performance-tab");
    Tab cacheTab = new Tab("Cache");
    cacheTab.setId("cache-tab");
    Tab databaseTab = new Tab("Database");
    databaseTab.setId("database-tab");
    Tab systemPulseTab = new Tab("System Pulse");
    systemPulseTab.setId("system-pulse");

    return new Tabs(performanceTab, cacheTab, databaseTab, systemPulseTab);
  }

  private void updateContent(@NonNull Tab selectedTab) {
    content.removeAll();
    String label = selectedTab.getLabel();

    if (label == null) {
      // Fallback if label is not set directly
      label = selectedTab.getElement().getText();
    }

    switch (label) {
      case "Performance" -> content.add(createPerformancePage());
      case "Cache" -> content.add(createCachePage());
      case "Database" -> content.add(createDatabasePage());
      case "System Pulse" -> content.add(createSystemPulsePage());
      default -> log.warn("Unknown tab selected: {}", label);
    }
  }

  private Component createPerformancePage() {
    VerticalLayout layout = new VerticalLayout();
    layout.setWidthFull();
    layout.setPadding(true);
    layout.setSpacing(true);

    Button refreshBtn = new Button("Refresh Data", new Icon(VaadinIcon.REFRESH));
    refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    refreshBtn.addClickListener(
        e -> {
          performanceService.captureSnapshot();
          updateContent(tabs.getSelectedTab());
          Notification.show("Performance snapshot captured");
        });

    List<PerformanceSnapshot> history = performanceService.getHistory();
    log.info("Creating performance page with {} history points", history.size());

    if (history.isEmpty()) {
      layout.add(
          refreshBtn,
          new Span("No performance data collected yet. High-frequency capture runs every minute."));
      return layout;
    }

    // Add a summary span for quick verification
    PerformanceSnapshot latest = history.get(history.size() - 1);
    Span summary =
        new Span(
            String.format(
                "Latest Stats: %.1f%% Memory, %d Threads (%d points in history)",
                latest.getHeapUsagePercent(), latest.getThreadCount(), history.size()));
    summary.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
    layout.add(refreshBtn, summary);

    Double[] memoryData =
        history.stream().map(PerformanceSnapshot::getHeapUsagePercent).toArray(Double[]::new);
    String[] labels =
        history.stream()
            .map(
                s ->
                    s.getTimestamp()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            .toArray(String[]::new);

    ApexCharts memoryChart = createAreaChart("Memory Usage (%)", memoryData, labels, "#2ecc71");
    memoryChart.setId("memory-chart");

    Double[] threadData =
        history.stream().map(s -> (double) s.getThreadCount()).toArray(Double[]::new);
    ApexCharts threadChart = createAreaChart("Active Threads", threadData, labels, "#3498db");
    threadChart.setId("thread-chart");

    layout.add(memoryChart, threadChart);

    // AI Performance Section
    layout.add(new H3("AI Performance"));

    // AI Response Time Chart
    ApexCharts aiResponseChart = createAiResponseTimeChart(history);
    aiResponseChart.setId("ai-response-chart");
    layout.add(aiResponseChart);

    // AI Token Usage Chart
    ApexCharts aiTokenChart = createAiTokenUsageChart(history);
    aiTokenChart.setId("ai-token-chart");
    layout.add(aiTokenChart);

    com.vaadin.flow.component.orderedlayout.Scroller scroller =
        new com.vaadin.flow.component.orderedlayout.Scroller(layout);
    scroller.setSizeFull();
    return scroller;
  }

  private ApexCharts createAiResponseTimeChart(List<PerformanceSnapshot> history) {
    ApexChartsBuilder builder =
        ApexChartsBuilder.get()
            .withChart(
                ChartBuilder.get()
                    .withType(Type.LINE)
                    .withHeight("300px")
                    .withWidth("100%")
                    .build())
            .withStroke(StrokeBuilder.get().withCurve(Curve.SMOOTH).build())
            .withMarkers(MarkersBuilder.get().withSize(4.0, 4.0).build())
            .withTitle(
                com.github.appreciated.apexcharts.config.builder.TitleSubtitleBuilder.get()
                    .withText("AI Response Times (ms)")
                    .build())
            .withXaxis(
                XAxisBuilder.get()
                    .withCategories(
                        history.stream()
                            .map(
                                s ->
                                    s.getTimestamp()
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                            .toArray(String[]::new))
                    .build());

    // Group by provider
    java.util.Set<String> providers =
        history.stream()
            .flatMap(s -> s.getAverageResponseTimes().keySet().stream())
            .filter(k -> k.contains("AI"))
            .collect(java.util.stream.Collectors.toSet());

    if (providers.isEmpty()) {
      builder.withSeries(new Series<>("No AI Data Yet", 0.0));
    } else {
      for (String provider : providers) {
        Double[] data =
            history.stream()
                .map(s -> s.getAverageResponseTimes().getOrDefault(provider, 0.0))
                .toArray(Double[]::new);
        String label = provider.replace("operations.duration.", "").replace("AI.Narration.", "");
        builder.withSeries(new Series<>(label, data));
      }
    }

    ApexCharts chart = builder.build();
    chart.getElement().getStyle().set("height", "300px");
    chart.getElement().getStyle().set("width", "100%");
    return chart;
  }

  private ApexCharts createAiTokenUsageChart(List<PerformanceSnapshot> history) {
    ApexChartsBuilder builder =
        ApexChartsBuilder.get()
            .withChart(
                ChartBuilder.get().withType(Type.BAR).withHeight("300px").withWidth("100%").build())
            .withTitle(
                com.github.appreciated.apexcharts.config.builder.TitleSubtitleBuilder.get()
                    .withText("AI Token Usage (Cumulative)")
                    .build())
            .withXaxis(
                XAxisBuilder.get()
                    .withCategories(
                        history.stream()
                            .map(
                                s ->
                                    s.getTimestamp()
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                            .toArray(String[]::new))
                    .build());

    java.util.Set<String> tokenMetrics =
        history.stream()
            .flatMap(s -> s.getCounterDeltas().keySet().stream())
            .filter(k -> k.startsWith("ai.tokens."))
            .collect(java.util.stream.Collectors.toSet());

    if (tokenMetrics.isEmpty()) {
      builder.withSeries(new Series<>("No Token Data Yet", 0.0));
    } else {
      for (String metric : tokenMetrics) {
        Double[] data =
            history.stream()
                .map(s -> s.getCounterDeltas().getOrDefault(metric, 0L).doubleValue())
                .toArray(Double[]::new);
        builder.withSeries(new Series<>(metric.replace("ai.tokens.", ""), data));
      }
    }

    ApexCharts chart = builder.build();
    chart.getElement().getStyle().set("height", "300px");
    chart.getElement().getStyle().set("width", "100%");
    return chart;
  }

  private ApexCharts createAreaChart(
      @NonNull String title,
      @NonNull Double[] data,
      @NonNull String[] labels,
      @NonNull String color) {
    ApexCharts chart =
        ApexChartsBuilder.get()
            .withChart(
                ChartBuilder.get()
                    .withType(Type.AREA)
                    .withHeight("300px")
                    .withWidth("100%")
                    .build())
            .withStroke(StrokeBuilder.get().withCurve(Curve.SMOOTH).build())
            .withMarkers(MarkersBuilder.get().withSize(4.0, 4.0).build())
            .withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
            .withSeries(new Series<>(title, data))
            .withColors(color)
            .withXaxis(XAxisBuilder.get().withCategories(labels).build())
            .build();
    chart.getElement().getStyle().set("height", "300px");
    chart.getElement().getStyle().set("width", "100%");
    return chart;
  }

  private Component createCachePage() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();

    Grid<Map<String, Object>> cacheGrid = new Grid<>();
    cacheGrid.addColumn(m -> m.get("name")).setHeader("Cache Name").setAutoWidth(true);
    cacheGrid.addColumn(m -> m.getOrDefault("size", "N/A")).setHeader("Entries");
    cacheGrid
        .addColumn(m -> String.format("%.2f%%", (double) m.getOrDefault("hitRate", 0.0) * 100))
        .setHeader("Hit Rate");
    cacheGrid.addColumn(m -> m.getOrDefault("evictionCount", "N/A")).setHeader("Evictions");

    cacheGrid
        .addComponentColumn(
            m -> {
              Button clearBtn = new Button(new Icon(VaadinIcon.TRASH));
              clearBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
              clearBtn.addClickListener(
                  e -> {
                    cacheMonitor.clearCache((String) m.get("name"));
                    Notification.show("Cache cleared: " + m.get("name"));
                    cacheGrid.setItems(cacheMonitor.getDetailedCacheStatistics());
                  });
              return clearBtn;
            })
        .setHeader("Actions");

    cacheGrid.setItems(cacheMonitor.getDetailedCacheStatistics());

    Button clearAllBtn = new Button("Clear All Caches", new Icon(VaadinIcon.ERASER));
    clearAllBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
    clearAllBtn.addClickListener(
        e -> {
          cacheMonitor.clearAllCaches();
          Notification.show("All caches cleared");
          cacheGrid.setItems(cacheMonitor.getDetailedCacheStatistics());
        });

    layout.add(clearAllBtn, cacheGrid);
    return layout;
  }

  private Component createDatabasePage() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();

    // Stats Grid
    Grid<Map.Entry<String, Object>> statsGrid = new Grid<>();
    statsGrid.addColumn(Map.Entry::getKey).setHeader("Metric");
    statsGrid.addColumn(Map.Entry::getValue).setHeader("Value");
    statsGrid.setItems(databaseConfig.getDatabaseStatistics().entrySet());
    layout.add(new H3("Database Statistics"), statsGrid);

    // Actions
    HorizontalLayout actions = new HorizontalLayout();
    Button optimizeBtn = new Button("Optimize Indexes", new Icon(VaadinIcon.MAGIC));
    optimizeBtn.addClickListener(
        e -> {
          databaseConfig.createDatabaseIndexes();
          Notification.show("Index creation triggered");
          statsGrid.setItems(databaseConfig.getDatabaseStatistics().entrySet());
        });

    Button analyzeBtn = new Button("Analyze Performance", new Icon(VaadinIcon.DOCTOR));
    analyzeBtn.addClickListener(
        e -> {
          List<String> suggestions = databaseConfig.analyzeQueryPerformance();
          if (suggestions.isEmpty()) {
            Notification.show("No issues found.");
          } else {
            VerticalLayout messages = new VerticalLayout();
            suggestions.forEach(s -> messages.add(new Span(s)));
            Notification notification = new Notification(messages);
            notification.setDuration(5000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
          }
        });

    actions.add(optimizeBtn, analyzeBtn);
    layout.add(new H3("Actions"), actions);

    return layout;
  }

  private Component createSystemPulsePage() {
    VerticalLayout layout = new VerticalLayout();
    Map<String, Object> health = performanceService.getHealthStatus();

    layout.add(new H3("Internal Health"));
    layout.add(new Span("Overall Status: " + health.get("status")));
    layout.add(new Span("Memory Pressure: " + health.get("memoryUsage")));
    layout.add(new Span("Active Thread Count: " + health.get("threadCount")));
    layout.add(new Span("Pending Operations: " + health.get("activeOperations")));

    layout.add(new H3("External Services Pulse"));
    Grid<Map.Entry<String, SystemPulseService.ServiceStatus>> pulseGrid = new Grid<>();
    pulseGrid.addColumn(Map.Entry::getKey).setHeader("Service");
    pulseGrid.addColumn(e -> e.getValue().status()).setHeader("Status").setAutoWidth(true);
    pulseGrid.addColumn(e -> e.getValue().message()).setHeader("Details");

    pulseGrid.setItems(pulseService.getPulse().entrySet());
    layout.add(pulseGrid);

    return layout;
  }
}
