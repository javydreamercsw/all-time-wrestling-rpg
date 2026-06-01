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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AccountFormViewTest extends AbstractViewTest {

  @MockitoBean(name = "managementAccountService")
  private AccountService accountService;

  @Mock private RoleRepository roleRepository;

  private AccountFormView view;

  @BeforeEach
  void setup() {
    Role adminRole = new Role();
    adminRole.setName(RoleName.ADMIN);
    when(roleRepository.findAll()).thenReturn(List.of(adminRole));

    view = new AccountFormView(accountService, roleRepository);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render username field")
  void shouldRenderUsernameField() {
    TextField usernameField = _get(view, TextField.class, spec -> spec.withId("username-field"));
    assertTrue(usernameField.isVisible());
  }

  @Test
  @DisplayName("Should render email field")
  void shouldRenderEmailField() {
    EmailField emailField = _get(view, EmailField.class, spec -> spec.withId("email-field"));
    assertTrue(emailField.isVisible());
  }

  @Test
  @DisplayName("Should render password field")
  void shouldRenderPasswordField() {
    PasswordField passwordField =
        _get(view, PasswordField.class, spec -> spec.withId("password-field"));
    assertTrue(passwordField.isVisible());
  }

  @Test
  @DisplayName("Should render role combo box")
  void shouldRenderRoleComboBox() {
    @SuppressWarnings("unchecked")
    ComboBox<Role> roleCombo = _get(view, ComboBox.class, spec -> spec.withId("role-field"));
    assertTrue(roleCombo.isVisible());
  }

  @Test
  @DisplayName("Save button should call accountService.update when form is valid")
  void saveButtonCallsUpdate() {
    Account existing = new Account();
    existing.setUsername("user1");
    existing.setEmail("user1@example.com");
    Role adminRole = new Role();
    adminRole.setName(RoleName.ADMIN);
    existing.setRoles(new java.util.HashSet<>(List.of(adminRole)));
    when(accountService.get(1L)).thenReturn(Optional.of(existing));
    when(accountService.findByUsername(any())).thenReturn(Optional.empty());
    when(accountService.findByEmail(any())).thenReturn(Optional.empty());
    when(accountService.update(any())).thenReturn(existing);

    view.setParameter(null, 1L);

    TextField usernameField = _get(view, TextField.class, spec -> spec.withId("username-field"));
    EmailField emailField = _get(view, EmailField.class, spec -> spec.withId("email-field"));
    PasswordField passwordField =
        _get(view, PasswordField.class, spec -> spec.withId("password-field"));
    @SuppressWarnings("unchecked")
    ComboBox<Role> roleCombo = _get(view, ComboBox.class, spec -> spec.withId("role-field"));

    usernameField.setValue("updatedUser");
    emailField.setValue("updated@example.com");
    passwordField.setValue("UpdatedPass1!");
    roleCombo.setValue(adminRole);

    Button saveButton = _get(view, Button.class, spec -> spec.withId("save-button"));
    saveButton.click();

    verify(accountService).update(any(Account.class));
  }
}
