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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionRateLimitService;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.CircuitBreakerService;
import com.github.javydreamercsw.management.service.sync.RetryService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class ShowSyncServiceIT {

  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;
  @Mock private CircuitBreakerService circuitBreakerService;
  @Mock private RetryService retryService;
  @Mock private SyncServiceDependencies syncServiceDependencies;

  private ShowSyncService showSyncService;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    lenient().when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(syncProperties);
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
    lenient()
        .when(objectMapper.getTypeFactory())
        .thenReturn(com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance());
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getProgressTracker()).thenReturn(progressTracker);
    lenient().when(syncServiceDependencies.getHealthMonitor()).thenReturn(healthMonitor);
    lenient().when(syncServiceDependencies.getRateLimitService()).thenReturn(rateLimitService);
    lenient()
        .when(syncServiceDependencies.getCircuitBreakerService())
        .thenReturn(circuitBreakerService);
    lenient().when(syncServiceDependencies.getRetryService()).thenReturn(retryService);

    showSyncService =
        new ShowSyncService(
            objectMapper,
            syncServiceDependencies,
            showService,
            showTypeService,
            seasonService,
            showTemplateService);

    // Mock resilience services to execute immediately
    lenient()
        .when(
            circuitBreakerService.execute(
                anyString(), any(CircuitBreakerService.SyncOperation.class)))
        .thenAnswer(
            invocation -> {
              try {
                return ((CircuitBreakerService.SyncOperation<Object>) invocation.getArgument(1))
                    .execute();
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            });
    lenient()
        .when(retryService.executeWithRetry(anyString(), any(RetryService.AttemptCallable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return ((RetryService.AttemptCallable<Object>) invocation.getArgument(1))
                    .call(1); // Pass attempt number 1
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void syncShows_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    when(showService.getAllExternalIds()).thenReturn(java.util.Collections.emptyList());
    when(notionHandler.getDatabasePageIds(anyString())).thenReturn(java.util.List.of("show-1"));

    com.github.javydreamercsw.base.ai.notion.ShowPage showPage =
        new com.github.javydreamercsw.base.ai.notion.ShowPage();
    showPage.setId("show-1");
    java.util.Map<String, Object> properties = new java.util.HashMap<>();
    properties.put("Name", "Test Show");
    properties.put("Show Type", "Weekly");
    showPage.setRawProperties(properties);
    when(notionHandler.loadShowById(anyString())).thenReturn(java.util.Optional.of(showPage));

    com.github.javydreamercsw.management.domain.show.type.ShowType showType =
        new com.github.javydreamercsw.management.domain.show.type.ShowType();
    showType.setName("Weekly");
    when(showTypeService.findAll()).thenReturn(java.util.List.of(showType));

    when(showService.save(any(com.github.javydreamercsw.management.domain.show.Show.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    // When
    SyncResult result = showSyncService.syncShows("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("shows", result.getEntityType());
    assertEquals(1, result.getSyncedCount());
    verify(healthMonitor, never()).recordFailure(anyString(), anyString());
  }

  @Test
  void syncShows_NoNewShows_ShouldReturnSuccess() {
    // Given
    when(showService.getAllExternalIds()).thenReturn(java.util.List.of("show-1"));
    when(notionHandler.getDatabasePageIds(anyString())).thenReturn(java.util.List.of("show-1"));

    // When
    SyncResult result = showSyncService.syncShows("test-operation");

    // Then
    assertTrue(result.isSuccess(), result.getErrorMessage());
    assertEquals("shows", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(healthMonitor, never()).recordSuccess(eq("shows"), anyLong(), anyInt());
  }
}
