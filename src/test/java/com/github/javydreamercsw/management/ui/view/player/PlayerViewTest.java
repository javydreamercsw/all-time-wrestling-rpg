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
package com.github.javydreamercsw.management.ui.view.player;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonStatsService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class PlayerViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private ShowService showService;
  @Mock private RivalryService rivalryService;
  @Mock private InboxService inboxService;
  @Mock private SecurityUtils securityUtils;
  @Mock private AccountService accountService;
  @Mock private SegmentService segmentService;
  @Mock private NewsService newsService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private AchievementRepository achievementRepository;
  @Mock private SeasonStatsService seasonStatsService;
  @Mock private SeasonRepository seasonRepository;
  @Mock private UniverseContextService universeContextService;

  @SuppressWarnings("unchecked")
  private PlayerDashboardView buildView() {
    when(transactionTemplate.execute(any(TransactionCallback.class)))
        .thenAnswer(
            inv -> {
              TransactionCallback<?> callback = inv.getArgument(0);
              return callback.doInTransaction(null);
            });
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());

    PlayerDashboardView view =
        new PlayerDashboardView(
            wrestlerService,
            wrestlerStatsService,
            rivalryService,
            inboxService,
            securityUtils,
            accountService,
            segmentService,
            newsService,
            transactionTemplate,
            achievementRepository,
            seasonStatsService,
            seasonRepository,
            universeContextService);
    UI.getCurrent().add(view);
    return view;
  }

  @Nested
  @DisplayName("No wrestler assigned")
  class NoWrestlerAssigned {

    private PlayerDashboardView view;

    @org.junit.jupiter.api.BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
      MockitoAnnotations.openMocks(PlayerViewTest.this);
      Account account = new Account();
      account.setUsername("testuser");
      account.setPassword("password");
      account.setEmail("test@example.com");
      CustomUserDetails userDetails = new CustomUserDetails(account);
      when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
      when(accountService.get(any())).thenReturn(Optional.of(account));
      when(wrestlerService.findAllByAccount(any())).thenReturn(Collections.emptyList());
      view = buildView();
    }

    @Test
    @DisplayName("Should render the Player Dashboard toolbar")
    void shouldRenderToolbar() {
      ViewToolbar toolbar = _get(view, ViewToolbar.class);
      assertTrue(toolbar.isVisible());
    }

    @Test
    @DisplayName("Switcher ComboBox should have no value when no wrestler is assigned")
    void switcherShouldHaveNoValueWhenNoWrestlerAssigned() {
      ComboBox<Wrestler> switcher =
          _get(view, ComboBox.class, spec -> spec.withId("active-wrestler-switcher"));
      assertNotNull(switcher);
      assertNull(switcher.getValue(), "ComboBox should be empty when account has no wrestler");
    }
  }

  @Nested
  @DisplayName("Wrestler assigned")
  class WrestlerAssigned {

    private PlayerDashboardView view;
    private Wrestler wrestler;

    @org.junit.jupiter.api.BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
      MockitoAnnotations.openMocks(PlayerViewTest.this);

      wrestler = new Wrestler();
      wrestler.setId(42L);
      wrestler.setName("Test Wrestler");

      WrestlerState state = new WrestlerState();

      Account account = new Account();
      account.setUsername("testuser");
      account.setPassword("password");
      account.setEmail("test@example.com");
      account.setActiveWrestlerId(42L);

      CustomUserDetails userDetails = new CustomUserDetails(account);
      when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
      when(accountService.get(any())).thenReturn(Optional.of(account));
      when(wrestlerService.findAllByAccount(any())).thenReturn(List.of(wrestler));
      when(wrestlerService.findById(42L)).thenReturn(Optional.of(wrestler));
      when(wrestlerService.findByIdWithDetails(42L)).thenReturn(Optional.of(wrestler));
      when(wrestlerService.getOrCreateState(anyLong(), any())).thenReturn(state);
      when(wrestlerStatsService.getWrestlerStats(anyLong(), any())).thenReturn(Optional.empty());
      when(seasonRepository.findByWrestler(any())).thenReturn(Collections.emptyList());
      when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());
      when(segmentService.getUpcomingSegmentsForWrestler(any(), anyInt()))
          .thenReturn(Collections.emptyList());
      when(universeContextService.getCurrentUniverseId()).thenReturn(1L);

      view = buildView();
    }

    @Test
    @DisplayName("Active wrestler should be pre-selected in the switcher ComboBox on load")
    void activewrestlershouldBePreSelectedInSwitcher() {
      ComboBox<Wrestler> switcher =
          _get(view, ComboBox.class, spec -> spec.withId("active-wrestler-switcher"));
      assertNotNull(switcher, "Active wrestler switcher ComboBox should be present");
      assertEquals(
          wrestler,
          switcher.getValue(),
          "ComboBox should be pre-selected with the active wrestler on page load");
    }
  }
}
