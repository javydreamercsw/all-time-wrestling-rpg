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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;

public class WrestlerDialog extends Dialog {

  private final WrestlerService wrestlerService;
  private final AccountService accountService;
  private final NpcService npcService;
  private final Wrestler wrestler;
  private final Binder<Wrestler> binder = new Binder<>(Wrestler.class);
  private final SecurityUtils securityUtils;

  public WrestlerDialog(
      @NonNull WrestlerService wrestlerService,
      @NonNull @Qualifier("baseAccountService") AccountService accountService,
      @NonNull NpcService npcService,
      @NonNull Runnable onSave,
      @NonNull SecurityUtils securityUtils) {
    this(
        wrestlerService,
        accountService,
        npcService,
        createDefaultWrestler(),
        onSave,
        securityUtils);
    setHeaderTitle("Create Wrestler");
  }

  private static Wrestler createDefaultWrestler() {
    return Wrestler.builder()
        .name("")
        .description("")
        .deckSize(15)
        .startingHealth(15)
        .lowHealth(0)
        .startingStamina(0)
        .lowStamina(0)
        .fans(0L)
        .isPlayer(false)
        .bumps(0)
        .gender(Gender.MALE)
        .tier(WrestlerTier.ROOKIE)
        .active(true)
        .build();
  }

  public WrestlerDialog(
      @NonNull WrestlerService wrestlerService,
      @NonNull @Qualifier("baseAccountService") AccountService accountService,
      @NonNull NpcService npcService,
      @NonNull Wrestler wrestler,
      @NonNull Runnable onSave,
      @NonNull SecurityUtils securityUtils) {
    this.wrestlerService = wrestlerService;
    this.accountService = accountService;
    this.npcService = npcService;
    this.wrestler = wrestler;
    this.securityUtils = securityUtils;

    setHeaderTitle("Edit Wrestler");

    FormLayout formLayout = new FormLayout();
    TextField nameField = new TextField("Name");
    nameField.setId("wrestler-dialog-name-field");
    nameField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    ComboBox<Gender> genderField = new ComboBox<>("Gender");
    genderField.setItems(Gender.values());
    genderField.setId("wrestler-dialog-gender-field");
    genderField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    ComboBox<Npc> managerField = new ComboBox<>("Manager");
    managerField.setItems(npcService.findAllByType("Manager"));
    managerField.setItemLabelGenerator(Npc::getName);
    managerField.setId("wrestler-dialog-manager-field");
    managerField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField deckSizeField = new TextField("Deck Size");
    deckSizeField.setId("wrestler-dialog-deck-size-field");
    deckSizeField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField startingHealthField = new TextField("Starting Health");
    startingHealthField.setId("wrestler-dialog-starting-health-field");
    startingHealthField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField lowHealthField = new TextField("Low Health");
    lowHealthField.setId("wrestler-dialog-low-health-field");
    lowHealthField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField startingStaminaField = new TextField("Starting Stamina");
    startingStaminaField.setId("wrestler-dialog-starting-stamina-field");
    startingStaminaField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField lowStaminaField = new TextField("Low Stamina");
    lowStaminaField.setId("wrestler-dialog-low-stamina-field");
    lowStaminaField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    TextField imageUrlField = new TextField("Image URL");
    imageUrlField.setId("wrestler-dialog-image-url-field");
    imageUrlField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    Checkbox isPlayerField = new Checkbox("Is Player");
    isPlayerField.setId("wrestler-dialog-is-player-field");
    isPlayerField.setReadOnly(!(securityUtils.isAdmin() || securityUtils.isBooker()));

    Checkbox activeField = new Checkbox("Active");
    activeField.setId("wrestler-dialog-active-field");
    activeField.setReadOnly(!securityUtils.canEdit(this.wrestler));

    ComboBox<Account> accountComboBox = new ComboBox<>("Player Account");
    accountComboBox.setId("wrestler-dialog-account-combo-box");
    accountComboBox.setPlaceholder("Select Account");
    accountComboBox.setItemLabelGenerator(Account::getUsername);
    accountComboBox.setItems(accountService.findAllNonPlayerAccounts());
    accountComboBox.setVisible(
        isPlayerField.getValue() && (securityUtils.isAdmin() || securityUtils.isBooker()));
    accountComboBox.setReadOnly(!(securityUtils.isAdmin() || securityUtils.isBooker()));

    if (wrestler.getAccount() != null) {
      accountComboBox.setValue(wrestler.getAccount());
    }

    isPlayerField.addValueChangeListener(
        event -> {
          accountComboBox.setVisible(
              event.getValue() && (securityUtils.isAdmin() || securityUtils.isBooker()));
          if (!event.getValue()) {
            accountComboBox.clear(); // Clear selection if not a player
          }
        });

    formLayout.add(
        nameField,
        genderField,
        managerField,
        deckSizeField,
        startingHealthField,
        lowHealthField,
        startingStaminaField,
        lowStaminaField,
        imageUrlField,
        isPlayerField,
        activeField,
        accountComboBox);

    binder.forField(nameField).bind(Wrestler::getName, Wrestler::setName);
    binder.forField(genderField).bind(Wrestler::getGender, Wrestler::setGender);
    binder.forField(managerField).bind(Wrestler::getManager, Wrestler::setManager);

    binder.readBean(this.wrestler);

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (binder.writeBeanIfValid(this.wrestler)) {
                try {
                  // Save the wrestler properties first
                  Wrestler savedWrestler =
                      this.wrestlerService.save(this.wrestler); // Capture the saved wrestler
                  this.wrestler.setId(
                      savedWrestler.getId()); // Ensure local wrestler object has the ID

                  // Handle account assignment if isPlayer is true and an account is selected
                  if (isPlayerField.getValue()) {
                    Account selectedAccount = accountComboBox.getValue();
                    if (selectedAccount == null) {
                      Notification.show(
                          "Please select an account for the player wrestler.",
                          3000,
                          Notification.Position.MIDDLE);
                      return;
                    }
                    // Use the ID from the savedWrestler
                    this.wrestlerService.setAccountForWrestler(
                        this.wrestler.getId(), selectedAccount.getId());
                  } else {
                    // If isPlayer is false, and there was a previously assigned account, unassign
                    // it
                    if (this.wrestler.getAccount() != null) {
                      this.wrestlerService.setAccountForWrestler(this.wrestler.getId(), null);
                    }
                  }

                  onSave.run();
                  close();
                } catch (IllegalArgumentException e) {
                  Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
                }
              }
            });
    saveButton.setId("wrestler-dialog-save-button");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.setVisible(securityUtils.canEdit(this.wrestler));

    Button cancelButton = new Button("Cancel", event -> close());
    cancelButton.setId("wrestler-dialog-cancel-button");

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
    add(formLayout);
  }
}
