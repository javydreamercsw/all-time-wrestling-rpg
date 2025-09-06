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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NpcSyncServiceTest {

  @Mock private NpcService npcService;
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

  @InjectMocks private NpcSyncService npcSyncService;

  @BeforeEach
  void setUp() {
    npcSyncService = new NpcSyncService(new ObjectMapper(), syncProperties);
    npcSyncService.npcService = npcService;
    npcSyncService.notionHandler = notionHandler;
    npcSyncService.progressTracker = progressTracker;
    npcSyncService.healthMonitor = healthMonitor;
    npcSyncService.retryService = retryService;
    npcSyncService.circuitBreakerService = circuitBreakerService;
    npcSyncService.validationService = validationService;
    npcSyncService.syncTransactionManager = syncTransactionManager;
    npcSyncService.integrityChecker = integrityChecker;
    npcSyncService.rateLimitService = rateLimitService;
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
