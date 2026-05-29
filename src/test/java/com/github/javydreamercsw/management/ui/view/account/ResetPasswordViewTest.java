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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.service.PasswordResetService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ResetPasswordViewTest extends AbstractViewTest {

  @Mock private PasswordResetService passwordResetService;
  @Mock private BeforeEnterEvent event;
  @Mock private Location location;
  @Mock private QueryParameters queryParameters;

  private ResetPasswordView view;

  @BeforeEach
  void setup() {
    when(event.getLocation()).thenReturn(location);
    when(location.getQueryParameters()).thenReturn(queryParameters);

    view = new ResetPasswordView(passwordResetService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should show invalid-token message when no token query param")
  void shouldShowInvalidTokenWhenNoParam() {
    when(queryParameters.getParameters()).thenReturn(Map.of());
    view.beforeEnter(event);

    H1 heading =
        _get(view, H1.class, spec -> spec.withText("Invalid or expired password reset token."));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName("Should show reset form when a valid token query param is provided")
  void shouldShowFormForValidToken() {
    when(queryParameters.getParameters()).thenReturn(Map.of("token", List.of("abc123")));
    when(passwordResetService.validatePasswordResetToken("abc123")).thenReturn(true);

    view.beforeEnter(event);

    H1 heading = _get(view, H1.class, spec -> spec.withText("Reset Password"));
    assertTrue(heading.isVisible());
  }

  @Test
  @DisplayName(
      "Reset button should call passwordResetService.resetPassword with matching passwords")
  void resetButtonCallsService() {
    when(queryParameters.getParameters()).thenReturn(Map.of("token", List.of("abc123")));
    when(passwordResetService.validatePasswordResetToken(anyString())).thenReturn(true);

    view.beforeEnter(event);

    PasswordField newPw = _get(view, PasswordField.class, spec -> spec.withCaption("New Password"));
    PasswordField confirmPw =
        _get(view, PasswordField.class, spec -> spec.withCaption("Confirm New Password"));
    newPw.setValue("ValidPass1!");
    confirmPw.setValue("ValidPass1!");

    Button resetBtn = _get(view, Button.class, spec -> spec.withText("Reset Password"));
    resetBtn.click();

    verify(passwordResetService).resetPassword("abc123", "ValidPass1!");
  }
}
