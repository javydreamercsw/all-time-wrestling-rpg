/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentSummaryServiceTest {

  @Mock private SegmentService segmentService;

  @Mock private SegmentNarrationServiceFactory narrationServiceFactory;

  @Mock private SegmentNarrationService narrationService;

  @InjectMocks private SegmentSummaryService segmentSummaryService;

  @Test
  void testSummarizeSegment() {
    // Given
    Long segmentId = 1L;
    Segment segment = new Segment();
    segment.setId(segmentId);
    segment.setNarration("This is a long narration.");

    when(segmentService.findById(segmentId)).thenReturn(Optional.of(segment));
    when(narrationServiceFactory.getAvailableServicesInPriorityOrder())
        .thenReturn(List.of(narrationService));
    when(narrationService.summarizeNarration(anyString())).thenReturn("This is a summary.");

    // When
    segmentSummaryService.summarizeSegment(segmentId);

    // Then
    ArgumentCaptor<Segment> segmentCaptor = ArgumentCaptor.forClass(Segment.class);
    verify(segmentService).updateSegment(segmentCaptor.capture());
    assertEquals("This is a summary.", segmentCaptor.getValue().getSummary());
  }
}
