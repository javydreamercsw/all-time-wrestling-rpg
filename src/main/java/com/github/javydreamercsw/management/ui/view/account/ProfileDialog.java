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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ProfileDialog extends Dialog {

  public ProfileDialog(
      Account account,
      AccountService accountService,
      PasswordEncoder passwordEncoder,
      ThemeService themeService) {

    setHeaderTitle("User Profile");

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);
    content.setAlignItems(FlexComponent.Alignment.CENTER);

    // Avatar
    Avatar avatar = new Avatar(account.getUsername());
    avatar.setWidth("64px");
    avatar.setHeight("64px");
    avatar.addClassName(LumoUtility.Margin.Bottom.MEDIUM);
    content.add(avatar);

    FormLayout formLayout = new FormLayout();
    formLayout.setWidthFull();

    TextField usernameField = new TextField("Username");
    usernameField.setValue(account.getUsername());
    usernameField.setReadOnly(true);

    EmailField emailField = new EmailField("Email");
    emailField.setValue(account.getEmail());

    ComboBox<String> themeSelection = new ComboBox<>("Theme");
    themeSelection.setId("theme-selection");
    themeSelection.setItems(themeService.getAvailableThemes());
    themeSelection.setValue(themeService.getEffectiveTheme(account));

    formLayout.add(usernameField, emailField, themeSelection);

    // Set responsive steps for single column
    formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

    content.add(formLayout);

    add(content);

    // Footer buttons
    Button cancel = new Button("Cancel", e -> close());

    Button changePasswordButton =
        new Button(
            "Change Password",
            event -> {
              new ChangePasswordDialog(accountService, passwordEncoder, account).open();
            });

    Button saveButton =
        new Button(
            "Save",
            event -> {
              account.setEmail(emailField.getValue());
              account.setThemePreference(themeSelection.getValue());
              accountService.update(account);
              Notification.show("Profile updated successfully!");
              UI.getCurrent().getPage().reload();
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    getFooter().add(changePasswordButton, cancel, saveButton);
  }
}
