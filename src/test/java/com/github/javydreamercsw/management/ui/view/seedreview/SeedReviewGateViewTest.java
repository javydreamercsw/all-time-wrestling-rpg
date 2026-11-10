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
package com.github.javydreamercsw.management.ui.view.seedreview;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Structure tests for the full-page seed-review gate screen (seed-update review feature, Task 7):
 * waiting panel always present; "Review now" button only for admins; gate-down renders nothing.
 */
class SeedReviewGateViewTest {

  private final SecurityUtils securityUtils = Mockito.mock(SecurityUtils.class);

  private SeedReviewGateView viewWithGate(boolean up) {
    SeedReviewGate gate = Mockito.mock(SeedReviewGate.class);
    Mockito.when(gate.isUp()).thenReturn(up);
    return new SeedReviewGateView(gate, securityUtils);
  }

  private List<Button> findReviewButtons(SeedReviewGateView view) {
    return _find(view, Button.class, spec -> spec.withId("gate-review-now-button"));
  }

  @Test
  @DisplayName("Waiting panel renders for non-admins; no review button")
  void gateViewShowsWaitingPanelForNonAdmin() {
    Mockito.when(securityUtils.isAdmin()).thenReturn(false);
    SeedReviewGateView view = viewWithGate(true);

    H2 waitingPanel = _get(view, H2.class, spec -> spec.withId("gate-waiting-panel"));
    assertTrue(waitingPanel.isVisible());

    assertTrue(findReviewButtons(view).isEmpty(), "Non-admins must not see the review button");

    // Spec tone: plain, non-alarming, reassuring.
    assertTrue(waitingPanel.getText().contains("Source data review required"));
    String text = view.getMessageText();
    assertTrue(text.contains("app is paused"));
    assertTrue(text.contains("Your data is safe"));
  }

  @Test
  @DisplayName("Review-now button renders for admins alongside the waiting panel")
  void gateViewShowsReviewButtonForAdmin() {
    Mockito.when(securityUtils.isAdmin()).thenReturn(true);
    SeedReviewGateView view = viewWithGate(true);

    Button reviewButton = _get(view, Button.class, spec -> spec.withId("gate-review-now-button"));
    assertTrue(reviewButton.isVisible());
    assertTrue(_get(view, H2.class, spec -> spec.withId("gate-waiting-panel")).isVisible());
  }

  @Test
  @DisplayName("With the gate down the view renders no freeze content")
  void passesWhenGateDown() {
    Mockito.when(securityUtils.isAdmin()).thenReturn(true);
    SeedReviewGateView view = viewWithGate(false);

    assertTrue(
        view.getChildren().filter(Span.class::isInstance).toList().isEmpty(),
        "Gate is down; the freeze screen must not claim a review is running");
  }
}
