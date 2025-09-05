package com.github.javydreamercsw.management.ui.view.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchNarrationContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchTypeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * View for generating AI-powered wrestling match narrations. Allows users to configure match
 * parameters and generate detailed match stories.
 */
@Route("match-narration")
@PageTitle("Match Narration")
@Menu(order = 5, icon = "vaadin:microphone", title = "Match Narration")
@PermitAll
@Slf4j
public class MatchNarrationView extends Main {

  private final WrestlerService wrestlerService;
  private final MatchTypeService matchTypeService;
  private final NpcService npcService;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  // Form components
  private MultiSelectComboBox<Wrestler> wrestlersCombo;
  private ComboBox<MatchType> matchTypeCombo;
  private TextField venueField;
  private TextField audienceField;
  private TextArea outcomeField;
  private ComboBox<Npc> refereeField;
  private MultiSelectComboBox<Npc> commentatorsField;
  private ComboBox<Npc> ringAnnouncerField;
  private Button generateButton;
  private Button testButton;

  // Results components
  private VerticalLayout resultsSection;
  private ProgressBar progressBar;
  private Pre narrationDisplay;
  private Div costDisplay;
  private Div providerDisplay;

  public MatchNarrationView(
      WrestlerService wrestlerService, MatchTypeService matchTypeService, NpcService npcService) {
    this.wrestlerService = wrestlerService;
    this.matchTypeService = matchTypeService;
    this.npcService = npcService;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();

    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Wrestlers selection
    wrestlersCombo = new MultiSelectComboBox<>("Select Wrestlers");
    wrestlersCombo.setItems(wrestlerService.findAll());
    wrestlersCombo.setItemLabelGenerator(Wrestler::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setRequired(true);
    wrestlersCombo.setHelperText("Select 2 or more wrestlers for the match");

    // Match type selection
    matchTypeCombo = new ComboBox<>("Match Type");
    matchTypeCombo.setItems(matchTypeService.findAll());
    matchTypeCombo.setItemLabelGenerator(MatchType::getName);
    matchTypeCombo.setWidthFull();
    matchTypeCombo.setRequired(true);

    // Venue configuration
    venueField = new TextField("Venue");
    venueField.setWidthFull();
    venueField.setPlaceholder("e.g., Madison Square Garden");
    venueField.setValue("Madison Square Garden");

    // Audience description
    audienceField = new TextField("Audience");
    audienceField.setWidthFull();
    audienceField.setPlaceholder("e.g., Sold-out crowd of 20,000");
    audienceField.setValue("Sold-out crowd of 20,000");

    // Match outcome
    outcomeField = new TextArea("Determined Outcome");
    outcomeField.setWidthFull();
    outcomeField.setHeight("100px");
    outcomeField.setPlaceholder("Describe how the match should end...");
    outcomeField.setHelperText("Optional: Specify the match outcome for more controlled narration");

    // Referee
    refereeField = new ComboBox<>("Referee");
    refereeField.setItems(npcService.findAllByType("Referee"));
    refereeField.setItemLabelGenerator(Npc::getName);
    refereeField.setWidthFull();

    // Commentators
    commentatorsField = new MultiSelectComboBox<>("Commentators");
    commentatorsField.setItems(npcService.findAllByType("Commentator"));
    commentatorsField.setItemLabelGenerator(Npc::getName);
    commentatorsField.setWidthFull();

    // Ring Announcer
    ringAnnouncerField = new ComboBox<>("Ring Announcer");
    ringAnnouncerField.setItems(npcService.findAllByType("Ring Announcer"));
    ringAnnouncerField.setItemLabelGenerator(Npc::getName);
    ringAnnouncerField.setWidthFull();

    // Action buttons
    generateButton = new Button("Generate Match Narration", new Icon(VaadinIcon.PLAY));
    generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateButton.addClickListener(e -> generateNarration());

    testButton = new Button("Test with Sample Match", new Icon(VaadinIcon.FLASK));
    testButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    testButton.addClickListener(e -> generateTestNarration());

    // Results components
    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);

    narrationDisplay = new Pre();
    narrationDisplay.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.FontSize.SMALL);
    narrationDisplay.getStyle().set("white-space", "pre-wrap");
    narrationDisplay.getStyle().set("max-height", "500px");
    narrationDisplay.getStyle().set("overflow-y", "auto");

    costDisplay = new Div();
    costDisplay.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    providerDisplay = new Div();
    providerDisplay.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.PRIMARY);

    resultsSection = new VerticalLayout();
    resultsSection.setVisible(false);
    resultsSection.setSpacing(true);
    resultsSection.setPadding(false);
  }

  private void setupLayout() {
    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    // Header
    add(new ViewToolbar("Match Narration Generator"));

    // Configuration form
    FormLayout formLayout = new FormLayout();
    formLayout.add(
        wrestlersCombo,
        matchTypeCombo,
        venueField,
        audienceField,
        outcomeField,
        refereeField,
        commentatorsField,
        ringAnnouncerField);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(outcomeField, 2);
    formLayout.setColspan(commentatorsField, 2);

    // Action buttons
    HorizontalLayout buttonLayout = new HorizontalLayout(generateButton, testButton);
    buttonLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

    // Configuration section
    VerticalLayout configSection = new VerticalLayout();
    configSection.setSpacing(true);
    configSection.setPadding(false);
    configSection.add(new H3("Match Configuration"), formLayout, buttonLayout, progressBar);

    // Results section setup
    resultsSection.add(
        new H3("Generated Narration"), providerDisplay, costDisplay, narrationDisplay);

    add(configSection, resultsSection);
  }

  private void generateNarration() {
    if (!validateForm()) {
      return;
    }

    showProgress(true);

    try {
      MatchNarrationContext context = buildMatchContext();

      // Call the REST API
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              "http://localhost:8080/api/match-narration/narrate", context, String.class);

      handleNarrationResponse(response.getBody());

    } catch (Exception e) {
      log.error("Error generating match narration", e);
      showError("Failed to generate narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  private void generateTestNarration() {
    showProgress(true);

    try {
      // Call the test endpoint using POST method
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              "http://localhost:8080/api/match-narration/test", null, String.class);

      handleNarrationResponse(response.getBody());

    } catch (Exception e) {
      log.error("Error generating test narration", e);
      showError("Failed to generate test narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  private boolean validateForm() {
    Set<Wrestler> selectedWrestlers = wrestlersCombo.getValue();
    if (selectedWrestlers == null || selectedWrestlers.size() < 2) {
      showError("Please select at least 2 wrestlers for the match");
      return false;
    }

    if (matchTypeCombo.getValue() == null) {
      showError("Please select a match type");
      return false;
    }

    return true;
  }

  private MatchNarrationContext buildMatchContext() {
    MatchNarrationContext context = new MatchNarrationContext();

    // Set wrestlers
    List<WrestlerContext> wrestlerContexts = new ArrayList<>();
    for (Wrestler wrestler : wrestlersCombo.getValue()) {
      WrestlerContext wc = new WrestlerContext();
      wc.setName(wrestler.getName());
      wc.setDescription(
          wrestler.getDescription() != null ? wrestler.getDescription() : "Determined competitor");
      wrestlerContexts.add(wc);
    }
    context.setWrestlers(wrestlerContexts);

    // Set match type
    MatchType selectedType = matchTypeCombo.getValue();
    MatchTypeContext mtc = new MatchTypeContext();
    mtc.setMatchType(selectedType.getName());
    // Note: MatchTypeContext doesn't have setDescription, using setStipulation instead
    if (selectedType.getDescription() != null && !selectedType.getDescription().trim().isEmpty()) {
      mtc.setStipulation(selectedType.getDescription());
    }
    context.setMatchType(mtc);

    // Set venue
    VenueContext venue = new VenueContext();
    venue.setName(venueField.getValue());
    venue.setDescription("Iconic wrestling venue");
    context.setVenue(venue);

    // Set audience
    context.setAudience(audienceField.getValue());

    // Set outcome if provided
    if (!outcomeField.getValue().trim().isEmpty()) {
      context.setDeterminedOutcome(outcomeField.getValue());
    }

    // Set referee
    if (refereeField.getValue() != null) {
      RefereeContext referee = new RefereeContext();
      referee.setName(refereeField.getValue().getName());
      referee.setDescription("Experienced wrestling referee");
      context.setReferee(referee);
    }

    List<NPCContext> npcs = new ArrayList<>();
    // Set commentators
    if (!commentatorsField.getValue().isEmpty()) {
      npcs.addAll(
          commentatorsField.getValue().stream()
              .map(
                  commentator -> {
                    NPCContext npc = new NPCContext();
                    npc.setName(commentator.getName());
                    npc.setRole("Commentator");
                    npc.setDescription("Wrestling commentator");
                    return npc;
                  })
              .collect(Collectors.toList()));
    }

    // Set Ring Announcer
    if (ringAnnouncerField.getValue() != null) {
      NPCContext npc = new NPCContext();
      npc.setName(ringAnnouncerField.getValue().getName());
      npc.setRole("Ring Announcer");
      npc.setDescription("Wrestling ring announcer");
      npcs.add(npc);
    }
    context.setNpcs(npcs);

    return context;
  }

  private void handleNarrationResponse(String response) {
    try {
      JsonNode jsonResponse = objectMapper.readTree(response);

      // Extract narration text
      String narration =
          jsonResponse.has("narration") ? jsonResponse.get("narration").asText() : response;

      // Extract provider info
      String provider =
          jsonResponse.has("provider") ? jsonResponse.get("provider").asText() : "Unknown";

      // Extract cost info
      double cost =
          jsonResponse.has("estimatedCost") ? jsonResponse.get("estimatedCost").asDouble() : 0.0;

      // Display results
      narrationDisplay.setText(narration);
      providerDisplay.setText("Provider: " + provider);
      costDisplay.setText(String.format("Estimated cost: $%.4f", cost));

      resultsSection.setVisible(true);

      Notification.show(
              "Match narration generated successfully!", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

    } catch (Exception e) {
      log.error("Error parsing narration response", e);
      // Fallback to displaying raw response
      narrationDisplay.setText(response);
      providerDisplay.setText("Provider: Unknown");
      costDisplay.setText("Estimated cost: $0.00");
      resultsSection.setVisible(true);
    }
  }

  private void showProgress(boolean show) {
    progressBar.setVisible(show);
    generateButton.setEnabled(!show);
    testButton.setEnabled(!show);
  }

  private void showError(String message) {
    Notification.show(message, 5000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
