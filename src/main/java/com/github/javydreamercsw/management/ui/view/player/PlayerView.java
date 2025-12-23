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
package com.github.javydreamercsw.management.ui.view.player; /*
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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.BOOKER_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.PLAYER_ROLE;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "player", layout = MainLayout.class)
@PageTitle("Player Dashboard | ATW RPG")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE, PLAYER_ROLE})
public class PlayerView extends VerticalLayout {

  private final WrestlerService wrestlerService;
  private final ShowService showService;
  private final RivalryService rivalryService;
  private final InboxService inboxService;
  private final SecurityUtils securityUtils;
  private final AccountService accountService;

  private Wrestler playerWrestler;

  @Autowired
  public PlayerView(
      WrestlerService wrestlerService,
      ShowService showService,
      RivalryService rivalryService,
      InboxService inboxService,
      SecurityUtils securityUtils,
      AccountService accountService) {
    this.wrestlerService = wrestlerService;
    this.showService = showService;
    this.rivalryService = rivalryService;
    this.inboxService = inboxService;
    this.securityUtils = securityUtils;
    this.accountService = accountService;

    add(new ViewToolbar("Player Dashboard"));
    init();
  }

  private void init() {
    Optional<CustomUserDetails> maybeUserDetails = securityUtils.getAuthenticatedUser();
    if (maybeUserDetails.isPresent()) {
      Account account = maybeUserDetails.get().getAccount();
      // Try to get wrestler directly from CustomUserDetails
      Optional<Wrestler> maybeWrestler = Optional.ofNullable(maybeUserDetails.get().getWrestler());

      // If not directly available, try finding by account
      if (maybeWrestler.isEmpty()) {
        maybeWrestler = wrestlerService.findByAccount(account);
      }

      if (maybeWrestler.isPresent()) {
        playerWrestler = maybeWrestler.get();
        buildDashboard();
      } else {
        add(new H2("No wrestler assigned to your account."));
      }
    } else {
      add(new H2("You must be logged in to see this page."));
    }
  }

  private void buildDashboard() {
    HorizontalLayout mainContent = new HorizontalLayout();
    mainContent.setSizeFull();

    VerticalLayout leftColumn = new VerticalLayout();
    leftColumn.setWidth("50%");
    VerticalLayout rightColumn = new VerticalLayout();
    rightColumn.setWidth("50%");

    leftColumn.add(createWrestlerProfileSummary());
    leftColumn.add(createInboxSummary());
    rightColumn.add(createUpcomingMatches());
    rightColumn.add(createActiveRivalries());

    mainContent.add(leftColumn, rightColumn);
    add(mainContent);
  }

  private VerticalLayout createWrestlerProfileSummary() {
    VerticalLayout summary = new VerticalLayout();
    summary.add(new H2("My Wrestler"));
    if (playerWrestler != null) {
      summary.add(new Span("Name: " + playerWrestler.getName()));
      summary.add(new Span("Tier: " + playerWrestler.getTier()));
    }
    return summary;
  }

  private VerticalLayout createUpcomingMatches() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Upcoming Matches"));

    Grid<Show> grid = new Grid<>();
    grid.addColumn(Show::getName).setHeader("Show");
    grid.addColumn(Show::getShowDate).setHeader("Date");

    if (playerWrestler != null) {
      grid.setItems(showService.getUpcomingShowsForWrestler(playerWrestler, 5));
    } else {
      grid.setItems(Collections.emptyList());
    }

    layout.add(grid);
    return layout;
  }

  private VerticalLayout createActiveRivalries() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Active Rivalries"));
    Grid<Rivalry> grid = new Grid<>();
    grid.addColumn(rivalry -> rivalry.getOpponent(playerWrestler).getName()).setHeader("Opponent");
    grid.addColumn(Rivalry::getHeat).setHeader("Heat");

    if (playerWrestler != null) {
      grid.setItems(rivalryService.getRivalriesForWrestler(playerWrestler.getId()));
    }

    layout.add(grid);
    return layout;
  }

  private VerticalLayout createInboxSummary() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Inbox"));

    Grid<InboxItem> grid = new Grid<>();
    grid.addColumn(InboxItem::getDescription).setHeader("Message");
    grid.addColumn(InboxItem::getEventTimestamp).setHeader("Date");

    if (playerWrestler != null) {
      grid.setItems(inboxService.getInboxItemsForWrestler(playerWrestler, 5));
    } else {
      grid.setItems(Collections.emptyList());
    }

    layout.add(grid);
    return layout;
  }
}
