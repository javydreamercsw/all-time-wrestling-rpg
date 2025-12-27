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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.dto.TeamDTO;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

/**
 * Main view for managing teams (tag teams) in the ATW RPG system. Provides CRUD operations for
 * teams with a grid-based interface.
 */
@Route(value = "teams", layout = MainLayout.class)
@PageTitle("Teams | ATW RPG")
@PermitAll
@Slf4j
public class TeamsView extends VerticalLayout {

  private final TeamService teamService;
  private final SecurityUtils securityUtils;
  private final Grid<TeamDTO> grid;
  private final TextField searchField;

  private TeamFormDialog teamFormDialog;

  public TeamsView(
      TeamService teamService, TeamFormDialog teamFormDialog, SecurityUtils securityUtils) {
    this.teamService = teamService;
    this.teamFormDialog = teamFormDialog;
    this.securityUtils = securityUtils;
    this.grid = new Grid<>(TeamDTO.class, false);
    this.searchField = new TextField();

    setSizeFull();
    configureGrid();
    configureToolbar();
    updateGrid();

    add(createToolbar(), grid);
  }

  private void configureGrid() {
    grid.addClassName("teams-grid");
    grid.setSizeFull();
    grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

    // Configure columns
    grid.addColumn(TeamDTO::getName).setHeader("Team Name").setSortable(true).setFlexGrow(2);

    grid.addColumn(TeamDTO::getMemberNames).setHeader("Members").setSortable(false).setFlexGrow(3);

    grid.addColumn(TeamDTO::getFactionName).setHeader("Faction").setSortable(true).setFlexGrow(1);

    grid.addColumn(dto -> dto.getStatus().getDisplayName())
        .setHeader("Status")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(
            dto ->
                dto.getFormedDate() != null ? dto.getFormedDate().toString().substring(0, 10) : "")
        .setHeader("Formed Date")
        .setSortable(true)
        .setFlexGrow(1);

    // Actions column
    grid.addComponentColumn(this::createActionsLayout)
        .setHeader("Actions")
        .setFlexGrow(1)
        .setFrozenToEnd(true);

    // Load data
    grid.setItems(
        query ->
            teamService
                .getAllTeams(
                    PageRequest.of(
                        query.getPage(),
                        query.getPageSize(),
                        VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream()
                .map(TeamDTO::fromEntity));
  }

  private HorizontalLayout createActionsLayout(TeamDTO team) {
    Optional<Team> teamEntityOptional = teamService.getTeamById(team.getId());
    Team teamEntity = teamEntityOptional.orElse(null);

    Button editButton = new Button(VaadinIcon.EDIT.create());
    editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
    editButton.setTooltipText("Edit team");
    editButton.addClickListener(e -> editTeam(team));
    editButton.setVisible(securityUtils.canEdit(teamEntity));

    Button deleteButton = new Button(VaadinIcon.TRASH.create());
    deleteButton.addThemeVariants(
        ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete team");
    deleteButton.addClickListener(e -> confirmDeleteTeam(team));
    deleteButton.setVisible(securityUtils.canDelete(teamEntity));

    Button statusButton = createStatusButton(team);
    statusButton.setVisible(securityUtils.canEdit(teamEntity));

    HorizontalLayout actions = new HorizontalLayout(editButton, statusButton, deleteButton);
    actions.setSpacing(false);
    return actions;
  }

  private Button createStatusButton(TeamDTO team) {
    Button statusButton = new Button();
    statusButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);

    if (team.isActive()) {
      statusButton.setIcon(VaadinIcon.PAUSE.create());
      statusButton.setTooltipText("Disband team");
      statusButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
      statusButton.addClickListener(e -> confirmDisbandTeam(team));
    } else if (team.isDisbanded()) {
      statusButton.setIcon(VaadinIcon.PLAY.create());
      statusButton.setTooltipText("Reactivate team");
      statusButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
      statusButton.addClickListener(e -> reactivateTeam(team));
    } else {
      statusButton.setIcon(VaadinIcon.QUESTION.create());
      statusButton.setTooltipText("Unknown status");
      statusButton.setEnabled(false);
    }

    return statusButton;
  }

  private void configureToolbar() {
    searchField.setPlaceholder("Search teams...");
    searchField.setClearButtonVisible(true);
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> updateGrid());
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
  }

  private HorizontalLayout createToolbar() {
    H2 title = new H2("Teams");
    title.addClassName("view-title");

    Button addButton = new Button("Add Team", VaadinIcon.PLUS.create());
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addButton.addClickListener(e -> addTeam());
    addButton.setVisible(securityUtils.canCreate());

    Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    refreshButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    refreshButton.setTooltipText("Refresh");
    refreshButton.addClickListener(e -> updateGrid());

    HorizontalLayout toolbar = new HorizontalLayout(title, searchField, addButton, refreshButton);
    toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    toolbar.setFlexGrow(1, title);
    toolbar.setWidthFull();
    toolbar.addClassName("toolbar");

    return toolbar;
  }

  private void updateGrid() {
    grid.getDataProvider().refreshAll();
  }

  private void addTeam() {
    openTeamDialog(null);
  }

  private void editTeam(TeamDTO team) {
    openTeamDialog(team);
  }

  private void openTeamDialog(TeamDTO team) {
    teamFormDialog.setTeam(team);
    teamFormDialog.addOpenedChangeListener(
        e -> {
          if (!e.isOpened()) {
            updateGrid();
          }
        });
    teamFormDialog.open();
  }

  private void confirmDeleteTeam(TeamDTO team) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Team");
    dialog.setText(
        "Are you sure you want to delete the team '"
            + team.getName()
            + "'? This action cannot be undone.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(e -> deleteTeam(team));
    dialog.open();
  }

  private void confirmDisbandTeam(TeamDTO team) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Disband Team");
    dialog.setText("Are you sure you want to disband the team '" + team.getName() + "'?");
    dialog.setCancelable(true);
    dialog.setConfirmText("Disband");
    dialog.setConfirmButtonTheme("contrast primary");
    dialog.addConfirmListener(e -> disbandTeam(team));
    dialog.open();
  }

  private void deleteTeam(TeamDTO team) {
    try {
      boolean deleted = teamService.deleteTeam(team.getId());
      if (deleted) {
        showSuccessNotification("Team '" + team.getName() + "' deleted successfully");
        updateGrid();
      } else {
        showErrorNotification("Failed to delete team");
      }
    } catch (Exception e) {
      log.error("Error deleting team: {}", team.getName(), e);
      showErrorNotification("Error deleting team: " + e.getMessage());
    }
  }

  private void disbandTeam(TeamDTO team) {
    try {
      Optional<Team> updated = teamService.disbandTeam(team.getId());
      if (updated.isPresent()) {
        showSuccessNotification("Team '" + team.getName() + "' disbanded successfully");
        updateGrid();
      } else {
        showErrorNotification("Failed to disband team");
      }
    } catch (Exception e) {
      log.error("Error disbanding team: {}", team.getName(), e);
      showErrorNotification("Error disbanding team: " + e.getMessage());
    }
  }

  private void reactivateTeam(TeamDTO team) {
    try {
      Optional<Team> updated = teamService.reactivateTeam(team.getId());
      if (updated.isPresent()) {
        showSuccessNotification("Team '" + team.getName() + "' reactivated successfully");
        updateGrid();
      } else {
        showErrorNotification("Failed to reactivate team");
      }
    } catch (Exception e) {
      log.error("Error reactivating team: {}", team.getName(), e);
      showErrorNotification("Error reactivating team: " + e.getMessage());
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
