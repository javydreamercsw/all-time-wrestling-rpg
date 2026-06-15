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
package com.github.javydreamercsw.management.ui.view.inbox;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Sort;

class InboxViewTest extends AbstractViewTest {

  @Mock private InboxService inboxService;
  @Mock private InboxEventTypeRegistry eventTypeRegistry;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private MatchFulfillmentService matchFulfillmentService;
  @Mock private SecurityUtils securityUtils;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private InboxView view;

  @BeforeEach
  void setup() {
    when(wrestlerRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());
    when(eventTypeRegistry.getEventTypes()).thenReturn(Collections.emptyList());
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    when(inboxService.search(any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    view =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the inbox grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Player with wrestler should have read-only target filter")
  void configureForUser_player_withWrestler_setsTargetFilterReadOnly() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    Account account = mock(Account.class);
    CustomUserDetails userDetails = new CustomUserDetails(account, wrestler);

    when(securityUtils.isPlayer()).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));

    InboxView playerView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(playerView);

    MultiSelectComboBox<?> targetFilter = _get(playerView, MultiSelectComboBox.class);
    assertTrue(targetFilter.isReadOnly());
  }

  @Test
  @DisplayName("Non-player should not have read-only target filter")
  void configureForUser_nonPlayer_doesNotLockTargetFilter() {
    when(securityUtils.isPlayer()).thenReturn(false);

    MultiSelectComboBox<?> targetFilter = _get(view, MultiSelectComboBox.class);
    assertFalse(targetFilter.isReadOnly());
  }

  @Test
  @DisplayName("Action buttons are disabled when no items are selected")
  void actionButtons_initiallyDisabled() {
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);

    InboxView freshView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(freshView);

    Button markRead = _get(freshView, Button.class, spec -> spec.withText("Mark Selected as Read"));
    Button markUnread =
        _get(freshView, Button.class, spec -> spec.withText("Mark Selected as Unread"));
    Button delete = _get(freshView, Button.class, spec -> spec.withText("Delete Selected"));

    assertFalse(markRead.isEnabled());
    assertFalse(markUnread.isEnabled());
    assertFalse(delete.isEnabled());
  }

  @Test
  @DisplayName("Clicking unread item auto-marks it as read")
  void showDetails_unreadItem_togglesReadStatus() {
    InboxEventType eventType = new InboxEventType("MATCH_REQUEST", "Match Request");
    InboxItem unreadItem = new InboxItem();
    unreadItem.setId(10L);
    unreadItem.setEventType(eventType);
    unreadItem.setRead(false);
    unreadItem.setDescription("Test description");

    when(inboxService.search(any(), any(), any(), any(), any())).thenReturn(List.of(unreadItem));
    when(inboxService.toggleReadStatus(any())).thenReturn(unreadItem);
    when(securityUtils.canEdit(any())).thenReturn(true);

    InboxView freshView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(freshView);

    Grid<InboxItem> grid = _get(freshView, Grid.class);
    GridKt._clickItem(grid, 0, 1, false, false, false, false);

    verify(inboxService).toggleReadStatus(unreadItem);
  }

  @Test
  @DisplayName("NAVIGATE action type renders a View button")
  void createActionComponent_navigate_rendersViewButton() {
    InboxEventType eventType = new InboxEventType("CHAMPIONSHIP_CHANGE", "Championship Change");
    InboxItem item = new InboxItem();
    item.setId(20L);
    item.setEventType(eventType);
    item.setRead(false);
    item.setDescription("New champion crowned");
    item.setActionType("NAVIGATE");
    item.setActionPayload("{\"route\":\"titles/5\"}");

    when(inboxService.search(any(), any(), any(), any(), any())).thenReturn(List.of(item));
    when(inboxService.toggleReadStatus(any())).thenReturn(item);
    when(securityUtils.canEdit(any())).thenReturn(true);

    InboxView freshView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(freshView);

    Grid<InboxItem> grid = _get(freshView, Grid.class);
    GridKt._clickItem(grid, 0, 1, false, false, false, false);

    _get(freshView, Button.class, spec -> spec.withId("navigate-btn-20"));
  }

  @Test
  @DisplayName("NAVIGATE action with missing payload renders no View button")
  void createActionComponent_navigate_missingPayload_noButton() {
    InboxEventType eventType = new InboxEventType("CHAMPIONSHIP_CHANGE", "Championship Change");
    InboxItem item = new InboxItem();
    item.setId(21L);
    item.setEventType(eventType);
    item.setRead(false);
    item.setDescription("New champion crowned");
    item.setActionType("NAVIGATE");
    item.setActionPayload(null);

    when(inboxService.search(any(), any(), any(), any(), any())).thenReturn(List.of(item));
    when(inboxService.toggleReadStatus(any())).thenReturn(item);
    when(securityUtils.canEdit(any())).thenReturn(true);

    InboxView freshView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(freshView);

    Grid<InboxItem> grid = _get(freshView, Grid.class);
    GridKt._clickItem(grid, 0, 1, false, false, false, false);

    boolean found = false;
    try {
      _get(freshView, Button.class, spec -> spec.withId("navigate-btn-21"));
      found = true;
    } catch (AssertionError ignored) {
    }
    assertFalse(found);
  }

  @Test
  @DisplayName("Clicking already-read item does not toggle read status")
  void showDetails_readItem_doesNotToggleReadStatus() {
    InboxEventType eventType = new InboxEventType("MATCH_REQUEST", "Match Request");
    InboxItem readItem = new InboxItem();
    readItem.setId(11L);
    readItem.setEventType(eventType);
    readItem.setRead(true);
    readItem.setDescription("Already read message");

    when(inboxService.search(any(), any(), any(), any(), any())).thenReturn(List.of(readItem));
    when(securityUtils.canEdit(any())).thenReturn(true);

    InboxView freshView =
        new InboxView(
            inboxService,
            eventTypeRegistry,
            wrestlerRepository,
            matchFulfillmentService,
            securityUtils,
            objectMapper);
    UI.getCurrent().add(freshView);

    Grid<InboxItem> grid = _get(freshView, Grid.class);
    GridKt._clickItem(grid, 0, 1, false, false, false, false);

    org.mockito.Mockito.verifyNoMoreInteractions(org.mockito.Mockito.ignoreStubs(inboxService));
  }
}
