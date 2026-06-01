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
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.component.AlignmentTrackComponent;
import com.github.javydreamercsw.management.ui.component.HistoryTimelineComponent;
import com.github.javydreamercsw.management.ui.component.ReignCardComponent;
import com.github.javydreamercsw.management.ui.component.WrestlerActionMenu;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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
  private final WrestlerStatsService wrestlerStatsService;
  private final WrestlerRepository wrestlerRepository;
  private final TitleService titleService;
  private final RankingService rankingService;
  private final SegmentService segmentService;
  private final MultiWrestlerFeudService multiWrestlerFeudService;
  private final RivalryService rivalryService;
  private final InjuryService injuryService;
  private final InjuryTypeService injuryTypeService;
  private final AccountService accountService;
  private final NpcService npcService;
  private final CampaignService campaignService;
  private final SeasonService seasonService;
  private final ImageStorageService imageStorageService;
  private final UniverseContextService universeContextService;
  private final com.github.javydreamercsw.management.service.relationship
          .WrestlerRelationshipService
      relationshipService;
  private final com.github.javydreamercsw.management.service.campaign.WrestlerStatusService
      wrestlerStatusService;
  private final com.github.javydreamercsw.management.service.campaign.StatusCardService
      statusCardService;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final AlignmentService alignmentService;

  private Wrestler wrestler;
  private Season selectedSeason; // To store the selected season for filtering

  private final H2 wrestlerName = new H2();
  private final Paragraph wrestlerDetails = new Paragraph();
  private final Image wrestlerImage = new Image();
  private final FlexLayout heroSection = new FlexLayout();
  private final VerticalLayout heroDetailsColumn = new VerticalLayout();
  private final VerticalLayout biographyContainer = new VerticalLayout();
  private final VerticalLayout relationshipsContainer = new VerticalLayout();
  private final VerticalLayout highlightsContainer = new VerticalLayout();

  private final Accordion secondaryInfoAccordion = new Accordion();
  private final VerticalLayout statsLayout = new VerticalLayout();
  private final VerticalLayout statusesLayout = new VerticalLayout();
  private final VerticalLayout titleHistoryLayout = new VerticalLayout();
  private final VerticalLayout recentMatchesLayout = new VerticalLayout();
  private final VerticalLayout injuriesLayout = new VerticalLayout();
  private final VerticalLayout feudHistoryLayout = new VerticalLayout();
  private final Grid<Segment> recentMatchesGrid = new Grid<>(Segment.class);

  @Autowired private SecurityUtils securityUtils;

  @Autowired
  public WrestlerProfileView(
      final WrestlerService wrestlerService,
      final WrestlerStatsService wrestlerStatsService,
      final WrestlerRepository wrestlerRepository,
      final TitleService titleService,
      final RankingService rankingService,
      final SegmentService segmentService,
      final MultiWrestlerFeudService multiWrestlerFeudService,
      final RivalryService rivalryService,
      final SeasonService seasonService,
      final InjuryService injuryService,
      final InjuryTypeService injuryTypeService,
      final NpcService npcService,
      @Qualifier("baseAccountService") final AccountService accountService,
      final CampaignService campaignService,
      final ImageStorageService imageStorageService,
      final UniverseContextService universeContextService,
      final com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
          relationshipService,
      final com.github.javydreamercsw.management.service.campaign.WrestlerStatusService
          wrestlerStatusService,
      final com.github.javydreamercsw.management.service.campaign.StatusCardService
          statusCardService,
      final WrestlerStateRepository wrestlerStateRepository,
      final AlignmentService alignmentService) {
    this.wrestlerService = wrestlerService;
    this.wrestlerStatsService = wrestlerStatsService;
    this.wrestlerRepository = wrestlerRepository;
    this.titleService = titleService;
    this.rankingService = rankingService;
    this.segmentService = segmentService;
    this.multiWrestlerFeudService = multiWrestlerFeudService;
    this.rivalryService = rivalryService;
    this.injuryService = injuryService;
    this.injuryTypeService = injuryTypeService;
    this.npcService = npcService;
    this.accountService = accountService;
    this.campaignService = campaignService;
    this.seasonService = seasonService;
    this.imageStorageService = imageStorageService;
    this.universeContextService = universeContextService;
    this.relationshipService = relationshipService;
    this.wrestlerStatusService = wrestlerStatusService;
    this.statusCardService = statusCardService;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.alignmentService = alignmentService;
    wrestlerName.setId("wrestler-name");
    wrestlerImage.setAlt("Wrestler Image");
    wrestlerImage.setId("wrestler-image");
    // Allow the image to be much larger to match the height of the text content
    wrestlerImage.setMaxWidth("600px");
    wrestlerImage.setWidthFull();
    wrestlerImage.getStyle().set("height", "auto");
    wrestlerImage.getStyle().set("max-height", "600px");
    wrestlerImage.getStyle().set("object-fit", "contain");
    wrestlerImage.addClassNames(
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.MEDIUM,
        LumoUtility.Margin.SMALL,
        LumoUtility.AlignSelf.START); // Align to top of the hero section

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(
        new ViewToolbar(
            "Wrestler Profile", new RouterLink("Back to List", WrestlerListView.class)));

    // Hero Section Setup
    heroSection.setWidthFull();
    heroSection.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.FlexDirection.Breakpoint.Medium.ROW,
        LumoUtility.Gap.MEDIUM,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Background.BASE);

    heroDetailsColumn.setPadding(false);
    heroDetailsColumn.setSpacing(false);
    heroDetailsColumn.addClassNames(LumoUtility.Flex.GROW);
    heroDetailsColumn.add(
        wrestlerName,
        wrestlerDetails,
        biographyContainer,
        relationshipsContainer,
        highlightsContainer);

    heroSection.add(wrestlerImage, heroDetailsColumn);

    // Accordion Setup
    secondaryInfoAccordion.setWidthFull();
    secondaryInfoAccordion.add("Stats", statsLayout);
    secondaryInfoAccordion.add("Status Cards", statusesLayout);
    secondaryInfoAccordion.add("Championships", titleHistoryLayout);
    secondaryInfoAccordion.add("Medical Record", injuriesLayout);

    // Match Logs Section
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

    recentMatchesLayout.setPadding(false);
    recentMatchesLayout.add(seasonFilter, recentMatchesGrid);
    secondaryInfoAccordion.add("Match Logs", recentMatchesLayout);
    secondaryInfoAccordion.add("Rivalry History", feudHistoryLayout);

    // Configure Grid
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

    add(heroSection, secondaryInfoAccordion);
  }

  @Override
  @Transactional
  public void beforeEnter(final BeforeEnterEvent event) {
    RouteParameters parameters = event.getRouteParameters();
    if (parameters.get("wrestlerId").isPresent()) {
      Long wrestlerId = Long.valueOf(parameters.get("wrestlerId").get());
      Optional<Wrestler> foundWrestler = wrestlerService.findByIdWithDetails(wrestlerId);
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
      // Re-fetch to ensure we have the latest state (e.g., after status changes)
      wrestlerService.findByIdWithDetails(wrestler.getId()).ifPresent(w -> wrestler = w);
      Long universeId = universeContextService.getCurrentUniverseId();
      WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);

      heroDetailsColumn
          .getChildren()
          .filter(c -> c instanceof WrestlerActionMenu)
          .forEach(heroDetailsColumn::remove);
      heroDetailsColumn.add(
          new WrestlerActionMenu(
              wrestler,
              wrestlerService,
              injuryService,
              injuryTypeService,
              npcService,
              campaignService,
              wrestlerStateRepository,
              this::updateView,
              true,
              securityUtils,
              accountService,
              imageStorageService,
              universeContextService,
              alignmentService));
      wrestlerName.setText(wrestler.getName());
      String details = "Gender: %s, Fans: %d".formatted(wrestler.getGender(), state.getFans());
      if (wrestler.getHeritageTag() != null && !wrestler.getHeritageTag().isEmpty()) {
        details += ", Heritage: %s".formatted(wrestler.getHeritageTag());
      }
      wrestlerDetails.setText(details);

      wrestlerImage.setSrc(wrestlerService.resolveWrestlerImage(wrestler).url());

      // Fetch and display wrestler stats
      Optional<WrestlerStats> stats =
          wrestlerStatsService.getWrestlerStats(wrestler.getId(), universeId);
      statsLayout.removeAll();
      statsLayout.add(new H3("Career Stats (Universe Context)"));

      Paragraph conditionPara =
          new Paragraph("Physical Condition: " + state.getPhysicalCondition() + "%");
      conditionPara.addClassNames(LumoUtility.FontWeight.BOLD);
      if (state.getPhysicalCondition() < 50) {
        conditionPara.addClassNames(LumoUtility.TextColor.ERROR);
      } else if (state.getPhysicalCondition() < 80) {
        conditionPara.addClassNames(LumoUtility.TextColor.WARNING);
      } else {
        conditionPara.addClassNames(LumoUtility.TextColor.SUCCESS);
      }
      statsLayout.add(conditionPara);

      if (stats.isPresent()) {
        WrestlerStats wrestlerStats = stats.get();
        statsLayout.add(new Paragraph("Wins: " + wrestlerStats.getWins()));
        statsLayout.add(new Paragraph("Losses: " + wrestlerStats.getLosses()));
        statsLayout.add(new Paragraph("Titles Held: " + wrestlerStats.getTitlesHeld()));
        double totalMatches = wrestlerStats.getWins() + wrestlerStats.getLosses();
        if (totalMatches > 0) {
          double winPercentage = (wrestlerStats.getWins() / totalMatches) * 100;
          statsLayout.add(new Paragraph("Win Percentage: %.2f%%".formatted(winPercentage)));
        }
      } else {
        statsLayout.add(new Paragraph("Stats not available."));
      }

      universeContextService
          .getCurrentUniverse()
          .ifPresent(
              universe -> {
                com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment alignment =
                    alignmentService.getOrCreateUniverseAlignment(wrestler, universe);
                statsLayout.add(new AlignmentTrackComponent(alignment, false));
              });

      // Status Cards
      statusesLayout.removeAll();
      HorizontalLayout statusHeader = new HorizontalLayout(new H3("Active Status Cards"));
      statusHeader.setWidthFull();
      statusHeader.setAlignItems(Alignment.CENTER);

      if (securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)) {
        Button manageBtn = new Button("Manage Statuses", event -> showManageStatusesDialog());
        manageBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        statusHeader.add(manageBtn);
      }
      statusesLayout.add(statusHeader);

      if (wrestler.getStatuses().isEmpty()) {
        statusesLayout.add(new Paragraph("No active status cards."));
      } else {
        wrestler
            .getStatuses()
            .forEach(
                status -> {
                  VerticalLayout card = new VerticalLayout();
                  card.setSpacing(false);
                  card.setPadding(true);
                  card.addClassNames(
                      LumoUtility.Background.CONTRAST_5,
                      LumoUtility.BorderRadius.MEDIUM,
                      LumoUtility.Margin.Bottom.SMALL);

                  String nameText =
                      status.getLevel() == 1
                          ? status.getStatusCard().getLevel1Name()
                          : status.getStatusCard().getLevel2Name();
                  H4 cardName = new H4(nameText + " (Level " + status.getLevel() + ")");
                  cardName.addClassNames(
                      LumoUtility.Margin.NONE,
                      status.getStatusCard().isPositive()
                          ? LumoUtility.TextColor.SUCCESS
                          : LumoUtility.TextColor.ERROR);

                  Paragraph desc = new Paragraph(status.getStatusCard().getDescription());
                  desc.addClassNames(
                      LumoUtility.FontSize.SMALL, LumoUtility.Margin.Vertical.XSMALL);

                  String effect =
                      status.getLevel() == 1
                          ? status.getStatusCard().getLevel1Effect()
                          : status.getStatusCard().getLevel2Effect();
                  Span effectSpan = new Span("Effect: " + (effect != null ? effect : "None"));
                  effectSpan.addClassNames(
                      LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.BOLD);

                  card.add(cardName, desc, effectSpan);
                  statusesLayout.add(card);
                });
      }

      // Populate biography
      biographyContainer.removeAll();
      biographyContainer.setPadding(false);
      biographyContainer.add(new H3("Biography"));
      if (state.getManager() != null) {
        Paragraph managerParagraph = new Paragraph("Managed by: " + state.getManager().getName());
        managerParagraph.setId("manager-name");
        biographyContainer.add(managerParagraph);
      }
      if (wrestler.getDescription() != null && !wrestler.getDescription().isEmpty()) {
        biographyContainer.add(new Paragraph(wrestler.getDescription()));
      } else {
        biographyContainer.add(new Paragraph("No biography available."));
      }

      // Relationships
      relationshipsContainer.removeAll();
      relationshipsContainer.setPadding(false);
      relationshipsContainer.add(new H3("Relationships"));
      List<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
          relationships = relationshipService.getRelationshipsForWrestler(wrestler.getId());
      if (relationships.isEmpty()) {
        relationshipsContainer.add(new Paragraph("No active social relationships."));
      } else {
        relationships.forEach(
            rel -> {
              Wrestler partner = rel.getPartner(wrestler);
              String text =
                  "%s: %s (Level: %d%s)"
                      .formatted(
                          rel.getType().getDisplayName(),
                          partner.getName(),
                          rel.getLevel(),
                          rel.getIsStoryline() ? ", Storyline" : "");
              relationshipsContainer.add(new Paragraph(text));
            });
      }

      // Career Highlights
      highlightsContainer.removeAll();
      highlightsContainer.setPadding(false);
      highlightsContainer.add(new H3("Career Highlights"));
      List<Title> titlesWon = titleService.findTitlesByChampion(wrestler, universeId);
      if (titlesWon.isEmpty()) {
        highlightsContainer.add(new Paragraph("No titles won yet in this universe."));
      } else {
        titlesWon.forEach(title -> highlightsContainer.add(new Paragraph(title.getName())));
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

      Paragraph physicalConditionPara =
          new Paragraph("Physical Condition: " + state.getPhysicalCondition() + "%");
      physicalConditionPara.addClassNames(LumoUtility.FontWeight.BOLD);
      if (state.getPhysicalCondition() < 50) {
        physicalConditionPara.addClassNames(LumoUtility.TextColor.ERROR);
      } else if (state.getPhysicalCondition() < 80) {
        physicalConditionPara.addClassNames(LumoUtility.TextColor.WARNING);
      } else {
        physicalConditionPara.addClassNames(LumoUtility.TextColor.SUCCESS);
      }
      injuriesLayout.add(physicalConditionPara);

      if (securityUtils.isAdmin() || securityUtils.isBooker()) {
        Button resetWearAndTearButton = new Button("Reset Wear & Tear");
        resetWearAndTearButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        resetWearAndTearButton.addClickListener(
            e -> {
              wrestlerService.resetWearAndTear(wrestler.getId(), universeId);
              updateView();
              com.vaadin.flow.component.notification.Notification.show(
                  "Wear & Tear reset to 100%!");
            });
        injuriesLayout.add(resetWearAndTearButton);
      }

      injuriesLayout.add(new Paragraph("Bumps: " + state.getBumps()));
      List<com.github.javydreamercsw.management.domain.injury.Injury> injuries =
          injuryService.getAllInjuriesForWrestler(wrestler.getId(), universeId);
      if (injuries.isEmpty()) {
        injuriesLayout.add(new Paragraph("No current injuries."));
      } else {
        injuries.forEach(
            injury -> injuriesLayout.add(new Paragraph("- " + injury.getDisplayString())));
      }

      updateMatchAndFeudHistory(); // Initial call to populate match and feud history
    }
  }

  private void showManageStatusesDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Manage Status Cards for " + wrestler.getName());

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    ComboBox<StatusCard> cardCombo = new ComboBox<>("Add Status Card");
    cardCombo.setItems(statusCardService.findAll());
    cardCombo.setItemLabelGenerator(StatusCard::getLevel1Name);
    cardCombo.setPlaceholder("Select a status card");
    cardCombo.setWidthFull();

    Button addBtn =
        new Button(
            "Add/Flip",
            event -> {
              if (cardCombo.getValue() != null) {
                wrestlerStatusService.assignStatus(wrestler.getId(), cardCombo.getValue().getKey());
                updateView(); // Refresh the profile
                Notification.show("Status assigned/flipped.");
                dialog.close();
              }
            });
    addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout addLayout = new HorizontalLayout(cardCombo, addBtn);
    addLayout.setAlignItems(Alignment.BASELINE);
    addLayout.setWidthFull();

    content.add(addLayout);

    if (!wrestler.getStatuses().isEmpty()) {
      content.add(new H4("Active Statuses (Click to Remove)"));
      wrestler
          .getStatuses()
          .forEach(
              status -> {
                Button removeBtn =
                    new Button(
                        (status.getLevel() == 1
                                ? status.getStatusCard().getLevel1Name()
                                : status.getStatusCard().getLevel2Name())
                            + " (L"
                            + status.getLevel()
                            + ")",
                        event -> {
                          wrestlerStatusService.removeStatus(
                              wrestler.getId(), status.getStatusCard().getKey());
                          updateView();
                          Notification.show("Status removed.");
                          dialog.close();
                        });
                removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                content.add(removeBtn);
              });
    }

    dialog.add(content);
    dialog.getFooter().add(new Button("Close", e -> dialog.close()));
    dialog.open();
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
                  new Paragraph("%s (Heat: %d)".formatted(item.name(), item.heat()))));
    }
  }
}
