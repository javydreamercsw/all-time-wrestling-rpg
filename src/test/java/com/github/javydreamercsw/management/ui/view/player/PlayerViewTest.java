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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

  private PlayerView view;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    Account account = new Account();
    account.setUsername("testuser");
    account.setPassword("password");
    account.setEmail("test@example.com");
    CustomUserDetails userDetails = new CustomUserDetails(account);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(accountService.get(any())).thenReturn(Optional.of(account));
    when(wrestlerService.findAllByAccount(any())).thenReturn(Collections.emptyList());
    when(transactionTemplate.execute(any(TransactionCallback.class)))
        .thenAnswer(
            inv -> {
              TransactionCallback<?> callback = inv.getArgument(0);
              return callback.doInTransaction(null);
            });
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());

    view =
        new PlayerView(
            wrestlerService,
            wrestlerStatsService,
            showService,
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
  }

  @Test
  @DisplayName("Should render the Player Dashboard toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
