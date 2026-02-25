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
package com.github.javydreamercsw.management.ui.view.world;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Route("location-list")
@PageTitle("Locations")
@PermitAll
@Slf4j
public class LocationListView extends Main {

  private final LocationService service;
  private final Grid<Location> grid = new Grid<>(Location.class);

  public LocationListView(LocationService service, SecurityUtils securityUtils) {
    this.service = service;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    add(new ViewToolbar("Locations"));
    add(createGrid());
    if (securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)) {
      add(createToolbar());
    }
    listItems();
  }

  private Component createGrid() {
    grid.addColumn(Location::getName).setHeader("Name").setSortable(true);
    grid.addColumn(Location::getDescription).setHeader("Description");
    grid.addColumn(
            location ->
                location.getCulturalTags().stream().sorted().collect(Collectors.joining(", ")))
        .setHeader("Cultural Tags");

    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setHeight("100%");
    grid.asSingleSelect().addValueChangeListener(event -> editItem(event.getValue()));

    return grid;
  }

  private HorizontalLayout createToolbar() {
    Button addItemButton = new Button("Add Location", new Icon(VaadinIcon.PLUS));
    addItemButton.addClickListener(e -> addItem());
    return new HorizontalLayout(addItemButton);
  }

  private void listItems() {
    grid.setItems(service.findAll());
  }

  private void addItem() {
    LocationFormDialog dialog =
        new LocationFormDialog(
            service,
            null,
            () -> {
              listItems();
              Notification.show(
                  "Location added successfully!", 3000, Notification.Position.BOTTOM_START);
            });
    dialog.open();
  }

  private void editItem(Location item) {
    if (item == null) {
      return;
    }
    LocationFormDialog dialog =
        new LocationFormDialog(
            service,
            item,
            () -> {
              listItems();
              Notification.show(
                  "Location updated successfully!", 3000, Notification.Position.BOTTOM_START);
            });
    dialog.open();
  }
}
