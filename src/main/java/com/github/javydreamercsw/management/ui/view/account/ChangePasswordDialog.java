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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.PasswordValidator;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ChangePasswordDialog extends Dialog {

  private final Binder<Void> binder = new Binder<>();

  public ChangePasswordDialog(
      @Qualifier("managementAccountService") AccountService accountService,
      PasswordEncoder passwordEncoder,
      Account account) {

    setHeaderTitle("Change Password");

    FormLayout formLayout = new FormLayout();
    PasswordField currentPassword = new PasswordField("Current Password");
    currentPassword.setId("current-password-field");
    currentPassword.setRequired(true);

    PasswordField newPassword = new PasswordField("New Password");
    newPassword.setId("new-password-field");
    newPassword.setRequired(true);

    PasswordField confirmPassword = new PasswordField("Confirm New Password");
    confirmPassword.setId("confirm-password-field");
    confirmPassword.setRequired(true);

    binder
        .forField(newPassword)
        .withValidator(new PasswordValidator("Invalid password"))
        .withValidator(p -> p.equals(confirmPassword.getValue()), "Passwords do not match")
        .bind(v -> null, (v, p) -> {}); // Dummy binder

    binder
        .forField(currentPassword)
        .withValidator(
            p -> passwordEncoder.matches(p, account.getPassword()), "Incorrect current password")
        .bind(v -> null, (v, p) -> {}); // Dummy binder

    formLayout.add(currentPassword, newPassword, confirmPassword);
    add(formLayout);

    Button saveButton = new Button("Save");
    saveButton.setId("save-button");
    saveButton.addClickListener(
        event -> {
          if (binder.validate().isOk()) {
            account.setPassword(newPassword.getValue());
            accountService.update(account);
            Notification.show("Password changed successfully!");
            close();
          }
        });

    Button cancelButton = new Button("Cancel");
    cancelButton.addClickListener(event -> close());

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
  }
}
