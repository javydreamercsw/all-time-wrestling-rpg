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
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

@PageTitle("Season Settings")
@Route(value = "season/settings", layout = MainLayout.class)
@RolesAllowed({"ROLE_ADMIN"}) // Only admin should do this
@Slf4j
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

    Button resetFansButton =
        new Button(
            "Reset Fan Counts",
            click -> {
              Dialog dialog = new Dialog();
              dialog.setHeaderTitle("Confirm Reset");
              dialog.add(
                  new Paragraph(
                      "Are you sure you want to reset all wrestler fan counts to their tier's"
                          + " default? This action cannot be undone."));

              Button confirmButton =
                  new Button(
                      "Confirm",
                      event -> {
                        resetFans();
                        resetTiers();
                        dialog.close();
                      });
              confirmButton.setId("confirm-reset-fans-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-reset-fans-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    resetFansButton.setId("reset-fans-button");

    add(resetFansButton, resetBoundariesButton);
  }

  private void resetFans() {
    wrestlerService.resetFanCounts();
    Notification.show("Fan counts reset successfully.");
  }

  private void resetTiers() {
    tierBoundaryService.resetTierBoundaries();
    Notification.show("Tier boundaries reset successfully.");
  }
}
