package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.FactionRivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.InjurySyncService;
import com.github.javydreamercsw.management.service.sync.entity.NpcSyncService;
import com.github.javydreamercsw.management.service.sync.entity.RivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.SegmentSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTemplateSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.TitleReignSyncService;
import com.github.javydreamercsw.management.service.sync.entity.TitleSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
@DisplayName("Notion Sync Service Unit Tests")
@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

  @Mock private ShowTypeSyncService showTypeSyncService;
  @Mock private SeasonSyncService seasonSyncService;
  @Mock private ShowTemplateSyncService showTemplateSyncService;
  @Mock private ShowSyncService showSyncService;
  @Mock private WrestlerSyncService wrestlerSyncService;
  @Mock private FactionSyncService factionSyncService;
  @Mock private TeamSyncService teamSyncService;
  @Mock private TitleSyncService titleSyncService;
  @Mock private TitleReignSyncService titleReignSyncService;
  @Mock private InjurySyncService injurySyncService;
  @Mock private NpcSyncService npcSyncService;
  @Mock private SegmentSyncService segmentSyncService;
  @Mock private RivalrySyncService rivalrySyncService;
  @Mock private FactionRivalrySyncService factionRivalrySyncService;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties syncProperties;

  private NotionSyncService notionSyncService;

  @BeforeEach
  public void setUp() {
    log.info("🧪 Setting up NotionSyncServiceTest");
    when(syncProperties.getParallelThreads()).thenReturn(1);
    notionSyncService = new NotionSyncService(objectMapper, syncProperties);
    ReflectionTestUtils.setField(notionSyncService, "showTypeSyncService", showTypeSyncService);
    ReflectionTestUtils.setField(notionSyncService, "seasonSyncService", seasonSyncService);
    ReflectionTestUtils.setField(
        notionSyncService, "showTemplateSyncService", showTemplateSyncService);
    ReflectionTestUtils.setField(notionSyncService, "showSyncService", showSyncService);
    ReflectionTestUtils.setField(notionSyncService, "wrestlerSyncService", wrestlerSyncService);
    ReflectionTestUtils.setField(notionSyncService, "factionSyncService", factionSyncService);
    ReflectionTestUtils.setField(notionSyncService, "teamSyncService", teamSyncService);
    ReflectionTestUtils.setField(notionSyncService, "titleSyncService", titleSyncService);
    ReflectionTestUtils.setField(notionSyncService, "titleReignSyncService", titleReignSyncService);
    ReflectionTestUtils.setField(notionSyncService, "injurySyncService", injurySyncService);
    ReflectionTestUtils.setField(notionSyncService, "npcSyncService", npcSyncService);
    ReflectionTestUtils.setField(notionSyncService, "segmentSyncService", segmentSyncService);
    ReflectionTestUtils.setField(notionSyncService, "rivalrySyncService", rivalrySyncService);
    ReflectionTestUtils.setField(
        notionSyncService, "factionRivalrySyncService", factionRivalrySyncService);
  }

  @Test
  @DisplayName("Should sync show types from Notion to database")
  void shouldSyncShowTypesFromNotionToDatabase() {
    log.info("🎭 Testing show types sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Show Types", 1, 0, 0);
    when(showTypeSyncService.syncShowTypes(anyString())).thenReturn(expectedResult);

    // When - Sync show types from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncShowTypes("integration-test-show-types");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(showTypeSyncService, times(1)).syncShowTypes(anyString());
  }

  @Test
  @DisplayName("Should sync wrestlers from Notion to database")
  void shouldSyncWrestlersFromNotionToDatabase() {
    log.info("🤼 Testing wrestlers sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Wrestlers", 1, 0, 0);
    when(wrestlerSyncService.syncWrestlers(anyString())).thenReturn(expectedResult);

    // When - Sync wrestlers from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncWrestlers("integration-test-wrestlers");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(wrestlerSyncService, times(1)).syncWrestlers(anyString());
  }

  @Test
  @DisplayName("Should sync seasons from Notion to database")
  void shouldSyncSeasonsFromNotionToDatabase() {
    log.info("📅 Testing seasons sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Seasons", 1, 0, 0);
    when(seasonSyncService.syncSeasons(anyString())).thenReturn(expectedResult);

    // When - Sync seasons from Notion
    BaseSyncService.SyncResult result = notionSyncService.syncSeasons("integration-test-seasons");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(seasonSyncService, times(1)).syncSeasons(anyString());
  }

  @Test
  @DisplayName("Should sync show templates from Notion to database")
  void shouldSyncShowTemplatesFromNotionToDatabase() {
    log.info("📋 Testing show templates sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Show Templates", 1, 0, 0);
    when(showTemplateSyncService.syncShowTemplates(anyString())).thenReturn(expectedResult);

    // When - Sync show templates from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncShowTemplates("integration-test-templates");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(showTemplateSyncService, times(1)).syncShowTemplates(anyString());
  }

  @Test
  @DisplayName("Should sync factions from Notion to database")
  void shouldSyncFactionsFromNotionToDatabase() {
    log.info("👥 Testing factions sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Factions", 1, 0, 0);
    when(factionSyncService.syncFactions(anyString())).thenReturn(expectedResult);

    // When - Sync factions from Notion
    BaseSyncService.SyncResult result = notionSyncService.syncFactions("integration-test-factions");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(factionSyncService, times(1)).syncFactions(anyString());
  }

  @Test
  @DisplayName("Should sync shows from Notion to database")
  void shouldSyncShowsFromNotionToDatabase() {
    log.info("📺 Testing shows sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Shows", 1, 0, 0);
    when(showSyncService.syncShows()).thenReturn(expectedResult);

    // When - Sync shows from Notion
    BaseSyncService.SyncResult result = notionSyncService.syncShows();

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(showSyncService, times(1)).syncShows();
  }

  @Test
  @DisplayName("Should sync injury types from Notion to database")
  void shouldSyncInjuryTypesFromNotionToDatabase() {
    log.info("🩹 Testing injury types sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Injury Types", 1, 0, 0);
    when(injurySyncService.syncInjuryTypes(anyString())).thenReturn(expectedResult);

    // When - Sync injury types from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncInjuryTypes("integration-test-injury-types");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(injurySyncService, times(1)).syncInjuryTypes(anyString());
  }

  @Test
  @DisplayName("Should sync NPCs from Notion to database")
  void shouldSyncNpcsFromNotionToDatabase() {
    log.info("🤖 Testing NPCs sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult = BaseSyncService.SyncResult.success("NPCs", 1, 0, 0);
    when(npcSyncService.syncNpcs(anyString())).thenReturn(expectedResult);

    // When - Sync NPCs from Notion
    BaseSyncService.SyncResult result = notionSyncService.syncNpcs("integration-test-npcs");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(npcSyncService, times(1)).syncNpcs(anyString());
  }

  @Test
  @DisplayName("Should sync titles from Notion to database")
  void shouldSyncTitlesFromNotionToDatabase() {
    log.info("🏆 Testing titles sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Titles", 1, 0, 0);
    when(titleSyncService.syncTitles(anyString())).thenReturn(expectedResult);

    // When - Sync titles from Notion
    BaseSyncService.SyncResult result = notionSyncService.syncTitles("integration-test-titles");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(titleSyncService, times(1)).syncTitles(anyString());
  }

  @Test
  @DisplayName("Should sync title reigns from Notion to database")
  void shouldSyncTitleReignsFromNotionToDatabase() {
    log.info("👑 Testing title reigns sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Title Reigns", 1, 0, 0);
    when(titleReignSyncService.syncTitleReigns(anyString())).thenReturn(expectedResult);

    // When - Sync title reigns from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncTitleReigns("integration-test-title-reigns");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(titleReignSyncService, times(1)).syncTitleReigns(anyString());
  }

  @Test
  @DisplayName("Should sync rivalries from Notion to database")
  void shouldSyncRivalriesFromNotionToDatabase() {
    log.info("🔥 Testing rivalries sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Rivalries", 1, 0, 0);
    when(rivalrySyncService.syncRivalries(anyString())).thenReturn(expectedResult);

    // When - Sync rivalries from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncRivalries("integration-test-rivalries");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(rivalrySyncService, times(1)).syncRivalries(anyString());
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database")
  void shouldSyncFactionRivalriesFromNotionToDatabase() {
    log.info("🔥🔥 Testing faction rivalries sync from Notion to database");

    // Given
    BaseSyncService.SyncResult expectedResult =
        BaseSyncService.SyncResult.success("Faction Rivalries", 1, 0, 0);
    when(factionRivalrySyncService.syncFactionRivalries(anyString())).thenReturn(expectedResult);

    // When - Sync faction rivalries from Notion
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactionRivalries("integration-test-faction-rivalries");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    assertThat(result).isEqualTo(expectedResult);
    verify(factionRivalrySyncService, times(1)).syncFactionRivalries(anyString());
  }
}
