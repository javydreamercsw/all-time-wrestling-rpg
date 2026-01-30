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
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Set;
import lombok.NonNull;

public class LeagueDialog extends Dialog {

  private final LeagueService leagueService;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final Runnable onSave;
  private final Binder<League> binder = new Binder<>(League.class);

  public LeagueDialog(
      @NonNull LeagueService leagueService,
      @NonNull AccountService accountService,
      @NonNull SecurityUtils securityUtils,
      @NonNull Runnable onSave) {
    this.leagueService = leagueService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.onSave = onSave;

    setHeaderTitle("Create New League");

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("League Name");
    nameField.setRequired(true);

    MultiSelectListBox<Account> participantList = new MultiSelectListBox<>();
    participantList.setItems(accountService.findAll());
    participantList.setItemLabelGenerator(Account::getUsername);

    formLayout.add(nameField);
    formLayout.add(participantList);
    formLayout.setColspan(participantList, 2);

    binder.forField(nameField).bind(League::getName, League::setName);

    Button saveButton =
        new Button(
            "Create",
            e -> {
              if (binder.validate().isOk()) {
                securityUtils
                    .getAuthenticatedUser()
                    .ifPresent(
                        user -> {
                          League created =
                              leagueService.createLeague(nameField.getValue(), user.getAccount());
                          Set<Account> participants = participantList.getSelectedItems();
                          for (Account p : participants) {
                            if (!p.equals(user.getAccount())) {
                              leagueService.addPlayer(created, p);
                            }
                          }
                          onSave.run();
                          close();
                        });
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
    add(formLayout);
  }
}
