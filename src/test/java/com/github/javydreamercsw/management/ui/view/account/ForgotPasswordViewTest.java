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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.PasswordResetService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.EmailField;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ForgotPasswordViewTest extends AbstractViewTest {

  @MockitoBean(name = "managementAccountService")
  private AccountService accountService;

  @MockitoBean private PasswordResetService passwordResetService;

  private ForgotPasswordView view;

  @BeforeEach
  void setup() {
    view = new ForgotPasswordView(accountService, passwordResetService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render Forgot Password heading")
  void shouldRenderHeading() {
    H1 heading = _get(view, H1.class, spec -> spec.withText("Forgot Password"));
    org.junit.jupiter.api.Assertions.assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should render email field")
  void shouldRenderEmailField() {
    EmailField emailField = _get(view, EmailField.class);
    org.junit.jupiter.api.Assertions.assertTrue(emailField.isVisible());
  }

  @Test
  @DisplayName("Submit with known email should call createPasswordResetTokenForUser")
  void submitWithKnownEmailCallsService() {
    var account = new com.github.javydreamercsw.base.domain.account.Account();
    when(accountService.findByEmail(anyString())).thenReturn(Optional.of(account));
    when(passwordResetService.createPasswordResetTokenForUser(account)).thenReturn("tok");

    EmailField emailField = _get(view, EmailField.class);
    emailField.setValue("test@example.com");

    Button resetButton = _get(view, Button.class, spec -> spec.withText("Reset Password"));
    resetButton.click();

    verify(passwordResetService).createPasswordResetTokenForUser(account);
  }
}
