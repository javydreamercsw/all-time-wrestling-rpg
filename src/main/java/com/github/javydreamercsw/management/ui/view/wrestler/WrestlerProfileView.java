package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
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
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Route("wrestler-profile/:wrestlerId?")
@PageTitle("Wrestler Profile")
@PermitAll
public class WrestlerProfileView extends Main implements BeforeEnterObserver {

  @Getter
  private static class FeudHistoryItem {
    private final String name;
    private final int heat;

    public FeudHistoryItem(String name, int heat) {
      this.name = name;
      this.heat = heat;
    }
  }

  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final SegmentService segmentService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final RivalryService rivalryService;

  private Wrestler wrestler;
  private Season selectedSeason; // To store the selected season for filtering

  private final H2 wrestlerName = new H2();
  private final Paragraph wrestlerDetails = new Paragraph();
  private final VerticalLayout statsLayout = new VerticalLayout();
  private final VerticalLayout biographyLayout = new VerticalLayout();
  private final VerticalLayout careerHighlightsLayout = new VerticalLayout();
  private final VerticalLayout recentMatchesLayout = new VerticalLayout();
  private final VerticalLayout injuriesLayout = new VerticalLayout();
  private final VerticalLayout feudHistoryLayout = new VerticalLayout();
  private final Grid<Segment> recentMatchesGrid = new Grid<>(Segment.class);

  @Autowired
  public WrestlerProfileView(
      WrestlerService wrestlerService,
      TitleService titleService,
      SegmentService segmentService,
      MultiWrestlerFeudService multiWrestlerFeudService,
      RivalryService rivalryService,
      SeasonService seasonService) {
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.segmentService = segmentService;
    this.multiWrestlerFeudService = multiWrestlerFeudService;
    this.rivalryService = rivalryService;

    wrestlerName.setId("wrestler-name");
    Image wrestlerImage = new Image();
    wrestlerImage.setSrc("https://via.placeholder.com/150");
    wrestlerImage.setAlt("Wrestler Image");

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(
        new ViewToolbar(
            "Wrestler Profile", new RouterLink("Back to List", WrestlerListView.class)));

    HorizontalLayout header =
        new HorizontalLayout(wrestlerImage, new VerticalLayout(wrestlerName, wrestlerDetails));
    header.setAlignItems(Alignment.CENTER);

    List<Season> seasons = seasonService.getAllSeasons(Pageable.unpaged()).getContent();
    ComboBox<Season> seasonFilter = new ComboBox<>("Filter by Season");
    seasonFilter.setItems(seasons);
    seasonFilter.setItemLabelGenerator(Season::getName);
    seasonFilter.setPlaceholder("Select a season");
    seasonFilter.addValueChangeListener(
        event -> {
          selectedSeason = event.getValue();
          updateMatchAndFeudHistory();
        });
    seasonFilter.setValue(
        seasons.isEmpty() ? null : seasons.get(seasons.size() - 1)); // Default to latest season

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
        statsLayout,
        biographyLayout,
        careerHighlightsLayout,
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
      Optional<Wrestler> foundWrestler = wrestlerService.findByIdWithInjuries(wrestlerId);
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
      wrestlerName.setText(wrestler.getName());
      wrestlerDetails.setText(
          String.format("Gender: %s, Fans: %d", wrestler.getGender(), wrestler.getFans()));

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
        titlesWon.forEach(
            title -> {
              careerHighlightsLayout.add(new Paragraph(title.getName()));
            });
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
            .forEach(
                injury -> {
                  injuriesLayout.add(new Paragraph("- " + injury.getDisplayString()));
                });
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
            query -> {
              Page<Segment> page =
                  segmentService.getSegmentsByWrestlerParticipation(
                      wrestler,
                      PageRequest.of(
                          query.getPage(),
                          query.getPageSize(),
                          Sort.by("segmentDate").descending()));
              return page.stream();
            },
            query -> (int) segmentService.countSegmentsByWrestler(wrestler)));

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
      feudHistoryItems.sort(Comparator.comparingInt(FeudHistoryItem::getHeat).reversed());
      feudHistoryItems.forEach(
          item ->
              feudHistoryLayout.add(
                  new Paragraph(String.format("%s (Heat: %d)", item.getName(), item.getHeat()))));
    }
  }
}
