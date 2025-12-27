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

import com.github.javydreamercsw.base.security.CustomPasswordValidator;
import com.github.javydreamercsw.management.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Reset Password")
@Route(value = "reset-password")
@AnonymousAllowed
public class ResetPasswordView extends Main implements BeforeEnterObserver {

  private final PasswordResetService passwordResetService;
  private String token;

  @Autowired
  public ResetPasswordView(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
    VerticalLayout layout = new VerticalLayout();
    layout.setAlignItems(VerticalLayout.Alignment.CENTER);
    add(layout);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();
    if (parameters.containsKey("token")) {
      token = parameters.get("token").get(0);
      if (passwordResetService.validatePasswordResetToken(token)) {
        showResetPasswordForm();
      } else {
        showInvalidTokenMessage();
      }
    } else {
      showInvalidTokenMessage();
    }
  }

  private void showResetPasswordForm() {
    removeAll();
    VerticalLayout layout = new VerticalLayout();
    layout.setAlignItems(VerticalLayout.Alignment.CENTER);
    add(layout);
    layout.add(new H1("Reset Password"));
    PasswordField newPassword = new PasswordField("New Password");
    PasswordField confirmPassword = new PasswordField("Confirm New Password");
    Button resetButton =
        new Button(
            "Reset Password",
            event -> {
              if (!newPassword.getValue().equals(confirmPassword.getValue())) {
                Notification.show("New passwords do not match.");
              } else if (!CustomPasswordValidator.isValid(newPassword.getValue())) {
                Notification.show("New password does not meet strength requirements.");
              } else {
                passwordResetService.resetPassword(token, newPassword.getValue());
                Notification.show("Password reset successfully.");
                getUI().ifPresent(ui -> ui.navigate("login"));
              }
            });
    layout.add(newPassword, confirmPassword, resetButton);
  }

  private void showInvalidTokenMessage() {
    removeAll();
    add(new H1("Invalid or expired password reset token."));
  }
}
