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
package com.github.javydreamercsw.management.ui.view.season;

import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@PageTitle("Season Settings")
@RolesAllowed({"ROLE_ADMIN"}) // Only admin should do this
@Slf4j
@Component
@Lazy
public class SeasonSettingsView extends VerticalLayout {
  private final WrestlerService wrestlerService;
  private final TierBoundaryService tierBoundaryService;

  public SeasonSettingsView(
      WrestlerService wrestlerService, TierBoundaryService tierBoundaryService) {
    this.wrestlerService = wrestlerService;
    this.tierBoundaryService = tierBoundaryService;
    log.info("Creating SeasonSettingsView");
    add(new H2("Season Management"));

    Button resetBoundariesButton =
        new Button(
            "Reset Tier Boundaries",
            click -> {
              Dialog dialog = new Dialog();
              dialog.setHeaderTitle("Confirm Reset");
              dialog.add(
                  new Paragraph(
                      "Are you sure you want to reset all tier boundaries to their default values?"
                          + " This might affect wrestler's tiers."));

              Button confirmButton =
                  new Button(
                      "Confirm",
                      event -> {
                        resetTiers();
                        Notification.show("Tier boundaries reset successfully.");
                        dialog.close();
                      });
              confirmButton.setId("confirm-reset-boundaries-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-reset-boundaries-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    resetBoundariesButton.setId("reset-boundaries-button");

    Button recalibrateFansButton =
        new Button(
            "Recalibrate Fan Counts",
            click -> {
              Dialog dialog = new Dialog();
              dialog.setHeaderTitle("Confirm Fan Count Recalibration");
              dialog.add(
                  new Paragraph(
                      "'Recalibration' means wrestler fan counts will be reset to the minimum of"
                          + " their current tier. Any Icon tiered wrestler will be downgraded to"
                          + " Main Eventer."));
              dialog.add(new Paragraph("This will also trigger a Tier boundary reset."));
              dialog.add(
                  new Paragraph(
                      "Are you sure you want to recalibrate all wrestler fan counts to their tier's"
                          + " minimum? This action cannot be undone."));

              Button confirmButton =
                  new Button(
                      "Confirm",
                      event -> {
                        recalibrateFans();
                        resetTiers();
                        dialog.close();
                      });
              confirmButton.setId("confirm-recalibrate-fans-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-recalibrate-fans-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    recalibrateFansButton.setId("recalibrate-fans-button");

    Button fullResetButton =
        new Button(
            "Full Fan Count Reset",
            click -> {
              Dialog dialog = new Dialog();
              dialog.setHeaderTitle("Confirm Full Fan Count Reset");
              dialog.add(
                  new Paragraph(
                      "This will reset all wrestler fan counts to 0 and their tier to ROOKIE."
                          + " This action cannot be undone."));
              dialog.add(new Paragraph("This will also trigger a Tier boundary reset."));

              Button confirmButton =
                  new Button(
                      "Confirm",
                      event -> {
                        wrestlerService.resetAllFanCountsToZero();
                        Notification.show(
                                "All wrestler fan counts have been reset to 0.",
                                3000,
                                Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        resetTiers();
                        dialog.close();
                      });
              confirmButton.setId("confirm-full-reset-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-full-reset-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    fullResetButton.setId("full-reset-button");

    add(recalibrateFansButton, resetBoundariesButton, fullResetButton);
  }

  private void recalibrateFans() {
    wrestlerService.recalibrateFanCounts();
    Notification.show(
            "Fan counts recalibrated successfully.", 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void resetTiers() {
    tierBoundaryService.resetTierBoundaries();
    Notification.show(
            "Tier boundaries reset successfully.", 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
