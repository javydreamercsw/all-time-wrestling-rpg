package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import org.springframework.stereotype.Service;

@Service
public class ShowPlanningAiService {

  public ProposedShow planShow(ShowPlanningContextDTO context) {
    // AI logic to plan the show will go here
    return new ProposedShow();
  }
}
