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

import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowSchedulerService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@PageTitle("Season Settings")
@RolesAllowed({"ROLE_ADMIN"}) // Only admin should do this
@Slf4j
@Component
@Lazy
@UIScope
public class SeasonSettingsView extends VerticalLayout {
  private final WrestlerService wrestlerService;
  private final TierBoundaryService tierBoundaryService;
  private final ShowSchedulerService showSchedulerService;
  private final SeasonService seasonService;
  private final UniverseContextService universeContextService;
  private final NotificationService notificationService;

  @Autowired
  public SeasonSettingsView(
      WrestlerService wrestlerService,
      TierBoundaryService tierBoundaryService,
      ShowSchedulerService showSchedulerService,
      SeasonService seasonService,
      UniverseContextService universeContextService,
      NotificationService notificationService) {
    this.wrestlerService = wrestlerService;
    this.tierBoundaryService = tierBoundaryService;
    this.showSchedulerService = showSchedulerService;
    this.seasonService = seasonService;
    this.universeContextService = universeContextService;
    this.notificationService = notificationService;
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
                        notificationService.showSuccess("Tier boundaries reset successfully.");
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
                          + " their current tier. This cannot be undone. Are you sure?"));

              Button confirmButton =
                  new Button(
                      "Recalibrate",
                      event -> {
                        try {
                          wrestlerService.recalibrateFanCounts(
                              universeContextService.getCurrentUniverseId());
                          notificationService.showSuccess("Fan counts recalibrated successfully.");
                        } catch (Exception e) {
                          notificationService.showError(
                              "Error during recalibration: " + e.getMessage());
                        }
                        dialog.close();
                      });
              confirmButton.setId("confirm-recalibrate-fans-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-recalibrate-fans-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    recalibrateFansButton.setId("recalibrate-fans-button");

    Button resetFansButton =
        new Button(
            "Reset All Fan Counts",
            click -> {
              Dialog dialog = new Dialog();
              dialog.setHeaderTitle("Confirm Reset All Fan Counts");
              dialog.add(
                  new Paragraph(
                      "This will reset ALL wrestlers in this universe to 0 fans and ROOKIE tier."
                          + " THIS CANNOT BE UNDONE."));

              Button confirmButton =
                  new Button(
                      "Reset Everything",
                      event -> {
                        try {
                          wrestlerService.resetAllFanCountsToZero(
                              universeContextService.getCurrentUniverseId());
                          notificationService.showSuccess("All fan counts reset to 0.");
                        } catch (Exception e) {
                          notificationService.showError("Error during reset: " + e.getMessage());
                        }
                        dialog.close();
                      });
              confirmButton.setId("confirm-reset-fans-button");

              Button cancelButton = new Button("Cancel", event -> dialog.close());
              cancelButton.setId("cancel-reset-fans-button");

              dialog.getFooter().add(cancelButton, confirmButton);
              dialog.open();
            });
    resetFansButton.setId("reset-fans-button");

    Button scheduleShowButton =
        new Button(
            "Schedule Next Show",
            click -> {
              Optional<Season> currentSeason = seasonService.getActiveSeason();
              if (currentSeason.isPresent()) {
                showSchedulerService.generateShowsForSeason(currentSeason.get());
                notificationService.showSuccess("Next show scheduled successfully.");
              } else {
                notificationService.showError("No active season found to schedule shows for.");
              }
            });
    scheduleShowButton.setId("schedule-show-button");

    add(resetBoundariesButton, recalibrateFansButton, resetFansButton, scheduleShowButton);
  }

  private void resetTiers() {
    tierBoundaryService.resetTierBoundaries();
    notificationService.showSuccess("Tier boundaries reset successfully.");
  }
}
