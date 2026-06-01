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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
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
            securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the inbox grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }
}
