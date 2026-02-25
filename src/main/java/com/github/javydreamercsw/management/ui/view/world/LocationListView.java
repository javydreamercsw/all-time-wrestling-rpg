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

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
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
  private final SecurityUtils securityUtils;
  private final ImageStorageService storageService;
  private final Grid<Location> grid = new Grid<>();

  public LocationListView(
      LocationService service, SecurityUtils securityUtils, ImageStorageService storageService) {
    this.service = service;
    this.securityUtils = securityUtils;
    this.storageService = storageService;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Height.FULL,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    if (securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)) {
      add(new ViewToolbar("Locations", ViewToolbar.group(createToolbar())));
    } else {
      add(new ViewToolbar("Locations"));
    }
    add(createGrid());
    listItems();
  }

  private Component createGrid() {
    grid.addColumn(Location::getName).setHeader("Name").setSortable(true).setAutoWidth(true);
    grid.addColumn(Location::getDescription).setHeader("Description").setFlexGrow(1);
    grid.addComponentColumn(
            location -> {
              if (location.getImageUrl() == null || location.getImageUrl().isBlank()) {
                return new Span("No Image");
              }
              Image image = new Image(location.getImageUrl(), "Location Image");
              image.setHeight("100px");
              image.setWidth("100px");
              image.addClassNames(LumoUtility.BorderRadius.MEDIUM);
              return image;
            })
        .setHeader("Image")
        .setAutoWidth(true);
    grid.addColumn(
            location ->
                location.getCulturalTags().stream().sorted().collect(Collectors.joining(", ")))
        .setHeader("Cultural Tags")
        .setAutoWidth(true);

    if (securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)) {
      grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setAutoWidth(true);
    }

    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.addClassNames(LumoUtility.Flex.GROW);
    grid.asSingleSelect().addValueChangeListener(event -> editItem(event.getValue()));

    return grid;
  }

  private Component createActionButtons(Location location) {
    Button editButton = new Button(new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.addClickListener(e -> editItem(location));

    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(e -> deleteItem(location));

    return new HorizontalLayout(editButton, deleteButton);
  }

  private void deleteItem(Location location) {
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Delete Location");
    confirmDialog.add(
        new Span(
            "Are you sure you want to delete '"
                + location.getName()
                + "'? This will fail if any arenas are linked to it."));

    Button confirmBtn =
        new Button(
            "Delete",
            e -> {
              try {
                service.deleteLocation(location.getId());
                Notification.show("Location deleted successfully!");
                listItems();
                confirmDialog.close();
              } catch (Exception ex) {
                Notification.show("Error deleting location: " + ex.getMessage());
              }
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

    Button cancelBtn = new Button("Cancel", e -> confirmDialog.close());

    confirmDialog.getFooter().add(cancelBtn, confirmBtn);
    confirmDialog.open();
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
            storageService,
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
            storageService,
            item,
            () -> {
              listItems();
              Notification.show(
                  "Location updated successfully!", 3000, Notification.Position.BOTTOM_START);
            });
    dialog.open();
  }
}
