package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.SegmentPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
@DisplayName("Segment Sync Integration Tests")
class SegmentSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private SegmentRepository segmentRepository;

  @MockBean private NotionHandler notionHandler;

  @Mock private SegmentPage segmentPage;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync segments from Notion to database successfully")
  void shouldSyncSegmentsFromNotionToDatabaseSuccessfully() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      // Given
      Wrestler wrestler1 = createTestWrestler("Wrestler 1");
      Wrestler wrestler2 = createTestWrestler("Wrestler 2");

      Show show = new Show();
      show.setName("Test Show");
      show.setShowDate(LocalDate.now());
      show.setExternalId("test-show-id");
      showRepository.save(show);

      SegmentType segmentType = new SegmentType();
      segmentType.setName("Test Segment Type");
      segmentTypeRepository.save(segmentType);

      String segmentId = UUID.randomUUID().toString();
      when(segmentPage.getId()).thenReturn(segmentId);
      when(segmentPage.getRawProperties())
          .thenReturn(
              Map.of(
                  "Name", "Test Segment",
                  "Show", "test-show-id",
                  "Participants", wrestler1.getName() + "," + wrestler2.getName(),
                  "Winners", wrestler1.getName(),
                  "Segment Type", segmentType.getName(),
                  "Date", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))));
      
      when(notionHandler.getDatabasePageIds("Segments")).thenReturn(List.of(segmentId));
      when(notionHandler.loadSegmentById(segmentId)).thenReturn(Optional.of(segmentPage));

      // When
      BaseSyncService.SyncResult result = notionSyncService.syncSegments("test-operation");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getSyncedCount()).isEqualTo(1);

      List<Segment> finalSegments = segmentRepository.findAll();
      assertThat(finalSegments).hasSize(1);
      Segment segment = finalSegments.get(0);
      assertThat(segment.getExternalId()).isEqualTo(segmentId);
      assertThat(segment.getShow().getName()).isEqualTo("Test Show");
      assertThat(segment.getParticipants()).hasSize(2);
      assertThat(segment.getWinners()).hasSize(1);
      assertThat(segment.getWinners().get(0).getName()).isEqualTo(wrestler1.getName());
    }
  }
}
