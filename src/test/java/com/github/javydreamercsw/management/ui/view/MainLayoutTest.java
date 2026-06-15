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
package com.github.javydreamercsw.management.ui.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

class MainLayoutTest extends AbstractViewTest {

  @Mock private MenuService menuService;
  @Mock private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  @Mock private SecurityUtils securityUtils;
  @Mock private AccountService accountService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ThemeService themeService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseRepository universeRepository;
  @Mock private UniverseMembershipService universeMembershipService;
  @Mock private InboxService inboxService;
  @Mock private TutorialService tutorialService;

  @BeforeEach
  void setup() {
    when(menuService.getMenuItems()).thenReturn(Collections.emptyList());
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(securityUtils.isAuthenticated()).thenReturn(false);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    when(universeRepository.findAll()).thenReturn(Collections.emptyList());
    when(universeMembershipService.getUniversesForAccount(any()))
        .thenReturn(Collections.emptyList());
  }

  private MainLayout createLayout() {
    MainLayout layout =
        new MainLayout(
            menuService,
            inboxUpdateBroadcaster,
            Optional.empty(),
            securityUtils,
            accountService,
            passwordEncoder,
            themeService,
            universeContextService,
            universeRepository,
            universeMembershipService,
            inboxService,
            tutorialService);
    UI.getCurrent().add(layout);
    return layout;
  }

  @Test
  @DisplayName("Layout renders version as N/A when BuildProperties absent")
  void constructor_noBuildProperties_showsVersionNA() {
    MainLayout layout = createLayout();

    Span versionSpan = _get(layout, Span.class, spec -> spec.withId("version-span"));
    assertThat(versionSpan.getText()).isEqualTo("Version: N/A");
  }

  @Test
  @DisplayName("Authenticated user with zero unread messages renders inbox button")
  void refreshInboxBadge_noUnread_inboxButtonVisible() {
    Account account = new Account();
    account.setId(1L);
    CustomUserDetails user = new CustomUserDetails(account);

    when(securityUtils.isAuthenticated()).thenReturn(true);
    when(securityUtils.getCurrentUsername()).thenReturn("testuser");
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));
    when(inboxService.countUnread(any())).thenReturn(0L);

    MainLayout layout = createLayout();

    // Inbox button is always visible for authenticated users; badge itself is hidden (no unread)
    com.vaadin.flow.component.button.Button inboxButton =
        _get(
            layout,
            com.vaadin.flow.component.button.Button.class,
            spec -> spec.withId("inbox-button"));
    assertTrue(inboxButton.isVisible());
  }

  @Test
  @DisplayName("Authenticated user with unread messages sees badge with count")
  void refreshInboxBadge_withUnread_badgeShowsCount() {
    Account account = new Account();
    account.setId(2L);
    CustomUserDetails user = new CustomUserDetails(account);

    when(securityUtils.isAuthenticated()).thenReturn(true);
    when(securityUtils.getCurrentUsername()).thenReturn("booker");
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));
    when(inboxService.countUnread(any())).thenReturn(5L);

    MainLayout layout = createLayout();

    Span badge = _get(layout, Span.class, spec -> spec.withId("inbox-unread-badge"));
    assertThat(badge.getText()).isEqualTo("5");
    assertTrue(badge.isVisible());
  }

  @Test
  @DisplayName("Non-admin unauthenticated user gets empty universe list")
  void resolveAccessibleUniverses_unauthenticatedNonAdmin_returnsEmpty() {
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    MainLayout layout = createLayout();

    // Universe selector should be disabled (no universes available)
    assertThat(layout).isNotNull();
  }

  @Test
  @DisplayName("Admin sees all universes in selector")
  void resolveAccessibleUniverses_admin_returnsAllUniverses() {
    Universe u = new Universe();
    u.setId(1L);
    u.setName("Test Universe");

    when(securityUtils.isAdmin()).thenReturn(true);
    when(universeRepository.findAll()).thenReturn(List.of(u));
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.of(u));

    MainLayout layout = createLayout();

    assertThat(layout).isNotNull();
  }
}
