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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ProfileDrawer extends Dialog {

  public ProfileDrawer(
      Account account,
      AccountService accountService,
      PasswordEncoder passwordEncoder,
      ThemeService themeService) {

    addClassName("profile-drawer");
    setModal(true);
    setDraggable(false);
    setResizable(false);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);

    // Header
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.addClassName(LumoUtility.Padding.MEDIUM);

    H3 title = new H3("Profile & Settings");
    title.addClassName(LumoUtility.Margin.NONE);

    Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    header.add(title, closeButton);

    // Content
    VerticalLayout content = new VerticalLayout();
    content.setPadding(true);
    content.setSpacing(true);
    content.setAlignItems(FlexComponent.Alignment.STRETCH);

    // User Info
    Div userHeader = new Div();
    userHeader.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.Margin.Bottom.LARGE);

    Avatar avatar = new Avatar(account.getUsername());
    avatar.setWidth("80px");
    avatar.setHeight("80px");
    avatar.addClassName(LumoUtility.Margin.Bottom.MEDIUM);

    userHeader.add(avatar);
    content.add(userHeader);

    // Form
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
    formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

    content.add(formLayout);

    // Actions
    VerticalLayout actions = new VerticalLayout();
    actions.setPadding(false);
    actions.setSpacing(true);

    Button changePasswordButton =
        new Button(
            "Change Password",
            VaadinIcon.PASSWORD.create(),
            event -> {
              new ChangePasswordDialog(accountService, passwordEncoder, account).open();
            });
    changePasswordButton.setWidthFull();

    Button saveButton =
        new Button(
            "Save Changes",
            event -> {
              account.setEmail(emailField.getValue());
              account.setThemePreference(themeSelection.getValue());
              accountService.update(account);
              Notification.show("Profile updated successfully!");
              UI.getCurrent().getPage().reload();
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.setWidthFull();

    actions.add(changePasswordButton, saveButton);
    content.add(actions);

    add(header, content);
  }
}
