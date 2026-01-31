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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
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
  private final Runnable onSave;
  private final Binder<League> binder = new Binder<>(League.class);
  private final League league;

  public LeagueDialog(
      @NonNull LeagueService leagueService,
      @NonNull AccountService accountService,
      @NonNull SecurityUtils securityUtils,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull LeagueMembershipRepository leagueMembershipRepository,
      @NonNull Runnable onSave) {
    this(
        leagueService,
        accountService,
        securityUtils,
        wrestlerRepository,
        leagueMembershipRepository,
        null,
        onSave);
  }

  public LeagueDialog(
      @NonNull LeagueService leagueService,
      @NonNull AccountService accountService,
      @NonNull SecurityUtils securityUtils,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull LeagueMembershipRepository leagueMembershipRepository,
      League league,
      @NonNull Runnable onSave) {
    this.leagueService = leagueService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.wrestlerRepository = wrestlerRepository;
    this.leagueMembershipRepository = leagueMembershipRepository;
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

    MultiSelectComboBox<Account> participantList = new MultiSelectComboBox<>("Participants");
    participantList.setId("participants-combo");
    participantList.setItems(accountService.findAll());
    participantList.setItemLabelGenerator(Account::getUsername);

    formLayout.add(nameField, maxPicksField, commissionerPlays, excludedWrestlers);
    formLayout.add(participantList);
    formLayout.setColspan(excludedWrestlers, 2);
    formLayout.setColspan(participantList, 2);

    binder
        .forField(nameField)
        .asRequired("League name is required")
        .bind(League::getName, League::setName);

    if (league != null) {
      nameField.setValue(league.getName());
      maxPicksField.setValue((double) league.getMaxPicksPerPlayer());
      excludedWrestlers.setValue(league.getExcludedWrestlers());

      Optional<LeagueMembership> commM =
          leagueMembershipRepository.findByLeagueAndMember(league, league.getCommissioner());
      commissionerPlays.setValue(
          commM
              .map(m -> m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER_PLAYER)
              .orElse(false));

      Set<Account> players =
          leagueMembershipRepository.findByLeague(league).stream()
              .filter(
                  m ->
                      m.getRole() == LeagueMembership.LeagueRole.PLAYER
                          || m.getRole() == LeagueMembership.LeagueRole.VIEWER)
              .map(LeagueMembership::getMember)
              .collect(Collectors.toSet());
      participantList.setValue(players);
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
                                    commissionerPlays.getValue());
                          } else {
                            targetLeague =
                                leagueService.updateLeague(
                                    league.getId(),
                                    nameField.getValue(),
                                    maxPicksField.getValue().intValue(),
                                    excludedWrestlers.getSelectedItems(),
                                    commissionerPlays.getValue());
                          }

                          // Sync participants
                          Set<Account> selected = participantList.getSelectedItems();
                          // For simplicity, just add as PLAYER for now.
                          // Ideally we'd have a UI to distinguish PLAYER vs VIEWER.
                          for (Account p : selected) {
                            if (!p.equals(targetLeague.getCommissioner())) {
                              leagueService.addPlayer(targetLeague, p);
                            }
                          }

                          // Remove members not in selected list
                          if (league != null) {
                            leagueMembershipRepository.findByLeague(league).stream()
                                .filter(
                                    m ->
                                        !m.getMember().equals(league.getCommissioner())
                                            && !selected.contains(m.getMember()))
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
