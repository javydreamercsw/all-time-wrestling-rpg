package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitleSyncServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private NotionRateLimitService rateLimitService;

  private TitleSyncService titleSyncService;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    titleSyncService = new TitleSyncService(new ObjectMapper(), syncProperties);

    // Manually inject the mocks using reflection
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "titleRepository", titleRepository);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "wrestlerRepository", wrestlerRepository);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "titleService", titleService);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "notionHandler", notionHandler);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "progressTracker", progressTracker);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "healthMonitor", healthMonitor);
    org.springframework.test.util.ReflectionTestUtils.setField(
        titleSyncService, "rateLimitService", rateLimitService);
  }

  @Test
  void syncTitles_shouldUpdateChampionAndContender_whenRelationsExist() {
    // Arrange
    Wrestler champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Champion Wrestler");
    champion.setExternalId("champion-wrestler-id");

    Wrestler contender = new Wrestler();
    contender.setId(2L);
    contender.setName("Contender Wrestler");
    contender.setExternalId("contender-wrestler-id");

    Title existingTitle = new Title();
    existingTitle.setId(100L);
    existingTitle.setName("ATW World");

    TitlePage titlePage = new TitlePage();
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", "ATW World");
    rawProperties.put("Current Champion", "champion-wrestler-id");
    rawProperties.put("👤 #1 Contenders", "contender-wrestler-id");
    titlePage.setRawProperties(rawProperties);

    when(syncProperties.isEntityEnabled("titles")).thenReturn(true);
    when(notionHandler.loadAllTitles()).thenReturn(Collections.singletonList(titlePage));
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(existingTitle));
    when(wrestlerRepository.findByExternalId("champion-wrestler-id"))
        .thenReturn(Optional.of(champion));
    when(wrestlerRepository.findByExternalId("contender-wrestler-id"))
        .thenReturn(Optional.of(contender));

    // Act
    titleSyncService.syncTitles("test-op");

    // Assert
    ArgumentCaptor<Title> titleCaptor = ArgumentCaptor.forClass(Title.class);
    verify(titleRepository, times(2)).save(titleCaptor.capture());

    List<Title> savedTitles = titleCaptor.getAllValues();
    Title finalSave = savedTitles.get(savedTitles.size() - 1);

    assertNotNull(finalSave.getCurrentChampion());
    assertEquals("Champion Wrestler", finalSave.getCurrentChampion().getName());
    assertNotNull(finalSave.getNumberOneContender());
    assertEquals("Contender Wrestler", finalSave.getNumberOneContender().getName());
  }
}
