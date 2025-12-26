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

import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.PasswordResetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PageTitle("Forgot Password")
@Route(value = "forgot-password")
@AnonymousAllowed
public class ForgotPasswordView extends Main {

  private static final Logger LOG = LoggerFactory.getLogger(ForgotPasswordView.class);

  public ForgotPasswordView(
      AccountService accountService, PasswordResetService passwordResetService) {
    VerticalLayout layout = new VerticalLayout();
    layout.setAlignItems(VerticalLayout.Alignment.CENTER);
    add(layout);
    layout.add(new H1("Forgot Password"));
    EmailField emailField = new EmailField("Email");
    Button resetButton =
        new Button(
            "Reset Password",
            event ->
                accountService
                    .findByEmail(emailField.getValue())
                    .ifPresentOrElse(
                        account -> {
                          String token =
                              passwordResetService.createPasswordResetTokenForUser(account);
                          LOG.info("Password reset token for {}: {}", account.getEmail(), token);
                          Notification.show(
                              "A password reset link has been sent to your email address.");
                        },
                        () -> Notification.show("No account found with that email address.")));
    layout.add(emailField, resetButton);
  }
}
