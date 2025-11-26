package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.CircuitBreakerService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.RetryService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShowSyncServiceTest {

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

  private ShowSyncService showSyncService;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
    lenient()
        .when(objectMapper.getTypeFactory())
        .thenReturn(com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance());

    showSyncService = new ShowSyncService(objectMapper, syncProperties);

    injectMockDependencies();

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

  private void injectMockDependencies() {
    ReflectionTestUtils.setField(showSyncService, "showService", showService);
    ReflectionTestUtils.setField(showSyncService, "showTypeService", showTypeService);
    ReflectionTestUtils.setField(showSyncService, "seasonService", seasonService);
    ReflectionTestUtils.setField(showSyncService, "showTemplateService", showTemplateService);
    ReflectionTestUtils.setField(showSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(showSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(showSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(showSyncService, "rateLimitService", rateLimitService);
    ReflectionTestUtils.setField(showSyncService, "circuitBreakerService", circuitBreakerService);
    ReflectionTestUtils.setField(showSyncService, "retryService", retryService);
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
    assertEquals("Shows", result.getEntityType());
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
    assertTrue(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(healthMonitor, never()).recordSuccess(eq("Shows"), anyLong(), anyInt());
  }
}
