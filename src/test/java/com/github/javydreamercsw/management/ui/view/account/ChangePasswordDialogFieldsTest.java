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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.PasswordField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

class ChangePasswordDialogFieldsTest extends AbstractViewTest {

  @Mock private AccountService accountService;
  @Mock private PasswordEncoder passwordEncoder;

  private ChangePasswordDialog dialog;

  @BeforeEach
  void setup() {
    Account account = new Account();
    account.setPassword("encoded-password");
    dialog = new ChangePasswordDialog(accountService, passwordEncoder, account);
  }

  @Test
  @DisplayName("ChangePasswordDialog should not be null after construction")
  void dialogConstructs() {
    assertNotNull(dialog, "ChangePasswordDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain 3 PasswordField components")
  void threePasswordFieldsExist() {
    List<PasswordField> passwordFields = _find(dialog, PasswordField.class);
    assertEquals(3, passwordFields.size(), "Expected 3 PasswordFields: old, new, confirm");
  }
}
