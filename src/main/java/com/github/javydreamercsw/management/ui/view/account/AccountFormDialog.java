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
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountFormDialog extends Dialog {

  private static final Logger LOG = LoggerFactory.getLogger(AccountFormDialog.class);
  private final AccountService accountService;
  private final Account account;
  private final BeanValidationBinder<Account> binder = new BeanValidationBinder<>(Account.class);

  public AccountFormDialog(AccountService accountService, Account account) {
    this.accountService = accountService;
    this.account = account;

    setHeaderTitle(account.getId() == null ? "New Account" : "Edit Account");

    FormLayout formLayout = new FormLayout();
    TextField username = new TextField("Username");
    username.setId("username-field");
    if (account.getId() != null) {
      username.setReadOnly(true);
    }
    EmailField email = new EmailField("Email");
    email.setId("email-field");
    if (account.getId() != null) {
      email.setReadOnly(true);
    }
    PasswordField password = new PasswordField("Password");
    password.setId("password-field");
    ComboBox<RoleName> role = new ComboBox<>("Role");
    role.setId("role-field");
    role.setItems(RoleName.values());

    binder.bind(username, "username");
    binder.bind(email, "email");
    if (account.getId() == null) {
      password.setRequiredIndicatorVisible(true);
      binder.forField(password).asRequired().bind("password");
    } else {
      password.setPlaceholder("Leave blank to keep current password");
      binder
          .forField(password)
          .bind(
              acc -> "",
              (acc, p) -> {
                if (p != null && !p.isEmpty()) {
                  acc.setPassword(p);
                }
              });
    }
    binder
        .forField(role)
        .asRequired()
        .bind(
            acc -> acc.getRoles().stream().findFirst().map(Role::getName).orElse(null),
            (acc, roleName) -> acc.setRoles(Set.of(accountService.getRole(roleName))));

    if (account.getRoles() != null && !account.getRoles().isEmpty()) {
      role.setValue(account.getRoles().iterator().next().getName());
    }

    formLayout.add(username, email, password, role);
    add(formLayout);

    Button saveButton = new Button("Save");
    saveButton.setId("save-button");
    saveButton.addClickListener(
        event -> {
          try {
            binder.writeBean(this.account);
            accountService.update(this.account);
            close();
          } catch (ValidationException e) {
            LOG.error("Validation failed", e);
          }
        });

    Button cancelButton = new Button("Cancel");
    cancelButton.addClickListener(event -> close());

    getFooter().add(new HorizontalLayout(saveButton, cancelButton));
  }
}
