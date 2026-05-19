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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.controller.show.ShowController;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.match.QrCodeDialog;
import com.github.javydreamercsw.management.ui.view.segment.NarrationDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/** Detail view for displaying comprehensive information about a specific show. */
@Route("show-detail")
@PageTitle("Show Details")
@PermitAll
@Slf4j
public class ShowDetailView extends Main
    implements HasUrlParameter<Long>, ApplicationListener<ApplicationEvent> {

  private final ShowService showService;
  private final SegmentService segmentService;
  private final SegmentRepository segmentRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final NpcService npcService;
  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final RivalryService rivalryService;
  private final SegmentNarrationServiceFactory segmentNarrationServiceFactory;
  private final SegmentNarrationController segmentNarrationController;
  private final ShowController showController;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final UniverseRepository universeRepository;
  private final UniverseContextService universeContextService;
  private final CommentaryTeamRepository commentaryTeamRepository;
  private final RingsideActionService ringsideActionService;
  private final ArenaService arenaService;
  private final WrestlerRelationshipService relationshipService;

  private Button backButton;
  private Registration backButtonListener;
  private H2 showTitle;
  private VerticalLayout contentLayout;
  private Long currentShowId;
  private Show currentShow;
  private Grid<Segment> segmentsGrid;
  private Button adjudicateButton;
  private Span noSegmentsMessage;

  private final NotificationService notificationService;
  private final ShowExportService exportService;

  @Autowired
  public ShowDetailView(
      final ShowService showService,
      final SegmentService segmentService,
      final SegmentRepository segmentRepository,
      final SegmentTypeRepository segmentTypeRepository,
      final SegmentRuleRepository segmentRuleRepository,
      final NpcService npcService,
      final WrestlerService wrestlerService,
      final TitleService titleService,
      final ShowTypeService showTypeService,
      final SeasonService seasonService,
      final ShowTemplateService showTemplateService,
      final RivalryService rivalryService,
      final SegmentNarrationServiceFactory segmentNarrationServiceFactory,
      final SegmentNarrationController segmentNarrationController,
      final ShowController showController,
      final MatchFulfillmentRepository matchFulfillmentRepository,
      final UniverseRepository universeRepository,
      final UniverseContextService universeContextService,
      final CommentaryTeamRepository commentaryTeamRepository,
      final RingsideActionService ringsideActionService,
      final ArenaService arenaService,
      final WrestlerRelationshipService relationshipService,
      final NotificationService notificationService,
      final ShowExportService exportService) {
    this.showService = showService;
    this.segmentService = segmentService;
    this.segmentRepository = segmentRepository;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.npcService = npcService;
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.rivalryService = rivalryService;
    this.segmentNarrationServiceFactory = segmentNarrationServiceFactory;
    this.segmentNarrationController = segmentNarrationController;
    this.showController = showController;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.universeRepository = universeRepository;
    this.universeContextService = universeContextService;
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.ringsideActionService = ringsideActionService;
    this.arenaService = arenaService;
    this.relationshipService = relationshipService;
    this.notificationService = notificationService;
    this.exportService = exportService;
    initializeComponents();
  }

  private void initializeComponents() {
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    backButton = new Button("Back", new Icon(VaadinIcon.ARROW_LEFT));
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    showTitle = new H2("Show Details");
    showTitle.addClassNames(LumoUtility.Margin.NONE);

    HorizontalLayout headerLayout = new HorizontalLayout(backButton, showTitle);
    headerLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    headerLayout.setSpacing(true);

    contentLayout = new VerticalLayout();
    contentLayout.setSizeFull();
    contentLayout.setSpacing(true);
    contentLayout.setPadding(false);

    add(new ViewToolbar("Show Details", ViewToolbar.group(headerLayout)));
    add(contentLayout);
  }

  @Override
  public void setParameter(final BeforeEvent event, final Long showId) {
    String referrer =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .getOrDefault("ref", List.of("shows"))
            .get(0);

    updateBackButton(referrer);
    this.currentShowId = showId;
    if (showId != null) {
      loadShow(showId);
    } else {
      showNotFound();
    }
  }

  private void updateBackButton(@NonNull final String referrer) {
    String navigationTarget =
        switch (referrer) {
          case "calendar" -> "show-calendar";
          case "booker" -> "booker";
          default -> "show-list";
        };

    backButton.setText("Back to " + referrer);
    if (backButtonListener != null) {
      backButtonListener.remove();
    }
    backButtonListener =
        backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(navigationTarget)));
  }

  private void loadShow(@NonNull final Long showId) {
    Optional<Show> showOpt = showService.getShowById(showId);
    if (showOpt.isPresent()) {
      currentShow = showOpt.get();
      displayShow(currentShow);
    } else {
      showNotFound();
    }
    refreshSegmentsGrid();
  }

  private void displayShow(@NonNull final Show show) {
    contentLayout.removeAll();
    showTitle.setText(show.getName());

    // Show header with basic info (Always visible)
    Div headerCard = createHeaderCard(show);
    contentLayout.add(headerCard);

    // Collapsible Show Details and Description
    VerticalLayout infoCollapseLayout = new VerticalLayout();
    infoCollapseLayout.setPadding(false);
    infoCollapseLayout.setSpacing(true);

    // Show details card
    Div detailsCard = createDetailsCard(show);
    infoCollapseLayout.add(detailsCard);

    // Show description card
    if (show.getDescription() != null && !show.getDescription().trim().isEmpty()) {
      Div descriptionCard = createDescriptionCard(show);
      infoCollapseLayout.add(descriptionCard);
    }

    Details infoDetails = new Details("Show Information & Description", infoCollapseLayout);
    infoDetails.setId("show-info-details");
    infoDetails.setOpened(false); // Collapsed by default to maximize segment space
    infoDetails.setWidthFull();
    infoDetails.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Padding.SMALL,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM);
    contentLayout.add(infoDetails);

    // Show segments card
    Div segmentsCard = createSegmentsCard(show);
    contentLayout.add(segmentsCard);
    contentLayout.setFlexGrow(1, segmentsCard);
  }

  private Div createHeaderCard(@NonNull final Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);

    H3 title = new H3(show.getName());
    title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.PRIMARY);

    Button editNameButton = new Button(new Icon(VaadinIcon.EDIT));
    editNameButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editNameButton.setTooltipText("Edit Show Name");
    editNameButton.addClickListener(e -> new EditShowNameDialog(showService, show).open());

    Span typeBadge = new Span(show.getType().getName());
    typeBadge.addClassNames(
        LumoUtility.Padding.Horizontal.SMALL,
        LumoUtility.Padding.Vertical.XSMALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.SEMIBOLD);

    Image showImage = new Image();
    if (show.getTemplate() != null) {
      showImage.setSrc(showTemplateService.resolveShowTemplateImage(show.getTemplate()));
    } else {
      showImage.setSrc("images/generic-show.png");
    }
    showImage.setHeight("100px");
    showImage.setWidth("100px");
    showImage.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Right.MEDIUM);

    VerticalLayout titleInfo = new VerticalLayout(title, typeBadge);
    titleInfo.setSpacing(false);
    titleInfo.setPadding(false);

    HorizontalLayout titleLayout = new HorizontalLayout(showImage, titleInfo, editNameButton);
    titleLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    titleLayout.setSpacing(true);

    card.add(titleLayout);
    return card;
  }

  private Div createDetailsCard(@NonNull final Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);

    H3 detailsTitle = new H3("Show Information");
    detailsTitle.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

    VerticalLayout detailsLayout = new VerticalLayout();
    detailsLayout.setSpacing(false);
    detailsLayout.setPadding(false);

    detailsLayout.add(
        createDetailRow(
            "Show Date:",
            show.getShowDate() != null
                ? show.getShowDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                : "Not scheduled"));
    detailsLayout.add(
        createDetailRow(
            "Type:", show.getType() != null ? show.getType().getName() : "No type assigned"));
    detailsLayout.add(
        createDetailRow(
            "Season:",
            show.getSeason() != null ? show.getSeason().getName() : "No season assigned"));
    detailsLayout.add(
        createDetailRow(
            "Universe:",
            show.getUniverse() != null ? show.getUniverse().getName() : "No universe assigned"));
    // Arena
    HorizontalLayout arenaLayout =
        createDetailRow(
            "Arena:",
            show.getArena() != null
                ? show.getArena().getName()
                    + " ("
                    + show.getArena().getLocation().getName()
                    + ", Capacity: "
                    + show.getArena().getCapacity()
                    + ", Bias: "
                    + show.getArena().getAlignmentBias().getDisplayName()
                    + ")"
                : "No arena assigned");
    detailsLayout.add(arenaLayout);

    // Event Results (only shown once show is finalized)
    if (show.getAttendance() != null && show.getAttendance() > 0) {
      String attendanceText = "%,d".formatted(show.getAttendance());
      if (show.getArena() != null && show.getArena().getCapacity() != null) {
        int fillPct = (int) ((double) show.getAttendance() / show.getArena().getCapacity() * 100);
        attendanceText += " / %,d (%d%%)".formatted(show.getArena().getCapacity(), fillPct);
      }
      detailsLayout.add(createDetailRow("Attendance:", attendanceText));
      if (show.getGateRevenue() != null) {
        detailsLayout.add(
            createDetailRow("Gate Revenue:", "$" + "%,.2f".formatted(show.getGateRevenue())));
      }
    }

    // Show ID
    assert show.getId() != null;
    HorizontalLayout idLayout = createDetailRow("Show ID:", show.getId().toString());
    detailsLayout.add(idLayout);

    // Show status/flags
    if (show.isPremiumLiveEvent()) {
      HorizontalLayout pleLayout = createDetailRow("Event Type:", "Premium Live Event");
      detailsLayout.add(pleLayout);
    } else if (show.isWeeklyShow()) {
      HorizontalLayout weeklyLayout = createDetailRow("Event Type:", "Weekly Show");
      detailsLayout.add(weeklyLayout);
    }

    // Creation date
    if (show.getCreationDate() != null) {
      HorizontalLayout createdLayout =
          createDetailRow(
              "Created:",
              show.getCreationDate()
                  .atZone(java.time.ZoneId.systemDefault())
                  .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")));
      detailsLayout.add(createdLayout);
    }

    HorizontalLayout detailsHeader = new HorizontalLayout(detailsTitle);
    detailsHeader.setAlignItems(FlexComponent.Alignment.CENTER);
    detailsHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    detailsHeader.setWidthFull();

    Button planShowButton = new Button("Plan Show", new Icon(VaadinIcon.CALENDAR_CLOCK));
    planShowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    planShowButton.setTooltipText("Plan this show");
    planShowButton.setId("plan-show-button");
    planShowButton.addClickListener(
        e -> getUI().ifPresent(ui -> ui.navigate(ShowPlanningView.class, show.getId())));

    Button editDetailsButton =
        new Button(
            new Icon(VaadinIcon.EDIT),
            e ->
                new EditShowDetailsDialog(
                        showService,
                        showTypeService,
                        seasonService,
                        showTemplateService,
                        universeRepository,
                        commentaryTeamRepository,
                        arenaService,
                        show)
                    .open());
    editDetailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    Button exportCardButton = new Button("Export Card", new Icon(VaadinIcon.DOWNLOAD));
    exportCardButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    exportCardButton.setTooltipText("Export this show card");
    exportCardButton.setId("export-show-card-button");
    exportCardButton.addClickListener(
        e -> new ShowExportDialog(exportService, notificationService, show).open());

    HorizontalLayout header =
        new HorizontalLayout(detailsTitle, planShowButton, exportCardButton, editDetailsButton);
    header.setWidthFull();
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

    card.add(header, detailsLayout);
    return card;
  }

  private HorizontalLayout createDetailRow(
      @NonNull final String label, @NonNull final String value) {
    Span labelSpan = new Span(label);
    labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SECONDARY);
    Span valueSpan = new Span(value);
    HorizontalLayout layout = new HorizontalLayout(labelSpan, valueSpan);
    layout.setSpacing(true);
    return layout;
  }

  private Div createDescriptionCard(@NonNull final Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);
    H3 title = new H3("Description");
    card.add(title, new Paragraph(show.getDescription()));
    return card;
  }

  private void showNotFound() {
    contentLayout.removeAll();
    contentLayout.add(new H3("Show Not Found"));
  }

  private Div createSegmentsCard(@NonNull final Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);
    card.setSizeFull();

    // Header with title and add button
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
    header.setAlignItems(HorizontalLayout.Alignment.CENTER);

    H3 segmentsTitle = new H3("Segments");
    segmentsTitle.addClassNames(LumoUtility.Margin.NONE);

    adjudicateButton = new Button("Adjudicate Fans", new Icon(VaadinIcon.GROUP));
    adjudicateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    adjudicateButton.setId("adjudicate-show-btn");
    adjudicateButton.addClickListener(e -> adjudicateShow(show));

    // Check if there are any pending segments
    boolean hasPendingSegments =
        segmentRepository.findByShow(show).stream()
            .anyMatch(
                segment ->
                    segment.getAdjudicationStatus()
                        == com.github.javydreamercsw.management.domain.AdjudicationStatus.PENDING);
    adjudicateButton.setEnabled(hasPendingSegments);

    Button addSegmentBtn =
        new Button("Add Segment", new Icon(VaadinIcon.PLUS), e -> openAddSegmentDialog(show));
    addSegmentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addSegmentBtn.setId("add-segment-btn");

    header.add(segmentsTitle, new HorizontalLayout(adjudicateButton, addSegmentBtn));

    // Get segments for this show
    List<Segment> segments = segmentRepository.findByShow(show);
    log.debug("Found {} segments for show: {}", segments.size(), show.getName());

    VerticalLayout segmentsLayout = new VerticalLayout();
    segmentsLayout.setSpacing(false);
    segmentsLayout.setPadding(false);
    segmentsLayout.setSizeFull();
    segmentsLayout.addClassNames(LumoUtility.Width.FULL);

    // Always initialize segmentsGrid and its wrapper
    segmentsGrid = createSegmentsGrid(segments);
    segmentsGrid.setSizeFull();
    segmentsGrid.setId("segments-grid");

    // Wrap the grid in a Div to enable horizontal scrolling
    Div gridWrapper = new Div(segmentsGrid);
    gridWrapper.addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Width.FULL);
    gridWrapper.setSizeFull();
    gridWrapper.setId("segments-grid-wrapper");
    segmentsLayout.add(gridWrapper);
    segmentsLayout.setFlexGrow(1, gridWrapper);

    noSegmentsMessage = new Span("No segments scheduled for this show yet.");
    noSegmentsMessage.addClassNames(LumoUtility.TextColor.SECONDARY);
    noSegmentsMessage.setId("no-segments-message");
    segmentsLayout.add(noSegmentsMessage);

    // Conditionally show/hide the grid and the "no segments" message
    if (segments.isEmpty()) {
      segmentsGrid.setVisible(false);
      noSegmentsMessage.setVisible(true);
    } else {
      segmentsGrid.setVisible(true);
      noSegmentsMessage.setVisible(false);
    }

    card.add(header, segmentsLayout);
    return card;
  }

  private Grid<Segment> createSegmentsGrid(@NonNull final List<Segment> segments) {
    Grid<Segment> grid = new Grid<>(Segment.class, false);
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setItems(segments);

    // Segment type column
    grid.addColumn(
            segment ->
                segment.getSegmentType() != null ? segment.getSegmentType().getName() : "N/A")
        .setHeader("Segment Type")
        .setSortable(true)
        .setFlexGrow(1);

    // Segment rules column
    grid.addColumn(
            segment -> {
              List<String> ruleNames =
                  segment.getSegmentRules().stream().map(SegmentRule::getName).toList();
              return String.join(", ", ruleNames);
            })
        .setHeader("Segment Rule(s)")
        .setSortable(true)
        .setFlexGrow(1);

    // Titles column
    grid.addColumn(
            segment -> {
              if (segment.getIsTitleSegment() && !segment.getTitles().isEmpty()) {
                return segment.getTitles().stream()
                    .map(Title::getName)
                    .collect(java.util.stream.Collectors.joining(", "));
              } else {
                return "N/A";
              }
            })
        .setHeader("Titles")
        .setSortable(false)
        .setFlexGrow(1);

    // Summary column
    grid.addColumn(Segment::getSummary).setHeader("Summary").setSortable(true).setFlexGrow(3);

    // Narration column
    grid.addColumn(Segment::getNarration).setHeader("Narration").setSortable(true).setFlexGrow(6);

    // Participants column
    grid.addColumn(
            segment -> {
              List<String> wrestlerNames =
                  segment.getWrestlers().stream().map(Wrestler::getName).toList();
              return String.join(", ", wrestlerNames);
            })
        .setHeader("Participants")
        .setSortable(false)
        .setFlexGrow(4);

    // Winner column
    grid.addColumn(
            segment -> {
              List<String> winnerNames =
                  segment.getWinners().stream().map(Wrestler::getName).toList();
              return String.join(", ", winnerNames);
            })
        .setHeader("Winner(s)")
        .setSortable(false)
        .setFlexGrow(2);

    // League Status column
    grid.addColumn(
            segment ->
                matchFulfillmentRepository
                    .findBySegment(segment)
                    .map(f -> f.getStatus().toString())
                    .orElse("N/A"))
        .setHeader("League Status")
        .setFlexGrow(1);

    // Segment date column
    grid.addColumn(
            segment ->
                segment
                    .getSegmentDate()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
        .setHeader("Date")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setFlexGrow(1);

    grid.addComponentColumn(this::createOrderButtons)
        .setHeader("Order")
        .setFlexGrow(1)
        .setKey("order");

    grid.addComponentColumn(this::createMainEventCheckbox).setHeader("Main Event").setFlexGrow(1);
    return grid;
  }

  private Component createOrderButtons(@NonNull Segment segment) {
    List<Segment> segments = segmentRepository.findByShow(segment.getShow());
    int currentIndex = segments.indexOf(segment);

    Button upButton = new Button(new Icon(VaadinIcon.ARROW_UP));
    upButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    upButton.setTooltipText("Move Up");
    upButton.setId("move-segment-up-button-" + segment.getId());
    upButton.addClickListener(e -> moveSegment(segment, -1));
    upButton.setEnabled(currentIndex > 0);

    Button downButton = new Button(new Icon(VaadinIcon.ARROW_DOWN));
    downButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    downButton.setTooltipText("Move Down");
    downButton.setId("move-segment-down-button-" + segment.getId());
    downButton.addClickListener(e -> moveSegment(segment, 1));
    downButton.setEnabled(currentIndex < segments.size() - 1);

    return new HorizontalLayout(upButton, downButton);
  }

  protected void moveSegment(@NonNull Segment segment, int direction) {
    Show show = segment.getShow();
    List<Segment> segments = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    int currentIndex = segments.indexOf(segment);
    int newIndex = currentIndex + direction;

    if (newIndex >= 0 && newIndex < segments.size()) {
      Segment otherSegment = segments.get(newIndex);
      int currentOrder = segment.getSegmentOrder();
      segment.setSegmentOrder(otherSegment.getSegmentOrder());
      otherSegment.setSegmentOrder(currentOrder);
      segmentRepository.save(segment);
      segmentRepository.save(otherSegment);
      refreshSegmentsGrid();
    }
  }

  private Component createMainEventCheckbox(@NonNull Segment segment) {
    Checkbox checkbox = new Checkbox();
    checkbox.setValue(segment.isMainEvent());
    checkbox.setId("main-event-checkbox");
    checkbox.addValueChangeListener(
        e -> {
          segment.setMainEvent(e.getValue());
          segmentRepository.save(segment);
          refreshSegmentsGrid();
        });
    return checkbox;
  }

  private Component createActionButtons(@NonNull Segment segment) {
    Button summaryButton = new Button("Summarize", new Icon(VaadinIcon.ACADEMY_CAP));
    summaryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    summaryButton.setTooltipText("Generate AI Summary");
    summaryButton.setId("generate-summary-button-" + segment.getId());
    summaryButton.addClickListener(e -> generateSummary(segment));
    summaryButton.setEnabled(segment.getNarration() != null && !segment.getNarration().isEmpty());

    Button narrateButton = new Button("Narrate", new Icon(VaadinIcon.MICROPHONE));
    narrateButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    narrateButton.setTooltipText("Generate AI Narration");
    narrateButton.setId("generate-narration-button-" + segment.getId());
    narrateButton.addClickListener(
        e -> {
          NarrationDialog dialog =
              new NarrationDialog(
                  segment,
                  npcService,
                  wrestlerService,
                  showService,
                  segmentService,
                  updatedSegment -> refreshSegmentsGrid(),
                  rivalryService,
                  segmentNarrationController,
                  segmentNarrationServiceFactory,
                  ringsideActionService,
                  relationshipService,
                  universeContextService,
                  notificationService);
          dialog.open();
        });

    Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.setTooltipText("Edit Segment");
    editButton.setId("edit-segment-button-" + segment.getId());
    editButton.addClickListener(e -> openEditSegmentDialog(segment));

    Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete Segment");
    deleteButton.setId("delete-segment-button-" + segment.getId());
    deleteButton.addClickListener(e -> deleteSegment(segment));

    SegmentType segmentType = segment.getSegmentType();
    boolean isMatch = segmentType != null && !"Promo".equalsIgnoreCase(segmentType.getName());
    Button qrButton = new Button(new Icon(VaadinIcon.QRCODE));
    qrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    qrButton.setTooltipText("Share Match QR Code");
    qrButton.setId("share-qr-button-" + segment.getId());
    qrButton.setVisible(isMatch);
    qrButton.addClickListener(e -> new QrCodeDialog(segment.getId()).open());

    return new VerticalLayout(summaryButton, narrateButton, editButton, deleteButton, qrButton);
  }

  private void generateSummary(@NonNull Segment segment) {
    if (segmentNarrationServiceFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      String reason = "No AI providers are currently enabled or reachable.";
      notificationService.showError(reason);
      return;
    }

    try {
      String summary = segmentNarrationServiceFactory.summarizeNarration(segment.getNarration());
      segment.setSummary(summary);
      segmentService.updateSegment(segment);
      notificationService.showSuccess("Summary generated successfully!");
      refreshSegmentsGrid();
    } catch (Exception e) {
      log.error("Error generating summary", e);
      notificationService.showAIServiceError(e);
    }
  }

  private void openAddSegmentDialog(@NonNull final Show show) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Segment to " + show.getName());
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    dialog.setId("add-segment-dialog");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    // Segment type selection
    ComboBox<SegmentType> segmentTypeCombo = new ComboBox<>("Segment Type");
    segmentTypeCombo.setItems(
        segmentTypeRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentType::getName))
            .collect(Collectors.toList()));
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    segmentTypeCombo.setId("segment-type-combo-box");

    // Segment rules selection (multi-select)
    MultiSelectComboBox<SegmentRule> rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(
        segmentRuleRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentRule::getName))
            .collect(Collectors.toList()));
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    rulesCombo.setId("segment-rules-combo-box");
    formLayout.setColspan(rulesCombo, 2);

    // Referee selection
    ComboBox<Npc> refereeCombo = new ComboBox<>("Referee");
    refereeCombo.setItems(
        npcService.findAllByType("Referee").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList()));
    refereeCombo.setItemLabelGenerator(Npc::getName);
    refereeCombo.setWidthFull();
    refereeCombo.setId("add-referee-combo-box");

    // Alignment and Gender Filters
    ComboBox<AlignmentType> alignmentFilter = new ComboBox<>("Alignment Filter");
    alignmentFilter.setItems(AlignmentType.values());
    alignmentFilter.setClearButtonVisible(true);
    alignmentFilter.setPlaceholder("All alignments");
    alignmentFilter.setWidthFull();
    alignmentFilter.setId("add-alignment-filter-combo-box");

    ComboBox<Gender> genderFilter = new ComboBox<>("Gender Filter");
    genderFilter.setItems(Gender.values());
    genderFilter.setClearButtonVisible(true);
    genderFilter.setPlaceholder("All genders");
    genderFilter.setWidthFull();
    Gender defaultGender =
        show.getTemplate() != null ? show.getTemplate().getGenderConstraint() : null;
    genderFilter.setValue(defaultGender);
    genderFilter.setId("add-gender-filter-combo-box");

    // Winner combo (defined first so team lambdas can capture it)
    MultiSelectComboBox<Wrestler> winnerCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnerCombo.setItemLabelGenerator(Wrestler::getName);
    winnerCombo.setWidthFull();
    winnerCombo.setClearButtonVisible(true);
    winnerCombo.setId("winner-combo-box");

    // Teams layout — one row per team
    List<MultiSelectComboBox<Wrestler>> addTeamCombos = new ArrayList<>();
    VerticalLayout addTeamsLayout = new VerticalLayout();
    addTeamsLayout.setSpacing(true);
    addTeamsLayout.setPadding(false);

    Runnable refreshAddWinners =
        () -> {
          Set<Wrestler> allSelected =
              addTeamCombos.stream()
                  .flatMap(c -> c.getValue().stream())
                  .collect(Collectors.toSet());
          Set<Wrestler> currentWinners = new HashSet<>(winnerCombo.getValue());
          winnerCombo.setItems(
              allSelected.stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnerCombo.setValue(
              currentWinners.stream().filter(allSelected::contains).collect(Collectors.toSet()));
        };

    java.util.function.Consumer<Set<Wrestler>> addAddTeamRow =
        initialWrestlers -> {
          int teamNumber = addTeamCombos.size() + 1;
          MultiSelectComboBox<Wrestler> teamCombo = new MultiSelectComboBox<>("Team " + teamNumber);
          teamCombo.setItemLabelGenerator(Wrestler::getName);
          teamCombo.setWidthFull();
          teamCombo.setId("add-team-combo-" + teamNumber);
          teamCombo.setItems(
              wrestlerService.findAllFiltered(
                  alignmentFilter.getValue(),
                  genderFilter.getValue(),
                  universeContextService.getCurrentUniverseId(),
                  initialWrestlers));
          if (!initialWrestlers.isEmpty()) {
            teamCombo.setValue(initialWrestlers);
          }
          teamCombo.addValueChangeListener(e -> refreshAddWinners.run());
          addTeamCombos.add(teamCombo);

          Button removeTeamButton = new Button(new Icon(VaadinIcon.MINUS));
          removeTeamButton.addThemeVariants(
              ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
          removeTeamButton.setTooltipText("Remove Team");
          HorizontalLayout teamRow = new HorizontalLayout(teamCombo, removeTeamButton);
          teamRow.setFlexGrow(1, teamCombo);
          teamRow.setAlignItems(HorizontalLayout.Alignment.END);
          teamRow.setWidthFull();
          removeTeamButton.addClickListener(
              e -> {
                addTeamsLayout.remove(teamRow);
                addTeamCombos.remove(teamCombo);
                for (int i = 0; i < addTeamCombos.size(); i++) {
                  addTeamCombos.get(i).setLabel("Team " + (i + 1));
                }
                refreshAddWinners.run();
              });
          addTeamsLayout.add(teamRow);
          refreshAddWinners.run();
        };

    // Start with two empty teams
    addAddTeamRow.accept(new HashSet<>());
    addAddTeamRow.accept(new HashSet<>());

    alignmentFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : addTeamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(
                wrestlerService.findAllFiltered(
                    e.getValue(),
                    genderFilter.getValue(),
                    universeContextService.getCurrentUniverseId(),
                    current));
            combo.setValue(current);
          }
        });
    genderFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : addTeamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(
                wrestlerService.findAllFiltered(
                    alignmentFilter.getValue(),
                    e.getValue(),
                    universeContextService.getCurrentUniverseId(),
                    current));
            combo.setValue(current);
          }
        });

    Button addTeamButton = new Button("Add Team", new Icon(VaadinIcon.PLUS));
    addTeamButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    addTeamButton.setId("add-add-team-button");
    addTeamButton.addClickListener(e -> addAddTeamRow.accept(new HashSet<>()));

    VerticalLayout addTeamsSection = new VerticalLayout(addTeamsLayout, addTeamButton);
    addTeamsSection.setSpacing(false);
    addTeamsSection.setPadding(false);
    formLayout.setColspan(addTeamsSection, 2);

    // Add title selection for new segments
    MultiSelectComboBox<Title> titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(
        titleService.findAll().stream()
            .sorted(Comparator.comparing(Title::getName))
            .collect(Collectors.toList()));
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setVisible(false);
    titleMultiSelectComboBox.setId("title-multi-select-combo-box");

    // Add checkbox to indicate if it's a title segment
    Checkbox isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setId("is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear();
          }
        });

    // Narration
    TextArea summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setId("summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    // Narration
    TextArea narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setId("narration-text-area");
    formLayout.setColspan(narrationArea, 2);

    // Notes
    TextArea notesArea = new TextArea("Notes/Feedback");
    notesArea.setWidthFull();
    notesArea.setId("notes-text-area");
    formLayout.setColspan(notesArea, 2);

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        refereeCombo,
        alignmentFilter,
        genderFilter,
        addTeamsSection,
        winnerCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        narrationArea,
        notesArea);

    // Buttons
    Button saveButton =
        new Button(
            "Add Segment",
            e -> {
              java.util.Map<Integer, List<Wrestler>> teamMap = new java.util.LinkedHashMap<>();
              for (int i = 0; i < addTeamCombos.size(); i++) {
                teamMap.put(i + 1, new ArrayList<>(addTeamCombos.get(i).getValue()));
              }
              Set<Wrestler> winners = winnerCombo.getValue();
              // Create a new segment object to pass to validation
              Segment newSegment = new Segment();
              newSegment.setNarration(narrationArea.getValue());
              newSegment.setSummary(summaryArea.getValue());
              newSegment.setNotes(notesArea.getValue());
              newSegment.setSegmentOrder(segmentRepository.findByShow(show).size() + 1);
              newSegment.setShow(show);
              newSegment.setSegmentDate(java.time.Instant.now());
              // Set isTitleSegment based on checkbox
              boolean isTitleSegment = isTitleSegmentCheckbox.getValue();
              newSegment.setIsTitleSegment(isTitleSegment);
              newSegment.setIsNpcGenerated(false);
              newSegment.syncParticipants(teamMap);
              newSegment.syncSegmentRules(new ArrayList<>(rulesCombo.getValue()));
              newSegment.setSegmentType(segmentTypeCombo.getValue());
              newSegment.setWinners(new ArrayList<>(winners));
              newSegment.setReferee(refereeCombo.getValue());

              // If it's a title segment, set the selected titles
              if (isTitleSegment) {
                newSegment.setTitles(titleMultiSelectComboBox.getValue());
              }

              if (validateAndSaveSegment(
                  show,
                  segmentTypeCombo.getValue(),
                  teamMap,
                  winners,
                  rulesCombo.getValue(),
                  newSegment)) {
                dialog.close();
                refreshSegmentsGrid();
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.setId("add-segment-save-button");

    Button cancelButton = new Button("Cancel", e -> dialog.close());
    cancelButton.setId("add-segment-cancel-button");

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonLayout.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(false);
    dialogLayout.setId("add-segment-dialog-layout");

    dialog.add(dialogLayout);
    dialog.open();
  }

  private void openEditSegmentDialog(@NonNull Segment segment) {
    final Segment seg = segmentRepository.findById(segment.getId()).orElse(segment);
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Segment for " + currentShow.getName());
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");
    dialog.setId("edit-segment-dialog");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    // Segment type selection
    ComboBox<SegmentType> segmentTypeCombo = new ComboBox<>("Segment Type");
    segmentTypeCombo.setItems(
        segmentTypeRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentType::getName))
            .collect(Collectors.toList()));
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    segmentTypeCombo.setValue(seg.getSegmentType());
    segmentTypeCombo.setId("edit-segment-type-combo-box");

    // Segment rules selection (multi-select)
    MultiSelectComboBox<SegmentRule> rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(
        segmentRuleRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentRule::getName))
            .collect(Collectors.toList()));
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    rulesCombo.setValue(seg.getSegmentRules());
    rulesCombo.setId("edit-segment-rules-combo-box");
    formLayout.setColspan(rulesCombo, 2);

    // Referee selection
    ComboBox<Npc> refereeCombo = new ComboBox<>("Referee");
    refereeCombo.setItems(
        npcService.findAllByType("Referee").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList()));
    refereeCombo.setItemLabelGenerator(Npc::getName);
    refereeCombo.setWidthFull();
    refereeCombo.setValue(seg.getReferee());
    refereeCombo.setId("edit-referee-combo-box");

    // Alignment and Gender Filters
    ComboBox<AlignmentType> alignmentFilter = new ComboBox<>("Alignment Filter");
    alignmentFilter.setItems(AlignmentType.values());
    alignmentFilter.setClearButtonVisible(true);
    alignmentFilter.setPlaceholder("All alignments");
    alignmentFilter.setWidthFull();
    alignmentFilter.setId("edit-alignment-filter-combo-box");

    ComboBox<Gender> genderFilter = new ComboBox<>("Gender Filter");
    genderFilter.setItems(Gender.values());
    genderFilter.setClearButtonVisible(true);
    genderFilter.setPlaceholder("All genders");
    genderFilter.setWidthFull();
    Gender defaultGender =
        currentShow.getTemplate() != null ? currentShow.getTemplate().getGenderConstraint() : null;
    genderFilter.setValue(defaultGender);
    genderFilter.setId("edit-gender-filter-combo-box");

    // Winners combo (defined first so team lambdas can capture it)
    MultiSelectComboBox<Wrestler> winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setId("edit-winners-combo-box");

    // Teams layout — one row per team, matching the NarrationDialog pattern
    List<MultiSelectComboBox<Wrestler>> teamCombos = new ArrayList<>();
    VerticalLayout teamsLayout = new VerticalLayout();
    teamsLayout.setSpacing(true);
    teamsLayout.setPadding(false);

    Runnable refreshWinners =
        () -> {
          Set<Wrestler> allSelected =
              teamCombos.stream().flatMap(c -> c.getValue().stream()).collect(Collectors.toSet());
          Set<Wrestler> currentWinners = new HashSet<>(winnersCombo.getValue());
          winnersCombo.setItems(
              allSelected.stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnersCombo.setValue(
              currentWinners.stream().filter(allSelected::contains).collect(Collectors.toSet()));
        };

    java.util.function.Consumer<Set<Wrestler>> addTeamRow =
        initialWrestlers -> {
          MultiSelectComboBox<Wrestler> teamCombo =
              new MultiSelectComboBox<>("Team " + (teamCombos.size() + 1));
          teamCombo.setItemLabelGenerator(Wrestler::getName);
          teamCombo.setWidthFull();
          teamCombo.setItems(
              wrestlerService.findAllFiltered(
                  alignmentFilter.getValue(),
                  genderFilter.getValue(),
                  universeContextService.getCurrentUniverseId(),
                  initialWrestlers));
          if (!initialWrestlers.isEmpty()) {
            teamCombo.setValue(initialWrestlers);
          }
          teamCombo.addValueChangeListener(e -> refreshWinners.run());
          teamCombos.add(teamCombo);

          Button removeTeamButton = new Button(new Icon(VaadinIcon.MINUS));
          removeTeamButton.addThemeVariants(
              ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
          removeTeamButton.setTooltipText("Remove Team");
          HorizontalLayout teamRow = new HorizontalLayout(teamCombo, removeTeamButton);
          teamRow.setFlexGrow(1, teamCombo);
          teamRow.setAlignItems(HorizontalLayout.Alignment.END);
          teamRow.setWidthFull();
          removeTeamButton.addClickListener(
              e -> {
                teamsLayout.remove(teamRow);
                teamCombos.remove(teamCombo);
                for (int i = 0; i < teamCombos.size(); i++) {
                  teamCombos.get(i).setLabel("Team " + (i + 1));
                }
                refreshWinners.run();
              });
          teamsLayout.add(teamRow);
          refreshWinners.run();
        };

    // One team per existing wrestler; fall back to two empty teams for a new segment
    for (Wrestler w : seg.getWrestlers()) {
      addTeamRow.accept(new HashSet<>(Set.of(w)));
    }
    if (teamCombos.isEmpty()) {
      addTeamRow.accept(new HashSet<>());
      addTeamRow.accept(new HashSet<>());
    }

    // Keep team combo items in sync with alignment/gender filters
    alignmentFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(
                wrestlerService.findAllFiltered(
                    e.getValue(),
                    genderFilter.getValue(),
                    universeContextService.getCurrentUniverseId(),
                    current));
            combo.setValue(current);
          }
        });
    genderFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(
                wrestlerService.findAllFiltered(
                    alignmentFilter.getValue(),
                    e.getValue(),
                    universeContextService.getCurrentUniverseId(),
                    current));
            combo.setValue(current);
          }
        });

    Button addTeamButton = new Button("Add Team", new Icon(VaadinIcon.PLUS));
    addTeamButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    addTeamButton.setId("edit-add-team-button");
    addTeamButton.addClickListener(e -> addTeamRow.accept(new HashSet<>()));

    VerticalLayout teamsSection = new VerticalLayout(teamsLayout, addTeamButton);
    teamsSection.setSpacing(false);
    teamsSection.setPadding(false);
    formLayout.setColspan(teamsSection, 2);

    winnersCombo.setValue(new HashSet<>(seg.getWinners()));

    // Narration
    TextArea summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(seg.getSummary() != null ? seg.getSummary() : "");
    summaryArea.setId("edit-summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    // Narration
    TextArea narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(seg.getNarration() != null ? seg.getNarration() : "");
    narrationArea.setId("edit-narration-text-area");
    formLayout.setColspan(narrationArea, 2);

    // Notes
    TextArea notesArea = new TextArea("Notes/Feedback");
    notesArea.setWidthFull();
    notesArea.setValue(seg.getNotes() != null ? seg.getNotes() : "");
    notesArea.setId("edit-notes-text-area");
    formLayout.setColspan(notesArea, 2);

    // Title selection (multi-select) - only visible if segment is a title segment
    MultiSelectComboBox<Title> titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(
        titleService.findAll().stream()
            .sorted(Comparator.comparing(Title::getName))
            .collect(Collectors.toList()));
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setVisible(seg.getIsTitleSegment());
    titleMultiSelectComboBox.setValue(seg.getTitles());
    titleMultiSelectComboBox.setId("edit-title-multi-select-combo-box");

    // Add checkbox to indicate if it's a title segment
    Checkbox isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setValue(seg.getIsTitleSegment());
    isTitleSegmentCheckbox.setId("edit-is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear();
          }
        });

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        refereeCombo,
        alignmentFilter,
        genderFilter,
        teamsSection,
        winnersCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        narrationArea,
        notesArea);

    // Buttons
    Button saveButton =
        new Button(
            "Save Changes",
            e -> {
              seg.setNarration(narrationArea.getValue());
              seg.setSummary(summaryArea.getValue());
              seg.setNotes(notesArea.getValue());
              seg.setReferee(refereeCombo.getValue());
              // Set isTitleSegment based on checkbox
              boolean isTitleSegment = isTitleSegmentCheckbox.getValue();
              seg.setIsTitleSegment(isTitleSegment);
              // If it's a title segment, set the selected titles
              if (isTitleSegment) {
                seg.setTitles(titleMultiSelectComboBox.getValue());
              }
              java.util.Map<Integer, List<Wrestler>> teamMap = new java.util.LinkedHashMap<>();
              for (int i = 0; i < teamCombos.size(); i++) {
                teamMap.put(i + 1, new ArrayList<>(teamCombos.get(i).getValue()));
              }
              if (validateAndSaveSegment(
                  currentShow,
                  segmentTypeCombo.getValue(),
                  teamMap,
                  winnersCombo.getValue(),
                  rulesCombo.getValue(),
                  seg)) {
                dialog.close();
                refreshSegmentsGrid();
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.setId("edit-segment-save-button");

    Button cancelButton = new Button("Cancel", e -> dialog.close());
    cancelButton.setId("edit-segment-cancel-button");

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonLayout.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(false);
    dialogLayout.setId("edit-segment-dialog-layout");

    dialog.add(dialogLayout);
    dialog.open();
  }

  private void deleteSegment(@NonNull Segment segment) {
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Delete Segment");
    confirmDialog.add(new Paragraph("Are you sure you want to delete this segment?"));

    Button deleteButton =
        new Button(
            "Delete",
            event -> {
              try {
                assert segment.getId() != null;
                segmentService.deleteSegment(segment.getId());
                notificationService.showSuccess("Segment deleted successfully!");
                confirmDialog.close();
                refreshSegmentsGrid();
              } catch (Exception e) {
                notificationService.showError("Error deleting segment: " + e.getMessage());
              }
            });
    deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button cancelButton = new Button("Cancel", event -> confirmDialog.close());

    confirmDialog.getFooter().add(cancelButton, deleteButton);
    confirmDialog.open();
  }

  private boolean validateAndSaveSegment(
      @NonNull final Show show,
      final SegmentType segmentType,
      final java.util.Map<Integer, List<Wrestler>> teamWrestlers,
      final Set<Wrestler> winners,
      final Set<SegmentRule> rules,
      final Segment segmentToUpdate) {
    Set<Wrestler> wrestlers =
        teamWrestlers.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    log.info("Validating and saving segment: {}", segmentToUpdate);
    // Validation
    if (segmentType == null) {
      log.warn("Validation failed: Segment type is null.");
      notificationService.showError("Please select a segment type");
      return false;
    }

    if (!"Promo".equalsIgnoreCase(segmentType.getName())) {
      if (wrestlers.isEmpty()) {
        log.warn("Validation failed: Wrestlers are null or empty for non-promo segment.");
        notificationService.showError("Please select at least one wrestler");
        return false;
      }

      if (wrestlers.size() < 2) {
        log.warn("Validation failed: Less than two wrestlers for a non-promo match.");
        notificationService.showError("Please select at least two wrestlers for a match");
        return false;
      }
    }

    if (winners != null) {
      for (Wrestler winner : winners) {
        if (!wrestlers.contains(winner)) {
          log.warn("Validation failed: Winner is not among selected wrestlers.");
          notificationService.showError("Winner must be one of the selected wrestlers");
          return false;
        }
      }
    }

    try {
      Segment segment;
      if (segmentToUpdate != null && segmentToUpdate.getId() != null) {
        segment = segmentToUpdate;
        segment.syncParticipants(teamWrestlers);
        segment.syncSegmentRules(new ArrayList<>(rules));
        segment.setAdjudicationStatus(AdjudicationStatus.PENDING);
        log.info("Updating existing segment: {}", segment.getId());
      } else {
        // Use the passed object if present (for new segments with data), or create new
        segment = segmentToUpdate != null ? segmentToUpdate : new Segment();

        if (segment.getShow() == null) {
          segment.setShow(show);
        }
        if (segment.getSegmentDate() == null) {
          segment.setSegmentDate(java.time.Instant.now());
        }
        if (segment.getIsTitleSegment() == null) {
          segment.setIsTitleSegment(false);
        }
        if (segment.getIsNpcGenerated() == null) {
          segment.setIsNpcGenerated(false);
        }

        segment.syncParticipants(teamWrestlers);
        segment.syncSegmentRules(new ArrayList<>(rules));
        log.info("Creating new segment for show: {}", show.getName());
      }

      segment.setSegmentType(segmentType);

      if (winners != null) {
        segment.setWinners(new ArrayList<>(winners));
      }

      // Save or update the segment
      if (segment.getId() != null) {
        segmentService.updateSegment(segment);
        notificationService.showSuccess("Segment updated successfully!");
      } else {
        segmentService.saveSegment(segment);
        notificationService.showSuccess("Segment added successfully!");
      }
      log.info("Segment saved successfully: {}", segment.getId());
      return true;
    } catch (Exception e) {
      log.error("Error saving segment: {}", e.getMessage(), e);
      notificationService.showError("Error saving segment: " + e.getMessage());
      return false;
    }
  }

  private void adjudicateShow(@NonNull final Show show) {
    showController.adjudicateShow(show.getId());
    notificationService.showSuccess("Fan adjudication completed!");
    refreshSegmentsGrid(); // Call refreshSegmentsGrid instead of loadShow
  }

  private void refreshSegmentsGrid() {
    if (currentShow != null && segmentsGrid != null) {
      List<Segment> updatedSegments =
          segmentRepository.findByShowOrderBySegmentOrderAsc(currentShow);
      segmentsGrid.setItems(updatedSegments);

      // Update visibility of grid and noSegmentsMessage
      boolean hasSegments = !updatedSegments.isEmpty();
      segmentsGrid.setVisible(hasSegments);
      if (noSegmentsMessage != null) {
        noSegmentsMessage.setVisible(!hasSegments);
      }

      // Re-enable/disable adjudicate button based on new segment status
      boolean hasPendingSegments =
          updatedSegments.stream()
              .anyMatch(segment -> segment.getAdjudicationStatus() == AdjudicationStatus.PENDING);

      if (adjudicateButton != null) {
        adjudicateButton.setEnabled(hasPendingSegments);
      }
    }
  }

  @Override
  public void onApplicationEvent(@NonNull final ApplicationEvent event) {
    if (event instanceof AdjudicationCompletedEvent e
        && e.getShow().getId().equals(currentShowId)) {
      getUI().ifPresent(ui -> ui.access(this::refreshSegmentsGrid));
    }
  }
}
