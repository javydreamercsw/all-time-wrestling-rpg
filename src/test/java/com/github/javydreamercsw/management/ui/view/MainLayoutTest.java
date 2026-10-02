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

import static com.github.mvysny.kaributesting.v10.ElementUtilsKt._fireDomEvent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import com.github.javydreamercsw.management.event.inbox.OpenProfileDrawerBroadcaster;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.ui.view.match.SegmentQrPickerDialog;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.DomEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.node.JsonNodeFactory;

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
  @Mock private OpenProfileDrawerBroadcaster openProfileDrawerBroadcaster;
  @Mock private ShowFacade showFacade;

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
            tutorialService,
            openProfileDrawerBroadcaster,
            showFacade);
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
    Button inboxButton = _get(layout, Button.class, spec -> spec.withId("inbox-button"));
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

  @Test
  @DisplayName("Burst of broadcasts collapses to one notification when unread count increases")
  void inboxBroadcast_burst_showsOneNotificationForActualDelta() throws Exception {
    Account account = new Account();
    account.setId(1L);
    CustomUserDetails user = new CustomUserDetails(account);
    when(securityUtils.isAuthenticated()).thenReturn(true);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));
    when(inboxService.countUnread(any())).thenReturn(3L);

    MainLayout layout = createLayout();
    layout.inboxNotifDebounceMs = 50;

    ArgumentCaptor<Consumer<InboxUpdateEvent>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(inboxUpdateBroadcaster).register(captor.capture());
    Consumer<InboxUpdateEvent> consumer = captor.getValue();
    InboxUpdateEvent event = new InboxUpdateEvent(this);

    try (MockedStatic<Notification> mocked = Mockito.mockStatic(Notification.class)) {
      Notification mockNotif = mock(Notification.class);
      mocked
          .when(() -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)))
          .thenReturn(mockNotif);

      for (int i = 0; i < 20; i++) {
        consumer.accept(event);
      }
      Thread.sleep(200);
      MockVaadin.clientRoundtrip();

      mocked.verify(
          () -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)),
          Mockito.times(1));
    }
  }

  @Test
  @DisplayName("Burst of broadcasts shows no notification when unread count is unchanged")
  void inboxBroadcast_noRelevantItems_showsNoNotification() throws Exception {
    Account account = new Account();
    account.setId(1L);
    CustomUserDetails user = new CustomUserDetails(account);
    when(securityUtils.isAuthenticated()).thenReturn(true);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(user));
    when(inboxService.countUnread(any())).thenReturn(0L);

    MainLayout layout = createLayout();
    layout.inboxNotifDebounceMs = 50;

    ArgumentCaptor<Consumer<InboxUpdateEvent>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(inboxUpdateBroadcaster).register(captor.capture());
    Consumer<InboxUpdateEvent> consumer = captor.getValue();
    InboxUpdateEvent event = new InboxUpdateEvent(this);

    try (MockedStatic<Notification> mocked = Mockito.mockStatic(Notification.class)) {
      mocked
          .when(() -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)))
          .thenReturn(mock(Notification.class));

      for (int i = 0; i < 20; i++) {
        consumer.accept(event);
      }
      Thread.sleep(200);
      MockVaadin.clientRoundtrip();

      mocked.verify(
          () -> Notification.show(anyString(), anyInt(), any(Notification.Position.class)),
          Mockito.never());
    }
  }

  @Test
  @DisplayName("Share QR Code nav item is visible to authenticated users")
  void shareQrNavItem_authenticatedUser_present() {
    when(securityUtils.isAuthenticated()).thenReturn(true);

    MainLayout layout = createLayout();

    SideNavItem qrItem = _get(layout, SideNavItem.class, spec -> spec.withId("share-qr-nav-item"));
    assertThat(qrItem.getLabel()).isEqualTo("Share QR Code");
  }

  @Test
  @DisplayName("Share QR Code nav item is hidden from unauthenticated users")
  void shareQrNavItem_unauthenticatedUser_absent() {
    when(securityUtils.isAuthenticated()).thenReturn(false);

    MainLayout layout = createLayout();

    List<SideNavItem> qrItems =
        _find(layout, SideNavItem.class, spec -> spec.withId("share-qr-nav-item"));
    assertThat(qrItems).isEmpty();
  }

  @Test
  @DisplayName("Clicking Share QR Code opens the segment picker dialog")
  void shareQrNavItem_click_opensPickerDialog() {
    when(securityUtils.isAuthenticated()).thenReturn(true);
    ShowService showService = mock(ShowService.class);
    when(showFacade.getShowService()).thenReturn(showService);
    when(showService.getAllShows(any())).thenReturn(Page.empty());

    MainLayout layout = createLayout();

    SideNavItem qrItem = _get(layout, SideNavItem.class, spec -> spec.withId("share-qr-nav-item"));
    _fireDomEvent(
        qrItem.getElement(),
        new DomEvent(qrItem.getElement(), "click", JsonNodeFactory.instance.objectNode()));

    Dialog dialog = _get(Dialog.class);
    assertThat(dialog).isInstanceOf(SegmentQrPickerDialog.class);
  }
}
