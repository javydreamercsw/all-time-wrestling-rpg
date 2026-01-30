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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import java.util.List;
import lombok.NonNull;

public class MatchReportDialog extends Dialog {

  private final MatchFulfillmentService fulfillmentService;
  private final MatchFulfillment fulfillment;
  private final SecurityUtils securityUtils;
  private final Runnable onSave;

  public MatchReportDialog(
      @NonNull MatchFulfillmentService fulfillmentService,
      @NonNull MatchFulfillment fulfillment,
      @NonNull SecurityUtils securityUtils,
      @NonNull Runnable onSave) {
    this.fulfillmentService = fulfillmentService;
    this.fulfillment = fulfillment;
    this.securityUtils = securityUtils;
    this.onSave = onSave;

    setHeaderTitle("Report Match Result");

    VerticalLayout layout = new VerticalLayout();
    layout.add(new H3(fulfillment.getSegment().getShow().getName()));
    layout.add(new Span("Match: " + fulfillment.getSegment().getSegmentType().getName()));

    List<Wrestler> participants = fulfillment.getSegment().getWrestlers();
    Select<Wrestler> winnerSelect = new Select<>();
    winnerSelect.setLabel("Winner");
    winnerSelect.setItems(participants);
    winnerSelect.setItemLabelGenerator(Wrestler::getName);

    if (fulfillment.getReportedWinner() != null) {
      winnerSelect.setValue(fulfillment.getReportedWinner());
    }

    layout.add(winnerSelect);

    Button submitButton =
        new Button(
            "Submit Result",
            e -> {
              if (winnerSelect.getValue() == null) {
                Notification.show("Please select a winner.");
                return;
              }
              securityUtils
                  .getAuthenticatedUser()
                  .ifPresent(
                      user -> {
                        fulfillmentService.submitResult(
                            fulfillment, winnerSelect.getValue(), user.getAccount());
                        onSave.run();
                        close();
                      });
            });
    submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(new HorizontalLayout(submitButton, cancelButton));
    add(layout);
  }
}
