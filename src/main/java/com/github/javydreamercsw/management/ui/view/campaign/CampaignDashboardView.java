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
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.ui.component.AlignmentTrackComponent;
import com.github.javydreamercsw.management.ui.component.CampaignAbilityCardComponent;
import com.github.javydreamercsw.management.ui.component.DashboardCard;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import jakarta.annotation.security.PermitAll;
import java.util.List;
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

  private Campaign currentCampaign;

  @Autowired
  public CampaignDashboardView(
      CampaignRepository campaignRepository,
      CampaignService campaignService,
      WrestlerRepository wrestlerRepository,
      CampaignAbilityCardRepository cardRepository,
      CampaignUpgradeService upgradeService,
      SecurityUtils securityUtils) {
    this.campaignRepository = campaignRepository;
    this.campaignService = campaignService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardRepository = cardRepository;
    this.upgradeService = upgradeService;
    this.securityUtils = securityUtils;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    log.info("Loading campaign for current user...");
    securityUtils
        .getAuthenticatedUser()
        .ifPresentOrElse(
            user -> {
              log.info("Authenticated user: {}", user.getUsername());
              wrestlerRepository
                  .findByAccount(user.getAccount())
                  .ifPresentOrElse(
                      wrestler -> {
                        log.info("Wrestler found: {}", wrestler.getName());
                        campaignRepository
                            .findActiveByWrestler(wrestler)
                            .ifPresentOrElse(
                                campaign -> {
                                  log.info("Active campaign found: {}", campaign.getId());
                                  currentCampaign = campaign;
                                },
                                () ->
                                    log.info(
                                        "No active campaign found for wrestler: {}",
                                        wrestler.getName()));
                      },
                      () -> log.warn("No wrestler found for account: {}", user.getUsername()));
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
                          log.info("Authenticated user found: {}", user.getUsername());

                          wrestlerRepository
                              .findByAccount(user.getAccount())
                              .ifPresentOrElse(
                                  wrestler -> {
                                    log.info("Wrestler found for user: {}", wrestler.getName());

                                    campaignService.startCampaign(wrestler);

                                    log.info("Campaign started successfully");

                                    refreshUI();
                                  },
                                  () -> {
                                    log.info(
                                        "No wrestler found for account {}, assigning first"
                                            + " available...",
                                        user.getUsername());

                                    List<Wrestler> all = wrestlerRepository.findAll();

                                    if (!all.isEmpty()) {

                                      Wrestler first = all.get(0);

                                      first.setAccount(user.getAccount());

                                      first.setIsPlayer(true);

                                      wrestlerRepository.save(first);

                                      log.info(
                                          "Assigned wrestler {} to account {}",
                                          first.getName(),
                                          user.getUsername());

                                      campaignService.startCampaign(first);

                                      refreshUI();

                                    } else {

                                      log.warn("No wrestlers exist in the database at all!");
                                    }
                                  });
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

    // Alignment Track
    if (alignment != null) {
      add(new AlignmentTrackComponent(alignment));
    }

    // Tournament Tracker (Chapter 2)
    if ("ch2_tournament".equals(state.getCurrentChapterId())) {
      if (state.isFinalsPhase()) {
        addTournamentBracket();
      } else {
        addTournamentTracker(state);
      }
    }

    // Stats Section
    add(new DashboardCard(currentCampaign));

    // Pending Picks Section
    addPendingPicksSection(currentCampaign);

    // My Cards Section
    add(new H4("My Ability Cards"));
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
    add(myCardsLayout);

    // Purchased Skills Section
    add(new H4("Purchased Skills"));
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
    add(skillsLayout);

    // Skill Upgrades Section
    addSkillUpgradesSection(currentCampaign);

    // Actions
    add(new H4("Actions"));

    Span actionsInfo =
        new Span("Backstage actions are only available before continuing the story narrative.");
    actionsInfo.addClassNames(FontSize.SMALL, TextColor.SECONDARY, Margin.Bottom.SMALL);
    add(actionsInfo);

    if (state.getCurrentPhase()
        == com.github.javydreamercsw.management.domain.campaign.CampaignPhase.BACKSTAGE) {
      Span remainingActions =
          new Span("Remaining actions for today: " + (2 - state.getActionsTaken()));
      remainingActions.addClassNames(FontSize.SMALL, FontWeight.BOLD, TextColor.PRIMARY);
      add(remainingActions);
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
                  log.info("Completing post-match phase before navigating to narrative.");
                  campaignService.completePostMatch(currentCampaign);
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

    add(actionsLayout);

    // Global Card Library
    addGlobalCardLibrary();
  }

  private void addPendingPicksSection(@NonNull Campaign campaign) {
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
    add(pendingSection);
  }

  private void addSkillUpgradesSection(@NonNull Campaign campaign) {
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

    for (var upgrade : available) {
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
    add(upgradeSection);
  }

  private void addTournamentBracket() {
    VerticalLayout bracketContainer = new VerticalLayout();
    bracketContainer.setPadding(false);
    bracketContainer.setSpacing(true);
    bracketContainer.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);

    H4 title = new H4("Tournament Finals Bracket");
    title.addClassNames(LumoUtility.Margin.NONE);
    bracketContainer.add(title);

    HorizontalLayout rounds = new HorizontalLayout();
    rounds.setSpacing(false);
    rounds.setAlignItems(Alignment.CENTER);

    // Semi-Finals
    VerticalLayout semiFinals = new VerticalLayout();
    semiFinals.setSpacing(true);
    semiFinals.setWidth("200px");
    semiFinals.add(createBracketBox(currentCampaign.getWrestler().getName(), true));
    semiFinals.add(createBracketBox("Opponent A", false));
    semiFinals.add(new Div()); // Spacer
    semiFinals.add(createBracketBox("Opponent B", false));
    semiFinals.add(createBracketBox("Opponent C", false));

    // Finals
    VerticalLayout finals = new VerticalLayout();
    finals.setSpacing(true);
    finals.setWidth("200px");
    finals.setPadding(true);
    finals.add(createBracketBox("TBD", false));

    rounds.add(semiFinals, finals);
    bracketContainer.add(rounds);
    add(bracketContainer);
  }

  private Div createBracketBox(@NonNull String name, boolean isPlayer) {
    Div box = new Div();
    box.setText(name);
    box.setWidthFull();
    box.setHeight("40px");
    box.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.Padding.Horizontal.SMALL,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.FontSize.SMALL);

    if (isPlayer) {
      box.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
      box.getStyle().set("border-color", "var(--lumo-primary-color)");
      box.getStyle().set("font-weight", "bold");
    } else {
      box.addClassNames(LumoUtility.Background.CONTRAST_5);
    }
    return box;
  }

  private void addTournamentTracker(@NonNull CampaignState state) {
    CampaignChapterDTO.ChapterRules rules =
        campaignService.getCurrentChapter(currentCampaign).getRules();

    if (rules.getQualifyingMatches() <= 0) return;

    VerticalLayout tracker = new VerticalLayout();
    tracker.setPadding(false);
    tracker.setSpacing(true);
    tracker.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);

    H4 title = new H4("Tournament Qualifying Tracker");
    title.addClassNames(LumoUtility.Margin.NONE);
    tracker.add(title);

    HorizontalLayout slots = new HorizontalLayout();
    slots.setSpacing(true);

    int total = rules.getQualifyingMatches();
    int wins = state.getWins();
    int losses = state.getLosses();

    for (int i = 0; i < total; i++) {
      Div slot = new Div();
      slot.setWidth("40px");
      slot.setHeight("40px");
      slot.addClassNames(
          LumoUtility.Display.FLEX,
          LumoUtility.AlignItems.CENTER,
          LumoUtility.JustifyContent.CENTER,
          LumoUtility.BorderRadius.SMALL,
          LumoUtility.FontWeight.BOLD,
          LumoUtility.Border.ALL);

      if (i < wins) {
        slot.setText("W");
        slot.getStyle().set("background-color", "#c8e6c9");
        slot.getStyle().set("color", "#2e7d32");
        slot.getStyle().set("border-color", "#2e7d32");
      } else if (i < (wins + losses)) {
        slot.setText("L");
        slot.getStyle().set("background-color", "#ffcdd2");
        slot.getStyle().set("color", "#c62828");
        slot.getStyle().set("border-color", "#c62828");
      } else {
        slot.setText("?");
        slot.addClassNames(LumoUtility.TextColor.DISABLED, LumoUtility.Background.CONTRAST_5);
      }
      slots.add(slot);
    }

    tracker.add(slots);

    String statusText;
    if (wins >= rules.getMinWinsToQualify()) {
      statusText = "STATUS: QUALIFIED FOR FINALS!";
    } else if (losses > (total - rules.getMinWinsToQualify())) {
      statusText = "STATUS: ELIMINATED FROM TOURNAMENT";
    } else {
      statusText = "NEED " + (rules.getMinWinsToQualify() - wins) + " MORE WINS TO QUALIFY";
    }

    Span status = new Span(statusText);
    status.addClassNames(
        LumoUtility.FontWeight.BOLD,
        wins >= rules.getMinWinsToQualify()
            ? LumoUtility.TextColor.SUCCESS
            : LumoUtility.TextColor.PRIMARY);
    tracker.add(status);

    add(tracker);
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
}
