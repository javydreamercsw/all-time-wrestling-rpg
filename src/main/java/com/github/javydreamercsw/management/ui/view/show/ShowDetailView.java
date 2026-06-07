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
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.controller.show.ShowController;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeNames;
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
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
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
import com.vaadin.flow.component.grid.dnd.GridDropMode;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Detail view for displaying comprehensive information about a specific show. */
@Route("show-detail")
@PageTitle("Show Details")
@PermitAll
@Slf4j
public class ShowDetailView extends Main
    implements HasUrlParameter<Long>,
        ApplicationListener<ApplicationEvent>,
        com.vaadin.flow.router.BeforeLeaveObserver {

  private final ShowService showService;
  private final SegmentService segmentService;
  private final SegmentRepository segmentRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final NpcService npcService;
  private final WrestlerService wrestlerService;
  private final WrestlerStatsService wrestlerStatsService;
  private final TitleService titleService;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final RivalryService rivalryService;
  private final ShowPlanningService showPlanningService;
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
  private Button addSegmentButton;
  private Button saveOrderButton;
  private Span noSegmentsMessage;
  private Segment draggedSegment;
  private com.vaadin.flow.component.progressbar.ProgressBar segmentsProgressBar;

  /** In-memory ordered list; mutations here are instant — persisted only on "Save Order". */
  private List<Segment> segmentOrder = new ArrayList<>();

  private boolean orderDirty = false;

  private final NotificationService notificationService;
  private final ShowExportService exportService;
  private final LeagueRepository leagueRepository;

  @Autowired
  public ShowDetailView(
      final ShowService showService,
      final SegmentService segmentService,
      final SegmentRepository segmentRepository,
      final SegmentTypeRepository segmentTypeRepository,
      final SegmentRuleRepository segmentRuleRepository,
      final NpcService npcService,
      final WrestlerService wrestlerService,
      final WrestlerStatsService wrestlerStatsService,
      final TitleService titleService,
      final ShowTypeService showTypeService,
      final SeasonService seasonService,
      final ShowTemplateService showTemplateService,
      final RivalryService rivalryService,
      final ShowPlanningService showPlanningService,
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
      final ShowExportService exportService,
      final LeagueRepository leagueRepository) {
    this.showService = showService;
    this.segmentService = segmentService;
    this.segmentRepository = segmentRepository;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.npcService = npcService;
    this.wrestlerService = wrestlerService;
    this.wrestlerStatsService = wrestlerStatsService;
    this.titleService = titleService;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.rivalryService = rivalryService;
    this.showPlanningService = showPlanningService;
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
    this.leagueRepository = leagueRepository;
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
                        leagueRepository,
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

    addSegmentButton =
        new Button("Add Segment", new Icon(VaadinIcon.PLUS), e -> openAddSegmentDialog(show));
    addSegmentButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addSegmentButton.setId("add-segment-btn");

    // Disable "Add Segment" only when all existing segments have been adjudicated
    List<Segment> existingSegments = segmentRepository.findByShow(show);
    boolean allAdjudicated =
        !existingSegments.isEmpty()
            && existingSegments.stream()
                .allMatch(
                    segment -> segment.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED);
    addSegmentButton.setEnabled(!allAdjudicated);

    saveOrderButton = new Button("Save Order", new Icon(VaadinIcon.CHECK));
    saveOrderButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    saveOrderButton.setId("save-order-btn");
    saveOrderButton.setVisible(false);
    saveOrderButton.addClickListener(e -> persistSegmentOrder());

    header.add(
        segmentsTitle, new HorizontalLayout(saveOrderButton, adjudicateButton, addSegmentButton));

    // Get segments for this show — also seeds the in-memory order
    List<Segment> segments = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    segmentOrder = new ArrayList<>(segments);
    orderDirty = false;
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

    segmentsProgressBar = new com.vaadin.flow.component.progressbar.ProgressBar();
    segmentsProgressBar.setIndeterminate(true);
    segmentsProgressBar.setWidthFull();
    segmentsProgressBar.setVisible(false);
    segmentsProgressBar.setId("segments-progress-bar");
    segmentsLayout.add(segmentsProgressBar);

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

    grid.setRowsDraggable(true);
    grid.setDropMode(GridDropMode.BETWEEN);
    grid.addDragStartListener(
        e -> e.getDraggedItems().stream().findFirst().ifPresent(s -> draggedSegment = s));
    grid.addDragEndListener(e -> draggedSegment = null);
    grid.addDropListener(
        e -> {
          if (draggedSegment == null) {
            return;
          }
          e.getDropTargetItem()
              .ifPresent(
                  target -> {
                    if (!draggedSegment.getId().equals(target.getId())) {
                      moveSegmentToPosition(draggedSegment, target);
                    }
                  });
        });

    // Drag handle column — visual affordance for row drag-and-drop
    grid.addComponentColumn(
            segment -> {
              Icon handle = new Icon(VaadinIcon.GRID_BEVEL);
              handle
                  .getStyle()
                  .set("cursor", "grab")
                  .set("color", "var(--lumo-secondary-text-color)");
              handle.setTooltipText("Drag to reorder");
              return handle;
            })
        .setWidth("3em")
        .setFlexGrow(0)
        .setHeader("");

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

    grid.addComponentColumn(
            segment -> {
              // Main event = last non-Promo segment in current in-memory order
              Long mainEventId = null;
              for (int i = segmentOrder.size() - 1; i >= 0; i--) {
                Segment s = segmentOrder.get(i);
                if (s.getSegmentType() == null
                    || !SegmentTypeNames.PROMO.equalsIgnoreCase(s.getSegmentType().getName())) {
                  mainEventId = s.getId();
                  break;
                }
              }
              if (mainEventId != null && mainEventId.equals(segment.getId())) {
                Span badge = new Span("★ Main Event");
                badge
                    .getStyle()
                    .set("background-color", "#fff3cd")
                    .set("color", "#856404")
                    .set("border-radius", "4px")
                    .set("padding", "2px 6px")
                    .set("font-weight", "bold")
                    .set("font-size", "var(--lumo-font-size-s)");
                return badge;
              }
              return new Span();
            })
        .setHeader("Main Event")
        .setFlexGrow(1);
    return grid;
  }

  private Component createOrderButtons(@NonNull Segment segment) {
    int currentIndex = segmentOrder.indexOf(segment);
    boolean isFirst = currentIndex == 0;
    boolean isLast = currentIndex == segmentOrder.size() - 1;

    Button topButton = new Button(new Icon(VaadinIcon.ANGLE_DOUBLE_UP));
    topButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    topButton.setTooltipText("Move to Top");
    topButton.setId("move-segment-top-button-" + segment.getId());
    topButton.addClickListener(e -> moveSegmentInMemory(segment, -currentIndex));
    topButton.setEnabled(!isFirst);

    Button upButton = new Button(new Icon(VaadinIcon.ARROW_UP));
    upButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    upButton.setTooltipText("Move Up");
    upButton.setId("move-segment-up-button-" + segment.getId());
    upButton.addClickListener(e -> moveSegmentInMemory(segment, -1));
    upButton.setEnabled(!isFirst);

    Button downButton = new Button(new Icon(VaadinIcon.ARROW_DOWN));
    downButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    downButton.setTooltipText("Move Down");
    downButton.setId("move-segment-down-button-" + segment.getId());
    downButton.addClickListener(e -> moveSegmentInMemory(segment, 1));
    downButton.setEnabled(!isLast);

    Button bottomButton = new Button(new Icon(VaadinIcon.ANGLE_DOUBLE_DOWN));
    bottomButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    bottomButton.setTooltipText("Move to Bottom");
    bottomButton.setId("move-segment-bottom-button-" + segment.getId());
    bottomButton.addClickListener(
        e -> moveSegmentInMemory(segment, segmentOrder.size() - 1 - currentIndex));
    bottomButton.setEnabled(!isLast);

    return new HorizontalLayout(topButton, upButton, downButton, bottomButton);
  }

  /** Moves a segment in the in-memory list by {@code delta} positions and refreshes the grid. */
  protected void moveSegmentInMemory(@NonNull Segment segment, int delta) {
    int idx = segmentOrder.indexOf(segment);
    int newIdx = Math.max(0, Math.min(segmentOrder.size() - 1, idx + delta));
    if (idx == newIdx) {
      return;
    }
    segmentOrder.remove(idx);
    segmentOrder.add(newIdx, segment);
    markOrderDirty();
    segmentsGrid.setItems(new ArrayList<>(segmentOrder));
  }

  /** Moves a dragged segment to just before {@code target} in the in-memory list. */
  private void moveSegmentToPosition(@NonNull Segment dragged, @NonNull Segment target) {
    segmentOrder.remove(dragged);
    int targetIndex = segmentOrder.indexOf(target);
    segmentOrder.add(targetIndex, dragged);
    markOrderDirty();
    segmentsGrid.setItems(new ArrayList<>(segmentOrder));
  }

  private void markOrderDirty() {
    orderDirty = true;
    if (saveOrderButton != null) {
      saveOrderButton.setVisible(true);
    }
  }

  private void persistSegmentOrder() {
    if (!orderDirty) {
      return;
    }
    showSegmentsProgress(true);
    if (saveOrderButton != null) {
      saveOrderButton.setEnabled(false);
    }
    com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
    SecurityContext ctx = SecurityContextHolder.getContext();
    List<Segment> snapshot = new ArrayList<>(segmentOrder);
    CompletableFuture.runAsync(
            () ->
                GeneralSecurityUtils.runWithContext(
                    ctx,
                    () -> {
                      // Find the last non-Promo segment index
                      int mainEventIdx = -1;
                      for (int i = snapshot.size() - 1; i >= 0; i--) {
                        Segment s = snapshot.get(i);
                        if (s.getSegmentType() == null
                            || !SegmentTypeNames.PROMO.equalsIgnoreCase(
                                s.getSegmentType().getName())) {
                          mainEventIdx = i;
                          break;
                        }
                      }
                      List<Segment> changed = new ArrayList<>();
                      for (int i = 0; i < snapshot.size(); i++) {
                        Segment s = snapshot.get(i);
                        int newOrder = i + 1;
                        boolean shouldBeMain = i == mainEventIdx;
                        boolean orderChanged =
                            !Integer.valueOf(newOrder).equals(s.getSegmentOrder());
                        boolean mainChanged = s.isMainEvent() != shouldBeMain;
                        if (orderChanged || mainChanged) {
                          s.setSegmentOrder(newOrder);
                          s.setMainEvent(shouldBeMain);
                          changed.add(s);
                        }
                      }
                      if (!changed.isEmpty()) {
                        segmentRepository.saveAll(changed);
                      }
                      return null;
                    }))
        .thenRun(
            () -> {
              if (ui != null) {
                ui.access(
                    () -> {
                      orderDirty = false;
                      if (saveOrderButton != null) {
                        saveOrderButton.setVisible(false);
                        saveOrderButton.setEnabled(true);
                      }
                      showSegmentsProgress(false);
                      notificationService.showSuccess("Segment order saved.");
                      segmentsGrid.setItems(new ArrayList<>(segmentOrder));
                    });
              }
            })
        .exceptionally(
            ex -> {
              log.error("Error persisting segment order", ex);
              if (ui != null) {
                ui.access(
                    () -> {
                      if (saveOrderButton != null) {
                        saveOrderButton.setEnabled(true);
                      }
                      showSegmentsProgress(false);
                      notificationService.showError("Failed to save order: " + ex.getMessage());
                    });
              }
              return null;
            });
  }

  @Override
  public void beforeLeave(com.vaadin.flow.router.BeforeLeaveEvent event) {
    if (orderDirty) {
      com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
      Dialog confirm = new Dialog();
      confirm.setHeaderTitle("Unsaved Order");
      confirm.add(new Paragraph("You have unsaved segment order changes. Leave without saving?"));
      Button leave =
          new Button(
              "Leave",
              e -> {
                confirm.close();
                action.proceed();
              });
      leave.addThemeVariants(ButtonVariant.LUMO_ERROR);
      Button stay = new Button("Stay", e -> confirm.close());
      confirm.getFooter().add(stay, leave);
      confirm.open();
    }
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
          final com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
          SecurityContext securityContext = SecurityContextHolder.getContext();
          CompletableFuture.supplyAsync(
                  () ->
                      GeneralSecurityUtils.runWithContext(
                          securityContext,
                          () ->
                              NarrationDialog.PreloadedData.load(
                                  segmentService,
                                  npcService,
                                  wrestlerStatsService,
                                  universeContextService,
                                  segment)))
              .thenAccept(
                  preloaded ->
                      ui.access(
                          () ->
                              new NarrationDialog(
                                      preloaded,
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
                                      notificationService,
                                      wrestlerStatsService)
                                  .open()))
              .exceptionally(
                  ex -> {
                    log.error("Error loading narration dialog data", ex);
                    ui.access(
                        () -> notificationService.showError("Failed to open narration dialog."));
                    return null;
                  });
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
    boolean isMatch =
        segmentType != null && !SegmentTypeNames.PROMO.equalsIgnoreCase(segmentType.getName());
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
      notificationService.showError("No AI providers are currently enabled or reachable.");
      return;
    }

    final com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
    SecurityContext securityContext = SecurityContextHolder.getContext();
    CompletableFuture.supplyAsync(
            () ->
                GeneralSecurityUtils.runWithContext(
                    securityContext,
                    () ->
                        segmentNarrationServiceFactory.summarizeNarration(segment.getNarration())))
        .thenAccept(
            summary ->
                ui.access(
                    () -> {
                      segment.setSummary(summary);
                      segmentService.updateSegment(segment);
                      notificationService.showSuccess("Summary generated successfully!");
                      refreshSegmentsGrid();
                      ui.push();
                    }))
        .exceptionally(
            ex -> {
              log.error("Error generating summary", ex);
              ui.access(
                  () -> {
                    notificationService.showAIServiceError(
                        ex.getCause() != null ? ex.getCause() : ex);
                    ui.push();
                  });
              return null;
            });
  }

  private void openAddSegmentDialog(@NonNull final Show show) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Segment to " + show.getName());
    dialog.setWidth("min(600px, 95vw)");
    dialog.setMaxWidth("90vw");
    dialog.setId("add-segment-dialog");

    // --- Rivalry suggestions banner ---
    VerticalLayout rivalryBanner = new VerticalLayout();
    rivalryBanner.setSpacing(false);
    rivalryBanner.setPadding(false);
    rivalryBanner.setId("add-segment-rivalry-banner");
    rivalryBanner.setVisible(false);

    List<Rivalry> unbookedRivalries = showPlanningService.getUnbookedRivalriesByHeat(List.of());
    if (!unbookedRivalries.isEmpty()) {
      Span rivalryHint = new Span("Suggested feuds (by heat):");
      rivalryHint.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.SMALL);
      rivalryBanner.add(rivalryHint);
      for (Rivalry r : unbookedRivalries) {
        Button suggest =
            new Button(
                r.getWrestler1().getName()
                    + " vs "
                    + r.getWrestler2().getName()
                    + "  (heat="
                    + r.getHeat()
                    + ")");
        suggest.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        suggest.setId("suggest-rivalry-" + r.getId());
        rivalryBanner.add(suggest);
      }
      rivalryBanner.setVisible(true);
    }

    // Advisory banner shown when selected participants have a hot rivalry without a stipulation
    Span stipulationAdvisory = new Span();
    stipulationAdvisory.setId("add-segment-stipulation-advisory");
    stipulationAdvisory.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
    stipulationAdvisory.setVisible(false);

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

    // Wire suggest-rivalry buttons: pre-fill team combos and evaluate advisory
    for (Rivalry r : unbookedRivalries) {
      rivalryBanner
          .getChildren()
          .filter(c -> c.getId().map(id -> id.equals("suggest-rivalry-" + r.getId())).orElse(false))
          .findFirst()
          .ifPresent(
              c -> {
                ((Button) c)
                    .addClickListener(
                        ev -> {
                          if (addTeamCombos.size() >= 2) {
                            addTeamCombos.get(0).setValue(Set.of(r.getWrestler1()));
                            addTeamCombos.get(1).setValue(Set.of(r.getWrestler2()));
                          }
                          refreshAddWinners.run();
                        });
              });
    }

    // Evaluate stipulation advisory whenever participants change
    Runnable checkStipulationAdvisory =
        () -> {
          List<String> selectedNames =
              addTeamCombos.stream()
                  .flatMap(c -> c.getValue().stream())
                  .map(Wrestler::getName)
                  .toList();
          boolean hotRivalryNeedsStipulation =
              rivalryService.getActiveRivalries().stream()
                  .filter(r -> r.getHeat() >= 30)
                  .anyMatch(
                      r ->
                          selectedNames.contains(r.getWrestler1().getName())
                              && selectedNames.contains(r.getWrestler2().getName()));
          if (hotRivalryNeedsStipulation && rulesCombo.getValue().isEmpty()) {
            stipulationAdvisory.setText(
                "These wrestlers have a high-heat rivalry (heat ≥30)."
                    + " Consider adding a stipulation rule.");
            stipulationAdvisory.setVisible(true);
          } else {
            stipulationAdvisory.setVisible(false);
          }
        };

    for (MultiSelectComboBox<Wrestler> tc : addTeamCombos) {
      tc.addValueChangeListener(e -> checkStipulationAdvisory.run());
    }
    rulesCombo.addValueChangeListener(e -> checkStipulationAdvisory.run());

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

    VerticalLayout dialogLayout =
        new VerticalLayout(rivalryBanner, stipulationAdvisory, formLayout, buttonLayout);
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(false);
    dialogLayout.setId("add-segment-dialog-layout");

    dialog.add(dialogLayout);
    dialog.open();
  }

  private void openEditSegmentDialog(@NonNull Segment segment) {
    final com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Gender defaultGender =
        currentShow.getTemplate() != null ? currentShow.getTemplate().getGenderConstraint() : null;
    Long universeId = universeContextService.getCurrentUniverseId();

    CompletableFuture.supplyAsync(
            () ->
                GeneralSecurityUtils.runWithContext(
                    securityContext,
                    () -> {
                      Segment seg = segmentRepository.findById(segment.getId()).orElse(segment);
                      EditSegmentDialog.PreloadedData preloaded =
                          EditSegmentDialog.PreloadedData.load(
                              segmentTypeRepository,
                              segmentRuleRepository,
                              npcService,
                              titleService,
                              wrestlerService,
                              universeId);
                      return new Object[] {seg, preloaded};
                    }))
        .thenAccept(
            result ->
                ui.access(
                    () -> {
                      Segment seg = (Segment) result[0];
                      EditSegmentDialog.PreloadedData preloaded =
                          (EditSegmentDialog.PreloadedData) result[1];
                      EditSegmentDialog.SegmentDialogData initial =
                          EditSegmentDialog.SegmentDialogData.from(seg);

                      EditSegmentDialog[] dialogHolder = new EditSegmentDialog[1];
                      dialogHolder[0] =
                          new EditSegmentDialog(
                              preloaded,
                              initial,
                              wrestlerService,
                              defaultGender,
                              universeId,
                              saveData -> {
                                // Non-DB field assignments
                                seg.setNarration(saveData.narration());
                                seg.setSummary(saveData.summary());
                                seg.setNotes(saveData.notes());
                                seg.setReferee(saveData.referee());
                                seg.setIsTitleSegment(saveData.isTitleSegment());
                                if (saveData.isTitleSegment()) {
                                  seg.setTitles(saveData.titles());
                                }
                                // Inline validation (no DB)
                                Set<Wrestler> wrestlers =
                                    saveData.teams().values().stream()
                                        .flatMap(List::stream)
                                        .collect(Collectors.toSet());
                                if (saveData.segmentType() == null) {
                                  notificationService.showError("Please select a segment type");
                                  return;
                                }
                                if (!SegmentTypeNames.PROMO.equalsIgnoreCase(
                                    saveData.segmentType().getName())) {
                                  if (wrestlers.isEmpty()) {
                                    notificationService.showError(
                                        "Please select at least one wrestler");
                                    return;
                                  }
                                  if (wrestlers.size() < 2) {
                                    notificationService.showError(
                                        "Please select at least two wrestlers for a match");
                                    return;
                                  }
                                }
                                if (saveData.winners() != null) {
                                  for (Wrestler winner : saveData.winners()) {
                                    if (!wrestlers.contains(winner)) {
                                      notificationService.showError(
                                          "Winner must be one of the selected wrestlers");
                                      return;
                                    }
                                  }
                                }
                                // In-memory sync (no DB)
                                seg.syncParticipants(saveData.teams());
                                seg.syncSegmentRules(new ArrayList<>(saveData.rules()));
                                seg.setAdjudicationStatus(AdjudicationStatus.PENDING);
                                seg.setSegmentType(saveData.segmentType());
                                if (saveData.winners() != null) {
                                  seg.setWinners(new ArrayList<>(saveData.winners()));
                                }
                                // Async DB write
                                dialogHolder[0].getSaveButton().setEnabled(false);
                                SecurityContext saveCtx = SecurityContextHolder.getContext();
                                CompletableFuture.supplyAsync(
                                        () ->
                                            GeneralSecurityUtils.runWithContext(
                                                saveCtx,
                                                () -> {
                                                  segmentService.updateSegment(seg);
                                                  return null;
                                                }))
                                    .thenRun(
                                        () ->
                                            ui.access(
                                                () -> {
                                                  notificationService.showSuccess(
                                                      "Segment updated successfully!");
                                                  dialogHolder[0].close();
                                                  refreshSegmentsGrid();
                                                }))
                                    .exceptionally(
                                        ex -> {
                                          log.error("Error saving segment", ex);
                                          ui.access(
                                              () -> {
                                                dialogHolder[0].getSaveButton().setEnabled(true);
                                                notificationService.showError(
                                                    "Error saving segment: " + ex.getMessage());
                                              });
                                          return null;
                                        });
                              });
                      dialogHolder[0].setHeaderTitle("Edit Segment for " + currentShow.getName());
                      dialogHolder[0].open();
                    }))
        .exceptionally(
            ex -> {
              log.error("Error loading segment edit data", ex);
              ui.access(
                  () ->
                      notificationService.showError(
                          "Failed to load segment data: " + ex.getMessage()));
              return null;
            });
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
    log.debug("Validating and saving segment: {}", segmentToUpdate);
    // Validation
    if (segmentType == null) {
      log.debug("Validation failed: Segment type is null.");
      notificationService.showError("Please select a segment type");
      return false;
    }

    if (!SegmentTypeNames.PROMO.equalsIgnoreCase(segmentType.getName())) {
      if (wrestlers.isEmpty()) {
        log.debug("Validation failed: Wrestlers are null or empty for non-promo segment.");
        notificationService.showError("Please select at least one wrestler");
        return false;
      }

      if (wrestlers.size() < 2) {
        log.debug("Validation failed: Less than two wrestlers for a non-promo match.");
        notificationService.showError("Please select at least two wrestlers for a match");
        return false;
      }
    }

    if (winners != null) {
      for (Wrestler winner : winners) {
        if (!wrestlers.contains(winner)) {
          log.debug("Validation failed: Winner is not among selected wrestlers.");
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
    adjudicateButton.setEnabled(false);
    addSegmentButton.setEnabled(false);
    com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
    SecurityContext ctx = SecurityContextHolder.getContext();
    CompletableFuture.runAsync(
            () ->
                GeneralSecurityUtils.runWithContext(
                    ctx,
                    () -> {
                      showController.adjudicateShow(show.getId());
                      return null;
                    }))
        .thenRun(
            () ->
                ui.access(
                    () -> {
                      notificationService.showSuccess("Fan adjudication completed!");
                      refreshSegmentsGrid();
                    }))
        .exceptionally(
            ex -> {
              log.error("Error adjudicating show", ex);
              ui.access(
                  () -> notificationService.showError("Adjudication failed: " + ex.getMessage()));
              return null;
            });
  }

  private void showSegmentsProgress(final boolean visible) {
    if (segmentsProgressBar != null) {
      segmentsProgressBar.setVisible(visible);
    }
    if (segmentsGrid != null) {
      segmentsGrid.setEnabled(!visible);
    }
    if (adjudicateButton != null) {
      adjudicateButton.setEnabled(!visible);
    }
    if (addSegmentButton != null && visible) {
      addSegmentButton.setEnabled(false);
    }
  }

  private void refreshSegmentsGrid() {
    if (currentShow == null || segmentsGrid == null) {
      return;
    }
    showSegmentsProgress(true);
    com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
    SecurityContext ctx = SecurityContextHolder.getContext();
    Show show = currentShow;
    CompletableFuture.supplyAsync(
            () ->
                GeneralSecurityUtils.runWithContext(
                    ctx, () -> segmentRepository.findByShowOrderBySegmentOrderAsc(show)))
        .thenAccept(
            updatedSegments ->
                ui.access(
                    () -> {
                      showSegmentsProgress(false);
                      segmentOrder = new ArrayList<>(updatedSegments);
                      orderDirty = false;
                      if (saveOrderButton != null) {
                        saveOrderButton.setVisible(false);
                      }
                      segmentsGrid.setItems(updatedSegments);
                      boolean hasSegments = !updatedSegments.isEmpty();
                      segmentsGrid.setVisible(hasSegments);
                      if (noSegmentsMessage != null) {
                        noSegmentsMessage.setVisible(!hasSegments);
                      }
                      boolean hasPending =
                          updatedSegments.stream()
                              .anyMatch(
                                  s -> s.getAdjudicationStatus() == AdjudicationStatus.PENDING);
                      boolean allAdjudicated =
                          hasSegments
                              && updatedSegments.stream()
                                  .allMatch(
                                      s ->
                                          s.getAdjudicationStatus()
                                              == AdjudicationStatus.ADJUDICATED);
                      if (adjudicateButton != null) {
                        adjudicateButton.setEnabled(hasPending);
                      }
                      if (addSegmentButton != null) {
                        addSegmentButton.setEnabled(!allAdjudicated);
                      }
                    }))
        .exceptionally(
            ex -> {
              log.error("Error refreshing segments grid", ex);
              ui.access(() -> showSegmentsProgress(false));
              return null;
            });
  }

  @Override
  public void onApplicationEvent(@NonNull final ApplicationEvent event) {
    if (event instanceof AdjudicationCompletedEvent e
        && e.getShow().getId().equals(currentShowId)) {
      getUI().ifPresent(ui -> ui.access(this::refreshSegmentsGrid));
    }
  }
}
