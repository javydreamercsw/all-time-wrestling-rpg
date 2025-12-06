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
package com.github.javydreamercsw.management.controller.segment;

import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
public class SegmentSummaryController {

  private final SegmentSummaryService segmentSummaryService;

  @PostMapping("/{segmentId}/summarize")
  public ResponseEntity<com.github.javydreamercsw.management.domain.show.segment.Segment>
      summarizeSegment(@PathVariable Long segmentId) {
    com.github.javydreamercsw.management.domain.show.segment.Segment updatedSegment =
        segmentSummaryService.summarizeSegment(segmentId);
    return ResponseEntity.ok(updatedSegment);
  }
}
