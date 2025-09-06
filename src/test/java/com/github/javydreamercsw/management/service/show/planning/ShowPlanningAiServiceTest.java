package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.*;

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
    ShowPlanningContext context = new ShowPlanningContext();

    // When
    ProposedShow proposedShow = showPlanningAiService.planShow(context);

    // Then
    assertNotNull(proposedShow);
  }
}
