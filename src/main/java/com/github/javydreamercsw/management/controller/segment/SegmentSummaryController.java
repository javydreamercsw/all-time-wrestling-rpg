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
  public ResponseEntity<Void> summarizeSegment(@PathVariable Long segmentId) {
    segmentSummaryService.summarizeSegment(segmentId);
    return ResponseEntity.ok().build();
  }
}
