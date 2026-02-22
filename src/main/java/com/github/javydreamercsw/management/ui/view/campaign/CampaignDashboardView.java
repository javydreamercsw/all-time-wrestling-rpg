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
package com.github.javydreamercsw.management.ui.view.campaign;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.StorylineDirectorService;
import com.github.javydreamercsw.management.service.campaign.StorylineExportService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.component.AlignmentTrackComponent;
import com.github.javydreamercsw.management.ui.component.CampaignAbilityCardComponent;
import com.github.javydreamercsw.management.ui.component.PlayerCampaignCard;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign", layout = MainLayout.class)
@PageTitle("Campaign Dashboard")
@PermitAll
@Slf4j
public class CampaignDashboardView extends VerticalLayout {

  private final CampaignRepository campaignRepository;
  private final CampaignService campaignService;
  private final WrestlerRepository wrestlerRepository;
  private final CampaignAbilityCardRepository cardRepository;
  private final CampaignUpgradeService upgradeService;
  private final SecurityUtils securityUtils;
  private final com.github.javydreamercsw.management.service.campaign.TournamentService
      tournamentService;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
  private final CampaignChapterService chapterService;
  private final TitleService titleService;
  private final com.github.javydreamercsw.management.domain.title.TitleRepository titleRepository;
  private final StorylineDirectorService storylineDirectorService;
  private final StorylineExportService storylineExportService;

  private Campaign currentCampaign;

  @Autowired
  public CampaignDashboardView(
      CampaignRepository campaignRepository,
      CampaignService campaignService,
      WrestlerRepository wrestlerRepository,
      CampaignAbilityCardRepository cardRepository,
      CampaignUpgradeService upgradeService,
      SecurityUtils securityUtils,
      com.github.javydreamercsw.management.service.campaign.TournamentService tournamentService,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper,
      CampaignChapterService chapterService,
      TitleService titleService,
      com.github.javydreamercsw.management.domain.title.TitleRepository titleRepository,
      StorylineDirectorService storylineDirectorService,
      StorylineExportService storylineExportService) {
    this.campaignRepository = campaignRepository;
    this.campaignService = campaignService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardRepository = cardRepository;
    this.upgradeService = upgradeService;
    this.securityUtils = securityUtils;
    this.tournamentService = tournamentService;
    this.objectMapper = objectMapper;
    this.chapterService = chapterService;
    this.titleService = titleService;
    this.titleRepository = titleRepository;
    this.storylineDirectorService = storylineDirectorService;
    this.storylineExportService = storylineExportService;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private boolean getFeatureBoolean(CampaignState state, String key) {
    if (state.getFeatureData() == null) return false;
    try {
      java.util.Map<String, Object> data =
          objectMapper.readValue(
              state.getFeatureData(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
      return Boolean.TRUE.equals(data.get(key));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.error("Error parsing feature data", e);
      return false;
    }
  }

  private void loadCampaign() {
    log.info("Loading campaign for current user...");
    securityUtils
        .getAuthenticatedUser()
        .ifPresentOrElse(
            user -> {
              log.info("Authenticated user: {}", user.getUsername());
              com.github.javydreamercsw.base.domain.account.Account account = user.getAccount();
              java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
              Wrestler active =
                  wrestlers.stream()
                      .filter(w -> w.getId().equals(account.getActiveWrestlerId()))
                      .findFirst()
                      .orElse(wrestlers.isEmpty() ? null : wrestlers.get(0));

              if (active != null) {
                log.info("Wrestler found: {}", active.getName());
                campaignService
                    .getCampaignForWrestler(active)
                    .ifPresentOrElse(
                        campaign -> {
                          log.info("Active campaign found: {}", campaign.getId());
                          currentCampaign = campaign;
                        },
                        () ->
                            log.info(
                                "No active campaign found for wrestler: {}", active.getName()));
              } else {
                log.warn("No wrestler found for account: {}", user.getUsername());
              }
            },
            () -> log.warn("No authenticated user found during loadCampaign"));
  }

  private void initUI() {
    if (currentCampaign == null) {
      add(new H2("Campaign Mode"));
      add(
          new Span(
              "No active campaign found. To start a campaign, please navigate to the Wrestler"
                  + " List and use the 'Start Campaign' action on your assigned wrestler."));

      // Debug button for E2E tests and quick start
      Button debugStartButton =
          new Button(
              "Start New Campaign (Debug)",
              e -> {
                log.info("Debug Start Campaign button clicked");
                securityUtils
                    .getAuthenticatedUser()
                    .ifPresentOrElse(
                        user -> {
                          com.github.javydreamercsw.base.domain.account.Account account =
                              user.getAccount();
                          java.util.List<Wrestler> wrestlers =
                              wrestlerRepository.findByAccount(account);
                          Wrestler active =
                              wrestlers.stream()
                                  .filter(w -> w.getId().equals(account.getActiveWrestlerId()))
                                  .findFirst()
                                  .orElse(wrestlers.isEmpty() ? null : wrestlers.get(0));

                          if (active != null) {
                            campaignService.startCampaign(active);
                            refreshUI();
                          } else {
                            List<Wrestler> all = wrestlerRepository.findAll();
                            if (!all.isEmpty()) {
                              Wrestler first = all.get(0);
                              first.setAccount(user.getAccount());
                              first.setIsPlayer(true);
                              wrestlerRepository.save(first);
                              campaignService.startCampaign(first);
                              refreshUI();
                            }
                          }
                        },
                        () ->
                            log.warn(
                                "No authenticated user found when clicking debug start button"));
              });
      debugStartButton.addThemeVariants(
          com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
      debugStartButton.setId("debug-start-campaign");
      add(debugStartButton);
      return;
    }

    CampaignState state = currentCampaign.getState();
    Wrestler wrestler = currentCampaign.getWrestler();
    WrestlerAlignment alignment = wrestler.getAlignment();

    add(new H2("Campaign: All or Nothing (Season 1)"));
    add(new H3("Wrestler: " + wrestler.getName()));

    Optional<CampaignChapterDTO> chapterOpt = campaignService.getCurrentChapter(currentCampaign);
    String chapterTitle = chapterOpt.map(CampaignChapterDTO::getTitle).orElse("Dynamic Story");
    boolean isTournament = chapterOpt.map(CampaignChapterDTO::isTournament).orElse(false);

    Span chapterLabel = new Span("Chapter: " + chapterTitle);
    chapterLabel.addClassNames(
        LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);
    chapterLabel.setId("campaign-chapter-title");
    add(chapterLabel);

    // 1. Alignment Track (Top, Full Width)
    if (alignment != null) {
      add(new AlignmentTrackComponent(alignment));
    }

    // 2. Main Split Layout
    HorizontalLayout mainLayout = new HorizontalLayout();
    mainLayout.setWidthFull();
    mainLayout.addClassNames(LumoUtility.Gap.XLARGE, LumoUtility.AlignItems.START);

    // 3. Tournament Tracker/Bracket (Top, Full Width, below Alignment)
    if (isTournament) {
      VerticalLayout tournamentSection = new VerticalLayout();
      tournamentSection.setPadding(false);
      tournamentSection.setSpacing(true);
      addTournamentBracket(tournamentSection);
      add(tournamentSection);
    }

    // Show "Continue Match" if in a match and NOT adjudicated
    if (state.getCurrentMatch() != null
        && state.getCurrentPhase()
            == com.github.javydreamercsw.management.domain.campaign.CampaignPhase.MATCH
        && state.getCurrentMatch().getAdjudicationStatus()
            != com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED) {
      Button continueMatchButton =
          new Button(
              "CONTINUE MATCH: " + state.getCurrentMatch().getNarration(),
              e ->
                  UI.getCurrent()
                      .navigate(
                          com.github.javydreamercsw.management.ui.view.match.MatchView.class,
                          new com.vaadin.flow.router.RouteParameters(
                              "matchId", String.valueOf(state.getCurrentMatch().getId()))));
      continueMatchButton.addThemeVariants(
          com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
          com.vaadin.flow.component.button.ButtonVariant.LUMO_LARGE,
          com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);
      continueMatchButton.setWidthFull();
      add(continueMatchButton);
    }

    // Left Column
    VerticalLayout leftColumn = new VerticalLayout();
    leftColumn.setPadding(false);
    leftColumn.setSpacing(true);
    leftColumn.setWidth("50%");

    // Storyline Section
    addStorylineSection(state, leftColumn);

    // Right Column
    VerticalLayout rightColumn = new VerticalLayout();
    rightColumn.setPadding(false);
    rightColumn.setSpacing(true);
    rightColumn.setWidth("50%");

    mainLayout.add(leftColumn, rightColumn);
    add(mainLayout);

    // --- LEFT COLUMN CONTENT ---

    // Player Card
    leftColumn.add(new PlayerCampaignCard(currentCampaign));

    // My Cards Section
    leftColumn.add(new H4("My Ability Cards"));
    HorizontalLayout myCardsLayout = new HorizontalLayout();
    myCardsLayout.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.MEDIUM);
    if (state.getActiveCards().isEmpty()) {
      myCardsLayout.add(
          new Span("No cards currently held. Advance your alignment track to earn cards!"));
    } else {
      state
          .getActiveCards()
          .forEach(card -> myCardsLayout.add(new CampaignAbilityCardComponent(card)));
    }
    leftColumn.add(myCardsLayout);

    // --- RIGHT COLUMN CONTENT ---

    // Pending Picks Section
    addPendingPicksSection(currentCampaign, rightColumn);

    // Purchased Skills Section
    rightColumn.add(new H4("Purchased Skills"));
    VerticalLayout skillsLayout = new VerticalLayout();
    skillsLayout.setPadding(false);
    if (state.getUpgrades().isEmpty()) {
      skillsLayout.add(new Span("No permanent skills purchased yet. Train to earn Skill Tokens!"));
    } else {
      state
          .getUpgrades()
          .forEach(
              upgrade -> {
                Span skill = new Span("✅ " + upgrade.getName() + ": " + upgrade.getDescription());
                skill.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD);
                skillsLayout.add(skill);
              });
    }
    rightColumn.add(skillsLayout);

    // Skill Upgrades Section (Placed near Purchased Skills)
    addSkillUpgradesSection(currentCampaign, rightColumn);

    // Actions Section (Below Skills)
    rightColumn.add(new H4("Actions"));

    Span actionsInfo =
        new Span("Backstage actions are only available before continuing the story narrative.");
    actionsInfo.addClassNames(FontSize.SMALL, TextColor.SECONDARY, Margin.Bottom.SMALL);
    rightColumn.add(actionsInfo);

    if (state.getCurrentPhase()
        == com.github.javydreamercsw.management.domain.campaign.CampaignPhase.BACKSTAGE) {
      Span remainingActions =
          new Span("Remaining actions for today: " + (2 - state.getActionsTaken()));
      remainingActions.addClassNames(FontSize.SMALL, FontWeight.BOLD, TextColor.PRIMARY);
      rightColumn.add(remainingActions);
    }

    HorizontalLayout actionsLayout = new HorizontalLayout();
    actionsLayout.add(
        new Button("Backstage Actions", e -> UI.getCurrent().navigate(BackstageActionView.class)));
    actionsLayout.add(
        new Button(
            "Story Narrative",
            e -> {
              if (state.getActionsTaken() < 2
                  && state.getCurrentPhase()
                      == com.github.javydreamercsw.management.domain.campaign.CampaignPhase
                          .BACKSTAGE) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Unused Actions");
                dialog.setText(
                    "You still have "
                        + (2 - state.getActionsTaken())
                        + " actions remaining for today. Proceeding to the story will skip these"
                        + " actions. Are you sure?");
                dialog.setCancelable(true);
                dialog.setConfirmText("Proceed Anyway");
                dialog.setConfirmButtonTheme("error primary");
                dialog.addConfirmListener(event -> UI.getCurrent().navigate("campaign/narrative"));
                dialog.open();
              } else {
                if (state.getCurrentPhase()
                    == com.github.javydreamercsw.management.domain.campaign.CampaignPhase
                        .POST_MATCH) {
                  log.info("Navigating to post-match narrative.");
                }
                UI.getCurrent().navigate("campaign/narrative");
              }
            }));

    // Only show "Next Show" if they are in POST_MATCH
    if (state.getCurrentPhase()
        == com.github.javydreamercsw.management.domain.campaign.CampaignPhase.POST_MATCH) {
      Button nextShowButton =
          new Button(
              "Continue to Next Day",
              e -> {
                campaignService.completePostMatch(currentCampaign);
                refreshUI();
              });
      nextShowButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);
      actionsLayout.add(nextShowButton);
    }

    rightColumn.add(actionsLayout);

    // Chapter Advancement (Bottom of Page)
    if (campaignService.isChapterComplete(currentCampaign)) {
      Button advanceButton =
          new Button(
              "Complete Chapter & Advance",
              e -> {
                campaignService
                    .advanceChapter(currentCampaign)
                    .ifPresent(
                        newChapterId -> {
                          // Navigate to narrative to show new chapter intro
                          UI.getCurrent().navigate("campaign/narrative");
                        });
              });
      advanceButton.addThemeVariants(
          com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
          com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS,
          com.vaadin.flow.component.button.ButtonVariant.LUMO_LARGE);
      advanceButton.setWidthFull();
      add(advanceButton);
    }

    // Global Card Library
    addGlobalCardLibrary();

    // Story Journal
    addStoryJournalSection();

    // Debug Section
    if (!VaadinService.getCurrent().getDeploymentConfiguration().isProductionMode()) {
      addDebugSection();
    }
  }

  private void addStorylineSection(CampaignState state, VerticalLayout parent) {
    if (state.getActiveStoryline() == null) return;

    com.github.javydreamercsw.management.domain.campaign.CampaignStoryline storyline =
        state.getActiveStoryline();
    VerticalLayout section = new VerticalLayout();
    section.setPadding(true);
    section.setSpacing(false);
    section.addClassNames(
        LumoUtility.Background.CONTRAST_10,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Margin.Bottom.MEDIUM);

    H4 title = new H4("Active Storyline: " + storyline.getTitle());
    title.addClassNames(
        LumoUtility.Margin.Top.NONE,
        LumoUtility.Margin.Bottom.XSMALL,
        LumoUtility.TextColor.PRIMARY);
    section.add(title);

    Span desc = new Span(storyline.getDescription());
    desc.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Margin.Bottom.SMALL);
    section.add(desc);

    VerticalLayout milestoneList = new VerticalLayout();
    milestoneList.setPadding(false);
    milestoneList.setSpacing(true);

    for (com.github.javydreamercsw.management.domain.campaign.StorylineMilestone milestone :
        storyline.getMilestones()) {
      HorizontalLayout item = new HorizontalLayout();
      item.setAlignItems(Alignment.CENTER);
      item.setSpacing(true);

      Span icon = new Span(getMilestoneIcon(milestone.getStatus()));
      Span mTitle = new Span(milestone.getTitle());
      mTitle.addClassNames(LumoUtility.FontSize.SMALL);

      if (milestone.getStatus()
          == com.github.javydreamercsw.management.domain.campaign.StorylineMilestone.MilestoneStatus
              .ACTIVE) {
        mTitle.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.HEADER);
        Tooltip.forComponent(item).setText("CURRENT GOAL: " + milestone.getNarrativeGoal());
      } else if (milestone.getStatus()
          == com.github.javydreamercsw.management.domain.campaign.StorylineMilestone.MilestoneStatus
              .PENDING) {
        mTitle.addClassNames(LumoUtility.TextColor.SECONDARY);
      }

      item.add(icon, mTitle);
      milestoneList.add(item);
    }

    section.add(milestoneList);
    parent.add(section);
  }

  private String getMilestoneIcon(
      com.github.javydreamercsw.management.domain.campaign.StorylineMilestone.MilestoneStatus
          status) {
    return switch (status) {
      case COMPLETED -> "✅";
      case FAILED -> "❌";
      case ACTIVE -> "🎯";
      case PENDING -> "⚪";
    };
  }

  private void addPendingPicksSection(@NonNull Campaign campaign, VerticalLayout parent) {
    CampaignState state = campaign.getState();
    if (state.getPendingL1Picks() <= 0
        && state.getPendingL2Picks() <= 0
        && state.getPendingL3Picks() <= 0) {
      return;
    }

    VerticalLayout pendingSection = new VerticalLayout();
    pendingSection.setPadding(true);
    pendingSection.addClassNames(
        LumoUtility.Background.CONTRAST_10, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Gap.SMALL);

    H4 header = new H4("Available Card Picks");
    header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.PRIMARY);
    pendingSection.add(header);

    Span info =
        new Span(
            "You have earned new ability slots! Choose a card to add to your permanent"
                + " inventory.");
    info.addClassNames(LumoUtility.FontSize.SMALL);
    pendingSection.add(info);

    HorizontalLayout picksContainer = new HorizontalLayout();
    picksContainer.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.MEDIUM);

    List<CampaignAbilityCard> pickable = campaignService.getPickableCards(campaign);
    for (CampaignAbilityCard card : pickable) {
      VerticalLayout cardWrapper = new VerticalLayout();
      cardWrapper.setPadding(false);
      cardWrapper.setSpacing(true);
      cardWrapper.setAlignItems(Alignment.CENTER);
      cardWrapper.setWidth("wrap-content");

      cardWrapper.add(new CampaignAbilityCardComponent(card));

      Button pickButton =
          new Button(
              "Pick This Card",
              e -> {
                campaignService.pickAbilityCard(campaign, card.getId());
                refreshUI();
              });
      pickButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
      cardWrapper.add(pickButton);

      picksContainer.add(cardWrapper);
    }

    pendingSection.add(picksContainer);
    parent.add(pendingSection);
  }

  private void addSkillUpgradesSection(@NonNull Campaign campaign, VerticalLayout parent) {
    CampaignState state = campaign.getState();
    if (state.getSkillTokens() < 8) return;

    VerticalLayout upgradeSection = new VerticalLayout();
    upgradeSection.setPadding(true);
    upgradeSection.addClassNames(
        LumoUtility.Background.CONTRAST_10, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Gap.SMALL);

    H4 header = new H4("Available Skill Upgrades (Cost: 8 Tokens)");
    header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextColor.SUCCESS);
    upgradeSection.add(header);

    HorizontalLayout upgradeContainer = new HorizontalLayout();
    upgradeContainer.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.MEDIUM);

    List<com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade> available =
        upgradeService.getAllUpgrades();

    // Filter out upgrades if the player already has one of that type
    List<String> ownedTypes =
        state.getUpgrades().stream()
            .map(com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade::getType)
            .toList();

    available.removeIf(u -> ownedTypes.contains(u.getType()));

    for (com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade upgrade : available) {
      Button buyButton =
          new Button(
              upgrade.getName(),
              e -> {
                upgradeService.purchaseUpgrade(campaign, upgrade.getId());
                refreshUI();
              });
      buyButton.setTooltipText(upgrade.getDescription());
      buyButton.addThemeVariants(
          com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
          com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
      upgradeContainer.add(buyButton);
    }

    upgradeSection.add(upgradeContainer);
    parent.add(upgradeSection);
  }

  private void addTournamentBracket(VerticalLayout parent) {
    com.github.javydreamercsw.management.dto.campaign.TournamentDTO tournament =
        tournamentService.getTournamentState(currentCampaign);

    if (tournament == null) {
      // Initialize if missing (e.g. legacy save)
      tournamentService.initializeTournament(currentCampaign);
      tournament = tournamentService.getTournamentState(currentCampaign);
    }

    VerticalLayout bracketContainer = new VerticalLayout();
    bracketContainer.setPadding(false);
    bracketContainer.setSpacing(true);
    bracketContainer.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);

    H4 title = new H4("Tournament Bracket (Round " + tournament.getCurrentRound() + ")");
    title.addClassNames(LumoUtility.Margin.NONE);
    bracketContainer.add(title);

    bracketContainer.add(
        new com.github.javydreamercsw.management.ui.component.TournamentBracketComponent(
            tournament));

    // Play Next Match Button
    com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch nextMatch =
        tournamentService.getCurrentPlayerMatch(currentCampaign);

    if (nextMatch != null && nextMatch.getWinnerId() == null) {
      if (currentCampaign.getState().getCurrentMatch() != null) {
        // Match already created/in-progress
        // Only show continue if NOT adjudicated
        if (currentCampaign.getState().getCurrentMatch().getAdjudicationStatus()
            != com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED) {
          Button continueMatchButton =
              new Button(
                  "Continue Tournament Match",
                  e ->
                      UI.getCurrent()
                          .navigate(
                              com.github.javydreamercsw.management.ui.view.match.MatchView.class,
                              new com.vaadin.flow.router.RouteParameters(
                                  "matchId",
                                  String.valueOf(
                                      currentCampaign.getState().getCurrentMatch().getId()))));
          continueMatchButton.addThemeVariants(
              com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
              com.vaadin.flow.component.button.ButtonVariant.LUMO_LARGE,
              com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);
          bracketContainer.add(continueMatchButton);
        } else {
          // Match finished but not cleared (Post-Match phase)
          Button advanceButton =
              new Button(
                  "Match Complete - Advance to Next Day",
                  e -> {
                    campaignService.completePostMatch(currentCampaign);
                    refreshUI();
                  });
          advanceButton.addThemeVariants(
              com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
              com.vaadin.flow.component.button.ButtonVariant.LUMO_LARGE,
              com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);
          bracketContainer.add(advanceButton);
        }
      } else {
        // Ready to book
        String opponentName =
            nextMatch.getWrestler1Id().equals(currentCampaign.getWrestler().getId())
                ? nextMatch.getWrestler2Name()
                : nextMatch.getWrestler1Name();

        Button playMatchButton =
            new Button(
                "Play Tournament Match vs " + opponentName,
                e -> {
                  campaignService.createMatchForEncounter(
                      currentCampaign,
                      opponentName,
                      "Tournament Match: " + opponentName,
                      "One on One",
                      "Normal");
                  refreshUI();
                });
        playMatchButton.addThemeVariants(
            com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY,
            com.vaadin.flow.component.button.ButtonVariant.LUMO_LARGE);
        bracketContainer.add(playMatchButton);
      }
    } else if (getFeatureBoolean(currentCampaign.getState(), "tournamentWinner")) {
      Span winnerMsg = new Span("🏆 You are the Tournament Champion!");
      winnerMsg.addClassNames(
          LumoUtility.TextColor.SUCCESS, LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
      bracketContainer.add(winnerMsg);
    } else if (getFeatureBoolean(currentCampaign.getState(), "finalsPhase")
        && nextMatch == null
        && !getFeatureBoolean(currentCampaign.getState(), "tournamentWinner")) {
      Span loserMsg = new Span("❌ You have been eliminated from the tournament.");
      loserMsg.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontWeight.BOLD);
      bracketContainer.add(loserMsg);
    }

    parent.add(bracketContainer);
  }

  private Div createBracketBox(@NonNull String name, boolean isPlayer) {
    // Legacy method, can be removed or kept if needed by other logic not yet updated
    return new Div();
  }

  private void refreshUI() {
    removeAll();
    loadCampaign();
    initUI();
  }

  private void addGlobalCardLibrary() {
    VerticalLayout libraryContent = new VerticalLayout();
    libraryContent.setPadding(false);

    HorizontalLayout splitLayout = new HorizontalLayout();
    splitLayout.setWidthFull();
    splitLayout.addClassNames(LumoUtility.Gap.LARGE);

    // Heel Section
    VerticalLayout heelCol = new VerticalLayout();
    heelCol.add(new H4("Heel Abilities"));
    HorizontalLayout heelCards = new HorizontalLayout();
    heelCards.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.SMALL);
    cardRepository.findByAlignmentType(AlignmentType.HEEL).stream()
        .sorted(java.util.Comparator.comparingInt(CampaignAbilityCard::getLevel))
        .forEach(card -> heelCards.add(new CampaignAbilityCardComponent(card)));
    heelCol.add(heelCards);

    // Face Section
    VerticalLayout faceCol = new VerticalLayout();
    faceCol.add(new H4("Face Abilities"));
    HorizontalLayout faceCards = new HorizontalLayout();
    faceCards.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.SMALL);
    cardRepository.findByAlignmentType(AlignmentType.FACE).stream()
        .sorted(java.util.Comparator.comparingInt(CampaignAbilityCard::getLevel))
        .forEach(card -> faceCards.add(new CampaignAbilityCardComponent(card)));
    faceCol.add(faceCards);

    splitLayout.add(heelCol, faceCol);
    libraryContent.add(splitLayout);

    Details libraryDetails = new Details("Campaign Card Library", libraryContent);
    libraryDetails.addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Top.LARGE);
    add(libraryDetails);
  }

  private void addStoryJournalSection() {
    List<com.github.javydreamercsw.management.domain.campaign.CampaignStoryline> history =
        campaignService.getStorylineHistory(currentCampaign);

    if (history.isEmpty()) return;

    VerticalLayout journalContent = new VerticalLayout();
    journalContent.setPadding(true);
    journalContent.setSpacing(true);

    for (com.github.javydreamercsw.management.domain.campaign.CampaignStoryline storyline :
        history) {
      HorizontalLayout row = new HorizontalLayout();
      row.setId("story-journal-row-" + storyline.getId());
      row.setWidthFull();
      row.setAlignItems(Alignment.CENTER);
      row.addClassNames(
          LumoUtility.Padding.SMALL,
          LumoUtility.Border.BOTTOM,
          LumoUtility.BorderColor.CONTRAST_10);

      VerticalLayout info = new VerticalLayout();
      info.setPadding(false);
      info.setSpacing(false);

      String statusLabel =
          storyline.getStatus()
                  == com.github.javydreamercsw.management.domain.campaign.CampaignStoryline
                      .StorylineStatus.ACTIVE
              ? " (Active)"
              : "";
      Span title = new Span(storyline.getTitle() + statusLabel);
      title.addClassNames(LumoUtility.FontWeight.BOLD);

      Span desc = new Span(storyline.getDescription());
      desc.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

      info.add(title, desc);

      // Download Button
      String fileName =
          storyline.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "_") + "_chapter.json";
      StreamResource resource =
          new StreamResource(
              fileName,
              () -> {
                String json = storylineExportService.exportStorylineAsChapter(storyline);
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
              });

      Anchor downloadAnchor = new Anchor(resource, "");
      downloadAnchor.setId("download-json-anchor-" + storyline.getId());
      downloadAnchor.getElement().setAttribute("download", true);
      Button downloadBtn = new Button("Download JSON", e -> {});
      downloadBtn.setId("download-json-button-" + storyline.getId());
      downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
      downloadAnchor.add(downloadBtn);

      row.add(info, downloadAnchor);
      row.expand(info);
      journalContent.add(row);
    }

    Details journalDetails = new Details("📜 Story Journal (AI Storylines)", journalContent);
    journalDetails.setId("story-journal-details");
    journalDetails.addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Top.MEDIUM);
    add(journalDetails);
  }

  private void addDebugSection() {
    VerticalLayout debugContent = new VerticalLayout();
    debugContent.setPadding(true);
    debugContent.setSpacing(true);

    // --- Chapter Jumper ---
    HorizontalLayout chapterLayout = new HorizontalLayout();
    chapterLayout.setAlignItems(Alignment.BASELINE);

    ComboBox<CampaignChapterDTO> chapterSelect = new ComboBox<>("Jump to Chapter");
    chapterSelect.setItems(chapterService.getAllChapters());
    chapterSelect.setItemLabelGenerator(CampaignChapterDTO::getTitle);
    chapterSelect.setPlaceholder("Select Chapter...");
    chapterSelect.setWidth("300px");

    Button jumpButton =
        new Button(
            "Jump",
            e -> {
              CampaignChapterDTO selected = chapterSelect.getValue();
              if (selected != null) {
                CampaignState state = currentCampaign.getState();
                state.setCurrentChapterId(selected.getId());
                // Reset Counters
                state.setMatchesPlayed(0);
                state.setWins(0);
                state.setLosses(0);
                // Clear flags (Naive clear - might need more robust reset if flags get complex)
                campaignService.setFeatureValue(state, "finalsPhase", false);
                campaignService.setFeatureValue(state, "tournamentWinner", false);
                campaignService.setFeatureValue(state, "wonFinale", false);
                campaignService.setFeatureValue(state, "failedToQualify", false);

                // Initialize specific logic if needed
                if (selected.isTournament()) {
                  campaignService.setFeatureValue(state, "finalsPhase", true);
                  tournamentService.initializeTournament(currentCampaign);
                }

                campaignRepository.save(currentCampaign);
                Notification.show("Jumped to chapter: " + selected.getTitle());
                refreshUI();
              }
            });
    chapterLayout.add(chapterSelect, jumpButton);
    debugContent.add(chapterLayout);

    // --- Storyline Export ---
    Button exportStorylineButton =
        new Button(
            "Export Active Storyline as Chapter",
            e -> {
              if (currentCampaign != null
                  && currentCampaign.getState().getActiveStoryline() != null) {
                String chapterJson =
                    storylineExportService.exportStorylineAsChapter(
                        currentCampaign.getState().getActiveStoryline());
                UI.getCurrent()
                    .getPage()
                    .executeJs("prompt('Copy AI Storyline Chapter JSON:', $0);", chapterJson);
                Notification.show("Storyline JSON copied to clipboard/prompt.");
              } else {
                Notification.show("No active storyline to export.");
              }
            });
    exportStorylineButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    debugContent.add(exportStorylineButton);

    // --- State & Flags (Entry Point Simulator) ---
    H4 stateHeader = new H4("State & Flags");
    stateHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM);
    debugContent.add(stateHeader);

    HorizontalLayout flagsLayout = new HorizontalLayout();
    flagsLayout.setAlignItems(Alignment.BASELINE);

    // Alignment
    Select<AlignmentType> alignSelect = new Select<>();
    alignSelect.setLabel("Alignment");
    alignSelect.setItems(AlignmentType.values());
    if (currentCampaign.getWrestler().getAlignment() != null) {
      alignSelect.setValue(currentCampaign.getWrestler().getAlignment().getAlignmentType());
    }

    IntegerField alignLevel = new IntegerField("Level");
    if (currentCampaign.getWrestler().getAlignment() != null) {
      alignLevel.setValue(currentCampaign.getWrestler().getAlignment().getLevel());
    }
    alignLevel.setMin(0);
    alignLevel.setMax(5);
    alignLevel.setStepButtonsVisible(true);

    Button setAlignButton =
        new Button(
            "Set",
            e -> {
              // We use shiftAlignment logic, or direct repo save?
              // Direct save is safer for "Force Set".
              WrestlerAlignment wa = currentCampaign.getWrestler().getAlignment();
              wa.setAlignmentType(alignSelect.getValue());
              wa.setLevel(alignLevel.getValue());
              // Save happens via cascade or we need repo
              // We didn't inject alignmentRepo here, but we can rely on persistence or existing
              // service methods
              // CampaignService has shiftAlignment but that's delta.
              // Let's just assume modifying the entity and refreshing works if transaction commits,
              // but we are in view.
              // We need to save. We have wrestlerRepo.
              wrestlerRepository.save(currentCampaign.getWrestler());
              refreshUI();
            });

    // Championship
    Button toggleTitleButton =
        new Button(
            "Toggle World Title",
            e -> {
              Wrestler w = currentCampaign.getWrestler();
              titleRepository
                  .findByName("ATW World")
                  .ifPresent(
                      title -> {
                        boolean isChamp =
                            title.getCurrentChampions().stream()
                                .anyMatch(c -> c.getId().equals(w.getId()));
                        if (isChamp) {
                          // Strip
                          titleService.vacateTitle(title.getId());
                          Notification.show("Stripped World Title");
                        } else {
                          // Award
                          titleService.awardTitleTo(title, List.of(w));
                          Notification.show("Awarded World Title");
                        }
                        refreshUI();
                      });
            });

    flagsLayout.add(alignSelect, alignLevel, setAlignButton, toggleTitleButton);
    debugContent.add(flagsLayout);

    HorizontalLayout checkLayout = new HorizontalLayout();
    // Helper to add checkbox toggles for campaign state features
    Checkbox finalsCheck =
        new Checkbox(
            "Finals Phase",
            e -> {
              campaignService.setFeatureValue(
                  currentCampaign.getState(), "finalsPhase", e.getValue());
            });
    finalsCheck.setValue(getFeatureBoolean(currentCampaign.getState(), "finalsPhase"));

    Checkbox winnerCheck =
        new Checkbox(
            "Tournament Winner",
            e -> {
              campaignService.setFeatureValue(
                  currentCampaign.getState(), "tournamentWinner", e.getValue());
            });
    winnerCheck.setValue(getFeatureBoolean(currentCampaign.getState(), "tournamentWinner"));

    checkLayout.add(finalsCheck, winnerCheck);
    debugContent.add(checkLayout);

    // --- Phase Control ---
    H4 phaseHeader = new H4("Phase Control");
    phaseHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM);
    debugContent.add(phaseHeader);

    HorizontalLayout phaseLayout = new HorizontalLayout();
    phaseLayout.add(
        new Button(
            "Force Backstage",
            e -> {
              currentCampaign
                  .getState()
                  .setCurrentPhase(
                      com.github.javydreamercsw.management.domain.campaign.CampaignPhase.BACKSTAGE);
              currentCampaign.getState().setActionsTaken(0);
              campaignRepository.save(currentCampaign);
              refreshUI();
            }));
    phaseLayout.add(
        new Button(
            "Force Post-Match",
            e -> {
              currentCampaign
                  .getState()
                  .setCurrentPhase(
                      com.github.javydreamercsw.management.domain.campaign.CampaignPhase
                          .POST_MATCH);
              campaignRepository.save(currentCampaign);
              refreshUI();
            }));
    phaseLayout.add(
        new Button(
            "Force Complete Chapter",
            e -> {
              campaignService
                  .advanceChapter(currentCampaign)
                  .ifPresent(id -> UI.getCurrent().navigate("campaign/narrative"));
              refreshUI();
            }));

    debugContent.add(phaseLayout);

    // Resources
    HorizontalLayout resourcesLayout = new HorizontalLayout();
    resourcesLayout.add(
        new Button(
            "+5 VP",
            e -> {
              currentCampaign
                  .getState()
                  .setVictoryPoints(currentCampaign.getState().getVictoryPoints() + 5);
              campaignRepository.save(currentCampaign);
              refreshUI();
            }));
    resourcesLayout.add(
        new Button(
            "+5 Skill Tokens",
            e -> {
              currentCampaign
                  .getState()
                  .setSkillTokens(currentCampaign.getState().getSkillTokens() + 5);
              campaignRepository.save(currentCampaign);
              refreshUI();
            }));
    resourcesLayout.add(
        new Button(
            "Set Bumps=2",
            e -> {
              currentCampaign.getWrestler().setBumps(2);
              wrestlerRepository.save(currentCampaign.getWrestler());
              refreshUI();
            }));
    resourcesLayout.add(
        new Button(
            "Heal All",
            e -> {
              currentCampaign.getWrestler().setBumps(0);
              currentCampaign
                  .getWrestler()
                  .getActiveInjuries()
                  .forEach(
                      i -> {
                        i.heal();
                      });
              wrestlerRepository.save(currentCampaign.getWrestler());
              refreshUI();
            }));
    debugContent.add(new Span("Resources & Status"), resourcesLayout);

    // Quick Loop
    HorizontalLayout matchLayout = new HorizontalLayout();
    matchLayout.add(
        new Button(
            "Sim. Win (Face)",
            e -> {
              campaignService.shiftAlignment(currentCampaign, 1);
              simulateMatch(true);
            }));
    matchLayout.add(
        new Button(
            "Sim. Win (Heel)",
            e -> {
              campaignService.shiftAlignment(currentCampaign, -1);
              simulateMatch(true);
            }));
    matchLayout.add(new Button("Sim. Loss", e -> simulateMatch(false)));

    debugContent.add(new Span("Match Simulation"), matchLayout);

    Details debugDetails = new Details("🛠️ Debug / Dev Tools", debugContent);
    debugDetails.setOpened(false);
    debugDetails.addClassNames(
        LumoUtility.Width.FULL,
        LumoUtility.Margin.Top.LARGE,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Border.ALL,
        LumoUtility.BorderColor.CONTRAST_10);
    add(debugDetails);
  }

  private void simulateMatch(boolean win) {
    wrestlerRepository.findAll().stream()
        .filter(w -> !w.equals(currentCampaign.getWrestler()))
        .findFirst()
        .ifPresentOrElse(
            opponent -> {
              campaignService.createMatchForEncounter(
                  currentCampaign, opponent.getName(), "Debug Match Simulation", "One on One");
              campaignService.processMatchResult(currentCampaign, win);
              refreshUI();
            },
            () ->
                com.vaadin.flow.component.notification.Notification.show(
                    "No opponent found for simulation!"));
  }
}
