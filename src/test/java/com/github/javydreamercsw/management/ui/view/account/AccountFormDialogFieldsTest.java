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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AccountFormDialogFieldsTest extends AbstractViewTest {

  @Mock private AccountService accountService;

  private AccountFormDialog dialog;

  @BeforeEach
  void setup() {
    when(accountService.getRole(any(RoleName.class)))
        .thenReturn(new Role(RoleName.VIEWER, "Viewer"));

    Account account = new Account();
    dialog = new AccountFormDialog(accountService, account);
  }

  @Test
  @DisplayName("Dialog should not be null after construction")
  void dialogConstructs() {
    assertNotNull(dialog, "AccountFormDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain a username TextField")
  void usernameFieldExists() {
    List<TextField> fields = _find(dialog, TextField.class);
    assertFalse(fields.isEmpty(), "Expected at least one TextField (username)");
  }

  @Test
  @DisplayName("Dialog should contain an email field")
  void emailFieldExists() {
    List<EmailField> emailFields = _find(dialog, EmailField.class);
    assertFalse(emailFields.isEmpty(), "Expected at least one EmailField");
  }
}
