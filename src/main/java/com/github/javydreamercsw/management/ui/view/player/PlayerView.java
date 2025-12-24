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
package com.github.javydreamercsw.management.ui.view.player;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.BOOKER_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.PLAYER_ROLE;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.match.MatchView;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerProfileView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
  private final SegmentService segmentService;

  private Wrestler playerWrestler;

  @Autowired
  public PlayerView(
      WrestlerService wrestlerService,
      ShowService showService,
      RivalryService rivalryService,
      InboxService inboxService,
      SecurityUtils securityUtils,
      @Qualifier("managementAccountService") AccountService accountService,
      SegmentService segmentService) {
    this.wrestlerService = wrestlerService;
    this.showService = showService;
    this.rivalryService = rivalryService;
    this.inboxService = inboxService;
    this.securityUtils = securityUtils;
    this.accountService = accountService;
    this.segmentService = segmentService;

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
        playerWrestler = wrestlerService.findByIdWithInjuries(maybeWrestler.get().getId()).get();
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
    leftColumn.add(createInjuriesSummary());
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
      // Wrestler Image
      Image wrestlerImage = new Image();
      wrestlerImage.setId("wrestler-image");
      if (playerWrestler.getImageUrl() != null && !playerWrestler.getImageUrl().isEmpty()) {
        wrestlerImage.setSrc(playerWrestler.getImageUrl());
      } else {
        wrestlerImage.setSrc("https://via.placeholder.com/150");
      }
      wrestlerImage.setAlt("Wrestler Image");
      wrestlerImage.setWidth("150px");
      wrestlerImage.setHeight("150px");

      summary.add(wrestlerImage);

      Span nameSpan = new Span("Name: " + playerWrestler.getName());
      nameSpan.setId("wrestler-name");
      summary.add(nameSpan);

      Span tierSpan = new Span("Tier: " + playerWrestler.getTier());
      tierSpan.setId("wrestler-tier");
      summary.add(tierSpan);

      Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(playerWrestler.getId());
      if (stats.isPresent()) {
        WrestlerStats wrestlerStats = stats.get();
        Span winsSpan = new Span("Wins: " + wrestlerStats.getWins());
        winsSpan.setId("wrestler-wins");
        summary.add(winsSpan);

        Span lossesSpan = new Span("Losses: " + wrestlerStats.getLosses());
        lossesSpan.setId("wrestler-losses");
        summary.add(lossesSpan);
      }

      RouterLink profileLink =
          new RouterLink(
              "View Full Profile",
              WrestlerProfileView.class,
              new RouteParameters("wrestlerId", String.valueOf(playerWrestler.getId())));
      profileLink.setId("view-full-profile-link");

      summary.add(profileLink);
    }
    return summary;
  }

  private VerticalLayout createUpcomingMatches() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H2("Upcoming Matches"));

    Grid<Segment> grid = new Grid<>();
    grid.setId("upcoming-matches-grid");

    grid.addColumn(segment -> segment.getShow().getName()).setHeader("Show");
    grid.addColumn(segment -> segment.getShow().getShowDate()).setHeader("Date");
    grid.addColumn(
            segment ->
                segment.getWrestlers().stream()
                    .filter(w -> !w.equals(playerWrestler))
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(", ")))
        .setHeader("Opponent(s)");
    grid.addColumn(segment -> segment.getSegmentType().getName()).setHeader("Match Type");
    grid.addComponentColumn(
            segment -> {
              Button button = new Button("Go to Match");
              button.addClickListener(
                  event ->
                      getUI()
                          .ifPresent(
                              ui ->
                                  ui.navigate(
                                      MatchView.class,
                                      new RouteParameters(
                                          "matchId", String.valueOf(segment.getId())))));
              button.setId("go-to-match-" + segment.getId());
              return button;
            })
        .setHeader("Actions");

    if (playerWrestler != null) {
      grid.setItems(segmentService.getUpcomingSegmentsForWrestler(playerWrestler, 5));
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
    grid.setId("active-rivalries-grid");
    grid.addColumn(rivalry -> rivalry.getOpponent(playerWrestler).getName())
        .setHeader("Opponent")
        .setSortable(true);
    grid.addColumn(Rivalry::getHeat).setHeader("Heat").setSortable(true);

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
    grid.setId("inbox-grid");
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

  private VerticalLayout createInjuriesSummary() {
    VerticalLayout layout = new VerticalLayout();
    layout.add(new H3("Bumps & Injuries"));
    if (playerWrestler != null) {
      Paragraph bumps = new Paragraph("Bumps: " + playerWrestler.getBumps());
      bumps.setId("wrestler-bumps");
      layout.add(bumps);
      if (playerWrestler.getInjuries().isEmpty()) {
        layout.add(new Paragraph("No current injuries."));
      } else {
        playerWrestler
            .getInjuries()
            .forEach(
                injury -> {
                  layout.add(new Paragraph("- " + injury.getDisplayString()));
                });
      }
    }
    layout.setId("injuries-summary");
    return layout;
  }
}
