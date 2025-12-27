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
import com.github.javydreamercsw.base.security.CustomPasswordValidator;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ChangePasswordDialog extends Dialog {

  public ChangePasswordDialog(
      AccountService accountService, PasswordEncoder passwordEncoder, Account account) {
    setHeaderTitle("Change Password");

    FormLayout formLayout = new FormLayout();
    PasswordField oldPassword = new PasswordField("Old Password");
    PasswordField newPassword = new PasswordField("New Password");
    PasswordField confirmPassword = new PasswordField("Confirm New Password");

    formLayout.add(oldPassword, newPassword, confirmPassword);

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (!passwordEncoder.matches(oldPassword.getValue(), account.getPassword())) {
                Notification.show("Invalid old password.");
              } else if (!newPassword.getValue().equals(confirmPassword.getValue())) {
                Notification.show("New passwords do not match.");
              } else if (!CustomPasswordValidator.isValid(newPassword.getValue())) {
                Notification.show("New password does not meet strength requirements.");
              } else {
                account.setPassword(newPassword.getValue());
                accountService.update(account);
                Notification.show("Password changed successfully.");
                close();
              }
            });

    Button cancelButton = new Button("Cancel", event -> close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);

    add(formLayout, buttonLayout);
  }
}
