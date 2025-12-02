package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.FactionRivalryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
@DisplayName("Faction Rivalry Sync Integration Tests")
class FactionRivalrySyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockBean private NotionHandler notionHandler;

  @Mock private FactionRivalryPage factionRivalryPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database successfully")
  void shouldSyncFactionRivalriesFromNotionToDatabaseSuccessfully() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      // Given
      String faction1Name = "Faction 1";
      Faction f1 = new Faction();
      f1.setName(faction1Name);
      factionRepository.save(f1);

      String faction2Name = "Faction 2";
      Faction f2 = new Faction();
      f2.setName(faction2Name);
      factionRepository.save(f2);

      String rivalryId = UUID.randomUUID().toString();
      when(factionRivalryPage.getId()).thenReturn(rivalryId);
      when(factionRivalryPage.getRawProperties())
          .thenReturn(Map.of("Faction 1", faction1Name, "Faction 2", faction2Name, "Heat", "10"));
      when(notionHandler.loadAllFactionRivalries()).thenReturn(List.of(factionRivalryPage));

      // When - Sync faction rivalries from real Notion database
      BaseSyncService.SyncResult result =
          notionSyncService.syncFactionRivalries("test-operation-faction-rivalry-123");

      // Then - Verify sync completed successfully
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getSyncedCount()).isEqualTo(1);

      // Verify database state is consistent
      List<FactionRivalry> finalRivalries = factionRivalryRepository.findAll();
      assertThat(finalRivalries).hasSize(1);
      FactionRivalry rivalry = finalRivalries.get(0);
      assertThat(rivalry.getExternalId()).isEqualTo(rivalryId);
      assertThat(rivalry.getHeat()).isEqualTo(10);
      assertThat(rivalry.getFaction1().getName()).isEqualTo(faction1Name);
      assertThat(rivalry.getFaction2().getName()).isEqualTo(faction2Name);
    }
  }
}
