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

import com.github.javydreamercsw.base.security.PasswordValidator;
import com.github.javydreamercsw.base.ui.view.LoginView;
import com.github.javydreamercsw.management.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Map;

@Route("reset-password")
@PageTitle("Reset Password")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver {

  private final PasswordResetService passwordResetService;
  private String token;
  private final Binder<Void> binder = new Binder<>();

  public ResetPasswordView(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
    setAlignItems(Alignment.CENTER);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Location location = event.getLocation();
    Map<String, List<String>> parameters = location.getQueryParameters().getParameters();
    if (parameters.containsKey("token")) {
      token = parameters.get("token").get(0);
      passwordResetService
          .getPasswordResetToken(token)
          .ifPresentOrElse(
              resetToken -> {
                if (resetToken.isExpired()) {
                  showInvalidTokenError();
                } else {
                  buildUi();
                }
              },
              this::showInvalidTokenError);
    } else {
      showInvalidTokenError();
    }
  }

  private void buildUi() {
    removeAll();
    add(new H1("Reset Password"));

    PasswordField newPassword = new PasswordField("New Password");
    newPassword.setRequired(true);

    PasswordField confirmPassword = new PasswordField("Confirm New Password");
    confirmPassword.setRequired(true);

    binder
        .forField(newPassword)
        .withValidator(new PasswordValidator("Invalid password"))
        .withValidator(p -> p.equals(confirmPassword.getValue()), "Passwords do not match")
        .bind(v -> null, (v, p) -> {});

    Button saveButton =
        new Button(
            "Reset Password",
            event -> {
              if (binder.validate().isOk()) {
                passwordResetService.resetPassword(token, newPassword.getValue());
                Notification.show(
                    "Password reset successfully. You can now log in.",
                    3000,
                    Notification.Position.MIDDLE);
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
              }
            });

    add(newPassword, confirmPassword, saveButton, new RouterLink("Back to Login", LoginView.class));
  }

  private void showInvalidTokenError() {
    removeAll();
    add(new H1("Invalid or Expired Token"));
    add(new RouterLink("Request a new reset link", ForgotPasswordView.class));
  }
}
