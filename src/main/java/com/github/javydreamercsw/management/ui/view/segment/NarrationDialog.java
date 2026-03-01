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
package com.github.javydreamercsw.management.ui.view.segment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationController;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class NarrationDialog extends Dialog {

  private final Segment segment;
  private final ObjectMapper objectMapper;
  private final WrestlerService wrestlerService;
  private final ShowService showService;
  private final SegmentService segmentService;
  private final RivalryService rivalryService;
  private final RingsideActionService ringsideActionService;
  private final SegmentNarrationServiceFactory aiFactory;

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
  private final Consumer<Segment> onSaveCallback;
  private final SegmentNarrationController segmentNarrationController;

  public NarrationDialog(
      Segment segment,
      NpcService npcService,
      WrestlerService wrestlerService,
      ShowService showService,
      SegmentService segmentService,
      Consumer<Segment> onSaveCallback,
      RivalryService rivalryService,
      SegmentNarrationController segmentNarrationController,
      SegmentNarrationServiceFactory aiFactory,
      RingsideActionService ringsideActionService) {
    this.segmentService = segmentService;
    this.segment = segmentService.findByIdWithDetails(segment.getId()).orElse(segment);
    this.objectMapper = new ObjectMapper();
    this.wrestlerService = wrestlerService;
    this.showService = showService;
    this.onSaveCallback = onSaveCallback;
    this.rivalryService = rivalryService;
    this.segmentNarrationController = segmentNarrationController;
    this.aiFactory = aiFactory;
    this.ringsideActionService = ringsideActionService;

    setHeaderTitle("Generate Narration for: " + segment.getSegmentType().getName());
    setWidth("800px");
    setMaxWidth("90vw");
    setId("narration-dialog");

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
    generateButton.setId("generate-narration-button");
    generateButton.addClickListener(e -> generateNarration());

    saveButton = new Button("Save Narration");
    saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    saveButton.addClickListener(e -> saveNarration());
    saveButton.setId("save-narration-button");
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
    List<Npc> referees =
        npcService.findAllByType("Referee").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList());
    refereeField.setItems(referees);

    Button randomRefereeButton = new Button(new Icon(VaadinIcon.RANDOM));
    randomRefereeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    randomRefereeButton.setId("random-referee-button");
    randomRefereeButton.addClickListener(
        e -> {
          if (!referees.isEmpty()) {
            int randomIndex = (int) (Math.random() * referees.size());
            refereeField.setValue(referees.get(randomIndex));
          } else {
            Notification.show(
                "No referees available to select randomly.",
                3000,
                Notification.Position.BOTTOM_START);
          }
        });
    HorizontalLayout refereeLayout = new HorizontalLayout(refereeField, randomRefereeButton);
    refereeLayout.setWidthFull();
    refereeLayout.setFlexGrow(1, refereeField);

    commissionerField = new ComboBox<>("Commissioner");
    commissionerField.setItemLabelGenerator(Npc::getName);
    commissionerField.setWidthFull();
    commissionerField.setItems(
        npcService.findAllByType("Commissioner").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList()));

    commentatorsField = new MultiSelectComboBox<>("Commentators");
    commentatorsField.setItemLabelGenerator(Npc::getName);
    commentatorsField.setWidthFull();
    List<Npc> commentators =
        npcService.findAllByType("Commentator").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList());
    commentatorsField.setItems(commentators);
    commentatorsField.setValue(new HashSet<>(commentators));

    ringAnnouncerField = new ComboBox<>("Ring Announcer");
    ringAnnouncerField.setItemLabelGenerator(Npc::getName);
    ringAnnouncerField.setWidthFull();
    ringAnnouncerField.setItems(
        npcService.findAllByType("Announcer").stream()
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList()));

    otherNpcsField = new MultiSelectComboBox<>("Other NPCs");
    otherNpcsField.setItemLabelGenerator(Npc::getName);
    otherNpcsField.setWidthFull();
    List<Npc> allNpcs = npcService.findAll();
    List<Npc> otherNpcs =
        allNpcs.stream()
            .filter(
                npc ->
                    !npc.getNpcType().equals("Referee")
                        && !npc.getNpcType().equals("Commissioner")
                        && !npc.getNpcType().equals("Commentator")
                        && !npc.getNpcType().equals("Announcer"))
            .sorted(Comparator.comparing(Npc::getName))
            .collect(Collectors.toList());
    otherNpcsField.setItems(otherNpcs);

    teamsLayout = new VerticalLayout();
    teamsLayout.setSpacing(true);
    teamsLayout.setPadding(false);

    if (segment.getNarration() != null && !segment.getNarration().isEmpty()) {
      narrationDisplay.setText(segment.getNarration());
      saveButton.setEnabled(true);
    }
    if (segment.getReferee() != null) {
      refereeField.setValue(segment.getReferee());
    }
    wrestlerService.findAllBySegment(segment).forEach(this::addTeamSelector);

    VerticalLayout layout =
        new VerticalLayout(
            progressBar,
            narrationDisplay,
            teamsLayout,
            refereeLayout,
            commissionerField,
            commentatorsField,
            ringAnnouncerField,
            otherNpcsField,
            feedbackArea,
            regenerateButton);
    add(layout);
    getFooter().add(generateButton, saveButton, new Button("Close", e -> close()));
  }

  private void addTeamSelector(@NonNull WrestlerDTO wrestler) {
    int teamNumber = teamsLayout.getComponentCount() + 1;
    MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
        new MultiSelectComboBox<>("Team " + teamNumber);
    wrestlersCombo.setItemLabelGenerator(WrestlerDTO::getName);
    wrestlersCombo.setWidthFull();
    wrestlersCombo.setItems(
        wrestlerService.findAllAsDTO().stream()
            .sorted(Comparator.comparing(WrestlerDTO::getName))
            .collect(Collectors.toList()));
    wrestlersCombo.setValue(new HashSet<>(List.of(wrestler)));

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
    if (aiFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      String reason = "No AI providers are currently enabled or reachable.";
      Notification.show(reason, 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    showProgress(true);
    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();
      log.debug("Sending narration context to AI: {}", context);

      ResponseEntity<Map<String, Object>> response =
          segmentNarrationController.narrateSegment(context);

      if (response.getStatusCode().isError()) {
        try {
          JsonNode errorResponse = objectMapper.valueToTree(response.getBody());
          if (errorResponse.has("alternativeProviders")) {
            showRetryDialog(errorResponse);
          } else {
            showError(
                "Failed to generate narration: "
                    + errorResponse.path("error").asText("Unknown error"));
          }
        } catch (Exception ex) {
          log.error("Error parsing error response", ex);
          showError("Failed to generate narration: " + response);
        }
      } else {
        handleNarrationResponse(objectMapper.writeValueAsString(response.getBody()));
      }
    } catch (Exception e) {
      log.error("Error generating segment narration", e);
      showError("Failed to generate narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  SegmentNarrationService.SegmentNarrationContext buildSegmentContext() {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    context.setSegmentOrder(segment.getSegmentOrder());
    context.setMainEvent(segment.isMainEvent());

    if (!segment.getTitles().isEmpty()) {
      String championshipNames =
          segment.getTitles().stream()
              .map(Title::getName)
              .collect(java.util.stream.Collectors.joining(" and "));
      context.setSegmentChampionship(championshipNames);

      List<SegmentNarrationService.TitleContext> titleContexts = new ArrayList<>();
      for (Title title : segment.getTitles()) {
        SegmentNarrationService.TitleContext titleContext =
            new SegmentNarrationService.TitleContext();
        titleContext.setName(title.getName());
        if (!title.getCurrentChampions().isEmpty()) {
          titleContext.setCurrentHolderName(title.getCurrentChampions().get(0).getName());
        }
        titleContext.setTier(title.getTier().name());
        titleContexts.add(titleContext);
      }
      context.setTitles(titleContexts);
    }

    context.setWrestlers(buildWrestlerContexts());

    SegmentNarrationService.SegmentTypeContext mtc =
        new SegmentNarrationService.SegmentTypeContext();
    mtc.setSegmentType(segment.getSegmentType().getName());
    mtc.setStipulation(segment.getSegmentType().getDescription());
    mtc.setRules(
        segment.getSegmentRules().stream()
            .map(rule -> rule.getName() + ": " + rule.getDescription())
            .collect(Collectors.toList()));
    context.setSegmentType(mtc);

    if (refereeField.getValue() != null) {
      SegmentNarrationService.RefereeContext referee = new SegmentNarrationService.RefereeContext();
      referee.setName(refereeField.getValue().getName());
      referee.setDescription("Experienced wrestling referee");
      context.setReferee(referee);
    }

    context.setNpcs(buildNpcContexts());
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
            + " the provided context. This is a strict rule.\n"
            + "5.  **Title Matches:** If the segment is a title match, the narration should"
            + " prominently feature the championship at stake. Emphasize the prestige of the"
            + " title, the champion's reign, and the challenger's quest to win it. The narration"
            + " should build drama around the championship, making it the central focus of the"
            + " segment.");

    StringBuilder outcomeBuilder = new StringBuilder();

    List<String> winners =
        segment.getParticipants().stream()
            .filter(p -> p.getIsWinner() != null && p.getIsWinner())
            .map(p -> p.getWrestler().getName())
            .collect(java.util.stream.Collectors.toList());

    if (!winners.isEmpty()) {
      outcomeBuilder.append(String.join(" and ", winners)).append(" wins the segment.");
    } else if (segment.getSummary() != null && !segment.getSummary().isEmpty()) {
      outcomeBuilder.append(segment.getSummary());
    }

    if (!feedbackArea.isEmpty()) {
      if (!outcomeBuilder.isEmpty()) {
        outcomeBuilder.append("\n\n");
      }
      outcomeBuilder.append("User Feedback: ").append(feedbackArea.getValue());
    }

    if (!outcomeBuilder.isEmpty()) {
      context.setDeterminedOutcome(outcomeBuilder.toString());
    }

    List<Segment> previousSegments =
        showService.getSegments(segment.getShow()).stream()
            .filter(s -> s.getSegmentOrder() < segment.getSegmentOrder())
            .toList();

    List<SegmentNarrationService.SegmentNarrationContext> previousSegmentContexts =
        new ArrayList<>();
    for (Segment prevSegment : previousSegments) {
      previousSegmentContexts.add(buildPreviousSegmentContext(prevSegment));
    }
    context.setPreviousSegments(previousSegmentContexts);

    return context;
  }

  protected List<SegmentNarrationService.WrestlerContext> buildWrestlerContexts() {

    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();

    for (int i = 0; i < teamsLayout.getComponentCount(); i++) {

      HorizontalLayout teamSelector = (HorizontalLayout) teamsLayout.getComponentAt(i);

      MultiSelectComboBox<WrestlerDTO> wrestlersCombo =
          (MultiSelectComboBox<WrestlerDTO>) teamSelector.getComponentAt(0);
      for (WrestlerDTO wrestler : wrestlersCombo.getValue()) {
        SegmentNarrationService.WrestlerContext wc = new SegmentNarrationService.WrestlerContext();
        wc.setName(wrestler.getName());
        wc.setDescription(wrestler.getDescription());
        wrestlerService
            .findByName(wrestler.getName())
            .ifPresent(
                w -> {
                  Object supporter = ringsideActionService.getBestSupporter(segment, w);
                  if (supporter != null) {
                    if (supporter
                        instanceof com.github.javydreamercsw.management.domain.npc.Npc n) {
                      wc.setManagerName(n.getName());
                    } else if (supporter instanceof Wrestler other) {
                      wc.setManagerName(other.getName());
                    }
                  }
                });
        wc.setTeam("Team " + (i + 1));
        wc.setGender(wrestler.getGender());
        wc.setTier(wrestler.getTier().name());
        wc.setMoveSet(wrestler.getMoveSet());
        List<String> feuds = new ArrayList<>();
        wrestlerService
            .findByName(wrestler.getName())
            .ifPresent(
                w -> {
                  for (Rivalry rivalry : rivalryService.getRivalriesForWrestler(w.getId())) {
                    Wrestler opponent = rivalry.getOpponent(w);
                    feuds.add(
                        String.format(
                            "Feuding with %s (Heat: %d)", opponent.getName(), rivalry.getHeat()));
                  }
                });
        wc.setFeudsAndHeat(feuds);
        wrestlerContexts.add(wc);
      }
    }
    return wrestlerContexts;
  }

  private List<SegmentNarrationService.NPCContext> buildNpcContexts() {
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
                    npc.setRole(otherNpc.getNpcType());
                    return npc;
                  })
              .toList());
    }
    return npcs;
  }

  private SegmentNarrationService.SegmentNarrationContext buildPreviousSegmentContext(
      @NonNull Segment segment) {
    // Fetch full segment details to avoid LazyInitializationException
    Segment loadedSegment = segmentService.findByIdWithDetails(segment.getId()).orElse(segment);

    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    context.setSegmentOrder(loadedSegment.getSegmentOrder());
    context.setMainEvent(loadedSegment.isMainEvent());

    if (!loadedSegment.getTitles().isEmpty()) {
      String championshipNames =
          loadedSegment.getTitles().stream()
              .map(Title::getName)
              .collect(java.util.stream.Collectors.joining(" and "));
      context.setSegmentChampionship(championshipNames);
    }

    List<SegmentNarrationService.WrestlerContext> wrestlerContexts = new ArrayList<>();
    // Use transactional DTO fetching for wrestlers
    List<WrestlerDTO> wrestlers = wrestlerService.findAllBySegment(loadedSegment);
    for (WrestlerDTO wrestler : wrestlers) {
      SegmentNarrationService.WrestlerContext wc = new SegmentNarrationService.WrestlerContext();
      wc.setName(wrestler.getName());
      wc.setDescription(wrestler.getDescription());
      wc.setTeam("Team " + (wrestlerContexts.size() + 1));
      wc.setGender(wrestler.getGender());
      wc.setTier(wrestler.getTier().name());
      wc.setMoveSet(wrestler.getMoveSet());
      List<String> feuds = new ArrayList<>();
      wrestlerService
          .findById(wrestler.getId())
          .ifPresent(
              w -> {
                for (Rivalry rivalry : rivalryService.getRivalriesForWrestler(w.getId())) {
                  Wrestler opponent = rivalry.getOpponent(w);
                  feuds.add(
                      String.format(
                          "Feuding with %s (Heat: %d)", opponent.getName(), rivalry.getHeat()));
                }
              });
      wc.setFeudsAndHeat(feuds);
      wrestlerContexts.add(wc);
    }
    context.setWrestlers(wrestlerContexts);

    SegmentNarrationService.SegmentTypeContext mtc =
        new SegmentNarrationService.SegmentTypeContext();
    mtc.setSegmentType(loadedSegment.getSegmentType().getName());
    context.setSegmentType(mtc);

    context.setNarration(loadedSegment.getNarration());
    context.setDeterminedOutcome(loadedSegment.getSummary());

    return context;
  }

  private void showProgress(boolean show) {
    progressBar.setVisible(show);
    generateButton.setEnabled(!show);
    saveButton.setEnabled(!show);
    regenerateButton.setEnabled(!show);
  }

  private void showError(@NonNull String message) {
    Notification.show(message, 5000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showRetryDialog(@NonNull JsonNode errorResponse) {
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
    showProgress(true);

    try {
      SegmentNarrationService.SegmentNarrationContext context = buildSegmentContext();
      log.info("Retrying narration with provider {} and context: {}", provider, context);

      ResponseEntity<Map<String, Object>> response =
          segmentNarrationController.narrateSegmentWithProvider(provider, context);

      if (response.getStatusCode().isError()) {
        try {
          JsonNode errorResponse = objectMapper.valueToTree(response.getBody());
          if (errorResponse.has("alternativeProviders")) {
            showRetryDialog(errorResponse);
          } else {
            showError(
                "Failed to generate narration: "
                    + errorResponse.path("error").asText("Unknown error"));
          }
        } catch (Exception ex) {
          log.error("Error parsing error response", ex);
          showError("Failed to generate narration: " + response.toString());
        }
      } else {
        if (response.getBody() != null) {
          try {
            handleNarrationResponse(objectMapper.writeValueAsString(response.getBody()));
          } catch (JsonProcessingException e) {
            log.error("Error processing response", e);
            showError("Failed to process narration response.");
          }
        } else {
          log.warn("Got an empty response from provider!");
        }
      }
    } catch (Exception e) {
      log.error("Error retrying with provider: {}", provider, e);
      showError("Failed to generate narration: " + e.getMessage());
    } finally {
      showProgress(false);
    }
  }

  private void saveNarration() {
    try {
      segment.setNarration(narrationDisplay.getText());
      if (refereeField.getValue() != null) {
        segment.setReferee(refereeField.getValue());
      }
      segmentService.updateSegment(segment);

      getUI()
          .ifPresent(
              ui ->
                  ui.access(
                      () -> {
                        Notification.show(
                                "Narration saved successfully!",
                                3000,
                                Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        onSaveCallback.accept(segment);
                        close();
                      }));
    } catch (Exception e) {
      log.error("Error saving narration", e);
      getUI()
          .ifPresent(
              ui -> ui.access(() -> showError("Failed to save narration: " + e.getMessage())));
    }
  }

  private void handleNarrationResponse(String response) {
    if (response == null || response.isEmpty()) {
      showError("Received an empty response from the AI service.");
      return;
    }
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
}
