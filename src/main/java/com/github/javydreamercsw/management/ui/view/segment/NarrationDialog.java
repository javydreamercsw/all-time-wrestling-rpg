package com.github.javydreamercsw.management.ui.view.segment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.util.UrlUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class NarrationDialog extends Dialog {

  private final Segment segment;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final WrestlerService wrestlerService;
  private final TitleService titleService;

  private final ProgressBar progressBar;
  private final Pre narrationDisplay;
  private final Button generateButton;
  private final Button saveButton;
  private final Button regenerateButton;
  private final TextArea feedbackArea;
  private final ComboBox<Npc> refereeField;
  private final ComboBox<Npc> commissionerField;
  private final MultiSelectComboBox<Npc> commentatorsField;
  private final ComboBox<Npc> ringAnnouncerField;
  private final MultiSelectComboBox<Npc> otherNpcsField;
  private final VerticalLayout teamsLayout;
  private final Consumer<Segment> onSaveCallback; // New field for callback

  public NarrationDialog(
      Segment segment,
      NpcService npcService,
      WrestlerService wrestlerService,
      TitleService titleService,
      Consumer<Segment> onSaveCallback) { // Modified constructor
    this.segment = segment;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.onSaveCallback = onSaveCallback; // Assign callback

    setHeaderTitle("Generate Narration for: " + segment.getSegmentType().getName());
    setWidth("800px");
    setMaxWidth("90vw");

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

    generateButton = new Button("Generate Narration");
    generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generateButton.addClickListener(e -> generateNarration());

    saveButton = new Button("Save Narration");
    saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    saveButton.addClickListener(e -> saveNarration());
    saveButton.setEnabled(false);

    feedbackArea = new TextArea("Feedback");
    feedbackArea.setWidthFull();
    feedbackArea.setPlaceholder("Provide feedback to the AI to improve the narration...");

    regenerateButton = new Button("Regenerate with Feedback");
    regenerateButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    regenerateButton.addClickListener(e -> generateNarration());

    refereeField = new ComboBox<>("Referee");
    refereeField.setItemLabelGenerator(Npc::getName);
    refereeField.setWidthFull();
    refereeField.setItems(npcService.findAllByType("Referee"));

    commissionerField = new ComboBox<>("Commissioner");
    commissionerField.setItemLabelGenerator(Npc::getName);
    commissionerField.setWidthFull();
    commissionerField.setItems(npcService.findAllByType("Commissioner"));

    commentatorsField = new MultiSelectComboBox<>("Commentators");
    commentatorsField.setItemLabelGenerator(Npc::getName);
    commentatorsField.setWidthFull();
    commentatorsField.setItems(npcService.findAllByType("Commentator"));

    ringAnnouncerField = new ComboBox<>("Ring Announcer");
    ringAnnouncerField.setItemLabelGenerator(Npc::getName);
    ringAnnouncerField.setWidthFull();
    ringAnnouncerField.setItems(npcService.findAllByType("Announcer"));

    otherNpcsField = new MultiSelectComboBox<>("Other NPCs");
    otherNpcsField.setItemLabelGenerator(Npc::getName);
    otherNpcsField.setWidthFull();
    otherNpcsField.setItems(npcService.findAllByType("Other"));

    teamsLayout = new VerticalLayout();
    teamsLayout.setSpacing(true);
    teamsLayout.setPadding(false);

    // Conditional logic for existing narration
    if (segment.getNarration() != null && !segment.getNarration().isEmpty()) {
      narrationDisplay.setText(segment.getNarration());
      saveButton.setEnabled(true); // Enable save button if narration already exists
    }

    for (com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler :
        segment.getWrestlers()) {
      addTeamSelector(new WrestlerDTO(wrestler));
    }

    VerticalLayout layout =
        new VerticalLayout(
            progressBar,
            narrationDisplay,
            teamsLayout,
            refereeField,
            commissionerField,
            commentatorsField,
            ringAnnouncerField,
            otherNpcsField,
            feedbackArea,
            regenerateButton);
    add(layout);
    getFooter().add(generateButton, saveButton, new Button("Close", e -> close()));
  }

  private void addTeamSelector(WrestlerDTO wrestler) {
    int teamNumber = teamsLayout.getComponentCount() + 1;
    MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
        new MultiSelectComboBox<>("Team " + teamNumber);
    wrestlersCombo.setItemLabelGenerator(WrestlerDTO::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setItems(wrestlerService.findAllAsDTO());
    if (wrestler != null) {
      wrestlersCombo.setValue(new java.util.HashSet<>(List.of(wrestler)));
    }

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

  private void generateNarration() {
    showProgress(true);

    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();
      log.debug("Sending narration context to AI: {}", context);

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

  private SegmentNarrationService.SegmentNarrationContext buildSegmentContext() {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    // Populate titles
    List<SegmentNarrationService.TitleContext> titleContexts = new ArrayList<>();
    titleService
        .findAll()
        .forEach(
            title -> {
              SegmentNarrationService.TitleContext tc = new SegmentNarrationService.TitleContext();
              tc.setName(title.getName());
              tc.setCurrentHolderName(title.isVacant() ? "Vacant" : title.getChampionNames());
              tc.setTier(
                  title
                      .getTier()
                      .name()); // Assuming Title has a getTier() method returning an enum
              titleContexts.add(tc);
            });
    context.setTitles(titleContexts);

    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();
    for (int i = 0; i < teamsLayout.getComponentCount(); i++) {
      HorizontalLayout teamSelector = (HorizontalLayout) teamsLayout.getComponentAt(i);
      MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
          (MultiSelectComboBox<WrestlerDTO>) teamSelector.getComponentAt(0);
      for (WrestlerDTO wrestler : wrestlersCombo.getValue()) {
        SegmentNarrationService.WrestlerContext wc = new SegmentNarrationService.WrestlerContext();
        wc.setName(wrestler.getName());
        wc.setDescription(wrestler.getDescription());
        wc.setTeam("Team " + (i + 1));
        wc.setGender(wrestler.getGender()); // Set gender
        wc.setTier(wrestler.getTier()); // Set tier
        wc.setMoveSet(wrestler.getMoveSet()); // Add this line
        wrestlerContexts.add(wc);
      }
    }
    context.setWrestlers(wrestlerContexts);

    SegmentNarrationService.SegmentTypeContext mtc =
        new SegmentNarrationService.SegmentTypeContext();
    mtc.setSegmentType(segment.getSegmentType().getName());
    context.setSegmentType(mtc);

    if (refereeField.getValue() != null) {
      SegmentNarrationService.RefereeContext referee = new SegmentNarrationService.RefereeContext();
      referee.setName(refereeField.getValue().getName());
      referee.setDescription("Experienced wrestling referee");
      context.setReferee(referee);
    }

    List<SegmentNarrationService.NPCContext> npcs = new ArrayList<>();
    if (commissionerField.getValue() != null) {
      SegmentNarrationService.NPCContext commissioner = new SegmentNarrationService.NPCContext();
      commissioner.setName(commissionerField.getValue().getName());
      commissioner.setRole("Commissioner");
      commissioner.setDescription("Wrestling commissioner");
      npcs.add(commissioner);
    }

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

    if (ringAnnouncerField.getValue() != null) {
      SegmentNarrationService.NPCContext npc = new SegmentNarrationService.NPCContext();
      npc.setName(ringAnnouncerField.getValue().getName());
      npc.setRole("Ring Announcer");
      npc.setDescription("Wrestling ring announcer");
      npcs.add(npc);
    }

    if (!otherNpcsField.getValue().isEmpty()) {
      npcs.addAll(
          otherNpcsField.getValue().stream()
              .map(
                  otherNpc -> {
                    SegmentNarrationService.NPCContext npc =
                        new SegmentNarrationService.NPCContext();
                    npc.setName(otherNpc.getName());
                    npc.setRole(otherNpc.getNpcType()); // Use the NPC's defined type as role
                    return npc;
                  })
              .toList());
    }
    context.setNpcs(npcs);

    // Add explicit instructions for the AI
    context.setInstructions(
        "You will be provided with a context object in JSON format.\n"
            + "The fields in this JSON object are described below.\n"
            + "Please adhere to the following rules when generating the narration:\n\n"
            + "1.  **Participants & Roles:** The characters physically present and acting in this"
            + " segment are exclusively those in the 'wrestlers' and 'npcs' lists. Wrestlers from"
            + " the 'fullRoster' who are not in the 'wrestlers' list are not present and cannot act"
            + " or speak, but they can be mentioned or referenced (e.g., in an announcement for a"
            + " future match).\n"
            + "2.  **Data Integrity:** All characters, champions, and contenders mentioned in the"
            + " narration MUST come from the 'fullRoster', 'npcs', and 'titles' lists provided. If"
            + " a title's champion is not specified or is empty, you must state that it is vacant"
            + " and not invent a champion. Do not invent new names or assume the existence of"
            + " characters not listed.\n"
            + "3.  **Empty Wrestler List:** If the 'wrestlers' list is empty, it signifies that no"
            + " wrestlers are physically present. The segment should only feature the characters"
            + " from the 'npcs' list.\n"
            + "4.  **No New Characters:** Do not invent or introduce any characters not listed in"
            + " the provided context. This is a strict rule.");

    StringBuilder outcomeBuilder = new StringBuilder();

    // Prioritize assigned winners
    List<String> winners =
        segment.getParticipants().stream()
            .filter(p -> p.getIsWinner() != null && p.getIsWinner())
            .map(p -> p.getWrestler().getName())
            .collect(java.util.stream.Collectors.toList());

    if (!winners.isEmpty()) {
      outcomeBuilder.append(String.join(" and ", winners)).append(" wins the segment.");
      // TODO: Add more detail about how they won (e.g., by pinfall, submission) if available
    } else if (segment.getSummary() != null && !segment.getSummary().isEmpty()) {
      outcomeBuilder.append(segment.getSummary());
    }

    // Append user feedback
    if (!feedbackArea.isEmpty()) {
      if (!outcomeBuilder.isEmpty()) {
        outcomeBuilder.append("\n\n");
      }
      outcomeBuilder.append("User Feedback: ").append(feedbackArea.getValue());
    }

    if (!outcomeBuilder.isEmpty()) {
      context.setDeterminedOutcome(outcomeBuilder.toString());
    }

    return context;
  }

  private void handleNarrationResponse(String response) {
    try {
      JsonNode jsonResponse = objectMapper.readTree(response);
      String narration =
          jsonResponse.has("narration") ? jsonResponse.get("narration").asText() : response;
      narrationDisplay.setText(narration);
      saveButton.setEnabled(true);
    } catch (Exception e) {
      log.error("Error parsing narration response", e);
      narrationDisplay.setText(response);
    }
  }

  private void showProgress(boolean show) {
    progressBar.setVisible(show);
    generateButton.setEnabled(!show);
    saveButton.setEnabled(!show);
    regenerateButton.setEnabled(!show);
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

  private void retryWithProvider(String provider) {
    showProgress(true);

    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();
      log.info(
          "Retrying narration with provider {} and context: {}",
          provider,
          context); // Add this line

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

  private void saveNarration() {
    try {
      String baseUrl = UrlUtil.getBaseUrl();
      restTemplate.put(
          baseUrl + "/api/segments/" + segment.getId() + "/narration", narrationDisplay.getText());
      Notification.show("Narration saved successfully!", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      onSaveCallback.accept(segment); // Call the callback with the updated segment
      close();
    } catch (Exception e) {
      log.error("Error saving narration", e);
      showError("Failed to save narration: " + e.getMessage());
    }
  }
}
