/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class MatchReportDialogTest extends AbstractViewTest {

  @Mock private MatchFulfillmentService fulfillmentService;
  @Mock private SecurityUtils securityUtils;

  private MatchReportDialog dialog;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(java.util.Optional.empty());

    Show show = new Show();
    show.setName("Test Show");

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match");

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);

    MatchFulfillment fulfillment = new MatchFulfillment();
    fulfillment.setSegment(segment);

    dialog = new MatchReportDialog(fulfillmentService, fulfillment, securityUtils, () -> {});
  }

  @Test
  @DisplayName("MatchReportDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "MatchReportDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain a winner ComboBox")
  void winnerComboBoxExists() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertFalse(combos.isEmpty(), "Expected at least one ComboBox (winner select)");
  }
}
