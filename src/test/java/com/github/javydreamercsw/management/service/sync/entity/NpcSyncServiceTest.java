package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.CircuitBreakerService;
import com.github.javydreamercsw.management.service.sync.DataIntegrityChecker;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.RetryService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncTransactionManager;
import com.github.javydreamercsw.management.service.sync.SyncValidationService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NpcSyncServiceTest {

  @Mock private NpcService npcService;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private RetryService retryService;
  @Mock private CircuitBreakerService circuitBreakerService;
  @Mock private SyncValidationService validationService;
  @Mock private SyncTransactionManager syncTransactionManager;
  @Mock private DataIntegrityChecker integrityChecker;
  @Mock private NotionRateLimitService rateLimitService;

  private NpcSyncService npcSyncService;

  @BeforeEach
  void setUp() {
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
    npcSyncService = new NpcSyncService(objectMapper, syncProperties);

    // Manually inject all mocked dependencies using reflection
    ReflectionTestUtils.setField(npcSyncService, "npcService", npcService);
    ReflectionTestUtils.setField(npcSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(npcSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(npcSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(npcSyncService, "retryService", retryService);
    ReflectionTestUtils.setField(npcSyncService, "circuitBreakerService", circuitBreakerService);
    ReflectionTestUtils.setField(npcSyncService, "validationService", validationService);
    ReflectionTestUtils.setField(npcSyncService, "syncTransactionManager", syncTransactionManager);
    ReflectionTestUtils.setField(npcSyncService, "integrityChecker", integrityChecker);
    ReflectionTestUtils.setField(npcSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  void testSyncNpcs() throws InterruptedException {
    // Given
    when(syncProperties.isEntityEnabled("npcs")).thenReturn(true);

    List<NpcPage> npcPages = new ArrayList<>();
    NpcPage npcPage = new NpcPage();
    npcPage.setId("test-id");
    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", "Test NPC");
    properties.put("Role", "Referee");
    npcPage.setRawProperties(properties);
    npcPages.add(npcPage);

    when(notionHandler.loadAllNpcs()).thenReturn(npcPages);

    when(npcService.findByExternalId("test-id")).thenReturn(null);
    when(npcService.findByName("Test NPC")).thenReturn(null);
    doNothing().when(rateLimitService).acquirePermit();

    // When
    SyncResult result = npcSyncService.syncNpcs("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(1, result.getSyncedCount());
    verify(npcService, times(1)).save(any(Npc.class));
  }
}
