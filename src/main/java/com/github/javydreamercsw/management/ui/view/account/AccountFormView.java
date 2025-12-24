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
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;

@Route("account")
@RolesAllowed("ADMIN")
public class AccountFormView extends Main implements HasUrlParameter<Long> {

  private final AccountService accountService;
  private final RoleRepository roleRepository;
  private final Binder<Account> binder = new Binder<>(Account.class);
  private Account account;

  private final TextField username = new TextField("Username");
  private final EmailField email = new EmailField("Email");
  private final PasswordField password = new PasswordField("Password");
  private final ComboBox<Role> role = new ComboBox<>("Role");

  public AccountFormView(
      @NonNull AccountService accountService, @NonNull RoleRepository roleRepository) {
    this.accountService = accountService;
    this.roleRepository = roleRepository;
    setupForm();
    setupBinder();
  }

  @Override
  public void setParameter(BeforeEvent event, Long parameter) {
    Optional<Account> optionalAccount = accountService.get(parameter);
    if (optionalAccount.isPresent()) {
      account = optionalAccount.get();
    } else {
      Notification.show("Account not found");
      UI.getCurrent().navigate(AccountListView.class);
      return;
    }
    binder.readBean(account);
    // Don't show existing password
    password.clear();
  }

  private void setupBinder() {
    binder
        .forField(username)
        .asRequired()
        .withValidator(
            u ->
                account == null
                    || account.getId() == null
                    || accountService.findByUsername(u).isEmpty()
                    || accountService.findByUsername(u).get().getId().equals(account.getId()),
            "Username already exists")
        .bind(Account::getUsername, Account::setUsername);

    binder
        .forField(email)
        .asRequired()
        .withValidator(
            e ->
                account == null
                    || account.getId() == null
                    || accountService.findByEmail(e).isEmpty()
                    || accountService.findByEmail(e).get().getId().equals(account.getId()),
            "Email already exists")
        .bind(Account::getEmail, Account::setEmail);

    binder
        .forField(password)
        .withValidator(
            pass ->
                account.getId() != null
                    || (pass.length() >= 8
                        && pass.matches(".*[a-zA-Z].*")
                        && pass.matches(".*[0-9].*")),
            "Password must be at least 8 characters long and contain at least one letter and one"
                + " number.")
        .bind(
            account -> "",
            (account, pass) -> {
              if (pass != null && !pass.isEmpty()) {
                account.setPassword(pass);
              }
            });

    binder
        .forField(role)
        .asRequired()
        .bind(
            a -> a.getRoles().stream().findFirst().orElse(null),
            (a, newRole) -> a.setRoles(Set.of(newRole)));
  }

  private void setupForm() {
    FormLayout formLayout = new FormLayout();
    role.setItems(roleRepository.findAll());
    role.setItemLabelGenerator(r -> r.getName().name());
    formLayout.add(username, email, password, role);
    username.setId("username-field");
    email.setId("email-field");
    password.setId("password-field");
    role.setId("role-field");
    formLayout.setId("form-layout");

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (binder.writeBeanIfValid(account)) {
                accountService.update(account);
                Notification.show("Account saved successfully.");
                UI.getCurrent().navigate(AccountListView.class);
              }
            });

    saveButton.setId("save-button");

    Button cancelButton =
        new Button("Cancel", e -> UI.getCurrent().navigate(AccountListView.class));

    cancelButton.setId("cancel-button");

    add(formLayout, saveButton, cancelButton);
  }
}
