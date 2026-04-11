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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.HistoryTimelineComponent;
import com.github.javydreamercsw.management.ui.component.ReignCardComponent;
import com.github.javydreamercsw.management.ui.component.WrestlerActionMenu;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Route("wrestler-profile/:wrestlerId?")
@PageTitle("Wrestler Profile")
@PermitAll
public class WrestlerProfileView extends Main implements BeforeEnterObserver {

  private record FeudHistoryItem(String name, int heat) {}

  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final TitleService titleService;
  private final RankingService rankingService;
  private final SegmentService segmentService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final RivalryService rivalryService;
  private final InjuryService injuryService;
  private final AccountService accountService;
  private final NpcService npcService;
  private final CampaignService campaignService;
  private final ImageStorageService imageStorageService;

  private Wrestler wrestler;
  private Season selectedSeason; // To store the selected season for filtering

  private final H2 wrestlerName = new H2();
  private final Paragraph wrestlerDetails = new Paragraph();
  private final VerticalLayout statsLayout = new VerticalLayout();
  private final VerticalLayout biographyLayout = new VerticalLayout();
  private final VerticalLayout careerHighlightsLayout = new VerticalLayout();
  private final VerticalLayout titleHistoryLayout = new VerticalLayout();
  private final VerticalLayout recentMatchesLayout = new VerticalLayout();
  private final VerticalLayout injuriesLayout = new VerticalLayout();
  private final VerticalLayout feudHistoryLayout = new VerticalLayout();
  private final Grid<Segment> recentMatchesGrid = new Grid<>(Segment.class);
  private final HorizontalLayout header;
  private final Image wrestlerImage = new Image();

  @Autowired private SecurityUtils securityUtils;

  @Autowired
  public WrestlerProfileView(
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      TitleService titleService,
      RankingService rankingService,
      SegmentService segmentService,
      MultiWrestlerFeudService multiWrestlerFeudService,
      RivalryService rivalryService,
      SeasonService seasonService,
      InjuryService injuryService,
      NpcService npcService,
      @Qualifier("baseAccountService") AccountService accountService,
      CampaignService campaignService,
      ImageStorageService imageStorageService) {
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.titleService = titleService;
    this.rankingService = rankingService;
    this.segmentService = segmentService;
    this.multiWrestlerFeudService = multiWrestlerFeudService;
    this.rivalryService = rivalryService;
    this.injuryService = injuryService;
    this.npcService = npcService;
    this.accountService = accountService;
    this.campaignService = campaignService;
    this.imageStorageService = imageStorageService;

    wrestlerName.setId("wrestler-name");
    wrestlerImage.setSrc("https://via.placeholder.com/150");
    wrestlerImage.setAlt("Wrestler Image");
    wrestlerImage.setId("wrestler-image");

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(
        new ViewToolbar(
            "Wrestler Profile", new RouterLink("Back to List", WrestlerListView.class)));

    VerticalLayout nameDetailsAndStatsLayout = new VerticalLayout();
    nameDetailsAndStatsLayout.add(wrestlerName, wrestlerDetails, statsLayout);
    header = new HorizontalLayout(wrestlerImage, nameDetailsAndStatsLayout);
    header.setAlignItems(Alignment.CENTER);

    List<Season> seasons =
        seasonService.getAllSeasons(Pageable.unpaged()).getContent().stream()
            .sorted(Comparator.comparing(Season::getName))
            .collect(Collectors.toList());
    ComboBox<Season> seasonFilter = new ComboBox<>("Filter by Season");
    seasonFilter.setItems(seasons);
    seasonFilter.setItemLabelGenerator(Season::getName);
    seasonFilter.setPlaceholder("Select a season");
    seasonFilter.addValueChangeListener(
        event -> {
          selectedSeason = event.getValue();
          updateMatchAndFeudHistory();
        });
    if (!seasons.isEmpty()) {
      seasonFilter.setValue(seasons.getLast()); // Default to latest season
    }

    recentMatchesGrid.removeAllColumns();
    recentMatchesGrid.addColumn(segment -> segment.getShow().getName()).setHeader("Show");
    recentMatchesGrid.addColumn(segment -> segment.getSegmentType().getName()).setHeader("Type");
    recentMatchesGrid
        .addColumn(
            segment ->
                segment.getSegmentRules().stream()
                    .map(SegmentRule::getName)
                    .collect(Collectors.joining(", ")))
        .setHeader("Rules");
    recentMatchesGrid
        .addColumn(
            segment ->
                segment.getWrestlers().stream()
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(", ")))
        .setHeader("Participants");
    recentMatchesGrid.addColumn(Segment::getSummary).setHeader("Summary");
    recentMatchesGrid
        .addColumn(
            segment ->
                segment.getWinners().stream()
                    .map(Wrestler::getName)
                    .collect(Collectors.joining(", ")))
        .setHeader("Winners");
    recentMatchesGrid
        .addColumn(
            segment ->
                segment.getTitles().stream().map(Title::getName).collect(Collectors.joining(", ")))
        .setHeader("Championships");

    add(
        header,
        biographyLayout,
        careerHighlightsLayout,
        titleHistoryLayout,
        injuriesLayout,
        seasonFilter,
        recentMatchesLayout,
        feudHistoryLayout);
  }

  @Override
  @Transactional
  public void beforeEnter(BeforeEnterEvent event) {
    RouteParameters parameters = event.getRouteParameters();
    if (parameters.get("wrestlerId").isPresent()) {
      Long wrestlerId = Long.valueOf(parameters.get("wrestlerId").get());
      Optional<Wrestler> foundWrestler = wrestlerRepository.findByIdWithInjuries(wrestlerId);
      if (foundWrestler.isPresent()) {
        wrestler = foundWrestler.get();
        updateView();
      } else {
        // Handle wrestler not found, e.g., navigate to an error page or list view
        event.rerouteTo(WrestlerListView.class);
      }
    } else {
      event.rerouteTo(WrestlerListView.class);
    }
  }

  private void updateView() {
    if (wrestler != null && wrestler.getId() != null) {
      header.getChildren().filter(c -> c instanceof WrestlerActionMenu).forEach(header::remove);
      header.add(
          new WrestlerActionMenu(
              wrestler,
              wrestlerService,
              injuryService,
              npcService,
              campaignService,
              this::updateView,
              true,
              securityUtils,
              accountService,
              imageStorageService));
      wrestlerName.setText(wrestler.getName());
      wrestlerDetails.setText(
          String.format("Gender: %s, Fans: %d", wrestler.getGender(), wrestler.getFans()));

      if (wrestler.getImageUrl() != null && !wrestler.getImageUrl().isEmpty()) {
        wrestlerImage.setSrc(wrestler.getImageUrl());
      } else {
        wrestlerImage.setSrc("https://via.placeholder.com/150");
      }

      // Fetch and display wrestler stats
      Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(wrestler.getId());
      statsLayout.removeAll();
      statsLayout.add(new H3("Stats"));
      if (stats.isPresent()) {
        WrestlerStats wrestlerStats = stats.get();
        statsLayout.add(new Paragraph("Wins: " + wrestlerStats.getWins()));
        statsLayout.add(new Paragraph("Losses: " + wrestlerStats.getLosses()));
        statsLayout.add(new Paragraph("Titles Held: " + wrestlerStats.getTitlesHeld()));
        double totalMatches = wrestlerStats.getWins() + wrestlerStats.getLosses();
        if (totalMatches > 0) {
          double winPercentage = (wrestlerStats.getWins() / totalMatches) * 100;
          statsLayout.add(new Paragraph(String.format("Win Percentage: %.2f%%", winPercentage)));
        }
      } else {
        statsLayout.add(new Paragraph("Stats not available."));
      }

      // Populate biography
      biographyLayout.removeAll();
      biographyLayout.add(new H3("Biography"));
      if (wrestler.getManager() != null) {
        Paragraph managerParagraph =
            new Paragraph("Managed by: " + wrestler.getManager().getName());
        managerParagraph.setId("manager-name");
        biographyLayout.add(managerParagraph);
      }
      if (wrestler.getDescription() != null && !wrestler.getDescription().isEmpty()) {
        biographyLayout.add(new Paragraph(wrestler.getDescription()));
      } else {
        biographyLayout.add(new Paragraph("No biography available."));
      }

      // Career Highlights
      careerHighlightsLayout.removeAll();
      careerHighlightsLayout.add(new H3("Career Highlights"));
      List<Title> titlesWon = titleService.findTitlesByChampion(wrestler);
      if (titlesWon.isEmpty()) {
        careerHighlightsLayout.add(new Paragraph("No titles won yet."));
      } else {
        titlesWon.forEach(title -> careerHighlightsLayout.add(new Paragraph(title.getName())));
      }

      // Title History
      titleHistoryLayout.removeAll();
      titleHistoryLayout.add(new H3("Title History"));
      List<TitleReignDTO> history = rankingService.getWrestlerTitleHistory(wrestler.getId());
      if (history.isEmpty()) {
        titleHistoryLayout.add(new Paragraph("No title history available."));
      } else {
        titleHistoryLayout.add(new HistoryTimelineComponent(history));

        Div cardsContainer = new Div();
        cardsContainer.addClassNames(
            LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.SMALL);
        history.forEach(reign -> cardsContainer.add(new ReignCardComponent(reign)));
        titleHistoryLayout.add(cardsContainer);
      }

      // Bumps and Injuries
      injuriesLayout.removeAll();
      injuriesLayout.add(new H3("Bumps & Injuries"));
      injuriesLayout.add(new Paragraph("Bumps: " + wrestler.getBumps()));
      if (wrestler.getInjuries().isEmpty()) {
        injuriesLayout.add(new Paragraph("No current injuries."));
      } else {
        wrestler
            .getInjuries()
            .forEach(injury -> injuriesLayout.add(new Paragraph("- " + injury.getDisplayString())));
      }

      updateMatchAndFeudHistory(); // Initial call to populate match and feud history
    }
  }

  private void updateMatchAndFeudHistory() {
    if (wrestler == null || wrestler.getId() == null) {
      return;
    }

    // Clear existing content
    recentMatchesLayout.removeAll();
    feudHistoryLayout.removeAll();

    // Recent Matches
    recentMatchesLayout.add(new H3("Recent Matches"));
    recentMatchesLayout.add(recentMatchesGrid);

    recentMatchesGrid.setDataProvider(
        DataProvider.fromCallbacks(
            query ->
                segmentService
                    .getSegmentsByWrestlerParticipationAndSeason(
                        wrestler,
                        selectedSeason,
                        PageRequest.of(
                            query.getPage(),
                            query.getPageSize(),
                            Sort.by("segmentDate").descending()))
                    .stream(),
            query ->
                (int) segmentService.countSegmentsByWrestlerAndSeason(wrestler, selectedSeason)));

    // Feud History
    feudHistoryLayout.add(new H3("Feud History"));
    List<MultiWrestlerFeud> multiWrestlerFeuds =
        multiWrestlerFeudService.getActiveFeudsForWrestler(wrestler.getId());
    List<Rivalry> rivalries = rivalryService.getRivalriesForWrestler(wrestler.getId());

    List<FeudHistoryItem> feudHistoryItems = new ArrayList<>();
    multiWrestlerFeuds.forEach(
        feud ->
            feudHistoryItems.add(new FeudHistoryItem("Feud: " + feud.getName(), feud.getHeat())));
    rivalries.forEach(
        rivalry ->
            feudHistoryItems.add(
                new FeudHistoryItem(
                    "Rivalry with "
                        + (rivalry.getWrestler1().equals(wrestler)
                            ? rivalry.getWrestler2().getName()
                            : rivalry.getWrestler1().getName()),
                    rivalry.getHeat())));

    if (feudHistoryItems.isEmpty()) {
      feudHistoryLayout.add(new Paragraph("No active feuds or rivalries found."));
    } else {
      feudHistoryItems.sort(Comparator.comparingInt(FeudHistoryItem::heat).reversed());
      feudHistoryItems.forEach(
          item ->
              feudHistoryLayout.add(
                  new Paragraph(String.format("%s (Heat: %d)", item.name(), item.heat()))));
    }
  }
}
