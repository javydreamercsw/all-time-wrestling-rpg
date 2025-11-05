package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@Route("wrestler-profile/:wrestlerId?")
@PageTitle("Wrestler Profile")
@PermitAll
public class WrestlerProfileView extends Main implements BeforeEnterObserver {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private TitleService titleService;
  @Autowired private SegmentService segmentService;
  @Autowired private MultiWrestlerFeudService multiWrestlerFeudService;
  @Autowired private RivalryService rivalryService;
  @Autowired private ShowService showService;
  @Autowired private SeasonService seasonService;

  private Wrestler wrestler;
  private Season selectedSeason; // To store the selected season for filtering

  private final H2 wrestlerName = new H2();
  private final Paragraph wrestlerDetails = new Paragraph();
  private final VerticalLayout statsLayout = new VerticalLayout();
  private final VerticalLayout biographyLayout = new VerticalLayout();
  private final VerticalLayout careerHighlightsLayout = new VerticalLayout();
  private final VerticalLayout upcomingMatchesLayout = new VerticalLayout();
  private final VerticalLayout feudHistoryLayout = new VerticalLayout();

  public WrestlerProfileView() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(
        new ViewToolbar(
            "Wrestler Profile", new RouterLink("Back to List", WrestlerListView.class)));

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

    add(
        wrestlerName,
        wrestlerDetails,
        statsLayout,
        biographyLayout,
        careerHighlightsLayout,
        seasonFilter,
        upcomingMatchesLayout,
        feudHistoryLayout);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    RouteParameters parameters = event.getRouteParameters();
    if (parameters.get("wrestlerId").isPresent()) {
      Long wrestlerId = Long.valueOf(parameters.get("wrestlerId").get());
      Optional<Wrestler> foundWrestler = wrestlerService.findById(wrestlerId);
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
    if (wrestler != null) {
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

      updateMatchAndFeudHistory(); // Initial call to populate match and feud history
    }
  }

  private void updateMatchAndFeudHistory() {
    if (wrestler == null) {
      return;
    }

    // Clear existing content
    upcomingMatchesLayout.removeAll();
    feudHistoryLayout.removeAll();

    // Recent Matches
    upcomingMatchesLayout.add(new H3("Recent Matches"));
    List<Segment> recentMatches;
    if (selectedSeason != null) {
      recentMatches = segmentService.getSegmentsByWrestlerAndSeason(wrestler, selectedSeason);
    } else {
      recentMatches = segmentService.getSegmentsByWrestlerParticipation(wrestler);
    }

    if (recentMatches.isEmpty()) {
      upcomingMatchesLayout.add(new Paragraph("No recent matches found."));
    } else {
      recentMatches.stream()
          .limit(10)
          .forEach(
              segment -> {
                upcomingMatchesLayout.add(
                    new Paragraph(
                        String.format(
                            "%s - %s (%s)",
                            segment.getSegmentDate(),
                            segment.getSegmentType().getName(),
                            segment.getShow().getName())));
              });
    }

    // Feud History
    feudHistoryLayout.add(new H3("Feud History"));
    List<MultiWrestlerFeud> multiWrestlerFeuds =
        multiWrestlerFeudService.getActiveFeudsForWrestler(wrestler.getId());
    List<Rivalry> rivalries = rivalryService.getRivalriesForWrestler(wrestler.getId());

    if (multiWrestlerFeuds.isEmpty() && rivalries.isEmpty()) {
      feudHistoryLayout.add(new Paragraph("No active feuds or rivalries found."));
    } else {
      multiWrestlerFeuds.forEach(
          feud -> {
            feudHistoryLayout.add(
                new Paragraph(
                    String.format("Feud: %s (Heat: %d)", feud.getName(), feud.getHeat())));
          });
      rivalries.forEach(
          rivalry -> {
            feudHistoryLayout.add(
                new Paragraph(
                    String.format(
                        "Rivalry with %s (Heat: %d)",
                        (rivalry.getWrestler1().equals(wrestler)
                            ? rivalry.getWrestler2().getName()
                            : rivalry.getWrestler1().getName()),
                        rivalry.getHeat())));
          });
    }
  }
}
