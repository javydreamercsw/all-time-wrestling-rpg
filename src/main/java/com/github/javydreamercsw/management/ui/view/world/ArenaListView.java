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
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Route("arena-list")
@PageTitle("Arenas")
@PermitAll
@Slf4j
public class ArenaListView extends Main {

  private final ArenaService arenaService;
  private final LocationService locationService;
  private final Grid<Arena> grid = new Grid<>(Arena.class);

  public ArenaListView(
      ArenaService arenaService, LocationService locationService, SecurityUtils securityUtils) {
    this.arenaService = arenaService;
    this.locationService = locationService;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    add(new ViewToolbar("Arenas"));
    add(createGrid());
    if (securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)) {
      add(createToolbar());
    }
    listItems();
  }

  private VerticalLayout createGrid() {
    grid.addColumn(Arena::getName).setHeader("Name").setSortable(true);
    grid.addColumn(arena -> arena.getLocation().getName()).setHeader("Location").setSortable(true);
    grid.addColumn(Arena::getCapacity).setHeader("Capacity").setSortable(true);
    grid.addColumn(arena -> arena.getAlignmentBias().getDisplayName())
        .setHeader("Bias")
        .setSortable(true);
    grid.addComponentColumn(
            arena -> {
              Image image = new Image(arena.getImageUrl(), "Arena Image");
              image.setHeight("100px");
              image.setWidth("100px");
              image.addClassNames(LumoUtility.BorderRadius.MEDIUM);
              return image;
            })
        .setHeader("Image");
    grid.addColumn(
            arena ->
                arena.getEnvironmentalTraits().stream().sorted().collect(Collectors.joining(", ")))
        .setHeader("Traits");

    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setHeight("100%");
    grid.asSingleSelect().addValueChangeListener(event -> editItem(event.getValue()));

    VerticalLayout layout = new VerticalLayout(grid);
    layout.setSizeFull();
    return layout;
  }

  private HorizontalLayout createToolbar() {
    Button addItemButton = new Button("Add Arena", new Icon(VaadinIcon.PLUS));
    addItemButton.addClickListener(e -> addItem());
    return new HorizontalLayout(addItemButton);
  }

  private void listItems() {
    grid.setItems(arenaService.findAll());
  }

  private void addItem() {
    ArenaFormDialog dialog =
        new ArenaFormDialog(
            arenaService,
            locationService,
            null,
            () -> {
              listItems();
              Notification.show(
                  "Arena added successfully!", 3000, Notification.Position.BOTTOM_START);
            });
    dialog.open();
  }

  private void editItem(Arena item) {
    if (item == null) {
      return;
    }
    ArenaFormDialog dialog =
        new ArenaFormDialog(
            arenaService,
            locationService,
            item,
            () -> {
              listItems();
              Notification.show(
                  "Arena updated successfully!", 3000, Notification.Position.BOTTOM_START);
            });
    dialog.open();
  }
}
