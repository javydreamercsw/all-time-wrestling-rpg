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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.HashSet;
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
  @Mock private WrestlerService wrestlerService;

  private AccountFormView view;

  @BeforeEach
  void setup() {
    Role adminRole = new Role();
    adminRole.setName(RoleName.ADMIN);
    when(roleRepository.findAll()).thenReturn(List.of(adminRole));

    when(wrestlerService.findAllActiveWrestlers()).thenReturn(List.of());
    view = new AccountFormView(accountService, roleRepository, wrestlerService);
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
    existing.setRoles(new HashSet<>(List.of(adminRole)));
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

  @Test
  @DisplayName("Wrestler field preloads the account's active wrestler")
  void wrestlerField_preloadsActiveWrestler() {
    Account account = account(1L, "player");
    account.setActiveWrestlerId(42L);
    when(accountService.get(1L)).thenReturn(Optional.of(account));
    com.github.javydreamercsw.management.domain.wrestler.Wrestler assigned =
        new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
    assigned.setId(42L);
    assigned.setName("Assigned Star");
    when(wrestlerService.findById(42L)).thenReturn(Optional.of(assigned));
    when(wrestlerService.findAllActiveWrestlers()).thenReturn(List.of(assigned));
    // Rebuild the view so setup() reflects the richer stubs.
    view = new AccountFormView(accountService, roleRepository, wrestlerService);
    UI.getCurrent().add(view);
    view.setParameter(null, 1L);

    @SuppressWarnings("unchecked")
    ComboBox<com.github.javydreamercsw.management.domain.wrestler.Wrestler> wrestlerCombo =
        (ComboBox<com.github.javydreamercsw.management.domain.wrestler.Wrestler>)
            _get(view, ComboBox.class, spec -> spec.withId("wrestler-field"));
    assertEquals(assigned, wrestlerCombo.getValue());
  }

  @Test
  @DisplayName("Save passes the picked wrestler id to setActiveWrestlerId")
  void save_callsSetActiveWrestlerId() {
    Account account = account(1L, "player");
    when(accountService.get(1L)).thenReturn(Optional.of(account));
    view.setParameter(null, 1L);

    // The stored password never round-trips into the field, so writeBeanIfValid needs a
    // valid one typed in before Save will reach the assignment call.
    PasswordField passwordField =
        _get(view, PasswordField.class, spec -> spec.withId("password-field"));
    passwordField.setValue("Password1!");

    Button saveButton = _get(view, Button.class, spec -> spec.withId("save-button"));
    saveButton.click();

    verify(accountService).setActiveWrestlerId(eq(1L), any());
  }

  private Account account(Long id, String username) {
    Account a = new Account(username, "Password1!", username + "@test.com");
    org.springframework.test.util.ReflectionTestUtils.setField(a, "id", id);
    Role adminRole = new Role();
    adminRole.setName(RoleName.ADMIN);
    a.setRoles(new HashSet<>(List.of(adminRole)));
    return a;
  }
}
