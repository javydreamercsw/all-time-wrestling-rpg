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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;

public class LeagueDialog extends Dialog {

  private final LeagueService leagueService;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final WrestlerRepository wrestlerRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;
  private final UniverseRepository universeRepository;
  private final Runnable onSave;
  private final Binder<League> binder = new Binder<>(League.class);
  private final League league;

  public LeagueDialog(
      @NonNull final LeagueService leagueService,
      @NonNull final AccountService accountService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final WrestlerRepository wrestlerRepository,
      @NonNull final LeagueMembershipRepository leagueMembershipRepository,
      @NonNull final Runnable onSave) {
    this(
        leagueService,
        accountService,
        securityUtils,
        wrestlerRepository,
        leagueMembershipRepository,
        null,
        null,
        onSave);
  }

  public LeagueDialog(
      @NonNull final LeagueService leagueService,
      @NonNull final AccountService accountService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final WrestlerRepository wrestlerRepository,
      @NonNull final LeagueMembershipRepository leagueMembershipRepository,
      final League league,
      @NonNull final Runnable onSave) {
    this(
        leagueService,
        accountService,
        securityUtils,
        wrestlerRepository,
        leagueMembershipRepository,
        null,
        league,
        onSave);
  }

  public LeagueDialog(
      @NonNull final LeagueService leagueService,
      @NonNull final AccountService accountService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final WrestlerRepository wrestlerRepository,
      @NonNull final LeagueMembershipRepository leagueMembershipRepository,
      final UniverseRepository universeRepository,
      final League league,
      @NonNull final Runnable onSave) {
    this.leagueService = leagueService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.wrestlerRepository = wrestlerRepository;
    this.leagueMembershipRepository = leagueMembershipRepository;
    this.universeRepository = universeRepository;
    this.league = league;
    this.onSave = onSave;

    setId("league-dialog");
    setHeaderTitle(league == null ? "Create New League" : "Edit League: " + league.getName());

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("League Name");
    nameField.setId("league-name-field");
    nameField.setRequired(true);

    NumberField maxPicksField = new NumberField("Wrestlers per Player");
    maxPicksField.setMin(1);
    maxPicksField.setValue(1.0);
    maxPicksField.setStepButtonsVisible(true);
    maxPicksField.setId("league-max-picks-field");

    Checkbox commissionerPlays = new Checkbox("I want to participate as a player");
    commissionerPlays.setId("league-commissioner-plays-checkbox");

    MultiSelectComboBox<Wrestler> excludedWrestlers =
        new MultiSelectComboBox<>("Exclude Wrestlers");
    excludedWrestlers.setItems(wrestlerRepository.findAll());
    excludedWrestlers.setItemLabelGenerator(Wrestler::getName);

    MultiSelectComboBox<Account> playerList = new MultiSelectComboBox<>("Players");
    playerList.setId("players-combo");
    playerList.setItems(accountService.findAll());
    playerList.setItemLabelGenerator(Account::getUsername);

    MultiSelectComboBox<Account> viewerList = new MultiSelectComboBox<>("Viewers");
    viewerList.setId("viewers-combo");
    viewerList.setItems(accountService.findAll());
    viewerList.setItemLabelGenerator(Account::getUsername);

    BigDecimalField budgetField = new BigDecimalField("Budget");
    budgetField.setId("league-budget-field");
    budgetField.setPrefixComponent(new com.vaadin.flow.component.html.Span("$"));
    budgetField.setValue(BigDecimal.ZERO);
    budgetField.setWidthFull();

    IntegerField durationWeeksField = new IntegerField("Duration (Weeks)");
    durationWeeksField.setId("league-duration-weeks-field");
    durationWeeksField.setMin(1);
    durationWeeksField.setStepButtonsVisible(true);
    durationWeeksField.setWidthFull();

    IntegerField moraleField = new IntegerField("Locker Room Morale");
    moraleField.setId("league-morale-field");
    moraleField.setMin(0);
    moraleField.setMax(100);
    moraleField.setValue(100);
    moraleField.setStepButtonsVisible(true);
    moraleField.setWidthFull();

    ComboBox<League.LeagueStatus> statusField = new ComboBox<>("Status");
    statusField.setId("league-status-field");
    statusField.setItems(League.LeagueStatus.values());
    statusField.setItemLabelGenerator(s -> s.name().replace('_', ' '));
    statusField.setValue(League.LeagueStatus.PRE_DRAFT);
    statusField.setWidthFull();

    ComboBox<Universe> universeField = new ComboBox<>("Universe");
    universeField.setId("league-universe-field");
    if (universeRepository != null) {
      universeField.setItems(universeRepository.findAll());
    }
    universeField.setItemLabelGenerator(Universe::getName);
    universeField.setClearButtonVisible(true);
    universeField.setWidthFull();

    formLayout.add(nameField, maxPicksField, commissionerPlays, statusField);
    formLayout.add(budgetField, durationWeeksField, moraleField, universeField);
    formLayout.add(excludedWrestlers);
    formLayout.add(playerList, viewerList);
    formLayout.setColspan(excludedWrestlers, 2);

    binder
        .forField(nameField)
        .asRequired("League name is required")
        .bind(League::getName, League::setName);

    if (league != null) {
      nameField.setValue(league.getName());
      maxPicksField.setValue((double) league.getMaxPicksPerPlayer());
      excludedWrestlers.setValue(league.getExcludedWrestlers());
      statusField.setValue(
          league.getStatus() != null ? league.getStatus() : League.LeagueStatus.PRE_DRAFT);
      budgetField.setValue(league.getBudget() != null ? league.getBudget() : BigDecimal.ZERO);
      if (league.getDurationWeeks() != null) {
        durationWeeksField.setValue(league.getDurationWeeks());
      }
      moraleField.setValue(
          league.getLockerRoomMorale() != null ? league.getLockerRoomMorale() : 100);
      universeField.setValue(league.getUniverse());

      Optional<LeagueMembership> commM =
          leagueMembershipRepository.findByLeagueAndMember(league, league.getCommissioner());
      commissionerPlays.setValue(
          commM
              .map(m -> m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER_PLAYER)
              .orElse(false));

      Set<Account> existingPlayers =
          leagueMembershipRepository.findByLeague(league).stream()
              .filter(m -> m.getRole() == LeagueMembership.LeagueRole.PLAYER)
              .map(LeagueMembership::getMember)
              .collect(Collectors.toSet());
      Set<Account> existingViewers =
          leagueMembershipRepository.findByLeague(league).stream()
              .filter(m -> m.getRole() == LeagueMembership.LeagueRole.VIEWER)
              .map(LeagueMembership::getMember)
              .collect(Collectors.toSet());
      playerList.setValue(existingPlayers);
      viewerList.setValue(existingViewers);
    }

    Button saveButton =
        new Button(
            league == null ? "Create" : "Save",
            e -> {
              if (binder.validate().isOk()) {
                securityUtils
                    .getAuthenticatedUser()
                    .ifPresent(
                        user -> {
                          League targetLeague;
                          if (league == null) {
                            targetLeague =
                                leagueService.createLeague(
                                    nameField.getValue(),
                                    user.getAccount(),
                                    maxPicksField.getValue().intValue(),
                                    excludedWrestlers.getSelectedItems(),
                                    commissionerPlays.getValue(),
                                    budgetField.getValue(),
                                    durationWeeksField.getValue(),
                                    moraleField.getValue() != null ? moraleField.getValue() : 100);
                          } else {
                            targetLeague =
                                leagueService.updateLeague(
                                    league.getId(),
                                    nameField.getValue(),
                                    maxPicksField.getValue().intValue(),
                                    excludedWrestlers.getSelectedItems(),
                                    commissionerPlays.getValue(),
                                    budgetField.getValue(),
                                    durationWeeksField.getValue(),
                                    moraleField.getValue() != null ? moraleField.getValue() : 100,
                                    statusField.getValue());
                          }

                          // Sync players
                          Set<Account> selectedPlayers = playerList.getSelectedItems();
                          for (Account p : selectedPlayers) {
                            if (!p.equals(targetLeague.getCommissioner())) {
                              leagueService.addPlayer(targetLeague, p);
                            }
                          }

                          // Sync viewers
                          Set<Account> selectedViewers = viewerList.getSelectedItems();
                          for (Account v : selectedViewers) {
                            if (!v.equals(targetLeague.getCommissioner())
                                && !selectedPlayers.contains(v)) {
                              leagueService.addMember(
                                  targetLeague, v, LeagueMembership.LeagueRole.VIEWER);
                            }
                          }

                          // Remove members not in either selected list
                          if (league != null) {
                            Set<Account> allSelected = new java.util.HashSet<>(selectedPlayers);
                            allSelected.addAll(selectedViewers);
                            leagueMembershipRepository.findByLeague(league).stream()
                                .filter(
                                    m ->
                                        !m.getMember().equals(league.getCommissioner())
                                            && !allSelected.contains(m.getMember()))
                                .forEach(m -> leagueService.removeMember(league, m.getMember()));
                          }

                          Notification.show(
                                  "League saved successfully",
                                  3000,
                                  Notification.Position.BOTTOM_START)
                              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                          onSave.run();
                          close();
                        });
              }
            });
    saveButton.setId("league-save-btn");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
    add(formLayout);
  }
}
