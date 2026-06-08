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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite;
import com.github.javydreamercsw.management.domain.universe.UniverseInvite.InviteType;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.JoinRequestService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class JoinViewTest extends AbstractViewTest {

  @Mock private InviteService inviteService;
  @Mock private JoinRequestService joinRequestService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;

  private JoinView view;
  private Universe universe;
  private UniverseInvite validInvite;

  @BeforeEach
  void setup() {
    universe = new Universe();
    universe.setId(1L);
    universe.setName("Karibu Test Universe");

    validInvite = new UniverseInvite();
    validInvite.setId("valid-token");
    validInvite.setUniverse(universe);
    validInvite.setType(InviteType.COMMUNITY);

    when(inviteService.validateInvite("valid-token")).thenReturn(validInvite);
    when(inviteService.validateInvite("bad-token"))
        .thenThrow(new IllegalArgumentException("Invite link not found or invalid."));
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    view = new JoinView(inviteService, joinRequestService, accountService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  void validToken_anonymousUser_showsRegistrationForm() {
    simulateBeforeEnter(view, "join/valid-token", "token", "valid-token");

    H2 heading = _get(view, H2.class);
    assertThat(heading.getText()).contains("Karibu Test Universe");

    // Registration form fields should be present
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.textfield.TextField.class,
                spec -> spec.withId("join-username")))
        .isNotNull();
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.textfield.PasswordField.class,
                spec -> spec.withId("join-password")))
        .isNotNull();
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.button.Button.class,
                spec -> spec.withId("join-register-submit")))
        .isNotNull();
  }

  @Test
  void invalidToken_showsErrorPage() {
    simulateBeforeEnter(view, "join/bad-token", "token", "bad-token");

    H2 heading = _get(view, H2.class);
    assertThat(heading.getText()).contains("Invite Link Invalid");
  }

  @Test
  void validToken_loggedInUser_showsRequestForm() {
    com.github.javydreamercsw.base.security.CustomUserDetails userDetails =
        org.mockito.Mockito.mock(com.github.javydreamercsw.base.security.CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(99L));

    com.github.javydreamercsw.base.domain.account.Account account =
        new com.github.javydreamercsw.base.domain.account.Account("user", "hash", null);
    account.setId(99L);
    when(accountService.get(99L)).thenReturn(Optional.of(account));

    simulateBeforeEnter(view, "join/valid-token", "token", "valid-token");

    // Logged-in form shows read-only username and email fields, and submit button
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.textfield.TextField.class,
                spec -> spec.withId("join-username-display")))
        .isNotNull();
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.textfield.EmailField.class,
                spec -> spec.withId("join-email-display")))
        .isNotNull();
    assertThat(
            _get(
                view,
                com.vaadin.flow.component.button.Button.class,
                spec -> spec.withId("join-submit-button")))
        .isNotNull();
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private void simulateBeforeEnter(
      final JoinView v, final String location, final String paramName, final String paramValue) {
    com.vaadin.flow.router.RouteParameters params =
        new com.vaadin.flow.router.RouteParameters(paramName, paramValue);
    com.vaadin.flow.router.BeforeEnterEvent event =
        org.mockito.Mockito.mock(com.vaadin.flow.router.BeforeEnterEvent.class);
    org.mockito.Mockito.when(event.getRouteParameters()).thenReturn(params);
    v.beforeEnter(event);
  }
}
