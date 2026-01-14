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
package com.github.javydreamercsw.management.ui.view.holiday;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.Holiday;
import com.github.javydreamercsw.management.service.HolidayService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * View for managing holidays in the ATW RPG system. Provides CRUD operations for holiday reference
 * data.
 */
@PageTitle("Holidays | ATW RPG")
@PermitAll
@Slf4j
@Component
@Lazy
@UIScope
public class HolidayListView extends Main {

  private final HolidayService holidayService;
  private final SecurityUtils securityUtils;
  private final Grid<Holiday> grid;
  private final TextField searchField;
  private final Button createButton;

  private Dialog editDialog;
  private Holiday editingHoliday;
  private HolidayFormDialog holidayFormDialog;

  public HolidayListView(
      @NonNull HolidayService holidayService, @NonNull SecurityUtils securityUtils) {
    this.holidayService = holidayService;
    this.securityUtils = securityUtils;
    this.grid = new Grid<>(Holiday.class, false);
    grid.setId("holiday-grid");
    this.searchField = new TextField();
    this.createButton = new Button("Create Holiday", VaadinIcon.PLUS.create());
    createButton.setId("create-holiday-button");

    setSizeFull();
    configureGrid();
    configureToolbar();
    configureEditDialog();
    updateGrid();

    add(createToolbar(), grid);
  }

  private void configureGrid() {
    grid.setSizeFull();

    // Configure columns
    grid.addColumn(Holiday::getDescription)
        .setHeader("Description")
        .setSortable(true)
        .setFlexGrow(2);

    grid.addColumn(Holiday::getTheme).setHeader("Theme").setSortable(true).setFlexGrow(1);

    grid.addComponentColumn(
            holiday -> {
              Span decorations = new Span();
              if (holiday.getDecorations() != null && !holiday.getDecorations().trim().isEmpty()) {
                String effect = holiday.getDecorations();
                if (effect.length() > 50) {
                  effect = effect.substring(0, 47) + "...";
                }
                decorations.setText(effect);
                decorations.setTitle(holiday.getDecorations()); // Full text on hover
              } else {
                decorations.setText("None");
                decorations.addClassNames(LumoUtility.TextColor.SECONDARY);
              }
              return decorations;
            })
        .setHeader("Decorations")
        .setFlexGrow(2);

    grid.addColumn(Holiday::getType).setHeader("Type").setSortable(true).setFlexGrow(1);

    grid.addColumn(
            holiday -> {
              if (holiday.getType()
                  == com.github.javydreamercsw.management.domain.HolidayType.FIXED) {
                return String.format("%s %d", holiday.getHolidayMonth(), holiday.getDayOfMonth());
              } else {
                return String.format(
                    "%s %s of %s",
                    holiday.getWeekOfMonth(), holiday.getDayOfWeek(), holiday.getHolidayMonth());
              }
            })
        .setHeader("Date Rule")
        .setFlexGrow(2);

    // Actions column
    grid.addComponentColumn(
            holiday -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              editButton.addClickListener(e -> editHoliday(holiday));
              editButton.setVisible(securityUtils.canEdit());
              editButton.setId("edit-holiday-" + holiday.getId());

              Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
              deleteButton.addThemeVariants(
                  ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
              deleteButton.addClickListener(e -> confirmDelete(holiday));
              deleteButton.setVisible(securityUtils.canDelete());
              deleteButton.setId("delete-holiday-" + holiday.getId());

              actions.add(editButton, deleteButton);
              actions.setId("actions-holiday-" + holiday.getId());
              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(1);

    // Configure data provider
    grid.setDataProvider(
        DataProvider.fromCallbacks(
            query -> {
              int offset = query.getOffset();
              int limit = query.getLimit();

              // Apply sorting
              String sortProperty = "description"; // default
              boolean ascending = true;

              if (!query.getSortOrders().isEmpty()) {
                var sortOrder = query.getSortOrders().get(0);
                sortProperty = sortOrder.getSorted();
                ascending = sortOrder.getDirection() == SortDirection.ASCENDING;
              }

              final String finalSortProperty = sortProperty;
              final boolean finalAscending = ascending;

              // This part needs adjustment based on HolidayService.findAll() capabilities
              // For simplicity, let's assume findAll() returns a sortable list for now
              // In a real app, you'd integrate with Spring Data JPA Pageable and Sort
              return holidayService.findAll().stream()
                  .sorted(
                      (h1, h2) -> {
                        int compare =
                            switch (finalSortProperty) {
                              case "description" ->
                                  h1.getDescription().compareTo(h2.getDescription());
                              case "theme" -> h1.getTheme().compareTo(h2.getTheme());
                              case "type" -> h1.getType().compareTo(h2.getType());
                              default -> 0;
                            };
                        return finalAscending ? compare : -compare;
                      })
                  .skip(offset)
                  .limit(limit);
            },
            query ->
                holidayService
                    .findAll()
                    .size())); // This count also needs adjustment for filtering/sorting
  }

  private void configureToolbar() {
    searchField.setPlaceholder("Search holidays...");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.addValueChangeListener(e -> updateGrid());
    searchField.setId("search-holiday");

    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> createHoliday());
    createButton.setVisible(securityUtils.canCreate());
    createButton.setId("create-holiday-button");
  }

  private HorizontalLayout createToolbar() {
    HorizontalLayout toolbar = new HorizontalLayout();
    toolbar.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Padding.MEDIUM);
    toolbar.setWidthFull();
    toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);

    // Left side - title and search
    VerticalLayout leftSide = new VerticalLayout();
    leftSide.setSpacing(false);
    leftSide.setPadding(false);

    H3 title = new H3("Holidays");
    title.addClassNames(LumoUtility.Margin.NONE);

    leftSide.add(title, searchField);

    // Right side - actions
    HorizontalLayout rightSide = new HorizontalLayout();
    rightSide.add(createButton);

    toolbar.add(leftSide, rightSide);
    return toolbar;
  }

  private void configureEditDialog() {
    holidayFormDialog = new HolidayFormDialog();
    editDialog = new Dialog();
    editDialog.add(holidayFormDialog);
    editDialog.setWidth("600px");
    editDialog.setCloseOnEsc(true);
    editDialog.setCloseOnOutsideClick(false);

    holidayFormDialog.addSaveListener(
        event -> {
          try {
            holidayService.save(event.getHoliday());
            showSuccessNotification("Holiday saved successfully");
            editDialog.close();
            updateGrid();
          } catch (Exception e) {
            log.error("Error saving holiday", e);
            showErrorNotification("Failed to save holiday: " + e.getMessage());
          }
        });
    holidayFormDialog.addCloseListener(event -> editDialog.close());
  }

  private void updateGrid() {
    grid.getDataProvider().refreshAll();
  }

  private void createHoliday() {
    editingHoliday = null;
    editDialog.setHeaderTitle("Create New Holiday");
    holidayFormDialog.setHoliday(new Holiday());
    editDialog.open();
  }

  private void editHoliday(@NonNull Holiday holiday) {
    editingHoliday = holiday;
    editDialog.setHeaderTitle("Edit Holiday: " + holiday.getDescription());
    holidayFormDialog.setHoliday(holiday);
    editDialog.open();
  }

  private void confirmDelete(@NonNull Holiday holiday) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Holiday");
    dialog.setText(
        "Are you sure you want to delete '"
            + holiday.getDescription()
            + "'? This action cannot be undone.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(e -> deleteHoliday(holiday));
    dialog.open();
  }

  private void deleteHoliday(@NonNull Holiday holiday) {
    try {
      holidayService.delete(holiday);
      showSuccessNotification("Holiday deleted successfully");
      updateGrid();
    } catch (Exception e) {
      log.error("Error deleting holiday", e);
      showErrorNotification("Failed to delete holiday: " + e.getMessage());
    }
  }

  private void showSuccessNotification(@NonNull String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showErrorNotification(@NonNull String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
