package com.github.javydreamercsw.management.controller.show;

import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningContext;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
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
  public ResponseEntity<ShowPlanningContext> getShowPlanningContext(@PathVariable Long showId) {
    return showService
        .getShowById(showId)
        .map(show -> ResponseEntity.ok(showPlanningService.getShowPlanningContext(show)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/plan")
  public ResponseEntity<ProposedShow> planShow(@RequestBody ShowPlanningContext context) {
    return ResponseEntity.ok(showPlanningAiService.planShow(context));
  }
}
