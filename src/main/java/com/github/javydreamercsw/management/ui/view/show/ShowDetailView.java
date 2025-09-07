package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Detail view for displaying comprehensive information about a specific show. Accessible via URL
 * parameter for direct linking and navigation.
 */
@Route("show-detail")
@PageTitle("Show Details")
@PermitAll
public class ShowDetailView extends Main implements HasUrlParameter<Long> {

  private final ShowService showService;
  private final MatchRepository matchRepository;
  private final MatchTypeRepository matchTypeRepository;
  private final WrestlerRepository wrestlerRepository;
  private Show currentShow;
  private String referrer = "shows"; // Default referrer

  private H2 showTitle;
  private VerticalLayout contentLayout;

  public ShowDetailView(
      ShowService showService,
      MatchRepository matchRepository,
      MatchTypeRepository matchTypeRepository,
      WrestlerRepository wrestlerRepository) {
    this.showService = showService;
    this.matchRepository = matchRepository;
    this.matchTypeRepository = matchTypeRepository;
    this.wrestlerRepository = wrestlerRepository;
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
    String refererParam =
        event
            .getLocation()
            .getQueryParameters()
            .getParameters()
            .getOrDefault("ref", java.util.List.of("shows"))
            .get(0);
    this.referrer = refererParam;

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
      currentShow = showOpt.get();
      displayShow(currentShow);
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

    // Show matches card
    Div matchesCard = createMatchesCard(show);
    contentLayout.add(matchesCard);
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

    HorizontalLayout titleLayout = new HorizontalLayout(title, typeBadge);
    titleLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
    titleLayout.setSpacing(true);

    card.add(titleLayout);
    return card;
  }

  private Div createDetailsCard(Show show) {
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
              show.getShowDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))); 
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
                  .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")));
      detailsLayout.add(createdLayout);
    }

    card.add(detailsTitle, detailsLayout);
    return card;
  }

  private HorizontalLayout createDetailRow(String label, String value) {
    Span labelSpan = new Span(label);
    labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SECONDARY);

    Span valueSpan = new Span(value);
    valueSpan.addClassNames(LumoUtility.TextColor.BODY);

    HorizontalLayout layout = new HorizontalLayout(labelSpan, valueSpan);
    layout.setSpacing(true);
    layout.addClassNames(LumoUtility.Padding.Vertical.SMALL);

    return layout;
  }

  private Div createDescriptionCard(Show show) {
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
        .setProperty("innerHTML", show.getDescription().replace("\n", "<br>"));
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

  private Div createMatchesCard(Show show) {
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

    H3 matchesTitle = new H3("Matches");
    matchesTitle.addClassNames(LumoUtility.Margin.NONE);

    Button addMatchBtn = new Button("Add Match", new Icon(VaadinIcon.PLUS));
    addMatchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addMatchBtn.addClickListener(e -> openAddMatchDialog(show));

    header.add(matchesTitle, addMatchBtn);

    // Get matches for this show
    List<Match> matches = matchRepository.findByShow(show);

    VerticalLayout matchesLayout = new VerticalLayout();
    matchesLayout.setSpacing(false);
    matchesLayout.setPadding(false);
    matchesLayout.setSizeFull();
    matchesLayout.addClassNames(LumoUtility.Width.FULL);

    if (matches.isEmpty()) {
      Span noMatches = new Span("No matches scheduled for this show yet.");
      noMatches.addClassNames(LumoUtility.TextColor.SECONDARY);
      matchesLayout.add(noMatches);
    } else {
      // Create matches grid
      Grid<Match> matchesGrid = createMatchesGrid(matches);
      matchesGrid.setHeight("400px"); // Set a reasonable height for the grid
      matchesLayout.add(matchesGrid);
      matchesLayout.setFlexGrow(1, matchesGrid); // Let grid expand
    }

    card.add(header, matchesLayout);
    return card;
  }

  private Grid<Match> createMatchesGrid(List<Match> matches) {
    Grid<Match> grid = new Grid<>(Match.class, false);
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setItems(matches);
    grid.setSizeFull(); // Make grid use full available space
    grid.addClassNames(LumoUtility.Width.FULL);

    // Match type column
    grid.addColumn(match -> match.getMatchType().getName())
        .setHeader("Match Type")
        .setSortable(true)
        .setFlexGrow(2);

    // Participants column
    grid.addColumn(
            match -> {
              List<String> wrestlerNames =
                  match.getWrestlers().stream().map(wrestler -> wrestler.getName()).toList();
              return String.join(" vs ", wrestlerNames);
            })
        .setHeader("Participants")
        .setSortable(false)
        .setFlexGrow(4); // Give more space to participants

    // Winner column
    grid.addColumn(match -> match.getWinner() != null ? match.getWinner().getName() : "TBD")
        .setHeader("Winner")
        .setSortable(true)
        .setFlexGrow(2);

    // Match date column
    grid.addColumn(
            match ->
                match
                    .getMatchDate()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
        .setHeader("Date")
        .setSortable(true)
        .setFlexGrow(2);

    // Rating column
    grid.addColumn(
            match ->
                match.getMatchRating() != null ? "★".repeat(match.getMatchRating()) : "Not rated")
        .setHeader("Rating")
        .setSortable(true)
        .setFlexGrow(1);

    // Duration column (if available)
    grid.addColumn(
            match ->
                match.getDurationMinutes() != null ? match.getDurationMinutes() + " min" : "N/A")
        .setHeader("Duration")
        .setSortable(true)
        .setFlexGrow(1);

    return grid;
  }

  private void openAddMatchDialog(Show show) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Match to " + show.getName());
    dialog.setWidth("600px");
    dialog.setMaxWidth("90vw");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    // Match type selection
    ComboBox<MatchType> matchTypeCombo = new ComboBox<>("Match Type");
    matchTypeCombo.setItems(matchTypeRepository.findAll());
    matchTypeCombo.setItemLabelGenerator(MatchType::getName);
    matchTypeCombo.setWidthFull();
    matchTypeCombo.setRequired(true);

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

    // Match rating
    IntegerField ratingField = new IntegerField("Match Rating (1-5)");
    ratingField.setMin(1);
    ratingField.setMax(5);
    ratingField.setStepButtonsVisible(true);
    ratingField.setWidthFull();

    // Duration
    IntegerField durationField = new IntegerField("Duration (minutes)");
    durationField.setMin(1);
    durationField.setMax(180);
    durationField.setStepButtonsVisible(true);
    durationField.setWidthFull();

    formLayout.add(matchTypeCombo, wrestlersCombo, winnerCombo, ratingField, durationField);

    // Buttons
    Button saveButton =
        new Button(
            "Add Match",
            e -> {
              if (validateAndSaveMatch(
                  show,
                  matchTypeCombo.getValue(),
                  wrestlersCombo.getValue(),
                  winnerCombo.getValue(),
                  ratingField.getValue(),
                  durationField.getValue())) {
                dialog.close();
                // Refresh the matches display
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

  private boolean validateAndSaveMatch(
      Show show,
      MatchType matchType,
      java.util.Set<Wrestler> wrestlers,
      Wrestler winner,
      Integer rating,
      Integer duration) {
    // Validation
    if (matchType == null) {
      Notification.show("Please select a match type", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }

    if (wrestlers == null || wrestlers.isEmpty()) {
      Notification.show("Please select at least one wrestler", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.Lumo_ERROR);
      return false;
    }

    if (wrestlers.size() < 2) {
      Notification.show("Please select at least two wrestlers", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }

    if (winner != null && !wrestlers.contains(winner)) {
      Notification.show(
              "Winner must be one of the selected wrestlers", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }

    try {
      // Create match
      Match match = new Match();
      match.setShow(show);
      match.setMatchType(matchType);
      match.setMatchDate(java.time.Instant.now());
      match.setMatchRating(rating);
      match.setDurationMinutes(duration);
      match.setIsTitleMatch(false);
      match.setIsNpcGenerated(false);

      // Add participants
      for (Wrestler wrestler : wrestlers) {
        match.addParticipant(wrestler);
      }
      
      if(winner != null){
          match.setWinner(winner);
      }

      // Save the match
      matchRepository.save(match);

      Notification.show("Match added successfully!", 3000, Notification.Position.BOTTOM_START)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      return true;

    } catch (Exception e) {
      Notification.show("Error saving match: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return false;
    }
  }
}