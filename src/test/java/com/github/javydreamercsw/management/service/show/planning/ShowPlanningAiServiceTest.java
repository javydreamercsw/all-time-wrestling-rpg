package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowPlanningAiServiceTest {

  private ShowPlanningAiService showPlanningAiService;

  @BeforeEach
  void setUp() {
    showPlanningAiService = new ShowPlanningAiService();
  }

  @Test
  void planShow() {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
  }
}
