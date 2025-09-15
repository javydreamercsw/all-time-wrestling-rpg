package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.sync.entity.SegmentSyncService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("SegmentSyncService Integration Tests")
@EnabledIf("isNotionTokenAvailable")
class SegmentSyncServiceIT extends BaseTest {

  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SegmentService segmentService;

  @Test
  @DisplayName("Should sync a single segment by ID")
  void shouldSyncSingleSegmentById() {
    List<String> matchIds = segmentSyncService.getSegmentIds();
    Random r = new Random();
    Arrays.asList(
            matchIds.get(r.nextInt(matchIds.size())), matchIds.get(r.nextInt(matchIds.size())))
        .forEach(
            knownSegmentId -> { // When
              SegmentSyncService.SyncResult result = segmentSyncService.syncSegment(knownSegmentId);
              // Then
              assertThat(result).isNotNull();
              assertThat(result.isSuccess()).isTrue();
              assertThat(result.getSyncedCount()).isEqualTo(1);
              Optional<Segment> segmentOptional = segmentService.findByExternalId(knownSegmentId);
              assertThat(segmentOptional).isPresent();
              Segment segment = segmentOptional.get();
              assertThat(segment.getSegmentType()).isNotNull();
              assertThat(segment.getSegmentDate()).isNotNull();
            });
  }

  @Test
  @DisplayName("Should return failure for non-existent segment ID")
  void shouldReturnFailureForNonExistentSegmentId() {
    SegmentSyncService.SyncResult result =
        segmentSyncService.syncSegment(UUID.randomUUID().toString());

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).contains("not found in Notion");
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }
}
