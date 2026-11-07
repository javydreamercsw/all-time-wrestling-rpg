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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Full-page screen the navigation freeze reroutes to while seed decisions are pending (seed-update
 * review feature, Task 7). Non-admins get the waiting panel; admins additionally get a "Review now"
 * button that jumps to the review screen ({@code seed-review}, served by {@code
 * SeedUpdateReviewView}).
 */
@Route(SeedReviewGateView.ROUTE)
@PageTitle("Source Data Review | All Time Wrestling RPG")
public class SeedReviewGateView extends VerticalLayout {

  /** This view's route — also the {@link SeedReviewGateObserver} exemption anchor. */
  public static final String ROUTE = "seed-review-gate";

  /** Route of the admin review screen (PR 3); the review button navigates here. */
  public static final String REVIEW_ROUTE = "seed-review";

  private final SecurityUtils securityUtils;

  public SeedReviewGateView(SeedReviewGate gate, SecurityUtils securityUtils) {
    this.securityUtils = securityUtils;

    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    if (!gate.isUp()) {
      // The gate is down (resolution landed between reroute and render): show nothing rather than
      // claim a freeze that is no longer happening. Navigation away happens on the next click.
      return;
    }

    H2 waitingPanel = new H2("Source data review required");
    waitingPanel.setId("gate-waiting-panel");
    Span message =
        new Span(
            """
            The app is paused until an administrator reviews the source data update. \
            Your data is safe. Nothing has been changed yet.\
            """);

    add(waitingPanel, message);

    if (securityUtils.isAdmin()) {
      Button reviewNow =
          new Button("Review now", e -> getUI().ifPresent(ui -> ui.navigate(REVIEW_ROUTE)));
      reviewNow.setId("gate-review-now-button");
      reviewNow.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      add(reviewNow);
    }
  }

  /** The freeze message; exposed so tests can assert the spec's tone without DOM walking. */
  String getMessageText() {
    return getChildren()
        .filter(Span.class::isInstance)
        .map(component -> ((Span) component).getText())
        .findFirst()
        .orElse("");
  }
}
