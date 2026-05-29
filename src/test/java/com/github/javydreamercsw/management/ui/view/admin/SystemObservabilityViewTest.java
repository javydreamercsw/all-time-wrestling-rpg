/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import com.github.javydreamercsw.management.config.DatabaseOptimizationConfig;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import com.github.javydreamercsw.management.service.performance.SystemPulseService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.tabs.Tabs;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class SystemObservabilityViewTest extends AbstractViewTest {

  @Mock private PerformanceMonitoringService performanceService;
  @Mock private CacheMonitor cacheMonitor;
  @Mock private DatabaseOptimizationConfig databaseConfig;
  @Mock private SystemPulseService pulseService;

  private SystemObservabilityView view;

  @BeforeEach
  void setup() {
    when(performanceService.getHistory()).thenReturn(Collections.emptyList());
    when(cacheMonitor.getDetailedCacheStatistics()).thenReturn(Collections.emptyList());
    view =
        new SystemObservabilityView(performanceService, cacheMonitor, databaseConfig, pulseService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the System Observability toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName("Should render tabs")
  void shouldRenderTabs() {
    Tabs tabs = _get(view, Tabs.class);
    assertTrue(tabs.isVisible());
  }
}
