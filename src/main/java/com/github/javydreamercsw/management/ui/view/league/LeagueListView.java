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
package com.github.javydreamercsw.management.ui.view.league;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.universe.InviteManagementDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import lombok.NonNull;

@Route(value = "leagues", layout = MainLayout.class)
@PageTitle("My Leagues")
@PermitAll
public class LeagueListView extends Main {

  private final LeagueService leagueService;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final WrestlerRepository wrestlerRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;
  private final UniverseContextService universeContextService;
  private final UniverseRepository universeRepository;
  private final InviteService inviteService;
  private final Grid<League> leagueGrid;

  public LeagueListView(
      @NonNull final LeagueService leagueService,
      @NonNull final AccountService accountService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final WrestlerRepository wrestlerRepository,
      @NonNull final LeagueMembershipRepository leagueMembershipRepository,
      @NonNull final UniverseContextService universeContextService,
      @NonNull final UniverseRepository universeRepository,
      @NonNull final InviteService inviteService) {
    this.leagueService = leagueService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.wrestlerRepository = wrestlerRepository;
    this.leagueMembershipRepository = leagueMembershipRepository;
    this.universeContextService = universeContextService;
    this.universeRepository = universeRepository;
    this.inviteService = inviteService;
    this.leagueGrid = new Grid<>(League.class, false);
    this.leagueGrid.setId("league-grid");

    configureGrid();
    reloadGrid();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(new ViewToolbar("My Leagues", createCreateLeagueButton()));
    add(leagueGrid);
  }

  private void configureGrid() {
    leagueGrid.addColumn(League::getName).setHeader("League Name").setSortable(true);
    leagueGrid.addColumn(l -> l.getCommissioner().getUsername()).setHeader("Commissioner");
    leagueGrid.addColumn(League::getStatus).setHeader("Status");
    leagueGrid
        .addColumn(l -> l.getBudget() != null ? "$" + l.getBudget().toPlainString() : "$0")
        .setHeader("Budget")
        .setSortable(true);
    leagueGrid
        .addColumn(l -> l.getDurationWeeks() != null ? l.getDurationWeeks() + " wks" : "—")
        .setHeader("Duration");
    leagueGrid
        .addColumn(l -> l.getLockerRoomMorale() != null ? l.getLockerRoomMorale() + "%" : "100%")
        .setHeader("Morale")
        .setSortable(true);

    leagueGrid
        .addComponentColumn(
            league -> {
              HorizontalLayout actions = new HorizontalLayout();
              actions.setSpacing(true);

              Button actionButton = new Button();
              if (league.getStatus() == League.LeagueStatus.DRAFTING
                  || league.getStatus() == League.LeagueStatus.PRE_DRAFT) {
                actionButton.setText("Draft Room");
                actionButton.setIcon(VaadinIcon.LIST.create());
                actionButton.addClassName("league-draft-room-btn");
                actionButton.setId("league-draft-room-btn-" + league.getId());
                actionButton.addClickListener(
                    e -> getUI().ifPresent(ui -> ui.navigate(DraftView.class, league.getId())));
              } else {
                actionButton.setText("Dashboard");
                actionButton.setIcon(VaadinIcon.DASHBOARD.create());
                actionButton.setId("league-dashboard-btn-" + league.getId());
                actionButton.addClickListener(
                    e ->
                        getUI()
                            .ifPresent(
                                ui -> ui.navigate(LeagueDashboardView.class, league.getId())));
              }
              actionButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              actions.add(actionButton);

              // Commissioner actions
              boolean isComm =
                  securityUtils
                      .getAuthenticatedUser()
                      .map(u -> u.getAccount().equals(league.getCommissioner()))
                      .orElse(false);

              if (isComm || securityUtils.isAdmin()) {
                if (league.getUniverse() != null) {
                  Button inviteBtn = new Button(new Icon(VaadinIcon.LINK));
                  inviteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                  inviteBtn.setTooltipText("Invite Players");
                  inviteBtn.setId("invite-league-" + league.getId());
                  inviteBtn.addClickListener(
                      e ->
                          securityUtils
                              .getAuthenticatedUser()
                              .ifPresent(
                                  u ->
                                      new InviteManagementDialog(
                                              league.getUniverse(), inviteService, u.getAccount())
                                          .open()));
                  actions.add(inviteBtn);
                }

                Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
                editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                editBtn.setTooltipText("Edit League");
                editBtn.setId("edit-league-" + league.getId());
                editBtn.addClickListener(e -> openEditDialog(league));

                Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
                deleteBtn.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR);
                deleteBtn.setTooltipText("Delete League");
                deleteBtn.setId("delete-league-" + league.getId());
                deleteBtn.addClickListener(e -> openDeleteDialog(league));

                actions.add(editBtn, deleteBtn);
              }

              return actions;
            })
        .setHeader("Actions")
        .setFlexGrow(1);

    leagueGrid.setSizeFull();
  }

  private void openEditDialog(@NonNull final League league) {
    leagueService
        .getLeagueWithExcludedWrestlers(league.getId())
        .ifPresent(
            fullyLoaded -> {
              new LeagueDialog(
                      leagueService,
                      accountService,
                      securityUtils,
                      wrestlerRepository,
                      leagueMembershipRepository,
                      universeRepository,
                      fullyLoaded,
                      this::reloadGrid)
                  .open();
            });
  }

  private void openDeleteDialog(@NonNull final League league) {
    Dialog confirm = new Dialog();
    confirm.setHeaderTitle("Delete League");
    confirm.add(new Span("Are you sure you want to delete '" + league.getName() + "'?"));

    Button delete =
        new Button(
            "Delete",
            e -> {
              try {
                leagueService.deleteLeague(league.getId());
                Notification.show("League deleted.", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                reloadGrid();
                confirm.close();
              } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

    Button cancel = new Button("Cancel", e -> confirm.close());
    confirm.getFooter().add(delete, cancel);
    confirm.open();
  }

  private Button createCreateLeagueButton() {
    Button button = new Button("New League", VaadinIcon.PLUS.create());
    button.setId("create-league-btn");
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.setVisible(securityUtils.isBooker() || securityUtils.isAdmin());
    button.addClickListener(
        e -> {
          new LeagueDialog(
                  leagueService,
                  accountService,
                  securityUtils,
                  wrestlerRepository,
                  leagueMembershipRepository,
                  this::reloadGrid)
              .open();
        });
    return button;
  }

  private void reloadGrid() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              Long universeId = universeContextService.getCurrentUniverseId();
              List<League> leagues = leagueService.getLeaguesForUser(user.getAccount());
              leagues.removeIf(
                  l -> l.getUniverse() == null || !universeId.equals(l.getUniverse().getId()));
              leagueGrid.setItems(leagues);
            });
  }
}
