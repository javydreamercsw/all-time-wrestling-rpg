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

import com.github.javydreamercsw.base.ui.view.LoginView;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Qualifier;

@Route("forgot-password")
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

  public ForgotPasswordView(
      @Qualifier("managementAccountService") AccountService accountService,
      PasswordResetService passwordResetService) {

    add(new H1("Forgot Password"));

    EmailField emailField = new EmailField("Enter your email address");
    emailField.setId("email-field");
    emailField.setRequiredIndicatorVisible(true);
    emailField.setWidth("300px");

    Button resetButton = new Button("Send Reset Link");
    resetButton.setId("reset-button");
    resetButton.addClickListener(
        event -> {
          accountService
              .findByEmail(emailField.getValue())
              .ifPresentOrElse(
                  account -> {
                    String token = passwordResetService.createPasswordResetTokenForUser(account);
                    // In a real application, you would send an email with the link.
                    // For now, we'll show a notification with the link for testing.
                    getUI()
                        .ifPresent(
                            ui ->
                                ui.getPage()
                                    .fetchCurrentURL(
                                        url -> {
                                          String resetUrl =
                                              url.toString()
                                                  .replace(
                                                      "forgot-password",
                                                      "reset-password?token=" + token);
                                          Notification.show(
                                              "Password reset link (for testing): " + resetUrl,
                                              5000,
                                              Notification.Position.MIDDLE);
                                        }));
                  },
                  () ->
                      Notification.show(
                          "No account found with that email address.",
                          3000,
                          Notification.Position.MIDDLE));
        });

    add(emailField, resetButton, new RouterLink("Back to Login", LoginView.class));

    setAlignItems(Alignment.CENTER);
  }
}
