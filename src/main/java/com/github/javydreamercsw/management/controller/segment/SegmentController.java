package com.github.javydreamercsw.management.controller.segment;

import com.github.javydreamercsw.management.service.segment.SegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
public class SegmentController {

  private final SegmentService segmentService;

  @PutMapping("/{segmentId}/narration")
  public ResponseEntity<Void> updateNarration(
      @PathVariable Long segmentId, @RequestBody String narration) {
    segmentService
        .findById(segmentId)
        .ifPresent(
            segment -> {
              segment.setNarration(narration);
              segmentService.updateSegment(segment);
            });
    return ResponseEntity.ok().build();
  }
}
