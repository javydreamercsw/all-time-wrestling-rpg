package com.github.javydreamercsw.management.ui.view.segment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.util.UrlUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Route("segment-narration")
@PageTitle("Segment Narration")
@Menu(order = 5, icon = "vaadin:microphone", title = "Segment Narration")
@PermitAll
@Slf4j
public class SegmentNarrationView extends Main {

  private final WrestlerService wrestlerService;
  private final SegmentTypeService segmentTypeService;
  private final NpcService npcService;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  // Form components
  private VerticalLayout teamsLayout;
  private Button addTeamButton;
  private ComboBox<SegmentType> segmentTypeCombo;
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
  private Checkbox showContextCheckbox;
  private TextArea contextDisplay;

  public SegmentNarrationView(
      WrestlerService wrestlerService,
      SegmentTypeService segmentTypeService,
      NpcService npcService) {
    this.wrestlerService = wrestlerService;
    this.segmentTypeService = segmentTypeService;
    this.npcService = npcService;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  @PostConstruct
  private void init() {
    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Wrestlers selection
    teamsLayout = new VerticalLayout();
    teamsLayout.setSpacing(true);
    teamsLayout.setPadding(false);

    addTeamButton = new Button("Add Team", new Icon(VaadinIcon.PLUS));
    addTeamButton.addClickListener(e -> addTeamSelector());

    // Add two teams by default
    addTeamSelector();
    addTeamSelector();

    // Segment type selection
    segmentTypeCombo = new ComboBox<>("Segment Type");
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);

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

    // Segment outcome
    outcomeField = new TextArea("Determined Outcome");
    outcomeField.setWidthFull();
    outcomeField.setHeight("100px");
    outcomeField.setPlaceholder("Describe how the segment should end...");
    outcomeField.setHelperText(
        "Optional: Specify the segment outcome for more controlled narration");

    // Referee
    refereeField = new ComboBox<>("Referee");
    refereeField.setItemLabelGenerator(Npc::getName);
    refereeField.setWidthFull();

    // Commentators
    commentatorsField = new MultiSelectComboBox<>("Commentators");
    commentatorsField.setItemLabelGenerator(Npc::getName);
    commentatorsField.setWidthFull();

    // Ring Announcer
    ringAnnouncerField = new ComboBox<>("Ring Announcer");
    ringAnnouncerField.setItemLabelGenerator(Npc::getName);
    ringAnnouncerField.setWidthFull();

    // Action buttons
    generateButton = new Button("Generate Segment Narration", new Icon(VaadinIcon.PLAY));
    generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateButton.addClickListener(e -> generateNarration());

    testButton = new Button("Test with Sample Segment", new Icon(VaadinIcon.FLASK));
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

    showContextCheckbox = new Checkbox("Show AI Context");
    contextDisplay = new TextArea("AI Context");
    contextDisplay.setWidthFull();
    contextDisplay.setReadOnly(true);
    contextDisplay.setVisible(false); // Initially hidden

    showContextCheckbox.addValueChangeListener(
        event -> contextDisplay.setVisible(event.getValue()));

    resultsSection = new VerticalLayout();
    resultsSection.setVisible(false);
    resultsSection.setSpacing(true);
    resultsSection.setPadding(false);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Load data when the view is attached to the UI
    List<WrestlerDTO> wrestlers = wrestlerService.findAllAsDTO();
    teamsLayout
        .getChildren()
        .forEach(
            teamSelector -> {
              MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
                  (MultiSelectComboBox<WrestlerDTO>)
                      ((HorizontalLayout) teamSelector).getComponentAt(0);
              wrestlersCombo.setItems(wrestlers);
            });
    segmentTypeCombo.setItems(segmentTypeService.findAll());
    refereeField.setItems(npcService.findAllByType("Referee"));
    commentatorsField.setItems(npcService.findAllByType("Commentator"));
    ringAnnouncerField.setItems(npcService.findAllByType("Announcer"));
  }

  private void addTeamSelector() {
    int teamNumber = teamsLayout.getComponentCount() + 1;
    MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
        new MultiSelectComboBox<>("Team " + teamNumber);
    wrestlersCombo.setItemLabelGenerator(WrestlerDTO::getName);
    wrestlersCombo.setWidthFull();

    Button removeTeamButton = new Button(new Icon(VaadinIcon.MINUS));
    removeTeamButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    HorizontalLayout teamSelector = new HorizontalLayout(wrestlersCombo, removeTeamButton);
    teamSelector.setFlexGrow(1, wrestlersCombo);
    removeTeamButton.addClickListener(
        e -> {
          teamsLayout.remove(teamSelector);
          updateTeamLabels();
        });

    teamsLayout.add(teamSelector);
  }

  private void updateTeamLabels() {
    for (int i = 0; i < teamsLayout.getComponentCount(); i++) {
      HorizontalLayout teamSelector = (HorizontalLayout) teamsLayout.getComponentAt(i);
      MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
          (MultiSelectComboBox<WrestlerDTO>) teamSelector.getComponentAt(0);
      wrestlersCombo.setLabel("Team " + (i + 1));
    }
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
    add(new ViewToolbar("Segment Narration Generator"));

    // Configuration form
    FormLayout formLayout = new FormLayout();
    formLayout.add(
        teamsLayout,
        addTeamButton,
        segmentTypeCombo,
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
    configSection.add(
        new H3("Segment Configuration"),
        formLayout,
        showContextCheckbox, // Moved here
        buttonLayout,
        progressBar);

    // Results section setup
    resultsSection.add(
        new H3("Generated Narration"),
        providerDisplay,
        costDisplay,
        narrationDisplay,
        contextDisplay); // Checkbox removed from here

    add(configSection, resultsSection);
  }

  private void generateNarration() {
    if (!validateForm()) {
      return;
    }

    showProgress(true);

    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();

      // Display the context if the checkbox is checked
      try {
        String contextJson =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        contextDisplay.setValue(contextJson);
      } catch (Exception e) {
        log.error("Error serializing segment context to JSON", e);
        contextDisplay.setValue("Error displaying context: " + e.getMessage());
      }

      // Call the REST API
      String baseUrl = UrlUtil.getBaseUrl();
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              baseUrl + "/api/segment-narration/narrate", context, String.class);

      handleNarrationResponse(response.getBody());

    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error("HTTP Client Error generating segment narration", e);
      try {
        JsonNode errorResponse = objectMapper.readTree(e.getResponseBodyAsString());
        if (errorResponse.has("alternativeProviders")) {
          showRetryDialog(errorResponse);
        } else {
          showError(
              "Failed to generate narration: "
                  + errorResponse.path("error").asText("Unknown error"));
        }
      } catch (Exception ex) {
        log.error("Error parsing error response", ex);
        showError("Failed to generate narration: " + e.getMessage());
      }
    } catch (Exception e) {
      log.error("Error generating segment narration", e);
      showError("Failed to generate narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  private void generateTestNarration() {
    showProgress(true);

    try {
      // Call the test endpoint using POST method
      String baseUrl = UrlUtil.getBaseUrl();
      ResponseEntity<String> response =
          restTemplate.postForEntity(baseUrl + "/api/segment-narration/test", null, String.class);

      handleNarrationResponse(response.getBody());

    } catch (Exception e) {
      log.error("Error generating test narration", e);
      showError("Failed to generate test narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  private boolean validateForm() {
    if (teamsLayout.getComponentCount() < 2) {
      showError("Please add at least 2 teams");
      return false;
    }

    for (int i = 0; i < teamsLayout.getComponentCount(); i++) {
      HorizontalLayout teamSelector = (HorizontalLayout) teamsLayout.getComponentAt(i);
      MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
          (MultiSelectComboBox<WrestlerDTO>) teamSelector.getComponentAt(0);
      if (wrestlersCombo.getValue().isEmpty()) {
        showError("Please select at least 1 wrestler for Team " + (i + 1));
        return false;
      }
    }

    if (segmentTypeCombo.getValue() == null) {
      showError("Please select a segment type");
      return false;
    }

    return true;
  }

  private SegmentNarrationService.SegmentNarrationContext buildSegmentContext() {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    // Set wrestlers
    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();
    for (int i = 0; i < teamsLayout.getComponentCount(); i++) {
      HorizontalLayout teamSelector = (HorizontalLayout) teamsLayout.getComponentAt(i);
      MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
          (MultiSelectComboBox<WrestlerDTO>) teamSelector.getComponentAt(0);
      for (WrestlerDTO wrestler : wrestlersCombo.getValue()) {
        SegmentNarrationService.WrestlerContext wc = new SegmentNarrationService.WrestlerContext();
        wc.setName(wrestler.getName());
        wc.setDescription(
            wrestler.getDescription() != null
                ? wrestler.getDescription()
                : "Determined competitor");
        wc.setTeam("Team " + (i + 1));
        wrestlerContexts.add(wc);
      }
    }
    context.setWrestlers(wrestlerContexts);

    // Set segment type
    SegmentType selectedType = segmentTypeCombo.getValue();
    SegmentNarrationService.SegmentTypeContext mtc =
        new SegmentNarrationService.SegmentTypeContext();
    mtc.setSegmentType(selectedType.getName());
    // Note: SegmentTypeContext doesn't have setDescription, using setStipulation instead
    if (selectedType.getDescription() != null && !selectedType.getDescription().trim().isEmpty()) {
      mtc.setStipulation(selectedType.getDescription());
    }
    context.setSegmentType(mtc);

    // Set venue
    SegmentNarrationService.VenueContext venue = new SegmentNarrationService.VenueContext();
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
      SegmentNarrationService.RefereeContext referee = new SegmentNarrationService.RefereeContext();
      referee.setName(refereeField.getValue().getName());
      referee.setDescription("Experienced wrestling referee");
      context.setReferee(referee);
    }

    List<SegmentNarrationService.NPCContext> npcs = new ArrayList<>();
    // Set commentators
    if (!commentatorsField.getValue().isEmpty()) {
      npcs.addAll(
          commentatorsField.getValue().stream()
              .map(
                  commentator -> {
                    SegmentNarrationService.NPCContext npc =
                        new SegmentNarrationService.NPCContext();
                    npc.setName(commentator.getName());
                    npc.setRole("Commentator");
                    npc.setDescription("Wrestling commentator");
                    return npc;
                  })
              .toList());
    }

    // Set Ring Announcer
    if (ringAnnouncerField.getValue() != null) {
      SegmentNarrationService.NPCContext npc = new SegmentNarrationService.NPCContext();
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

      // Display context if available in the response
      if (jsonResponse.has("context")) {
        try {
          String contextJson =
              objectMapper
                  .writerWithDefaultPrettyPrinter()
                  .writeValueAsString(jsonResponse.get("context"));
          contextDisplay.setValue(contextJson);
        } catch (Exception e) {
          log.error("Error serializing context from response to JSON", e);
          contextDisplay.setValue("Error displaying context: " + e.getMessage());
        }
      }

      // Display results
      narrationDisplay.setText(narration);
      providerDisplay.setText("Provider: " + provider);
      costDisplay.setText(String.format("Estimated cost: $%.4f", cost));

      resultsSection.setVisible(true);

      Notification.show(
              "Segment narration generated successfully!", 3000, Notification.Position.BOTTOM_END)
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

  private void showRetryDialog(JsonNode errorResponse) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Narration Failed");

    VerticalLayout content = new VerticalLayout();
    content.add(
        new com.vaadin.flow.component.html.Span(
            errorResponse.path("error").asText("Unknown error")));

    JsonNode alternativeProviders = errorResponse.path("alternativeProviders");
    if (alternativeProviders.isArray()) {
      content.add(new H3("Try with another provider:"));
      for (JsonNode providerNode : alternativeProviders) {
        String provider = providerNode.path("provider").asText();
        double estimatedCost = providerNode.path("estimatedCost").asDouble();
        Button retryButton =
            new Button(
                "Retry with " + provider + String.format(" (~$%.4f)", estimatedCost),
                e -> {
                  retryWithProvider(provider);
                  dialog.close();
                });
        content.add(retryButton);
      }
    }

    dialog.add(content);
    dialog.getFooter().add(new Button("Cancel", e -> dialog.close()));
    dialog.open();
  }

  private void retryWithProvider(@NonNull String provider) {
    if (!validateForm()) {
      return;
    }

    showProgress(true);

    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();

      // Call the REST API with the specified provider
      String baseUrl = UrlUtil.getBaseUrl();
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              baseUrl + "/api/segment-narration/narrate/" + provider, context, String.class);

      handleNarrationResponse(response.getBody());

    } catch (org.springframework.web.client.HttpClientErrorException e) {
      log.error("HTTP Client Error retrying with provider {}: {}", provider, e.getMessage(), e);
      try {
        JsonNode errorResponse = objectMapper.readTree(e.getResponseBodyAsString());
        if (errorResponse.has("alternativeProviders")) {
          showRetryDialog(errorResponse);
        } else {
          showError(
              "Failed to generate narration: "
                  + errorResponse.path("error").asText("Unknown error"));
        }
      } catch (Exception ex) {
        log.error("Error parsing error response", ex);
        showError("Failed to generate narration: " + e.getMessage());
      }
    } catch (Exception e) {
      log.error("Error retrying with provider: " + provider, e);
      showError("Failed to generate narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }
}
