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
package com.github.javydreamercsw.management.ui.view.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

/**
 * View for managing seasons in the ATW RPG system. Provides CRUD operations for seasons with a
 * grid-based interface.
 */
@Route("season-list")
@PageTitle("Season List")
@Menu(order = 6, icon = "vaadin:calendar-clock", title = "Seasons")
@PermitAll
@Slf4j
public class SeasonListView extends Main {

  private final SeasonService seasonService;
  private final Grid<Season> grid;
  private final TextField searchField;

  private Dialog editDialog;
  private TextField editName;
  private TextArea editDescription;
  private IntegerField editShowsPerPpv;
  private Checkbox editIsActive;
  private Season editingSeason;
  private Binder<Season> binder;

  public SeasonListView(SeasonService seasonService) {
    this.seasonService = seasonService;
    this.grid = new Grid<>(Season.class, false);
    this.searchField = new TextField();

    setSizeFull();
    configureGrid();
    configureEditDialog();
    updateGrid();

    add(createToolbar(), grid);
  }

  private HorizontalLayout createToolbar() {
    searchField.setPlaceholder("Search seasons...");
    searchField.setClearButtonVisible(true);
    searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> updateGrid());

    Button addSeasonBtn = new Button("Add Season");
    addSeasonBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addSeasonBtn.setIcon(VaadinIcon.PLUS.create());
    addSeasonBtn.addClickListener(e -> openEditDialog(null));

    Button refreshBtn = new Button("Refresh");
    refreshBtn.setIcon(VaadinIcon.REFRESH.create());
    refreshBtn.addClickListener(e -> updateGrid());

    HorizontalLayout toolbar = new HorizontalLayout(searchField, addSeasonBtn, refreshBtn);
    toolbar.addClassName("toolbar");
    return toolbar;
  }

  private void configureGrid() {
    grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
    grid.setSizeFull();

    // Add columns
    grid.addColumn(Season::getName).setHeader("Name").setSortable(true).setResizable(true);

    grid.addColumn(
            season ->
                season.getDescription() != null
                    ? (season.getDescription().length() > 100
                        ? season.getDescription().substring(0, 97) + "..."
                        : season.getDescription())
                    : "")
        .setHeader("Description")
        .setResizable(true);

    grid.addColumn(season -> season.getIsActive() ? "Active" : "Inactive")
        .setHeader("Status")
        .setSortable(true);

    grid.addColumn(Season::getShowsPerPpv).setHeader("Shows per PPV").setSortable(true);

    grid.addColumn(
            season ->
                season.getStartDate() != null
                    ? DateTimeFormatter.ofPattern("MMM dd, yyyy")
                        .format(season.getStartDate().atZone(java.time.ZoneId.systemDefault()))
                    : "")
        .setHeader("Start Date")
        .setSortable(true);

    grid.addColumn(
            season ->
                season.getEndDate() != null
                    ? DateTimeFormatter.ofPattern("MMM dd, yyyy")
                        .format(season.getEndDate().atZone(java.time.ZoneId.systemDefault()))
                    : "")
        .setHeader("End Date")
        .setSortable(true);

    // Add actions column
    grid.addComponentColumn(
            season -> {
              Button editBtn = new Button("Edit", VaadinIcon.EDIT.create());
              editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              editBtn.addClickListener(e -> openEditDialog(season));

              Button deleteBtn = new Button("Delete", VaadinIcon.TRASH.create());
              deleteBtn.addThemeVariants(
                  ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
              deleteBtn.addClickListener(e -> deleteSeason(season));

              return new HorizontalLayout(editBtn, deleteBtn);
            })
        .setHeader("Actions")
        .setFlexGrow(0);

    grid.getColumns().forEach(col -> col.setAutoWidth(true));
  }

  private void configureEditDialog() {
    editDialog = new Dialog();
    editDialog.setWidth("500px");
    editDialog.setCloseOnEsc(true);
    editDialog.setCloseOnOutsideClick(false);

    // Form fields
    editName = new TextField("Name");
    editName.setWidthFull();
    editName.setRequired(true);

    editDescription = new TextArea("Description");
    editDescription.setWidthFull();
    editDescription.setMaxLength(1000);

    editShowsPerPpv = new IntegerField("Shows per PPV");
    editShowsPerPpv.setWidthFull();
    editShowsPerPpv.setMin(4);
    editShowsPerPpv.setMax(10);
    editShowsPerPpv.setValue(5);

    editIsActive = new Checkbox("Active");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.add(editName, editDescription, editShowsPerPpv, editIsActive);

    // Buttons
    Button saveBtn = new Button("Save", e -> saveSeason());
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelBtn = new Button("Cancel", e -> editDialog.close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveBtn, cancelBtn);
    buttonLayout.setJustifyContentMode(
        com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);

    // Dialog content
    VerticalLayout dialogContent = new VerticalLayout();
    dialogContent.add(new H2("Season Details"), formLayout, buttonLayout);
    dialogContent.setPadding(false);
    dialogContent.setSpacing(true);

    editDialog.add(dialogContent);

    // Configure binder
    binder = new Binder<>(Season.class);
    binder.forField(editName).asRequired("Name is required").bind(Season::getName, Season::setName);
    binder.forField(editDescription).bind(Season::getDescription, Season::setDescription);
    binder
        .forField(editShowsPerPpv)
        .asRequired("Shows per PPV is required")
        .bind(Season::getShowsPerPpv, Season::setShowsPerPpv);
    binder.forField(editIsActive).bind(Season::getIsActive, Season::setIsActive);
  }

  private void openEditDialog(Season season) {
    editingSeason = season;

    if (season == null) {
      // Creating new season
      editDialog.setHeaderTitle("Add New Season");
      Season newSeason = new Season();
      newSeason.setIsActive(false);
      newSeason.setShowsPerPpv(5);
      binder.setBean(newSeason);
    } else {
      // Editing existing season
      editDialog.setHeaderTitle("Edit Season");
      binder.setBean(season);
    }

    editDialog.open();
  }

  private void saveSeason() {
    try {
      Season season = binder.getBean();
      binder.writeBean(season);

      if (editingSeason == null) {
        // Creating new season
        seasonService.createSeason(
            season.getName(), season.getDescription(), season.getShowsPerPpv());
        showSuccessNotification("Season created successfully");
      } else {
        // Updating existing season
        seasonService.updateSeason(season);
        showSuccessNotification("Season updated successfully");
      }

      editDialog.close();
      updateGrid();

    } catch (ValidationException e) {
      showErrorNotification("Please fix the validation errors");
    } catch (Exception e) {
      log.error("Error saving season", e);
      showErrorNotification("Error saving season: " + e.getMessage());
    }
  }

  private void deleteSeason(Season season) {
    try {
      seasonService.deleteSeason(season.getId());
      showSuccessNotification("Season deleted successfully");
      updateGrid();
    } catch (Exception e) {
      log.error("Error deleting season: {}", season.getName(), e);
      showErrorNotification("Error deleting season: " + e.getMessage());
    }
  }

  private void updateGrid() {
    try {
      String searchTerm = searchField.getValue();
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        grid.setItems(seasonService.getAllSeasons(Pageable.unpaged()).getContent());
      } else {
        grid.setItems(seasonService.searchSeasons(searchTerm, Pageable.unpaged()).getContent());
      }
    } catch (Exception e) {
      log.error("Error updating grid", e);
      showErrorNotification("Error loading seasons: " + e.getMessage());
    }
  }

  private void showSuccessNotification(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showErrorNotification(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
