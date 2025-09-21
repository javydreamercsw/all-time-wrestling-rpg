package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.base.test.BaseTest;
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
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@EnabledIf("isNotionTokenAvailable")
class NpcSyncServiceTest extends BaseTest {

  @Mock private NpcService npcService;
  @Mock private ObjectMapper objectMapper;
  private NotionSyncProperties syncProperties; // Declare without @Mock
  @Mock private NotionHandler notionHandler;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private RetryService retryService;
  @Mock private CircuitBreakerService circuitBreakerService;
  @Mock private SyncValidationService validationService;
  @Mock private SyncTransactionManager syncTransactionManager;
  @Mock private DataIntegrityChecker integrityChecker;
  @Mock private NotionRateLimitService rateLimitService;

  @InjectMocks NpcSyncService npcSyncService;

  // Constructor to configure the mock before setUp()
  public NpcSyncServiceTest() {
    syncProperties = mock(NotionSyncProperties.class); // Manually create mock
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
  }

  @BeforeEach
  void setUp() {
    // Manually inject all mocked dependencies using reflection
    setField(npcSyncService, "npcService", npcService); // Add this line
    setField(npcSyncService, "notionHandler", notionHandler);
    setField(npcSyncService, "progressTracker", progressTracker);
    setField(npcSyncService, "healthMonitor", healthMonitor);
    setField(npcSyncService, "retryService", retryService);
    setField(npcSyncService, "circuitBreakerService", circuitBreakerService);
    setField(npcSyncService, "validationService", validationService);
    setField(npcSyncService, "syncTransactionManager", syncTransactionManager);
    setField(npcSyncService, "integrityChecker", integrityChecker);
    setField(npcSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  @EnabledIf("isNotionTokenAvailable") // Add this annotation
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
