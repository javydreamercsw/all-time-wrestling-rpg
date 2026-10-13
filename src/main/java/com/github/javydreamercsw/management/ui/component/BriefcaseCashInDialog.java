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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Shared cash-in dialog for the Money in the Bank-style briefcase (ATW-8p72, ATW-3fhh): pick the
 * championship to challenge (active, reigning champion, division-eligible) and the show to book on.
 * Validation errors surface as notifications; {@code onBooked} lets the embedding view refresh.
 */
@Slf4j
public class BriefcaseCashInDialog extends Dialog {

  public BriefcaseCashInDialog(
      TitleOpportunity opportunity,
      TitleOpportunityService titleOpportunityService,
      TitleService titleService,
      ShowService showService,
      @Nullable Runnable onBooked) {
    setHeaderTitle("Cash In: " + opportunity.getName());

    // Cashable championships: active titles that currently have a reigning champion. The title's
    // denormalized champion list is fine here — the combo renders before any cash-in call, and
    // the service re-validates from the reign table inside the transaction. A gendered briefcase
    // is a division credential — only its division's titles (or ungendered ones) are offered.
    List<Title> cashable =
        titleService.findAll().stream()
            .filter(t -> Boolean.TRUE.equals(t.getIsActive()) && !t.getCurrentChampions().isEmpty())
            .filter(
                t ->
                    opportunity.getGender() == null
                        || t.getGender() == null
                        || t.getGender() == opportunity.getGender())
            .toList();
    ComboBox<Title> titleCombo = new ComboBox<>("Championship");
    titleCombo.setItems(cashable);
    titleCombo.setItemLabelGenerator(Title::getName);
    titleCombo.setWidthFull();
    titleCombo.setAllowCustomValue(false);
    titleCombo.setId("cash-in-title-combo");

    ComboBox<Show> showCombo = new ComboBox<>("Show");
    showCombo.setItems(showService.getUpcomingShows(50));
    showCombo.setItemLabelGenerator(
        s -> s.getName() + (s.getShowDate() != null ? " — " + s.getShowDate() : ""));
    showCombo.setWidthFull();
    showCombo.setAllowCustomValue(false);
    showCombo.setId("cash-in-show-combo");

    Span warning =
        new Span(
            "The briefcase is spent when the match is booked — win or lose. The winner takes the"
                + " championship.");
    warning.getStyle().set("color", "var(--lumo-error-text-color)");

    Button cancel = new Button("Cancel", e -> close());
    Button confirm =
        new Button(
            "Cash In",
            e -> {
              if (titleCombo.getValue() == null || showCombo.getValue() == null) {
                Notification.show(
                    "Select a championship and a show.", 3000, Notification.Position.MIDDLE);
                return;
              }
              try {
                titleOpportunityService.cashIn(
                    opportunity.getId(),
                    titleCombo.getValue().getId(),
                    showCombo.getValue().getId());
                close();
                Notification.show(
                        "Cash-in booked on " + showCombo.getValue().getName() + "!",
                        3000,
                        Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                if (onBooked != null) {
                  onBooked.run();
                }
              } catch (Exception ex) {
                log.error("Error cashing in briefcase", ex);
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirm.setId("cash-in-confirm");
    confirm.setEnabled(false);
    titleCombo.addValueChangeListener(
        e -> confirm.setEnabled(e.getValue() != null && showCombo.getValue() != null));
    showCombo.addValueChangeListener(
        e -> confirm.setEnabled(e.getValue() != null && titleCombo.getValue() != null));

    add(new VerticalLayout(titleCombo, showCombo, warning));
    getFooter().add(cancel, confirm);
  }
}
