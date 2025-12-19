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

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.exception.NotionAPIError;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SegmentSummaryService {

  private final SegmentService segmentService;
  private final SegmentNarrationServiceFactory narrationServiceFactory;

  @PreAuthorize("hasAnyAuthority('ADMIN', 'BOOKER')")
  public Segment summarizeSegment(@NonNull Long segmentId) {
    Segment segment =
        segmentService
            .findById(segmentId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid segment Id:" + segmentId));

    if (segment.getNarration() != null && !segment.getNarration().isEmpty()) {
      List<SegmentNarrationService> services =
          narrationServiceFactory.getAvailableServicesInPriorityOrder();

      String summary;
      for (SegmentNarrationService narrationService : services) {
        try {
          summary = narrationService.summarizeNarration(segment.getNarration());
          if (summary != null && !summary.isEmpty()) {
            segment.setSummary(summary);
            return segmentService.updateSegment(segment);
          }
        } catch (RuntimeException e) {
          if (e.getCause() instanceof NotionAPIError apiError) {
            log.error(
                "AI summary failed for segment {}: Notion API Error - {} (Status: {}). Trying next"
                    + " provider...",
                segmentId,
                apiError.getError().getMessage(),
                apiError.getError().getStatus());
          } else {
            log.error(
                "AI summary failed for segment {}: Unexpected error. Trying next provider...",
                segmentId,
                e);
          }
        }
      }
      log.warn(
          "Failed to generate summary for segment {} after trying all available AI providers.",
          segmentId);
    }
    return segment;
  }
}
