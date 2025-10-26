package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.segment.NarrationDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.RestTemplate;

/**
 * Detail view for displaying comprehensive information about a specific show. Accessible via URL
 * parameter for direct linking and navigation.
 */
@Route("show-detail")
@PageTitle("Show Details")
@PermitAll
public class ShowDetailView extends Main
    implements HasUrlParameter<Long>, ApplicationListener<AdjudicationCompletedEvent> {

  @Autowired private ShowService showService;
  @Autowired private SegmentService segmentService;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private NpcService npcService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private TitleService titleService;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private SegmentAdjudicationService segmentAdjudicationService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private SeasonService seasonService;
  @Autowired private ShowTemplateService showTemplateService;
  private String referrer = "shows"; // Default referrer

  private H2 showTitle;
  private VerticalLayout contentLayout;
  private Long currentShowId;

  public ShowDetailView() {
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
    String navigationTarget;

    switch (referrer) {
      case "calendar":
        buttonText = "Back to Calendar";
        navigationTarget = "show-calendar";
        break;
      case "shows":
      default:
        buttonText = "Back to Shows";
        navigationTarget = "show-list";
        break;
    }

    Button backButton = new Button(buttonText, new Icon(VaadinIcon.ARROW_LEFT));
    backButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(navigationTarget)));
    return backButton;
  }

  private void loadShow(Long showId) {
    Optional<Show> showOpt = showService.getShowById(showId);
    if (showOpt.isPresent()) {
      displayShow(showOpt.get());
    } else {
      showNotFound();
    }
  }

  private void displayShow(Show show) {
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
                  loadShow(show.getId());
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
                  loadShow(show.getId());
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

    VerticalLayout segmentsLayout = new VerticalLayout();
    segmentsLayout.setSpacing(false);
    segmentsLayout.setPadding(false);
    segmentsLayout.setSizeFull();
    segmentsLayout.addClassNames(LumoUtility.Width.FULL);

    if (segments.isEmpty()) {
      Span noSegments = new Span("No segments scheduled for this show yet.");
      noSegments.addClassNames(LumoUtility.TextColor.SECONDARY);
      segmentsLayout.add(noSegments);
    } else {
      // Create segments grid
      Grid<Segment> segmentsGrid = createSegmentsGrid(segments);
      segmentsGrid.setHeight("400px"); // Set a reasonable height for the grid

      // Wrap the grid in a Div to enable horizontal scrolling
      Div gridWrapper = new Div(segmentsGrid);
      gridWrapper.addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Width.FULL);
      gridWrapper.getStyle().set("flex-grow", "4"); // Allow wrapper to grow

      segmentsLayout.add(gridWrapper);
      segmentsLayout.setFlexGrow(4, gridWrapper); // Let grid wrapper expand
    }

    card.add(header, segmentsLayout);
    return card;
  }

  private Grid<Segment> createSegmentsGrid(List<Segment> segments) {
    Grid<Segment> grid = new Grid<>(Segment.class, false);
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setItems(segments);

    // Segment type column
    grid.addColumn(segment -> segment.getSegmentType().getName())
        .setHeader("Segment Type")
        .setSortable(true)
        .setFlexGrow(1);

    // Segment type column
    grid.addColumn(
            segment -> {
              List<String> wrestlerNames =
                  segment.getSegmentRules().stream().map(SegmentRule::getName).toList();
              return String.join(", ", wrestlerNames);
            })
        .setHeader("Segment Rule(s)")
        .setSortable(true)
        .setFlexGrow(1);

    // Segment type column
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

    return grid;
  }

  private Component createActionButtons(@NonNull Segment segment) {
    Button summaryButton = new Button("Summarize", new Icon(VaadinIcon.ACADEMY_CAP));
    summaryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    summaryButton.setTooltipText("Generate AI Summary");
    summaryButton.addClickListener(e -> generateSummary(segment));
    summaryButton.setEnabled(segment.getNarration() != null && !segment.getNarration().isEmpty());

    Button narrateButton = new Button("Narrate", new Icon(VaadinIcon.MICROPHONE));
    narrateButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    narrateButton.setTooltipText("Generate AI Narration");
    narrateButton.addClickListener(
        e -> {
          NarrationDialog dialog =
              new NarrationDialog(
                  segment,
                  npcService,
                  wrestlerService,
                  titleService,
                  updatedSegment -> {
                    displayShow(updatedSegment.getShow());
                  });
          dialog.open();
        });

    Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.setTooltipText("Edit Segment");
    editButton.addClickListener(e -> openEditSegmentDialog(segment));

    Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete Segment");
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
    loadShow(this.currentShowId);
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
    segmentTypeCombo.setItems(segmentTypeRepository.findAll());
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);

    // Segment rules selection (multi-select)
    MultiSelectComboBox<SegmentRule> rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(segmentRuleRepository.findAll());
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    formLayout.setColspan(rulesCombo, 2);

    // Wrestlers selection (multi-select)
    MultiSelectComboBox<Wrestler> wrestlersCombo = new MultiSelectComboBox<>("Wrestlers");
    wrestlersCombo.setItems(wrestlerRepository.findAll());
    wrestlersCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setRequired(true);

    // Winner selection (will be populated based on selected wrestlers)
    ComboBox<Wrestler> winnerCombo = new ComboBox<>("Winner (Optional)");
    winnerCombo.setItemLabelGenerator(Wrestler::getName);
    winnerCombo.setWidthFull();
    winnerCombo.setClearButtonVisible(true);

    // Update winner options when wrestlers change
    wrestlersCombo.addValueChangeListener(
        e -> {
          winnerCombo.setItems(e.getValue());
          winnerCombo.clear();
        });

    formLayout.add(segmentTypeCombo, rulesCombo, wrestlersCombo, winnerCombo);

    // Buttons
    Button saveButton =
        new Button(
            "Add Segment",
            e -> {
              Set<Wrestler> winners = new HashSet<>();
              if (winnerCombo.getValue() != null) {
                winners.add(winnerCombo.getValue());
              }
              if (validateAndSaveSegment(
                  show,
                  segmentTypeCombo.getValue(),
                  wrestlersCombo.getValue(),
                  winners,
                  rulesCombo.getValue(),
                  null)) {
                dialog.close();
                // Refresh the segments display
                displayShow(show);
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

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
    segmentTypeCombo.setItems(segmentTypeRepository.findAll());
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    segmentTypeCombo.setValue(segment.getSegmentType());

    // Segment rules selection (multi-select)
    MultiSelectComboBox<SegmentRule> rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(segmentRuleRepository.findAll());
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    rulesCombo.setValue(segment.getSegmentRules());
    formLayout.setColspan(rulesCombo, 2);

    // Wrestlers selection (multi-select)
    MultiSelectComboBox<Wrestler> wrestlersCombo = new MultiSelectComboBox<>("Wrestlers");
    wrestlersCombo.setItems(wrestlerRepository.findAll());
    wrestlersCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setRequired(true);
    wrestlersCombo.setValue(segment.getWrestlers());

    // Winner selection (multi-select)
    MultiSelectComboBox<Wrestler> winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setItems(segment.getWrestlers());
    winnersCombo.setValue(segment.getWinners());

    // Update winner options when wrestlers change
    wrestlersCombo.addValueChangeListener(
        e -> {
          winnersCombo.setItems(e.getValue());
          winnersCombo.clear();
        });

    // Narration
    TextArea summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(segment.getSummary() != null ? segment.getSummary() : "");
    formLayout.setColspan(summaryArea, 2);

    // Narration
    TextArea narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(segment.getNarration() != null ? segment.getNarration() : "");
    formLayout.setColspan(narrationArea, 2);

    formLayout.add(
        segmentTypeCombo, rulesCombo, wrestlersCombo, winnersCombo, summaryArea, narrationArea);

    // Buttons
    Button saveButton =
        new Button(
            "Save Changes",
            e -> {
              segment.setNarration(narrationArea.getValue());
              segment.setSummary(summaryArea.getValue());
              if (validateAndSaveSegment(
                  segment.getShow(),
                  segmentTypeCombo.getValue(),
                  wrestlersCombo.getValue(),
                  winnersCombo.getValue(),
                  rulesCombo.getValue(),
                  segment)) {
                dialog.close();
                // Refresh the segments display
                displayShow(segment.getShow());
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

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
                segmentService.deleteSegment(segment.getId());
                Notification.show(
                        "Segment deleted successfully!", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                confirmDialog.close();
                displayShow(segment.getShow()); // Refresh the segments display
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
    // Validation
    if (segmentType == null) {
      Notification.show("Please select a segment type", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }

    if (!"Promo".equalsIgnoreCase(segmentType.getName())) {
      if (wrestlers == null || wrestlers.isEmpty()) {
        Notification.show("Please select at least one wrestler", 3000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return false;
      }

      if (wrestlers.size() < 2) {
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
        segment.setAdjudicationStatus(
            com.github.javydreamercsw.management.domain.AdjudicationStatus.PENDING);
      } else {
        segment = new Segment();
        segment.setShow(show);
        segment.setSegmentDate(java.time.Instant.now());
        segment.setIsTitleSegment(false);
        segment.setIsNpcGenerated(false);
        segment.syncParticipants(new ArrayList<>(wrestlers));
        segment.syncSegmentRules(new ArrayList<>(rules));
      }

      segment.setSegmentType(segmentType);

      if (winners != null) {
        segment.setWinners(new ArrayList<>(winners));
      }

      // Save or update the segment
      Segment savedSegment = segmentRepository.save(segment);
      if (segmentToUpdate != null) {
        Notification.show("Segment updated successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      } else {
        Notification.show("Segment added successfully!", 3000, Notification.Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      }
      return true;
    } catch (Exception e) {
      Notification.show(
              "Error saving segment: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }
  }

  private void adjudicateShow(Show show) {
    String baseUrl = com.github.javydreamercsw.management.util.UrlUtil.getBaseUrl();
    new RestTemplate()
        .postForObject(baseUrl + "/api/shows/" + show.getId() + "/adjudicate", null, Void.class);
    Notification.show("Fan adjudication completed!", 3000, Notification.Position.BOTTOM_START)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    loadShow(this.currentShowId);
  }

  @Override
  public void onApplicationEvent(AdjudicationCompletedEvent event) {
    // Check if the completed show is the one currently being viewed
    if (event.getShow().getId().equals(currentShowId)) {
      getUI()
          .ifPresent(
              ui -> {
                ui.access(() -> loadShow(currentShowId));
              });
    }
  }
}
