package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.util.List;
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
class TitleReignSyncIT extends ManagementIntegrationTest {
  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @MockBean private NotionHandler notionHandler;

  @Mock private TitleReignPage titleReignPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should Sync Title Reigns From Notion")
  void shouldSyncTitleReignsFromNotion() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      log.info("👑 Starting title reign sync integration test...");

      // Given
      Wrestler wrestler = createTestWrestler("Test Wrestler");
      wrestler.setExternalId("wrestler-id");
      wrestlerRepository.save(wrestler);

      Title title = new Title();
      title.setName("Test Title");
      title.setExternalId("title-id");
      titleRepository.save(title);

      String reignId = UUID.randomUUID().toString();
      when(titleReignPage.getId()).thenReturn(reignId);
      when(titleReignPage.getTitleRelationId()).thenReturn("title-id");
      when(titleReignPage.getChampionRelationId()).thenReturn("wrestler-id");
      when(titleReignPage.getReignNumber()).thenReturn(1);
      when(titleReignPage.getNotes()).thenReturn("Test Notes");
      when(titleReignPage.getStartDate()).thenReturn(LocalDate.now().toString());

      when(notionHandler.loadAllTitleReigns()).thenReturn(List.of(titleReignPage));

      // When
      BaseSyncService.SyncResult result =
          notionSyncService.syncTitleReigns("integration-test-title-reigns");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getSyncedCount()).isEqualTo(1);

      List<TitleReign> reigns = titleReignRepository.findAll();
      assertThat(reigns).hasSize(1);
      TitleReign reign = reigns.get(0);
      assertThat(reign.getExternalId()).isEqualTo(reignId);
      assertThat(reign.getTitle().getName()).isEqualTo("Test Title");
      assertThat(reign.getChampions()).hasSize(1);
      assertThat(reign.getChampions().get(0).getName()).isEqualTo("Test Wrestler");
    }
  }
}
