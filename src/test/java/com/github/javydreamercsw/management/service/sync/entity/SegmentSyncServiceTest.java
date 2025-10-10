package com.github.javydreamercsw.management.service.sync.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentSyncServiceTest extends BaseTest {

  @Mock private SegmentService segmentService;
  @Mock private ShowService showService;
  @Mock private WrestlerService wrestlerService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private ShowSyncService showSyncService;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotionRateLimitService rateLimitService;

  private SegmentSyncService segmentSyncService;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    segmentSyncService = new SegmentSyncService(objectMapper, syncProperties);
    injectMockDependencies();
  }

  private void injectMockDependencies() {
    try {
      setField(segmentSyncService, "segmentService", segmentService);
      setField(segmentSyncService, "showService", showService);
      setField(segmentSyncService, "wrestlerService", wrestlerService);
      setField(segmentSyncService, "segmentTypeService", segmentTypeService);
      setField(segmentSyncService, "showSyncService", showSyncService);
      setField(segmentSyncService, "notionHandler", notionHandler);
      setField(segmentSyncService, "progressTracker", progressTracker);
      setField(segmentSyncService, "healthMonitor", healthMonitor);
      setField(segmentSyncService, "rateLimitService", rateLimitService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mock dependencies", e);
    }
  }

  @Test
  void syncSegments_WithExistingParticipants_ShouldUpdateCorrectly() {
    // Given
    SegmentDTO segmentDTO = new SegmentDTO();
    segmentDTO.setExternalId("segment-1");
    segmentDTO.setName("Test Segment");
    segmentDTO.setShowExternalId("show-1");
    segmentDTO.setSegmentTypeName("Test Type");
    segmentDTO.setSegmentDate(Instant.now());
    segmentDTO.setParticipantNames(List.of("Wrestler A", "Wrestler C"));
    segmentDTO.setWinnerNames(List.of("Wrestler C"));

    Segment existingSegment = spy(new Segment());
    Wrestler wrestlerA = new Wrestler();
    wrestlerA.setName("Wrestler A");
    Wrestler wrestlerB = new Wrestler();
    wrestlerB.setName("Wrestler B");
    existingSegment.addParticipant(wrestlerA);
    existingSegment.addParticipant(wrestlerB);

    Show show = new Show();
    show.setExternalId("show-1");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Type");

    Wrestler wrestlerC = new Wrestler();
    wrestlerC.setName("Wrestler C");

    when(segmentService.findByExternalId("segment-1")).thenReturn(Optional.of(existingSegment));
    when(showService.findByExternalId("show-1")).thenReturn(Optional.of(show));
    when(segmentTypeService.findByName("Test Type")).thenReturn(Optional.of(segmentType));
    when(wrestlerService.findByName("Wrestler A")).thenReturn(Optional.of(wrestlerA));
    when(wrestlerService.findByName("Wrestler C")).thenReturn(Optional.of(wrestlerC));

    // When
    boolean result = segmentSyncService.processSingleSegment(segmentDTO);

    // Then
    assertTrue(result);
    verify(existingSegment, times(1)).syncParticipants(any());
    verify(segmentService, times(1)).updateSegment(existingSegment);

    // Verify participants were updated correctly
    List<String> participantNames = new ArrayList<>();
    existingSegment.getParticipants().forEach(p -> participantNames.add(p.getWrestler().getName()));
    assertTrue(participantNames.contains("Wrestler A"));
    assertTrue(participantNames.contains("Wrestler C"));
    assertTrue(participantNames.size() == 2);

    // Verify winners were updated correctly
    List<String> winnerNames = new ArrayList<>();
    existingSegment.getWinners().forEach(w -> winnerNames.add(w.getName()));
    assertTrue(winnerNames.contains("Wrestler C"));
    assertTrue(winnerNames.size() == 1);
  }
}
