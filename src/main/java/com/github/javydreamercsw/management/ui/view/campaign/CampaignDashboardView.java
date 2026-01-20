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
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.component.AlignmentTrackComponent;
import com.github.javydreamercsw.management.ui.component.CampaignAbilityCardComponent;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
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
  private final SecurityUtils securityUtils;

  private Campaign currentCampaign;

  @Autowired
  public CampaignDashboardView(
      CampaignRepository campaignRepository,
      CampaignService campaignService,
      WrestlerRepository wrestlerRepository,
      CampaignAbilityCardRepository cardRepository,
      SecurityUtils securityUtils) {
    this.campaignRepository = campaignRepository;
    this.campaignService = campaignService;
    this.wrestlerRepository = wrestlerRepository;
    this.cardRepository = cardRepository;
    this.securityUtils = securityUtils;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              wrestlerRepository
                  .findByAccount(user.getAccount())
                  .ifPresent(
                      wrestler -> {
                        campaignRepository
                            .findActiveByWrestler(wrestler)
                            .ifPresent(campaign -> currentCampaign = campaign);
                      });
            });
  }

  private void initUI() {
    if (currentCampaign == null) {
      add(new H2("Campaign Mode"));
      add(
          new Span(
              "No active campaign found. To start a campaign, please navigate to the Wrestler"
                  + " List and use the 'Start Campaign' action on your assigned wrestler."));
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
    if (state.getCurrentChapter() == 2) {
      if (state.isFinalsPhase()) {
        addTournamentBracket(state);
      } else {
        addTournamentTracker(state);
      }
    }

    HorizontalLayout statsLayout = new HorizontalLayout();
    statsLayout.add(createStatCard("Chapter", String.valueOf(state.getCurrentChapter())));
    statsLayout.add(createStatCard("Victory Points", String.valueOf(state.getVictoryPoints())));
    statsLayout.add(createStatCard("Skill Tokens", String.valueOf(state.getSkillTokens())));
    statsLayout.add(createStatCard("Bumps", String.valueOf(state.getBumps())));

    if (alignment != null) {
      statsLayout.add(
          createStatCard(
              "Alignment",
              (alignment.getAlignmentType() == AlignmentType.NEUTRAL
                      ? "NEUTRAL"
                      : alignment.getAlignmentType().name())
                  + " (Lvl "
                  + alignment.getLevel()
                  + ")"));
    }

    add(statsLayout);

    HorizontalLayout healthLayout = new HorizontalLayout();
    healthLayout.add(
        createStatCard(
            "Health",
            wrestler.getCurrentHealthWithPenalties() + " / " + wrestler.getStartingHealth()));
    healthLayout.add(
        createStatCard(
            "Penalties",
            "HP: -"
                + state.getHealthPenalty()
                + ", Stam: -"
                + state.getStaminaPenalty()
                + ", Hand: -"
                + state.getHandSizePenalty()));
    add(healthLayout);

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

    // Actions
    add(new H4("Actions"));
    HorizontalLayout actionsLayout = new HorizontalLayout();
    actionsLayout.add(
        new Button("Backstage Actions", e -> UI.getCurrent().navigate(BackstageActionView.class)));
    actionsLayout.add(
        new Button(
            "Story Narrative",
            e -> UI.getCurrent().navigate("campaign/narrative"))); // To be implemented
    add(actionsLayout);

    // Global Card Library
    addGlobalCardLibrary();
  }

  private void addPendingPicksSection(Campaign campaign) {
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

  private void addTournamentBracket(CampaignState state) {
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

  private Div createBracketBox(String name, boolean isPlayer) {
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

  private void addTournamentTracker(CampaignState state) {
    com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO.ChapterRules rules =
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

  private VerticalLayout createStatCard(String label, String value) {
    VerticalLayout card = new VerticalLayout();
    card.addClassName("stat-card"); // CSS class needed
    card.setPadding(true);
    card.setSpacing(false);
    card.getStyle().set("border", "1px solid #ccc");
    card.getStyle().set("border-radius", "5px");

    card.add(new Span(label));
    Span valueSpan = new Span(value);
    valueSpan.getStyle().set("font-weight", "bold");
    valueSpan.getStyle().set("font-size", "1.2em");
    card.add(valueSpan);
    return card;
  }
}
