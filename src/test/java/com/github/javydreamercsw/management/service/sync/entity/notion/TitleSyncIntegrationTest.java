package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
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
class TitleSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockBean private NotionHandler notionHandler;

  @Mock private TitlePage titlePage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should Sync Titles From Notion")
  void shouldSyncTitlesFromNotion() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      log.info("🚀 Starting real title sync integration test...");

      // Given
      String titleId = UUID.randomUUID().toString();
      when(titlePage.getId()).thenReturn(titleId);
      when(titlePage.getRawProperties()).thenReturn(Map.of("Name", "Test Title"));
      when(titlePage.getTier()).thenReturn("Main Event");
      when(titlePage.getGender()).thenReturn("MALE");

      when(notionHandler.loadAllTitles()).thenReturn(List.of(titlePage));

      // Act
      BaseSyncService.SyncResult result =
          notionSyncService.syncTitles("integration-test-titles");

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getEntityType()).isEqualTo("Titles");
      assertThat(result.getSyncedCount()).isEqualTo(1);

      List<Title> titles = titleRepository.findAll();
      assertThat(titles).hasSize(1);
      Title title = titles.get(0);
      assertThat(title.getName()).isEqualTo("Test Title");
      assertThat(title.getExternalId()).isEqualTo(titleId);
      assertThat(title.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);

      log.info("✅ Title sync completed successfully!");
    }
  }
}
