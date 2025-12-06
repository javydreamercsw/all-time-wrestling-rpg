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
package com.github.javydreamercsw.management.controller.show;

import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/show-planning")
@RequiredArgsConstructor
public class ShowPlanningController {

  private final ShowPlanningService showPlanningService;
  private final ShowPlanningAiService showPlanningAiService;
  private final ShowService showService;

  @GetMapping("/context/{showId}")
  public ResponseEntity<ShowPlanningContextDTO> getShowPlanningContext(@PathVariable Long showId) {
    return showService
        .getShowById(showId)
        .map(show -> ResponseEntity.ok(showPlanningService.getShowPlanningContext(show)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/plan")
  public ResponseEntity<ProposedShow> planShow(@RequestBody ShowPlanningContextDTO context) {
    return ResponseEntity.ok(showPlanningAiService.planShow(context));
  }

  @PostMapping("/approve/{showId}")
  public ResponseEntity<Void> approveSegments(
      @PathVariable Long showId, @RequestBody List<ProposedSegment> segments) {
    showService
        .getShowById(showId)
        .ifPresent(show -> showPlanningService.approveSegments(show, segments));
    return ResponseEntity.ok().build();
  }
}
