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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.JoinRequestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public no-auth page for following a universe invite link.
 *
 * <p>Route: {@code /join/{token}}
 *
 * <p>Behaviour:
 *
 * <ul>
 *   <li>Validates the token — shows an error page if invalid/expired/revoked.
 *   <li>If the user is already logged in → shows a simple "Request to Join" form.
 *   <li>If the user is not logged in → shows a self-registration form (username + password) that
 *       creates a PLAYER account and submits the join request atomically.
 * </ul>
 */
@Route("join/:token")
@PageTitle("Join Universe | All Time Wrestling RPG")
@AnonymousAllowed
@Slf4j
public class JoinView extends VerticalLayout implements BeforeEnterObserver {

  private final InviteService inviteService;
  private final JoinRequestService joinRequestService;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;

  private UniverseInvite invite;

  @Autowired
  public JoinView(
      final InviteService inviteService,
      final JoinRequestService joinRequestService,
      final AccountService accountService,
      final SecurityUtils securityUtils) {
    this.inviteService = inviteService;
    this.joinRequestService = joinRequestService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;

    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    addClassNames(LumoUtility.Padding.LARGE);
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    String token = event.getRouteParameters().get("token").orElse("");
    try {
      invite = inviteService.validateInvite(token);
    } catch (Exception e) {
      renderError(e.getMessage());
      return;
    }

    if (securityUtils.getAuthenticatedUser().isPresent()) {
      renderLoggedInForm();
    } else {
      renderRegistrationForm();
    }
  }

  private void renderError(final String message) {
    removeAll();
    add(new H2("Invite Link Invalid"));
    Paragraph p = new Paragraph(message);
    p.addClassName(LumoUtility.TextColor.ERROR);
    add(p);
    add(new Paragraph("Please ask the universe admin for a new invite link."));
  }

  private void renderLoggedInForm() {
    removeAll();
    add(new H2("Join " + invite.getUniverse().getName()));
    add(
        new Paragraph(
            "Click below to submit your request to join. The admin will review it shortly."));

    TextField nameField = new TextField("Your display name");
    nameField.setId("join-display-name");
    nameField.setWidthFull();

    Button submitBtn = new Button("Request to Join");
    submitBtn.setId("join-submit-button");
    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submitBtn.addClickListener(
        e -> {
          String name = nameField.getValue().trim();
          if (name.isBlank()) {
            Notification.show("Please enter your display name.")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
          }
          try {
            Long accountId =
                securityUtils
                    .getCurrentAccountId()
                    .orElseThrow(() -> new IllegalStateException("Not logged in"));
            Account current =
                accountService
                    .get(accountId)
                    .orElseThrow(() -> new IllegalStateException("Account not found"));
            joinRequestService.submitRequest(invite, name, null, current);
            renderConfirmation();
          } catch (Exception ex) {
            log.warn("Failed to submit join request: {}", ex.getMessage());
            Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    add(nameField, submitBtn);
  }

  private void renderRegistrationForm() {
    removeAll();
    add(new H2("Join " + invite.getUniverse().getName()));
    add(new Paragraph("Create a free account to request membership."));

    H3 accountHeader = new H3("Create Account");

    TextField usernameField = new TextField("Username");
    usernameField.setId("join-username");
    usernameField.setWidthFull();

    PasswordField passwordField = new PasswordField("Password");
    passwordField.setId("join-password");
    passwordField.setWidthFull();

    PasswordField confirmField = new PasswordField("Confirm Password");
    confirmField.setId("join-confirm-password");
    confirmField.setWidthFull();

    EmailField emailField = new EmailField("Email (optional)");
    emailField.setId("join-email");
    emailField.setWidthFull();

    Button submitBtn = new Button("Create Account & Request to Join");
    submitBtn.setId("join-register-submit");
    submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submitBtn.setWidthFull();
    submitBtn.addClickListener(
        e -> {
          String username = usernameField.getValue().trim();
          String password = passwordField.getValue();
          String confirm = confirmField.getValue();
          String email = emailField.getValue().trim();

          if (username.isBlank()) {
            showError("Username is required.");
            return;
          }
          if (password.isBlank()) {
            showError("Password is required.");
            return;
          }
          if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
          }

          try {
            Account account =
                accountService.createAccount(username, password, email, RoleName.PLAYER);
            joinRequestService.submitRequest(invite, username, email, account);
            renderConfirmation();
          } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
          } catch (Exception ex) {
            log.error("Registration + join failed", ex);
            showError("Something went wrong. Please try again.");
          }
        });

    add(accountHeader, usernameField, passwordField, confirmField, emailField, submitBtn);
  }

  private void renderConfirmation() {
    removeAll();
    add(new H2("Request Submitted!"));
    add(
        new Paragraph(
            "Your request to join \""
                + invite.getUniverse().getName()
                + "\" has been submitted. "
                + "The admin will review it and you will be notified once approved."));
  }

  private void showError(final String message) {
    Notification.show(message, 4000, Notification.Position.MIDDLE)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
