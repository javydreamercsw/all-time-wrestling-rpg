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
package com.github.javydreamercsw.management.ui.view.admin;

import com.github.javydreamercsw.management.service.expansion.Expansion;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;

@SpringComponent
@UIScope
@Slf4j
public class ExpansionManagementView extends VerticalLayout {

  private final ExpansionService expansionService;
  private final Grid<Expansion> grid = new Grid<>(Expansion.class, false);

  public ExpansionManagementView(ExpansionService expansionService) {
    this.expansionService = expansionService;
    initializeUI();
  }

  private void initializeUI() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    add(new H3("Expansion Packs"));
    add(
        new Span(
            "Enable or disable expansion packs to control content visibility (Wrestlers, Teams,"
                + " Factions)."));

    grid.addColumn(Expansion::getName)
        .setHeader("Expansion Name")
        .setSortable(true)
        .setAutoWidth(true);
    grid.addColumn(Expansion::getCode).setHeader("Code").setSortable(true).setAutoWidth(true);
    grid.addComponentColumn(this::createEnabledCheckbox).setHeader("Enabled").setAutoWidth(true);

    grid.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
    grid.setItems(expansionService.getExpansions());

    add(grid);
  }

  private Checkbox createEnabledCheckbox(Expansion expansion) {
    Checkbox checkbox = new Checkbox(expansion.isEnabled());
    checkbox.setId("expansion-toggle-" + expansion.getCode());
    checkbox.addValueChangeListener(
        event -> {
          expansionService.setExpansionEnabled(expansion.getCode(), event.getValue());
          String status = event.getValue() ? "enabled" : "disabled";
          Notification.show(
                  String.format("Expansion '%s' has been %s.", expansion.getName(), status),
                  3000,
                  Notification.Position.TOP_END)
              .addThemeVariants(
                  event.getValue()
                      ? NotificationVariant.LUMO_SUCCESS
                      : NotificationVariant.LUMO_CONTRAST);
          log.info(
              "Expansion '{}' ({}) toggled to: {}",
              expansion.getName(),
              expansion.getCode(),
              event.getValue());
        });
    return checkbox;
  }

  public void refresh() {
    grid.setItems(expansionService.getExpansions());
  }
}
