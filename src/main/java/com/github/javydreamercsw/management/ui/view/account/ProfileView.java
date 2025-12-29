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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.crypto.password.PasswordEncoder;

@PageTitle("Profile")
@Route(value = "profile")
@PermitAll
public class ProfileView extends Main {

  public ProfileView(
      AccountService accountService, SecurityUtils securityUtils, PasswordEncoder passwordEncoder) {
    add(new H1("Profile"));

    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user ->
                accountService
                    .findByUsername(user.getUsername())
                    .ifPresent(
                        account -> {
                          FormLayout formLayout = new FormLayout();

                          TextField usernameField = new TextField("Username");
                          usernameField.setValue(account.getUsername());
                          usernameField.setReadOnly(true);

                          EmailField emailField = new EmailField("Email");
                          emailField.setValue(account.getEmail());

                          Button changePasswordButton =
                              new Button(
                                  "Change Password",
                                  event -> {
                                    ChangePasswordDialog dialog =
                                        new ChangePasswordDialog(
                                            accountService, passwordEncoder, account);
                                    dialog.open();
                                  });

                          Button saveButton =
                              new Button(
                                  "Save",
                                  event -> {
                                    account.setEmail(emailField.getValue());
                                    accountService.update(account);
                                    Notification.show("Profile updated successfully!");
                                  });

                          formLayout.add(
                              usernameField, emailField, changePasswordButton, saveButton);
                          add(formLayout);
                        }));
  }
}
