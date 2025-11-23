package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for FactionSyncService covering faction synchronization scenarios including
 * relationship handling and error conditions.
 */
@ExtendWith(MockitoExtension.class)
@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
class FactionSyncServiceTest {

  @Mock private FactionRepository factionRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;

  private FactionSyncService factionSyncService;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    Mockito.lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);

    factionSyncService = new FactionSyncService(objectMapper, syncProperties);
    injectMockDependencies();
    factionSyncService = spy(factionSyncService); // create a spy for stubbing

    lenient().doReturn(true).when(factionSyncService).validateNotionToken("Factions");
  }

  private void injectMockDependencies() {
    ReflectionTestUtils.setField(factionSyncService, "factionRepository", factionRepository);
    ReflectionTestUtils.setField(factionSyncService, "wrestlerRepository", wrestlerRepository);
    ReflectionTestUtils.setField(factionSyncService, "notionHandler", notionHandler);
    ReflectionTestUtils.setField(factionSyncService, "progressTracker", progressTracker);
    ReflectionTestUtils.setField(factionSyncService, "healthMonitor", healthMonitor);
    ReflectionTestUtils.setField(factionSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  void syncFactions_WhenDisabled_ShouldReturnSuccessWithoutSync() {
    // Given
    when(syncProperties.isEntityEnabled("factions")).thenReturn(false);

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    verify(notionHandler, never()).loadAllFactions();
  }

  @Test
  void syncFactions_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(factionRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(factionRepository.saveAndFlush(any(Faction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    verify(factionRepository, times(2)).saveAndFlush(any(Faction.class));
    verify(healthMonitor).recordSuccess(eq("Factions"), anyLong(), anyInt());
  }

  @Test
  void syncFactions_WhenNoFactionsFound_ShouldReturnSuccessWithZeroCount() {
    // Given
    when(notionHandler.loadAllFactions()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(factionRepository, never()).save(any(Faction.class));
  }

  @Test
  void syncFactions_WhenDuplicateFactionsExist_ShouldUpdateExisting() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    Faction existingFaction = Faction.builder().build();
    existingFaction.setId(1L);
    existingFaction.setName("The Shield");

    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionRepository.findByExternalId("faction-1")).thenReturn(Optional.of(existingFaction));
    when(factionRepository.saveAndFlush(any(Faction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(factionRepository, atLeast(1)).saveAndFlush(any(Faction.class));
  }

  @Test
  void syncFactions_WhenNotionHandlerThrowsException_ShouldReturnFailure() {
    // Given
    when(notionHandler.loadAllFactions()).thenThrow(new RuntimeException("Notion API error"));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().contains("Notion API error"));
    verify(progressTracker).failOperation(eq("test-operation"), anyString());
    verify(healthMonitor).recordFailure(eq("Factions"), anyString());
  }

  @Test
  void syncFactions_WhenRepositoryThrowsException_ShouldReturnFailure() {
    // Given
    List<FactionPage> mockPages = createMockFactionPages();
    when(notionHandler.loadAllFactions()).thenReturn(mockPages);
    when(factionRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
    when(factionRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(factionRepository.saveAndFlush(any(Faction.class)))
        .thenThrow(new RuntimeException("Database error"));

    // When
    SyncResult result = factionSyncService.syncFactions("test-operation");

    // Then
    assertFalse(result.isSuccess());
    assertTrue(result.getErrorMessage().contains("Some factions failed to sync"));
  }

  private List<FactionPage> createMockFactionPages() {
    FactionPage faction1 = createMockFactionPage("faction-1", "The Shield", "Active", "2012-11-18");
    FactionPage faction2 = createMockFactionPage("faction-2", "DX", "Disbanded", "1997-08-11");
    return Arrays.asList(faction1, faction2);
  }

  private FactionPage createMockFactionPage(
      String id, String name, String status, String formedDate) {
    FactionPage page = mock(FactionPage.class);
    when(page.getId()).thenReturn(id);

    // Mock raw properties
    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Status", Map.of("select", Map.of("name", status)));
    properties.put("FormedDate", Map.of("date", Map.of("start", formedDate)));
    when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
