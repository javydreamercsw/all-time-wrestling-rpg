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
import com.github.javydreamercsw.base.domain.account.AchievementType;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.news.NewsTickerComponent;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignDashboardView;
import com.github.javydreamercsw.management.ui.view.match.MatchView;
import com.github.javydreamercsw.management.ui.view.wrestler.WrestlerProfileView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final NewsService newsService;
  private final TransactionTemplate transactionTemplate;

  private Wrestler playerWrestler;

  @Autowired
  public PlayerView(
      WrestlerService wrestlerService,
      ShowService showService,
      RivalryService rivalryService,
      InboxService inboxService,
      SecurityUtils securityUtils,
      @Qualifier("managementAccountService") AccountService accountService,
      SegmentService segmentService,
      NewsService newsService,
      TransactionTemplate transactionTemplate) {
    this.wrestlerService = wrestlerService;
    this.showService = showService;
    this.rivalryService = rivalryService;
    this.inboxService = inboxService;
    this.securityUtils = securityUtils;
    this.accountService = accountService;
    this.segmentService = segmentService;
    this.newsService = newsService;
    this.transactionTemplate = transactionTemplate;

    setHeightFull();
    setPadding(false);
    setSpacing(false);

    init();
  }

  private void init() {
    transactionTemplate.execute(
        status -> {
          Optional<CustomUserDetails> maybeUserDetails = securityUtils.getAuthenticatedUser();
          if (maybeUserDetails.isPresent()) {
            Account account = maybeUserDetails.get().getAccount();
            // Reload account to get latest activeWrestlerId and initialize collections
            account = accountService.get(account.getId()).get();
            org.hibernate.Hibernate.initialize(account.getAchievements());

            Wrestler active = null;
            if (account.getActiveWrestlerId() != null) {
              active = wrestlerService.findById(account.getActiveWrestlerId()).orElse(null);
            }

            if (active == null) {
              java.util.List<Wrestler> owned = wrestlerService.findAllByAccount(account);
              if (!owned.isEmpty()) {
                active = owned.get(0);
                accountService.setActiveWrestlerId(account.getId(), active.getId());
              }
            }

            removeAll();
            add(new ViewToolbar("Player Dashboard", createWrestlerSwitcher(account)));

            if (active != null) {
              playerWrestler = wrestlerService.findByIdWithInjuries(active.getId()).get();
              buildDashboard();
            } else {
              add(new H2("No wrestler assigned to your account."));
            }
          } else {
            add(new H2("You must be logged in to see this page."));
          }
          return null;
        });
  }

  private Component createWrestlerSwitcher(Account account) {
    ComboBox<Wrestler> switcher = new ComboBox<>("Active Wrestler");
    java.util.List<Wrestler> owned = wrestlerService.findAllByAccount(account);
    switcher.setItems(owned);
    switcher.setItemLabelGenerator(Wrestler::getName);
    if (playerWrestler != null) {
      switcher.setValue(playerWrestler);
    }
    switcher.addValueChangeListener(
        event -> {
          if (event.getValue() != null
              && (playerWrestler == null || !event.getValue().equals(playerWrestler))) {
            accountService.setActiveWrestlerId(account.getId(), event.getValue().getId());
            // Refresh the view
            init();
          }
        });
    return switcher;
  }

  private void buildDashboard() {
    Component profileCard = createProfileCard();
    NewsTickerComponent newsTicker = new NewsTickerComponent(newsService);
    Component tabsComponent = createTabs();

    add(profileCard, newsTicker, tabsComponent);
    setFlexGrow(0, profileCard); // Profile card doesn't grow
    setFlexGrow(0, newsTicker);
    setFlexGrow(1, tabsComponent); // Tabs component grows to fill space
    getStyle().set("padding", "1em");
  }

  private Component createProfileCard() {
    Avatar avatar = new Avatar(playerWrestler.getName());
    if (playerWrestler.getImageUrl() != null && !playerWrestler.getImageUrl().isEmpty()) {
      avatar.setImage(playerWrestler.getImageUrl());
    }
    avatar.setThemeName("xxlarge");
    avatar.setId("wrestler-image");

    H2 name = new H2(playerWrestler.getName());
    name.setId("wrestler-name");
    name.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.Top.NONE);

    Span tierBadge = createBadge(playerWrestler.getTier().getDisplayName(), "pill");
    tierBadge.setId("wrestler-tier");

    HorizontalLayout nameAndTier = new HorizontalLayout(name, tierBadge);
    nameAndTier.setAlignItems(FlexComponent.Alignment.BASELINE);
    nameAndTier.setSpacing(true);

    Optional<WrestlerStats> statsOpt = wrestlerService.getWrestlerStats(playerWrestler.getId());
    HorizontalLayout statsLayout = new HorizontalLayout();
    if (statsOpt.isPresent()) {
      WrestlerStats stats = statsOpt.get();
      statsLayout.add(createStat("Wins", String.valueOf(stats.getWins()), "wrestler-wins"));
      statsLayout.add(createStat("Losses", String.valueOf(stats.getLosses()), "wrestler-losses"));
    }
    statsLayout.add(
        createStat("Bumps", String.valueOf(playerWrestler.getBumps()), "wrestler-bumps"));
    statsLayout.setSpacing(true);

    Button profileButton =
        new Button(
            "View Full Profile",
            new Icon(VaadinIcon.ARROW_RIGHT),
            e ->
                getUI()
                    .ifPresent(
                        ui ->
                            ui.navigate(
                                WrestlerProfileView.class,
                                new RouteParameters(
                                    "wrestlerId", String.valueOf(playerWrestler.getId())))));
    profileButton.setIconAfterText(true);
    profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    profileButton.setId("view-full-profile-link");

    Button campaignButton =
        new Button(
            "Campaign Dashboard",
            new Icon(VaadinIcon.GAMEPAD),
            e -> getUI().ifPresent(ui -> ui.navigate(CampaignDashboardView.class)));
    campaignButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    campaignButton.setId("view-campaign-dashboard-link");

    Div injuries = createInjuriesSummary();

    // Career Legacy Info
    HorizontalLayout legacyLayout = new HorizontalLayout();
    legacyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    legacyLayout.setSpacing(true);

    Span legacyScoreLabel = new Span("Legacy: " + playerWrestler.getAccount().getLegacyScore());
    legacyScoreLabel.getElement().getThemeList().add("badge contrast");
    Tooltip.forComponent(legacyScoreLabel)
        .setText("Total points earned through fans and achievements");

    Span prestigeLabel = new Span("Prestige: " + playerWrestler.getAccount().getPrestige());
    prestigeLabel.getElement().getThemeList().add("badge");
    Tooltip.forComponent(prestigeLabel).setText("Permanent XP earned from achievements");

    legacyLayout.add(legacyScoreLabel, prestigeLabel);

    HorizontalLayout badgesLayout = new HorizontalLayout();
    badgesLayout.setPadding(false);
    badgesLayout.setSpacing(true);
    playerWrestler
        .getAccount()
        .getAchievements()
        .forEach(
            achievement -> {
              Icon badgeIcon = VaadinIcon.MEDAL.create();
              badgeIcon.getStyle().set("width", "16px");
              badgeIcon.getStyle().set("height", "16px");
              badgeIcon.setColor("var(--lumo-success-color)");
              Tooltip.forComponent(badgeIcon)
                  .setText(achievement.getName() + ": " + achievement.getDescription());
              badgesLayout.add(badgeIcon);
            });

    VerticalLayout infoLayout =
        new VerticalLayout(
            nameAndTier,
            legacyLayout,
            badgesLayout,
            statsLayout,
            injuries,
            profileButton,
            campaignButton);
    infoLayout.setPadding(false);
    infoLayout.setSpacing(false);
    infoLayout.getStyle().set("gap", "0.5em");

    HorizontalLayout card = new HorizontalLayout(avatar, infoLayout);
    card.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.SMALL);
    card.setAlignItems(FlexComponent.Alignment.CENTER);
    card.setSpacing(true);
    return card;
  }

  private Component createStat(@NonNull String label, @NonNull String value, @NonNull String id) {
    VerticalLayout stat = new VerticalLayout(new H4(label), new Span(value));
    stat.setPadding(false);
    stat.setSpacing(false);
    stat.setAlignItems(FlexComponent.Alignment.CENTER);
    stat.setId(id);
    return stat;
  }

  private Component createTabs() {
    Div pages = new Div();
    pages.setSizeFull();

    Grid<Segment> upcomingMatchesGrid = createUpcomingMatchesGrid();
    Grid<Rivalry> rivalriesGrid = createActiveRivalriesGrid();
    Grid<InboxItem> inboxGrid = createInboxGrid();
    Grid<AchievementType> achievementsGrid = createAchievementsGrid();

    pages.add(upcomingMatchesGrid, rivalriesGrid, inboxGrid, achievementsGrid);
    rivalriesGrid.setVisible(false);
    inboxGrid.setVisible(false);
    achievementsGrid.setVisible(false);

    Tab matchesTab = new Tab("Upcoming Matches");
    Tab rivalriesTab = new Tab("Rivalries");
    Tab inboxTab = new Tab("Inbox");
    Tab achievementsTab = new Tab("Achievements");

    Map<Tab, Component> tabsToPages =
        Map.of(
            matchesTab, upcomingMatchesGrid,
            rivalriesTab, rivalriesGrid,
            inboxTab, inboxGrid,
            achievementsTab, achievementsGrid);

    Tabs tabs = new Tabs(matchesTab, rivalriesTab, achievementsTab, inboxTab);
    tabs.addSelectedChangeListener(
        event -> {
          tabsToPages.values().forEach(page -> page.setVisible(false));
          Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
          selectedPage.setVisible(true);
        });
    tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
    tabs.setWidthFull();

    VerticalLayout layout = new VerticalLayout(tabs, pages);
    layout.setPadding(false);
    layout.setSpacing(false);
    layout.setAlignItems(FlexComponent.Alignment.STRETCH);
    layout.setSizeFull(); // Add this
    layout.setFlexGrow(1, pages); // Make pages div grow inside this layout
    return layout;
  }

  private Grid<Segment> createUpcomingMatchesGrid() {
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
              button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
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
    grid.setSizeFull();
    return grid;
  }

  private Grid<Rivalry> createActiveRivalriesGrid() {
    Grid<Rivalry> grid = new Grid<>();
    grid.setId("active-rivalries-grid");
    grid.addColumn(rivalry -> rivalry.getOpponent(playerWrestler).getName())
        .setHeader("Opponent")
        .setSortable(true);
    grid.addColumn(Rivalry::getHeat).setHeader("Heat").setSortable(true);

    if (playerWrestler != null) {
      grid.setItems(rivalryService.getRivalriesForWrestler(playerWrestler.getId()));
    }
    grid.setSizeFull();
    return grid;
  }

  private Grid<InboxItem> createInboxGrid() {
    Grid<InboxItem> grid = new Grid<>();
    grid.setId("inbox-grid");
    grid.addColumn(InboxItem::getDescription).setHeader("Message");
    grid.addColumn(InboxItem::getEventTimestamp).setHeader("Date");

    if (playerWrestler != null) {
      grid.setItems(inboxService.getInboxItemsForWrestler(playerWrestler, 10));
    } else {
      grid.setItems(Collections.emptyList());
    }
    grid.setSizeFull();
    return grid;
  }

  private Grid<AchievementType> createAchievementsGrid() {
    Grid<AchievementType> grid = new Grid<>();
    grid.setId("achievements-grid");

    grid.addComponentColumn(
            type -> {
              Icon icon = VaadinIcon.MEDAL.create();
              boolean earned =
                  playerWrestler.getAccount().getAchievements().stream()
                      .anyMatch(a -> a.getType() == type);
              if (earned) {
                icon.setColor("var(--lumo-success-color)");
              } else {
                icon.setColor("var(--lumo-disabled-text-color)");
                icon.getStyle().set("opacity", "0.5");
              }
              return icon;
            })
        .setHeader("")
        .setFlexGrow(0)
        .setWidth("50px");

    grid.addColumn(AchievementType::getDisplayName).setHeader("Achievement").setSortable(true);
    grid.addColumn(AchievementType::getDescription).setHeader("Requirement");
    grid.addColumn(type -> type.getXpValue() + " XP").setHeader("Reward").setSortable(true);

    grid.setItems(java.util.Arrays.asList(AchievementType.values()));
    grid.setSizeFull();
    return grid;
  }

  private Div createInjuriesSummary() {
    Div layout = new Div();
    if (playerWrestler != null && !playerWrestler.getActiveInjuries().isEmpty()) {
      Span injuriesBadge = createBadge("Injured", "error", "pill");

      VerticalLayout injuriesList = new VerticalLayout();
      injuriesList.setSpacing(false);
      injuriesList.setPadding(false);

      for (Injury injury : playerWrestler.getActiveInjuries()) {
        Span injurySpan = new Span(injury.getDisplayString());
        injurySpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
        injuriesList.add(injurySpan);
      }
      layout.add(new HorizontalLayout(injuriesBadge, injuriesList));
    } else {
      Span healthyBadge = createBadge("Healthy", "success", "pill");
      layout.add(healthyBadge);
    }
    layout.setId("injuries-summary");
    return layout;
  }

  private Span createBadge(String text, String... themeNames) {
    Span badge = new Span(text);
    badge.getElement().getThemeList().add("badge");
    for (String theme : themeNames) {
      badge.getElement().getThemeList().add(theme);
    }
    return badge;
  }
}
