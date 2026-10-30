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
package com.github.javydreamercsw.management.ui.view.league;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentResult;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MatchReportDialog} booked-result gating. */
class MatchReportDialogTest {

  private MatchFulfillmentService fulfillmentService;
  private SecurityUtils securityUtils;
  private UI ui;

  @BeforeEach
  void setUp() {
    fulfillmentService = mock(MatchFulfillmentService.class);
    securityUtils = mock(SecurityUtils.class);
    ui = new UI();
    UI.setCurrent(ui);
  }

  @AfterEach
  void tearDown() {
    UI.setCurrent(null);
  }

  private MatchFulfillment fulfillmentWithResult(SegmentResult result) {
    SegmentType type = new SegmentType();
    type.setName("One on One");
    Show show = new Show();
    show.setName("Test Show");

    Segment segment = new Segment();
    segment.setSegmentType(type);
    segment.setShow(show);
    segment.setResult(result);

    MatchFulfillment fulfillment = new MatchFulfillment();
    fulfillment.setSegment(segment);
    return fulfillment;
  }

  private ComboBox<?> findWinnerSelect(MatchReportDialog dialog) {
    for (Component child : dialog.getChildren().toList()) {
      ComboBox<?> found = findComboBox(child, "match-winner-select");
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private ComboBox<?> findComboBox(Component root, String id) {
    if (id.equals(root.getId().orElse(null)) && root instanceof ComboBox<?>) {
      return (ComboBox<?>) root;
    }
    for (Component child : root.getChildren().toList()) {
      ComboBox<?> found = findComboBox(child, id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private Button findButton(Component root, String id) {
    if (id.equals(root.getId().orElse(null)) && root instanceof Button) {
      return (Button) root;
    }
    for (Component child : root.getChildren().toList()) {
      Button found = findButton(child, id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  @Test
  void bookedDrawDialogHasNoWinnerSelector() {
    MatchReportDialog dialog =
        new MatchReportDialog(
            fulfillmentService, fulfillmentWithResult(SegmentResult.DRAW), securityUtils, () -> {});
    dialog.open();

    assertNull(
        findWinnerSelect(dialog), "a booked draw offers no result to report — no winner select");
  }

  @Test
  void bookedNoContestDialogHasNoWinnerSelector() {
    MatchReportDialog dialog =
        new MatchReportDialog(
            fulfillmentService,
            fulfillmentWithResult(SegmentResult.NO_CONTEST),
            securityUtils,
            () -> {});
    dialog.open();

    assertNull(findWinnerSelect(dialog));
  }

  @Test
  void winnersResultDialogKeepsWinnerSelector() {
    MatchFulfillment fulfillment = fulfillmentWithResult(SegmentResult.WINNERS);
    Segment segment = fulfillment.getSegment();
    com.github.javydreamercsw.management.domain.wrestler.Wrestler w =
        new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
    w.setName("Alice");
    segment.addParticipant(w);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    MatchReportDialog dialog =
        new MatchReportDialog(fulfillmentService, fulfillment, securityUtils, () -> {});
    dialog.open();

    assertNotNull(findWinnerSelect(dialog));
  }

  @Test
  void bookedDrawDialogHidesSubmitButton() {
    MatchReportDialog dialog =
        new MatchReportDialog(
            fulfillmentService, fulfillmentWithResult(SegmentResult.DRAW), securityUtils, () -> {});
    dialog.open();

    assertTrue(
        findButton(dialog, "submit-match-result-btn") == null,
        "no result entry means no submit button");
    assertFalse(dialog.getChildren().toList().isEmpty());
  }
}
