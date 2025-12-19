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

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.SegmentNarrationConfig;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.event.SegmentsApprovedEvent;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.segment.NarrationDialog;
import com.github.javydreamercsw.management.util.UrlUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

/**
 * Detail view for displaying comprehensive information about a specific show. Accessible via URL
 * parameter for direct linking and navigation.
 */
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
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final SegmentRuleRepository segmentRuleRepository;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final RivalryService rivalryService;
  private final LocalAIStatusService localAIStatusService;
  private final SegmentNarrationConfig segmentNarrationConfig;
  private String referrer = "shows"; // Default referrer

  private H2 showTitle;
  private VerticalLayout contentLayout;
  private Long currentShowId;
  private Show currentShow; // Store the current show object
  private Grid<Segment> segmentsGrid; // Declare segmentsGrid as a class member

  public ShowDetailView(
      ShowService showService,
      SegmentService segmentService,
      SegmentRepository segmentRepository,
      SegmentTypeRepository segmentTypeRepository,
      WrestlerRepository wrestlerRepository,
      NpcService npcService,
      WrestlerService wrestlerService,
      TitleService titleService,
      SegmentRuleRepository segmentRuleRepository,
      ShowTypeService showTypeService,
      SeasonService seasonService,
      ShowTemplateService showTemplateService,
      RivalryService rivalryService,
      LocalAIStatusService localAIStatusService,
      SegmentNarrationConfig segmentNarrationConfig) {
    this.showService = showService;
    this.segmentService = segmentService;
    this.segmentRepository = segmentRepository;
    this.segmentTypeRepository = segmentTypeRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.segmentRuleRepository = segmentRuleRepository;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.rivalryService = rivalryService;
    this.localAIStatusService = localAIStatusService;
    this.segmentNarrationConfig = segmentNarrationConfig;
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

    // Context-aware back button
    Button backButton = createBackButton();
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
  public void setParameter(BeforeEvent event, Long showId) {
    // Detect referrer from query parameters or referer header
    this.referrer =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .getOrDefault("ref", List.of("shows"))
            .get(0);

    this.currentShowId = showId; // Store the showId

    if (showId != null) {
      loadShow(showId);
    } else {
      showNotFound();
    }
  }

  private Button createBackButton() {
    String buttonText;
    String navigationTarget =
        switch (referrer) {
          case "calendar" -> {
            buttonText = "Back to Calendar";
            yield "show-calendar";
          }
          default -> {
            buttonText = "Back to Shows";
            yield "show-list";
          }
        };

    Button backButton = new Button(buttonText, new Icon(VaadinIcon.ARROW_LEFT));
    backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(navigationTarget)));
    return backButton;
  }

  private void loadShow(Long showId) {
    Optional<Show> showOpt = showService.getShowById(showId);
    if (showOpt.isPresent()) {
      currentShow = showOpt.get(); // Store the show object
      displayShow(currentShow);
    } else {
      showNotFound();
    }
  }

  private void displayShow(@NonNull Show show) {
    contentLayout.removeAll();
    showTitle.setText(show.getName());

    // Show header with basic info
    Div headerCard = createHeaderCard(show);
    contentLayout.add(headerCard);

    // Show details card
    Div detailsCard = createDetailsCard(show);
    contentLayout.add(detailsCard);

    // Show description card
    if (show.getDescription() != null && !show.getDescription().trim().isEmpty()) {
      Div descriptionCard = createDescriptionCard(show);
      contentLayout.add(descriptionCard);
    }

    // Show segments card
    Div segmentsCard = createSegmentsCard(show);
    contentLayout.add(segmentsCard);
  }

  private Div createHeaderCard(Show show) {
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
    editNameButton.addClickListener(
        e -> {
          EditShowNameDialog dialog = new EditShowNameDialog(showService, show);
          dialog.addOpenedChangeListener(
              event -> {
                if (!event.isOpened()) {
                  refreshSegmentsGrid();
                }
              });
          dialog.open();
        });

    // Show type badge
    Span typeBadge = new Span(show.getType().getName());
    typeBadge.addClassNames(
        LumoUtility.Padding.Horizontal.SMALL,
        LumoUtility.Padding.Vertical.XSMALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.SEMIBOLD);

    if (show.isPremiumLiveEvent()) {
      typeBadge.addClassNames(LumoUtility.Background.ERROR, LumoUtility.TextColor.ERROR_CONTRAST);
    } else if (show.isWeeklyShow()) {
      typeBadge.addClassNames(
          LumoUtility.Background.PRIMARY, LumoUtility.TextColor.PRIMARY_CONTRAST);
    } else {
      typeBadge.addClassNames(
          LumoUtility.Background.SUCCESS, LumoUtility.TextColor.SUCCESS_CONTRAST);
    }

    HorizontalLayout titleLayout = new HorizontalLayout(title, editNameButton, typeBadge);
    titleLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    titleLayout.setSpacing(true);

    card.add(titleLayout);
    return card;
  }

  private Div createDetailsCard(@NonNull Show show) {
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

    // Show date
    if (show.getShowDate() != null) {
      HorizontalLayout dateLayout =
          createDetailRow(
              "Show Date:",
              show.getShowDate()
                  .format(
                      DateTimeFormatter.ofPattern(
                          "EEEE, MMMM d, yyyy"))); // Corrected: Removed unnecessary escaping of
      // double quotes within the pattern string.
      detailsLayout.add(dateLayout);
    } else {
      HorizontalLayout dateLayout = createDetailRow("Show Date:", "Not scheduled");
      detailsLayout.add(dateLayout);
    }

    // Show type
    HorizontalLayout typeLayout =
        createDetailRow(
            "Type:", show.getType() != null ? show.getType().getName() : "No type assigned");
    detailsLayout.add(typeLayout);

    // Season
    HorizontalLayout seasonLayout =
        createDetailRow(
            "Season:",
            show.getSeason() != null ? show.getSeason().getName() : "No season assigned");
    detailsLayout.add(seasonLayout);

    // Template
    HorizontalLayout templateLayout =
        createDetailRow(
            "Template:",
            show.getTemplate() != null ? show.getTemplate().getName() : "No template assigned");
    detailsLayout.add(templateLayout);

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
                  .format(
                      DateTimeFormatter.ofPattern(
                          "MMM d, yyyy 'at' h:mm a"))); // Corrected: Removed unnecessary escaping
      // of double quotes within the pattern
      // string.
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

    Button editDetailsButton = new Button(new Icon(VaadinIcon.EDIT));
    editDetailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editDetailsButton.setTooltipText("Edit Show Details");
    editDetailsButton.addClickListener(
        e -> {
          EditShowDetailsDialog dialog =
              new EditShowDetailsDialog(
                  showService, showTypeService, seasonService, showTemplateService, show);
          dialog.addOpenedChangeListener(
              event -> {
                if (!event.isOpened()) {
                  refreshSegmentsGrid();
                }
              });
          dialog.open();
        });

    HorizontalLayout buttonGroup = new HorizontalLayout(planShowButton, editDetailsButton);
    detailsHeader.add(buttonGroup);

    card.add(detailsHeader, detailsLayout);
    return card;
  }

  private HorizontalLayout createDetailRow(@NonNull String label, @NonNull String value) {
    Span labelSpan = new Span(label);
    labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SECONDARY);

    Span valueSpan = new Span(value);
    valueSpan.addClassNames(LumoUtility.TextColor.BODY);

    HorizontalLayout layout = new HorizontalLayout(labelSpan, valueSpan);
    layout.setSpacing(true);
    layout.addClassNames(LumoUtility.Padding.Vertical.SMALL);

    return layout;
  }

  private Div createDescriptionCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE);

    H3 descriptionTitle = new H3("Description");
    descriptionTitle.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

    Div descriptionContent = new Div();
    descriptionContent
        .getElement()
        .setProperty(
            "innerHTML",
            show.getDescription()
                .replace("\n", "<br>")); // Corrected: Replaced \n with <br> for HTML rendering.
    descriptionContent.addClassNames(LumoUtility.TextColor.BODY);

    card.add(descriptionTitle, descriptionContent);
    return card;
  }

  private void showNotFound() {
    contentLayout.removeAll();
    showTitle.setText("Show Not Found");

    Div notFoundCard = new Div();
    notFoundCard.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.ERROR_10,
        LumoUtility.TextAlignment.CENTER);

    Icon errorIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
    errorIcon.addClassNames(LumoUtility.TextColor.ERROR);
    errorIcon.setSize("48px");

    H3 errorTitle = new H3("Show Not Found");
    errorTitle.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.Margin.NONE);

    Paragraph errorMessage =
        new Paragraph(
            "The requested show could not be found. It may have been deleted or the URL is"
                + " incorrect.");
    errorMessage.addClassNames(LumoUtility.TextColor.SECONDARY);

    Button backButton = new Button("Back to Calendar", new Icon(VaadinIcon.CALENDAR));
    backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("show-calendar")));
    backButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    VerticalLayout errorLayout =
        new VerticalLayout(errorIcon, errorTitle, errorMessage, backButton);
    errorLayout.setAlignItems(VerticalLayout.Alignment.CENTER);
    errorLayout.setSpacing(true);

    notFoundCard.add(errorLayout);
    contentLayout.add(notFoundCard);
  }

  private Div createSegmentsCard(@NonNull Show show) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.BASE,
        LumoUtility.Width.FULL);
    card.setSizeFull();

    // Header with title and add button
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
    header.setAlignItems(HorizontalLayout.Alignment.CENTER);

    H3 segmentsTitle = new H3("Segments");
    segmentsTitle.addClassNames(LumoUtility.Margin.NONE);

    Button adjudicateButton = new Button("Adjudicate Fans", new Icon(VaadinIcon.GROUP));
    adjudicateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    adjudicateButton.addClickListener(e -> adjudicateShow(show));

    // Check if there are any pending segments
    boolean hasPendingSegments =
        segmentRepository.findByShow(show).stream()
            .anyMatch(
                segment ->
                    segment.getAdjudicationStatus()
                        == com.github.javydreamercsw.management.domain.AdjudicationStatus.PENDING);
    adjudicateButton.setEnabled(hasPendingSegments);

    Button addSegmentBtn = new Button("Add Segment", new Icon(VaadinIcon.PLUS));
    addSegmentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addSegmentBtn.addClickListener(e -> openAddSegmentDialog(show));

    header.add(segmentsTitle, adjudicateButton, addSegmentBtn);

    // Get segments for this show
    List<Segment> segments = segmentRepository.findByShow(show);
    log.debug("Found {} segments for show: {}", segments.size(), show.getName());

    VerticalLayout segmentsLayout = new VerticalLayout();
    segmentsLayout.setSpacing(false);
    segmentsLayout.setPadding(false);
    segmentsLayout.setSizeFull();
    segmentsLayout.addClassNames(LumoUtility.Width.FULL);

    // Always initialize segmentsGrid and its wrapper
    if (segmentsGrid == null) {
      segmentsGrid = createSegmentsGrid(segments);
      segmentsGrid.setHeight("400px"); // Set a reasonable height for the grid
      segmentsGrid.setId("segments-grid");

      // Wrap the grid in a Div to enable horizontal scrolling
      Div gridWrapper = new Div(segmentsGrid);
      gridWrapper.addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Width.FULL);
      gridWrapper.getStyle().set("flex-grow", "4"); // Allow wrapper to grow
      gridWrapper.setId("segments-grid-wrapper");
      segmentsLayout.add(gridWrapper);
      segmentsLayout.setFlexGrow(4, gridWrapper); // Let grid wrapper expand
    } else {
      segmentsGrid.setItems(segments);
    }

    Span noSegmentsMessage = new Span("No segments scheduled for this show yet.");
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

  private Grid<Segment> createSegmentsGrid(List<Segment> segments) {
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

    // Segment type column
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
        .setFlexGrow(4); // Give more space to participants

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

  Grid<Segment> getSegmentsGrid(List<Segment> segments) {
    return createSegmentsGrid(segments);
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
      refreshSegmentsGrid(); // Call refreshSegmentsGrid instead of loadShow
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
          refreshSegmentsGrid(); // Call refreshSegmentsGrid instead of loadShow
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
                  updatedSegment -> refreshSegmentsGrid(),
                  rivalryService,
                  localAIStatusService,
                  segmentNarrationConfig); // Call refreshSegmentsGrid
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

    return new VerticalLayout(summaryButton, narrateButton, editButton, deleteButton);
  }

  private void generateSummary(@NonNull Segment segment) {
    String baseUrl = com.github.javydreamercsw.management.util.UrlUtil.getBaseUrl();

    new RestTemplate()
        .postForObject(
            baseUrl + "/api/segments/" + segment.getId() + "/summarize", null, Segment.class);
    Notification.show("Summary generated successfully!", 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    refreshSegmentsGrid(); // Call refreshSegmentsGrid instead of loadShow
  }

  private void openAddSegmentDialog(@NonNull Show show) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Segment to " + show.getName());
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");

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

    // Wrestlers selection (multi-select)
    MultiSelectComboBox<Wrestler> wrestlersCombo = new MultiSelectComboBox<>("Wrestlers");
    wrestlersCombo.setItems(
        wrestlerRepository.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestlersCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setRequired(true);
    wrestlersCombo.setId("wrestlers-combo-box");

    // Winner selection (will be populated based on selected wrestlers)
    ComboBox<Wrestler> winnerCombo = new ComboBox<>("Winner (Optional)");
    winnerCombo.setItemLabelGenerator(Wrestler::getName);
    winnerCombo.setWidthFull();
    winnerCombo.setClearButtonVisible(true);
    winnerCombo.setId("winner-combo-box");

    // Update winner options when wrestlers change
    wrestlersCombo.addValueChangeListener(
        e -> {
          winnerCombo.setItems(
              e.getValue().stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnerCombo.clear();
        });

    // Add title selection for new segments
    MultiSelectComboBox<Title> titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(
        titleService.findAll().stream()
            .sorted(Comparator.comparing(Title::getName))
            .collect(Collectors.toList()));
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setVisible(false); // Initially hidden
    titleMultiSelectComboBox.setId("title-multi-select-combo-box");

    // Add checkbox to indicate if it's a title segment
    Checkbox isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setId("is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear(); // Clear selection if not a title segment
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

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        wrestlersCombo,
        winnerCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        narrationArea);

    // Buttons
    Button saveButton =
        new Button(
            "Add Segment",
            e -> {
              Set<Wrestler> winners = new HashSet<>();
              if (winnerCombo.getValue() != null) {
                winners.add(winnerCombo.getValue());
              }
              // Create a new segment object to pass to validation
              Segment newSegment = new Segment();
              newSegment.setNarration(narrationArea.getValue());
              newSegment.setSummary(summaryArea.getValue());
              newSegment.setSegmentOrder(segmentRepository.findByShow(show).size() + 1);
              newSegment.setShow(show);
              newSegment.setSegmentDate(java.time.Instant.now());
              // Set isTitleSegment based on checkbox
              boolean isTitleSegment = isTitleSegmentCheckbox.getValue();
              newSegment.setIsTitleSegment(isTitleSegment);
              newSegment.setIsNpcGenerated(false);
              newSegment.syncParticipants(new ArrayList<>(wrestlersCombo.getValue()));
              newSegment.syncSegmentRules(new ArrayList<>(rulesCombo.getValue()));
              newSegment.setSegmentType(segmentTypeCombo.getValue());
              newSegment.setWinners(new ArrayList<>(winners));

              // If it's a title segment, set the selected titles
              if (isTitleSegment) {
                newSegment.setTitles(titleMultiSelectComboBox.getValue());
              }

              if (validateAndSaveSegment(
                  show,
                  segmentTypeCombo.getValue(),
                  wrestlersCombo.getValue(),
                  winners,
                  rulesCombo.getValue(),
                  newSegment)) { // Pass the new segment object
                dialog.close();
                refreshSegmentsGrid(); // Call refreshSegmentsGrid
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

    dialog.add(dialogLayout);
    dialog.open();
  }

  private void openEditSegmentDialog(@NonNull Segment segment) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Segment for " + segment.getShow().getName());
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");

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
    segmentTypeCombo.setValue(segment.getSegmentType());
    segmentTypeCombo.setId("edit-segment-type-combo-box");

    // Segment rules selection (multi-select)
    MultiSelectComboBox<SegmentRule> rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(
        segmentRuleRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentRule::getName))
            .collect(Collectors.toList()));
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    rulesCombo.setValue(segment.getSegmentRules());
    rulesCombo.setId("edit-segment-rules-combo-box");
    formLayout.setColspan(rulesCombo, 2);

    // Wrestlers selection (multi-select)
    MultiSelectComboBox<Wrestler> wrestlersCombo = new MultiSelectComboBox<>("Wrestlers");
    wrestlersCombo.setItems(
        wrestlerRepository.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestlersCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setRequired(true);
    wrestlersCombo.setValue(segment.getWrestlers());
    wrestlersCombo.setId("edit-wrestlers-combo-box");

    // Winner selection (multi-select)
    MultiSelectComboBox<Wrestler> winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setItems(
        segment.getWrestlers().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    winnersCombo.setValue(new HashSet<>(segment.getWinners()));
    winnersCombo.setId("edit-winners-combo-box");

    // Update winner options when wrestlers change
    wrestlersCombo.addValueChangeListener(
        e -> {
          winnersCombo.setItems(
              e.getValue().stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnersCombo.clear();
        });

    // Narration
    TextArea summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(segment.getSummary() != null ? segment.getSummary() : "");
    summaryArea.setId("edit-summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    // Narration
    TextArea narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(segment.getNarration() != null ? segment.getNarration() : "");
    narrationArea.setId("edit-narration-text-area");
    formLayout.setColspan(narrationArea, 2);

    // ... other fields ...

    // Title selection (multi-select) - only visible if segment is a title segment
    MultiSelectComboBox<Title> titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(
        titleService.findAll().stream()
            .sorted(Comparator.comparing(Title::getName))
            .collect(Collectors.toList()));
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setVisible(segment.getIsTitleSegment()); // Control visibility
    titleMultiSelectComboBox.setValue(segment.getTitles()); // Set initial value
    titleMultiSelectComboBox.setId("edit-title-multi-select-combo-box");

    // Add checkbox to indicate if it's a title segment
    Checkbox isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setValue(segment.getIsTitleSegment());
    isTitleSegmentCheckbox.setId("edit-is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear(); // Clear selection if not a title segment
          }
        });

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        wrestlersCombo,
        winnersCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        narrationArea);

    // Buttons
    Button saveButton =
        new Button(
            "Save Changes",
            e -> {
              segment.setNarration(narrationArea.getValue());
              segment.setSummary(summaryArea.getValue());
              // Set isTitleSegment based on checkbox
              boolean isTitleSegment = isTitleSegmentCheckbox.getValue();
              segment.setIsTitleSegment(isTitleSegment);
              // If it's a title segment, set the selected titles
              if (isTitleSegment) {
                segment.setTitles(titleMultiSelectComboBox.getValue());
              }
              if (validateAndSaveSegment(
                  segment.getShow(),
                  segmentTypeCombo.getValue(),
                  wrestlersCombo.getValue(),
                  winnersCombo.getValue(),
                  rulesCombo.getValue(),
                  segment)) { // Pass the segment to update
                dialog.close();
                refreshSegmentsGrid(); // Call refreshSegmentsGrid
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
                Notification.show(
                        "Segment deleted successfully!", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                confirmDialog.close();
                refreshSegmentsGrid(); // Call refreshSegmentsGrid
              } catch (Exception e) {
                Notification.show(
                        "Error deleting segment: " + e.getMessage(),
                        5000,
                        Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
              }
            });
    deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button cancelButton = new Button("Cancel", event -> confirmDialog.close());

    confirmDialog.getFooter().add(cancelButton, deleteButton);
    confirmDialog.open();
  }

  private boolean validateAndSaveSegment(
      @NonNull Show show,
      SegmentType segmentType,
      Set<Wrestler> wrestlers,
      Set<Wrestler> winners,
      Set<SegmentRule> rules,
      Segment segmentToUpdate) {
    log.info("Validating and saving segment: {}", segmentToUpdate);
    // Validation
    if (segmentType == null) {
      log.warn("Validation failed: Segment type is null.");
      Notification.show("Please select a segment type", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }

    if (!"Promo".equalsIgnoreCase(segmentType.getName())) {
      if (wrestlers == null || wrestlers.isEmpty()) {
        log.warn("Validation failed: Wrestlers are null or empty for non-promo segment.");
        Notification.show("Please select at least one wrestler", 3000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return false;
      }

      if (wrestlers.size() < 2) {
        log.warn("Validation failed: Less than two wrestlers for a non-promo match.");
        Notification.show(
                "Please select at least two wrestlers for a match",
                3000,
                Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return false;
      }
    }

    if (winners != null) {
      for (Wrestler winner : winners) {
        if (!wrestlers.contains(winner)) {
          log.warn("Validation failed: Winner is not among selected wrestlers.");
          Notification.show(
                  "Winner must be one of the selected wrestlers",
                  3000,
                  Notification.Position.MIDDLE)
              .addThemeVariants(NotificationVariant.LUMO_ERROR);
          return false;
        }
      }
    }

    try {
      Segment segment;
      if (segmentToUpdate != null) {
        segment = segmentToUpdate;
        segment.syncParticipants(new ArrayList<>(wrestlers));
        segment.syncSegmentRules(new ArrayList<>(rules));
        segment.setAdjudicationStatus(AdjudicationStatus.PENDING);
        log.info("Updating existing segment: {}", segment.getId());
      } else {
        segment = new Segment();
        segment.setShow(show);
        segment.setSegmentDate(java.time.Instant.now());
        segment.setIsTitleSegment(false);
        segment.setIsNpcGenerated(false);
        segment.syncParticipants(new ArrayList<>(wrestlers));
        segment.syncSegmentRules(new ArrayList<>(rules));
        log.info("Creating new segment for show: {}", show.getName());
      }

      segment.setSegmentType(segmentType);

      if (winners != null) {
        segment.setWinners(new ArrayList<>(winners));
      }

      // Save or update the segment
      segmentRepository.save(segment);
      log.info("Segment saved successfully: {}", segment.getId());
      if (segmentToUpdate != null) {
        Notification.show("Segment updated successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      } else {
        Notification.show("Segment added successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      }
      return true;
    } catch (Exception e) {
      log.error("Error saving segment: {}", e.getMessage(), e);
      Notification.show(
              "Error saving segment: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }
  }

  private void adjudicateShow(Show show) {
    String baseUrl = UrlUtil.getBaseUrl();
    new RestTemplate()
        .postForObject(baseUrl + "/api/shows/" + show.getId() + "/adjudicate", null, Void.class);
    Notification.show("Fan adjudication completed!", 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    refreshSegmentsGrid(); // Call refreshSegmentsGrid instead of loadShow
  }

  private void refreshSegmentsGrid() {
    if (currentShow != null && segmentsGrid != null) {
      List<Segment> updatedSegments = segmentRepository.findByShow(currentShow);
      segmentsGrid.setItems(updatedSegments);

      // Update visibility of grid and noSegmentsMessage
      boolean hasSegments = !updatedSegments.isEmpty();
      segmentsGrid.setVisible(hasSegments);
      // Find the noSegmentsMessage and set its visibility
      contentLayout
          .getChildren()
          .filter(VerticalLayout.class::isInstance)
          .map(VerticalLayout.class::cast)
          .filter(
              layout ->
                  layout
                      .getChildren()
                      .anyMatch(
                          component ->
                              component instanceof Span
                                  && "no-segments-message".equals(component.getId().get())))
          .findFirst()
          .flatMap(
              layout ->
                  layout
                      .getChildren()
                      .filter(Span.class::isInstance)
                      .map(Span.class::cast)
                      .filter(span -> "no-segments-message".equals(span.getId().get()))
                      .findFirst())
          .ifPresent(span -> span.setVisible(!hasSegments));
      // Re-enable/disable adjudicate button based on new segment status
      boolean hasPendingSegments =
          updatedSegments.stream()
              .anyMatch(
                  segment ->
                      segment.getAdjudicationStatus()
                          == com.github.javydreamercsw.management.domain.AdjudicationStatus
                              .PENDING);
      // Find the adjudicate button and update its enabled state
      contentLayout
          .getChildren()
          .filter(Div.class::isInstance)
          .map(Div.class::cast)
          .filter(
              card ->
                  card.getChildren()
                      .anyMatch(
                          component ->
                              component instanceof HorizontalLayout
                                  && ((HorizontalLayout) component)
                                      .getChildren()
                                      .anyMatch(
                                          btn ->
                                              btn instanceof Button
                                                  && "Adjudicate Fans"
                                                      .equals(((Button) btn).getText()))))
          .findFirst()
          .flatMap(
              card ->
                  card.getChildren()
                      .filter(HorizontalLayout.class::isInstance)
                      .map(HorizontalLayout.class::cast)
                      .findFirst())
          .flatMap(
              header ->
                  header
                      .getChildren()
                      .filter(Button.class::isInstance)
                      .map(Button.class::cast)
                      .filter(btn -> "Adjudicate Fans".equals(btn.getText()))
                      .findFirst())
          .ifPresent(btn -> btn.setEnabled(hasPendingSegments));
    }
  }

  @Override
  public void onApplicationEvent(@NotNull ApplicationEvent event) {
    if (event instanceof AdjudicationCompletedEvent adjudicationCompletedEvent) {
      // Check if the completed show is the one currently being viewed
      assert adjudicationCompletedEvent.getShow().getId() != null;
      if (adjudicationCompletedEvent.getShow().getId().equals(currentShowId)) {
        getUI().ifPresent(ui -> ui.access(this::refreshSegmentsGrid)); // Call refreshSegmentsGrid
      }
    } else if (event instanceof SegmentsApprovedEvent segmentsApprovedEvent) {
      // Check if the completed show is the one currently being viewed
      assert segmentsApprovedEvent.getShow().getId() != null;
      if (segmentsApprovedEvent.getShow().getId().equals(currentShowId)) {
        getUI().ifPresent(ui -> ui.access(this::refreshSegmentsGrid)); // Call refreshSegmentsGrid
      }
    }
  }
}
