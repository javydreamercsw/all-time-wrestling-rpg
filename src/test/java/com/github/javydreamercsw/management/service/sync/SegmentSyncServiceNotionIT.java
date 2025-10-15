package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.sync.entity.SegmentSyncService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("SegmentSyncService Integration Tests")
@EnabledIf("isNotionTokenAvailable")
class SegmentSyncServiceNotionIT extends ManagementIntegrationTest {

  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SegmentService segmentService;

  @Test
  @DisplayName("Should sync a single segment by ID")
  void shouldSyncSingleSegmentById() {
    List<String> matchIds = segmentSyncService.getSegmentIds();
    Assumptions.assumeFalse(matchIds.isEmpty(), "No segments found in Notion to test sync.");
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
}
