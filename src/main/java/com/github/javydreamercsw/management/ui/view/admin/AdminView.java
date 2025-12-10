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
package com.github.javydreamercsw.management.ui.view.admin;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;

@Route("admin")
@PageTitle("Admin")
@Menu(order = 11, icon = "vaadin:tools", title = "Admin")
@Slf4j
public class AdminView extends Main {

  private final TierRecalculationService tierRecalculationService;

  public AdminView(TierRecalculationService tierRecalculationService) {
    this.tierRecalculationService = tierRecalculationService;
    initializeUI();
  }

  private void initializeUI() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    add(new ViewToolbar("Admin Tools"));

    VerticalLayout content = new VerticalLayout();
    content.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);

    Button recalculateTiersButton = new Button("Recalculate Wrestler Tiers");
    recalculateTiersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    recalculateTiersButton.addClickListener(
        event -> {
          try {
            tierRecalculationService.recalculateTiers();
            Notification.show(
                    "Wrestler tiers recalculated successfully!",
                    3000,
                    Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            log.info("Manual tier recalculation triggered and completed successfully.");
          } catch (Exception e) {
            Notification.show(
                    "Error during tier recalculation: " + e.getMessage(),
                    5000,
                    Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Error during manual tier recalculation", e);
          }
        });

    content.add(recalculateTiersButton);
    add(content);
  }
}
