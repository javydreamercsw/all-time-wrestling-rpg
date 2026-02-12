/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.match;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.CommentatorContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.PromoService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.DashboardCard;
import com.github.javydreamercsw.management.ui.component.WrestlerSummaryCard;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexWrap;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "match/:matchId", layout = MainLayout.class)
@PageTitle("Match | ATW RPG")
@PermitAll
@Slf4j
public class MatchView extends VerticalLayout implements BeforeEnterObserver {

  private final SegmentService segmentService;
  private final WrestlerService wrestlerService;
  private final SecurityUtils securityUtils;
  private final CampaignService campaignService;
  private final CampaignRepository campaignRepository;
  private final SegmentNarrationServiceFactory narrationServiceFactory;
  private final NpcService npcService;
  private final SegmentAdjudicationService segmentAdjudicationService;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final MatchFulfillmentService matchFulfillmentService;
  private final PromoService promoService;
  private final CommentaryTeamRepository commentaryTeamRepository;

  private Segment segment;
  private TextArea narrationArea;
  private TextArea feedbackArea;
  private MultiSelectComboBox<Wrestler> winnersComboBox;

  @Autowired
  public MatchView(
      SegmentService segmentService,
      WrestlerService wrestlerService,
      SecurityUtils securityUtils,
      CampaignService campaignService,
      CampaignRepository campaignRepository,
      SegmentNarrationServiceFactory narrationServiceFactory,
      NpcService npcService,
      SegmentAdjudicationService segmentAdjudicationService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      MatchFulfillmentService matchFulfillmentService,
      PromoService promoService,
      CommentaryTeamRepository commentaryTeamRepository) {
    this.segmentService = segmentService;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;
    this.campaignService = campaignService;
    this.campaignRepository = campaignRepository;
    this.narrationServiceFactory = narrationServiceFactory;
    this.npcService = npcService;
    this.segmentAdjudicationService = segmentAdjudicationService;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.matchFulfillmentService = matchFulfillmentService;
    this.promoService = promoService;
    this.commentaryTeamRepository = commentaryTeamRepository;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    try {
      removeAll();
      String matchId = event.getRouteParameters().get("matchId").orElse(null);
      log.info("Entering MatchView for matchId: {}", matchId);
      if (matchId != null) {
        Optional<Segment> foundSegment = segmentService.findByIdWithShow(Long.valueOf(matchId));
        if (foundSegment.isPresent()) {
          segment = foundSegment.get();
          List<Wrestler> wrestlers = segment.getWrestlers();
          log.info(
              "Found segment: {} with {} wrestlers",
              segment.getId(),
              wrestlers != null ? wrestlers.size() : "NULL");
          buildView();
        } else {
          log.warn("Segment not found for id: {}", matchId);
          add(new H2("Match not found."));
        }
      }
    } catch (Exception e) {
      log.error("Error in MatchView.beforeEnter", e);
      add(new H2("Error displaying match details."));
    }
  }

  private void buildView() {
    log.info("Building MatchView for segment: {}", segment.getId());
    setId("match-view-" + segment.getId());
    setAlignItems(Alignment.CENTER);
    getStyle().set("background-color", "var(--lumo-contrast-5pct)");

    Optional<CustomUserDetails> userDetails = securityUtils.getAuthenticatedUser();
    Wrestler playerWrestler = userDetails.map(CustomUserDetails::getWrestler).orElse(null);

    buildHeader(playerWrestler);

    // Initialize narrationArea
    narrationArea = new TextArea("Match Story");
    narrationArea.setWidthFull();
    narrationArea.setMinHeight("200px");
    narrationArea.setPlaceholder("The story of the match will appear here...");
    narrationArea.setValue(segment.getNarration() == null ? "" : segment.getNarration());
    narrationArea.setId("narration-area");

    if (segment.getSegmentType() != null
        && "Promo".equalsIgnoreCase(segment.getSegmentType().getName())) {
      buildPromoInterface(playerWrestler);
    } else {
      buildMatchInterface(playerWrestler);
    }
  }

  private void buildHeader(Wrestler playerWrestler) {
    // Header Section
    VerticalLayout header = new VerticalLayout();
    header.setPadding(false);
    header.setSpacing(false);
    header.setAlignItems(Alignment.CENTER);
    header.addClassNames(Margin.Bottom.LARGE);

    H2 title = new H2("Match Details");
    title.addClassNames(Margin.Top.NONE, Margin.Bottom.XSMALL);
    header.add(title);

    if (segment.getShow() != null) {
      Span showName = new Span(segment.getShow().getName());
      showName.addClassNames(FontSize.LARGE, TextColor.SECONDARY, FontWeight.MEDIUM);
      showName.setId("show-name");
      header.add(showName);
    }

    if (segment.getSegmentType() != null) {
      Span matchType = new Span(segment.getSegmentType().getName());
      matchType.addClassNames(
          FontSize.SMALL,
          FontWeight.BOLD,
          Background.PRIMARY_10,
          TextColor.PRIMARY,
          Padding.Horizontal.SMALL,
          BorderRadius.MEDIUM,
          Margin.Top.SMALL);
      matchType.setId("match-type");
      header.add(matchType);
    }
    add(header);

    // Navigation / Campaign context
    if (playerWrestler != null) {
      campaignRepository
          .findActiveByWrestler(playerWrestler)
          .ifPresent(
              campaign -> {
                if (campaign.getState() != null
                    && campaign.getState().getCurrentMatch() != null
                    && campaign.getState().getCurrentMatch().getId().equals(segment.getId())) {
                  Button backToCampaignBtn =
                      new Button("Back to Campaign", e -> UI.getCurrent().navigate("campaign"));
                  backToCampaignBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                  backToCampaignBtn.setId("back-to-campaign-button");
                  add(backToCampaignBtn);
                }
              });
    }
  }

  private void buildPromoInterface(Wrestler playerWrestler) {
    DashboardCard promoCard = new DashboardCard("Interactive Promo");
    VerticalLayout promoLayout = new VerticalLayout();
    promoLayout.setPadding(false);
    promoLayout.setSpacing(true);
    promoLayout.setSizeFull();

    // Chat History
    MessageList messageList = new MessageList();
    messageList.setWidthFull();

    // Populate existing narration if any
    if (segment.getNarration() != null && !segment.getNarration().isBlank()) {
      // Simple parsing: assuming lines are separated by newlines
      // In a real scenario, we might want structured data
      String[] lines = segment.getNarration().split("\n");
      List<MessageListItem> items = new ArrayList<>();
      for (String line : lines) {
        if (!line.isBlank()) {
          // Determine user based on prefix if possible, else default
          items.add(new MessageListItem(line, java.time.Instant.now(), "History"));
        }
      }
      messageList.setItems(items);
    }

    // Input Area
    MessageInput messageInput = new MessageInput();
    messageInput.setWidthFull();
    messageInput.addSubmitListener(
        e -> {
          String text = e.getValue();
          // Add player message
          MessageListItem playerItem = new MessageListItem(text, java.time.Instant.now(), "You");
          playerItem.setUserColorIndex(1);
          List<MessageListItem> items = new ArrayList<>(messageList.getItems());
          items.add(playerItem);
          messageList.setItems(items);

          // Generate Retort
          // Find opponent (anyone not the player)
          Wrestler opponent =
              segment.getWrestlers().stream()
                  .filter(w -> !w.equals(playerWrestler))
                  .findFirst()
                  .orElse(null);

          if (opponent != null) {
            String retort = promoService.generateRetort(text, segment, opponent);
            MessageListItem opponentItem =
                new MessageListItem(retort, java.time.Instant.now(), opponent.getName());
            opponentItem.setUserColorIndex(2);
            items.add(opponentItem);
            messageList.setItems(items);

            // Save to narration
            String currentNarration = narrationArea.getValue();
            if (currentNarration == null) currentNarration = "";
            currentNarration += "\n\nYou: " + text + "\n" + opponent.getName() + ": " + retort;
            narrationArea.setValue(currentNarration.trim());

            segment.setNarration(narrationArea.getValue());
            segmentService.updateSegment(segment);
          }
        });

    promoLayout.add(messageList, messageInput);
    promoCard.add(promoLayout);
    add(promoCard);

    // Also add the Save Transcript button for manual saves/viewing
    DashboardCard transcriptCard = new DashboardCard("Transcript");
    transcriptCard.add(narrationArea);
    Button saveButton = new Button("Save Transcript", event -> saveNarration());
    saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    transcriptCard.add(saveButton);
    add(transcriptCard);
  }

  private void buildMatchInterface(Wrestler playerWrestler) {
    // Main Content Grid
    HorizontalLayout mainContent = new HorizontalLayout();
    mainContent.setWidthFull();
    mainContent.setMaxWidth("1200px");
    mainContent.addClassNames(FlexWrap.WRAP, Gap.MEDIUM, JustifyContent.CENTER);

    // Left Column: Participants
    VerticalLayout participantsCol = new VerticalLayout();
    participantsCol.setPadding(false);
    participantsCol.setWidth("auto");
    participantsCol.setMinWidth("300px");
    participantsCol.setFlexGrow(1);

    DashboardCard participantsCard = new DashboardCard("Participants");
    List<Wrestler> wrestlers = segment.getWrestlers();
    boolean isPlayerInMatch = playerWrestler != null && wrestlers.contains(playerWrestler);

    if (isPlayerInMatch) {
      participantsCard.add(new WrestlerSummaryCard(playerWrestler, wrestlerService, true));

      // Fetch player campaign to get opponent penalty
      int opponentPenalty = 0;
      var playerCampaignOpt = campaignRepository.findActiveByWrestler(playerWrestler);
      if (playerCampaignOpt.isPresent() && playerCampaignOpt.get().getState() != null) {
        opponentPenalty = playerCampaignOpt.get().getState().getOpponentHealthPenalty();
      }
      final int penalty = opponentPenalty;

      wrestlers.stream()
          .filter(w -> w != null && !w.equals(playerWrestler))
          .forEach(
              opponent ->
                  participantsCard.add(
                      new WrestlerSummaryCard(opponent, wrestlerService, false, penalty)));
    } else {
      wrestlers.stream()
          .filter(java.util.Objects::nonNull)
          .forEach(w -> participantsCard.add(new WrestlerSummaryCard(w, wrestlerService, false)));
    }
    participantsCol.add(participantsCard);

    // Right Column: Rules, Titles, and Adjudication
    VerticalLayout sideCol = new VerticalLayout();
    sideCol.setPadding(false);
    sideCol.setWidth("auto");
    sideCol.setMinWidth("300px");
    sideCol.setFlexGrow(1);

    // Rules & Titles
    DashboardCard infoCard = new DashboardCard("Match Info");
    infoCard.add(new Span("Rules"));
    var rules = segment.getSegmentRules();
    if (rules == null || rules.isEmpty()) {
      Span p = new Span("Standard Match");
      p.addClassNames(FontSize.SMALL, TextColor.SECONDARY, Margin.Left.MEDIUM);
      infoCard.add(p);
    } else {
      rules.forEach(
          rule -> {
            if (rule != null) {
              Span p = new Span("• " + rule.getName());
              p.addClassNames(FontSize.SMALL, Margin.Left.MEDIUM);
              infoCard.add(p);
            }
          });
    }

    infoCard.add(new Span("Titles Contested"));
    var titles = segment.getTitles();
    if (titles == null || titles.isEmpty()) {
      Span p = new Span("None");
      p.addClassNames(FontSize.SMALL, TextColor.SECONDARY, Margin.Left.MEDIUM);
      infoCard.add(p);
    } else {
      titles.forEach(
          t -> {
            if (t != null) {
              Span p = new Span("🏆 " + t.getName());
              p.addClassNames(FontSize.SMALL, FontWeight.BOLD, Margin.Left.MEDIUM);
              infoCard.add(p);
            }
          });
    }
    sideCol.add(infoCard);

    // Winners Section
    DashboardCard winnersCard = new DashboardCard("Match Result");
    winnersComboBox = new MultiSelectComboBox<>("Select Winner(s)");
    winnersComboBox.setWidthFull();
    winnersComboBox.setItems(wrestlers.stream().filter(java.util.Objects::nonNull).toList());
    winnersComboBox.setItemLabelGenerator(Wrestler::getName);
    var winners = segment.getWinners();
    winnersComboBox.setValue(
        new HashSet<>(
            winners != null
                ? winners.stream().filter(java.util.Objects::nonNull).toList()
                : List.of()));
    winnersComboBox.setId("winners-combobox");

    String saveButtonText = "Adjudicate Match";
    if (securityUtils.isPlayer() && !securityUtils.isBooker() && !securityUtils.isAdmin()) {
      saveButtonText = "Save Results";
      // If league match, it might be "Report Result" conceptually, but "Save Results" is fine.
    }

    Button saveWinnersButton = new Button(saveButtonText, event -> saveWinners());
    saveWinnersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
    saveWinnersButton.setWidthFull();
    saveWinnersButton.setId("save-winners-button");

    winnersCard.add(winnersComboBox, saveWinnersButton);
    sideCol.add(winnersCard);

    mainContent.add(participantsCol, sideCol);
    add(mainContent);

    // Full Width: Narration Section
    DashboardCard narrationCard = new DashboardCard("Match Narration");
    narrationCard.setMaxWidth("1200px");
    VerticalLayout narrationContent = new VerticalLayout();
    narrationContent.setPadding(false);

    feedbackArea = new TextArea("Generation Feedback");
    feedbackArea.setWidthFull();
    feedbackArea.setPlaceholder(
        "Provide specific details about the match (key spots, interference, etc.) to guide the"
            + " AI...");
    feedbackArea.setId("feedback-area");

    HorizontalLayout narrationButtons = new HorizontalLayout();
    narrationButtons.setWidthFull();

    Button aiGenerateButton = new Button("Generate with Feedback", event -> generateAiNarration());
    aiGenerateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    aiGenerateButton.setId("ai-generate-narration-button");

    Button saveButton = new Button("Save Narration", event -> saveNarration());
    saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
    saveButton.setId("save-narration-button");

    boolean isCampaignMatch = false;
    if (playerWrestler != null) {
      var campaignOpt = campaignRepository.findActiveByWrestler(playerWrestler);
      if (campaignOpt.isPresent()) {
        var campaign = campaignOpt.get();
        if (campaign.getState() != null
            && campaign.getState().getCurrentMatch() != null
            && campaign.getState().getCurrentMatch().getId().equals(segment.getId())) {
          isCampaignMatch = true;
        }
      }
    }

    boolean isLeagueMatch = false;
    if (playerWrestler != null) {
      Optional<MatchFulfillment> fulfillment = matchFulfillmentRepository.findBySegment(segment);
      if (fulfillment.isPresent() && segment.getWrestlers().contains(playerWrestler)) {
        isLeagueMatch = true;
      }
    }

    boolean showGenerateButton =
        securityUtils.isBooker() || securityUtils.isAdmin() || isCampaignMatch || isLeagueMatch;

    if (showGenerateButton) {
      narrationButtons.add(aiGenerateButton);
    }
    narrationButtons.add(saveButton);

    if (showGenerateButton) {
      narrationContent.add(feedbackArea, narrationArea, narrationButtons);
    } else {
      narrationContent.add(narrationArea, narrationButtons);
    }

    narrationCard.add(narrationContent);
    add(narrationCard);
  }

  private void generateAiNarration() {
    if (narrationServiceFactory.getAvailableServicesInPriorityOrder().isEmpty()) {
      String reason = "No AI providers are currently enabled or reachable.";
      Notification.show(reason).addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    log.info("Generating AI narration for segment: {}", segment.getId());
    Notification.show("Connecting to Story Director...")
        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);

    try {
      SegmentNarrationContext context = new SegmentNarrationContext();

      SegmentTypeContext typeContext = new SegmentTypeContext();
      typeContext.setSegmentType(segment.getSegmentType().getName());
      typeContext.setRules(segment.getSegmentRules().stream().map(SegmentRule::getName).toList());
      context.setSegmentType(typeContext);

      context.setWrestlers(
          segment.getParticipants().stream()
              .map(
                  p -> {
                    WrestlerContext wc = new WrestlerContext();
                    wc.setName(p.getWrestler().getName());
                    wc.setDescription(p.getWrestler().getDescription());
                    return wc;
                  })
              .toList());

      context.setDeterminedOutcome(
          segment.getWinners().stream()
              .map(Wrestler::getName)
              .collect(java.util.stream.Collectors.joining(", ")));

      // Add all available NPCs to the context to help AI stay within roster

      context.setNpcs(
          npcService.findAll().stream()
              .map(
                  npc -> {
                    NPCContext nc = new NPCContext();
                    nc.setName(npc.getName());
                    nc.setDescription(npc.getDescription());
                    if (npc.getNpcType() != null) {
                      nc.setRole(npc.getNpcType());
                    }
                    return nc;
                  })
              .toList());

      // Commentary Team Context
      CommentaryTeam team = segment.getShow().getCommentaryTeam();
      if (team == null) {
        // Default to All-Time Broadcast Team if not set
        team =
            commentaryTeamRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase("All-Time Broadcast Team"))
                .findFirst()
                .orElse(null);
      }

      if (team != null) {
        context.setCommentators(
            team.getCommentators().stream()
                .map(
                    c -> {
                      CommentatorContext cc = new CommentatorContext();
                      cc.setName(c.getNpc().getName());
                      cc.setGender(
                          c.getNpc().getGender() != null ? c.getNpc().getGender().name() : null);
                      cc.setAlignment(
                          c.getNpc().getAlignment() != null
                              ? c.getNpc().getAlignment().name()
                              : null);
                      cc.setStyle(c.getStyle());
                      cc.setCatchphrase(c.getCatchphrase());
                      cc.setPersonaDescription(c.getPersonaDescription());
                      cc.setDescription(c.getNpc().getDescription());
                      return cc;
                    })
                .toList());
      }

      String feedback = feedbackArea.getValue();
      String instructions =
          "Narrate a compelling wrestling match based on the provided wrestlers and rules. The"
              + " match should be narrated as a conversation between the commentary team members"
              + " provided in the context. Each commentator has a distinct persona (Alignment,"
              + " Style, Catchphrase) that MUST be respected. Ensure the narration flows as dynamic"
              + " dialogue, capturing their different perspectives on the match. IMPORTANT: You"
              + " MUST ONLY use the wrestlers, commentators and NPCs provided in the context. Do"
              + " NOT invent new characters, announcers, or managers. Stick strictly to the All"
              + " Time Wrestling roster provided.";

      if (feedback != null && !feedback.isBlank()) {
        instructions += "\n\nPlease also incorporate this specific feedback: " + feedback;
      }

      context.setInstructions(instructions);

      String generated = narrationServiceFactory.getBestAvailableService().narrateSegment(context);

      if (generated != null && !generated.isEmpty()) {
        narrationArea.setValue(generated);
        segment.setNarration(generated);
        segmentService.updateSegment(segment);
        Notification.show("Narration generated!")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      }
    } catch (Exception e) {
      log.error("Failed to generate AI narration", e);
      Notification.show("Failed to generate narration. Please check AI settings.")
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void saveWinners() {
    List<Wrestler> winners = new ArrayList<>(winnersComboBox.getValue());
    log.info(
        "Adjudicating match {}: winners={}",
        segment.getId(),
        winners.stream().map(Wrestler::getName).toList());
    segment.setWinners(winners);
    try {
      segmentService.updateSegment(segment);

      // Check for league match reporting first
      Optional<MatchFulfillment> fulfillmentOpt = matchFulfillmentRepository.findBySegment(segment);
      if (fulfillmentOpt.isPresent()
          && securityUtils.isPlayer()
          && !securityUtils.isAdmin()
          && !securityUtils.isBooker()) {
        matchFulfillmentService.submitResult(
            fulfillmentOpt.get(),
            winners.isEmpty() ? null : winners.get(0),
            securityUtils.getAuthenticatedUser().get().getAccount());
        Notification.show("Match result reported to league commissioner.")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("player");
        return;
      }

      // Check if this is a campaign match for any participant
      boolean isCampaignMatch = false;
      for (Wrestler w : segment.getWrestlers()) {
        if (w == null) continue;
        var campaignOpt = campaignRepository.findActiveByWrestler(w);
        if (campaignOpt.isPresent()) {
          var campaign = campaignOpt.get();
          if (campaign.getState().getCurrentMatch() != null
              && campaign.getState().getCurrentMatch().getId().equals(segment.getId())) {
            log.info("Updating campaign {} for wrestler {}", campaign.getId(), w.getName());
            boolean won = winners.contains(w);
            campaignService.processMatchResult(campaign, won);
            isCampaignMatch = true;
          }
        }
      }

      if (isCampaignMatch) {
        // For campaign matches, processMatchResult handles rewards.
        // We set status to ADJUDICATED to mark it done.
        segment.setAdjudicationStatus(
            com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED);
        segmentService.updateSegment(segment);
        Notification n = Notification.show("Match adjudicated & Campaign Progress Updated!");
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        UI.getCurrent().navigate("campaign");
      } else {
        if (securityUtils.isBooker() || securityUtils.isAdmin()) {
          // For standard matches, perform full adjudication (Booker/Admin only)
          segmentAdjudicationService.adjudicateMatch(segment);
          segment.setAdjudicationStatus(
              com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED);
          segmentService.updateSegment(segment);
          Notification.show("Match adjudicated successfully!")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          UI.getCurrent().navigate("show-list");
        } else {
          // For players editing a match (e.g. proposed result), just save.
          Notification.show("Match results saved.")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
      }

    } catch (Exception e) {
      log.error("Failed to adjudicate segment", e);
      Notification.show("Failed to adjudicate match: " + e.getMessage())
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void saveNarration() {
    segment.setNarration(narrationArea.getValue());
    segmentService.updateSegment(segment);
    Notification.show("Narration saved!");
  }
}
