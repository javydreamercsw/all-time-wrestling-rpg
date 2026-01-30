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
import com.github.javydreamercsw.management.domain.league.Draft;
import com.github.javydreamercsw.management.domain.league.DraftPick;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.league.DraftBroadcaster;
import com.github.javydreamercsw.management.service.league.DraftService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Route(value = "draft", layout = MainLayout.class)
@PageTitle("Draft Room")
@PermitAll
@Slf4j
public class DraftView extends VerticalLayout implements HasUrlParameter<Long> {

  private final DraftService draftService;
  private final DraftRepository draftRepository;
  private final LeagueRepository leagueRepository;
  private final DraftPickRepository draftPickRepository;
  private final WrestlerRepository wrestlerRepository;
  private final LeagueRosterRepository leagueRosterRepository;
  private final SecurityUtils securityUtils;
  private final DraftBroadcaster draftBroadcaster;

  private Long leagueId;
  private League league;
  private Draft draft;
  private Registration registration;

  private final Grid<Wrestler> availableWrestlersGrid = new Grid<>(Wrestler.class, false);
  private final Grid<DraftPick> pickHistoryGrid = new Grid<>(DraftPick.class, false);
  private final Span statusLabel = new Span();
  private final Span turnLabel = new Span();

  public DraftView(
      DraftService draftService,
      DraftRepository draftRepository,
      LeagueRepository leagueRepository,
      DraftPickRepository draftPickRepository,
      WrestlerRepository wrestlerRepository,
      LeagueRosterRepository leagueRosterRepository,
      SecurityUtils securityUtils,
      DraftBroadcaster draftBroadcaster) {
    this.draftService = draftService;
    this.draftRepository = draftRepository;
    this.leagueRepository = leagueRepository;
    this.draftPickRepository = draftPickRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.leagueRosterRepository = leagueRosterRepository;
    this.securityUtils = securityUtils;
    this.draftBroadcaster = draftBroadcaster;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
  }

  @Override
  public void setParameter(BeforeEvent event, Long parameter) {
    this.leagueId = parameter;
    refreshData();
    initUI();
  }

  private void refreshData() {
    leagueRepository
        .findById(leagueId)
        .ifPresent(
            l -> {
              this.league = l;
              draftRepository
                  .findByLeague(l)
                  .ifPresentOrElse(
                      d -> this.draft = d,
                      () -> {
                        if (securityUtils.isBooker() || securityUtils.isAdmin()) {
                          this.draft = draftService.startDraft(l);
                        }
                      });
            });
  }

  private void initUI() {
    removeAll();
    if (league == null || draft == null) {
      add(new H2("Draft not available."));
      return;
    }

    add(new H2("Draft Room: " + league.getName()));

    HorizontalLayout header = new HorizontalLayout(statusLabel, turnLabel);
    header.addClassNames(LumoUtility.Gap.XLARGE, LumoUtility.FontSize.LARGE);
    add(header);

    updateStatusLabels();

    HorizontalLayout mainLayout = new HorizontalLayout();
    mainLayout.setSizeFull();

    // Available Wrestlers
    VerticalLayout availableLayout =
        new VerticalLayout(new H3("Available Wrestlers"), availableWrestlersGrid);
    availableLayout.setSizeFull();
    configureAvailableGrid();

    // History
    VerticalLayout historyLayout = new VerticalLayout(new H3("Draft History"), pickHistoryGrid);
    historyLayout.setWidth("400px");
    historyLayout.setHeightFull();
    configureHistoryGrid();

    mainLayout.add(availableLayout, historyLayout);
    add(mainLayout);

    refreshGrids();
  }

  private void updateStatusLabels() {
    statusLabel.setText(
        "Round: " + draft.getCurrentRound() + " | Pick: " + draft.getCurrentPickNumber());
    if (draft.getStatus() == Draft.DraftStatus.COMPLETED) {
      turnLabel.setText("Draft Completed");
      turnLabel.addClassNames(LumoUtility.TextColor.SUCCESS);
    } else if (draft.getCurrentTurnUser() != null) {
      turnLabel.setText("Current Turn: " + draft.getCurrentTurnUser().getUsername());
      securityUtils
          .getAuthenticatedUser()
          .ifPresent(
              user -> {
                if (draft.getCurrentTurnUser().equals(user.getAccount())) {
                  turnLabel.addClassNames(
                      LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.BOLD);
                } else {
                  turnLabel.removeClassName(LumoUtility.FontWeight.BOLD);
                  turnLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
                }
              });
    }
  }

  private void configureAvailableGrid() {
    availableWrestlersGrid.addColumn(Wrestler::getName).setHeader("Wrestler").setSortable(true);
    availableWrestlersGrid
        .addComponentColumn(
            wrestler -> {
              Button pickButton =
                  new Button(
                      "Draft",
                      e -> {
                        securityUtils
                            .getAuthenticatedUser()
                            .ifPresent(
                                user -> {
                                  draftService.makePick(draft, user.getAccount(), wrestler);
                                });
                      });
              pickButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

              // Enable only if it's user's turn
              boolean myTurn =
                  draft.getStatus() == Draft.DraftStatus.ACTIVE
                      && securityUtils
                          .getAuthenticatedUser()
                          .map(u -> u.getAccount().equals(draft.getCurrentTurnUser()))
                          .orElse(false);
              pickButton.setEnabled(myTurn);

              return pickButton;
            })
        .setHeader("Action");
    availableWrestlersGrid.setSizeFull();
  }

  private void configureHistoryGrid() {
    pickHistoryGrid.addColumn(DraftPick::getPickNumber).setHeader("#").setWidth("50px");
    pickHistoryGrid.addColumn(p -> p.getUser().getUsername()).setHeader("User");
    pickHistoryGrid.addColumn(p -> p.getWrestler().getName()).setHeader("Wrestler");
    pickHistoryGrid.setSizeFull();
  }

  private void refreshGrids() {
    // Get drafted IDs
    Set<Long> draftedWrestlerIds =
        leagueRosterRepository.findByLeague(league).stream()
            .map(r -> r.getWrestler().getId())
            .collect(Collectors.toSet());

    List<Wrestler> available =
        wrestlerRepository.findAll().stream()
            .filter(w -> !draftedWrestlerIds.contains(w.getId()))
            .collect(Collectors.toList());

    availableWrestlersGrid.setItems(available);
    pickHistoryGrid.setItems(draftPickRepository.findByDraftOrderByPickNumberAsc(draft));
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    UI ui = attachEvent.getUI();
    registration =
        draftBroadcaster.register(
            event -> {
              if (event.getDraftId().equals(draft.getId())) {
                ui.access(
                    () -> {
                      refreshData();
                      updateStatusLabels();
                      refreshGrids();
                    });
              }
            });
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    if (registration != null) {
      registration.remove();
    }
  }
}
