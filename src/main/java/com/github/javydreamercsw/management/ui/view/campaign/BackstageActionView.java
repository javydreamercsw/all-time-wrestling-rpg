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
import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageActionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.DashboardCard;
import com.github.javydreamercsw.management.ui.component.WrestlerSummaryCard;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign/actions", layout = MainLayout.class)
@PageTitle("Backstage Actions")
@PermitAll
@Slf4j
public class BackstageActionView extends VerticalLayout {

  private final BackstageActionService backstageActionService;
  private final CampaignRepository campaignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final SecurityUtils securityUtils;

  private Campaign currentCampaign;

  @Autowired
  public BackstageActionView(
      BackstageActionService backstageActionService,
      CampaignRepository campaignRepository,
      WrestlerRepository wrestlerRepository,
      WrestlerService wrestlerService,
      SecurityUtils securityUtils) {
    this.backstageActionService = backstageActionService;
    this.campaignRepository = campaignRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;

    setSpacing(true);
    setPadding(true);
    setAlignItems(Alignment.CENTER);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              com.github.javydreamercsw.base.domain.account.Account account = user.getAccount();
              java.util.List<com.github.javydreamercsw.management.domain.wrestler.Wrestler>
                  wrestlers = wrestlerRepository.findByAccount(account);
              com.github.javydreamercsw.management.domain.wrestler.Wrestler active =
                  wrestlers.stream()
                      .filter(w -> w.getId().equals(account.getActiveWrestlerId()))
                      .findFirst()
                      .orElse(wrestlers.isEmpty() ? null : wrestlers.get(0));

              if (active != null) {
                campaignRepository.findActiveByWrestler(active).ifPresent(c -> currentCampaign = c);
              }
            });
  }

  private void initUI() {
    if (currentCampaign == null) {
      add(new H2("Backstage Actions"));
      add(new Paragraph("No active campaign found."));
      add(new Button("Back", e -> UI.getCurrent().navigate("campaign")));
      return;
    }

    CampaignState state = currentCampaign.getState();
    var wrestler = currentCampaign.getWrestler();

    // Header
    VerticalLayout header = new VerticalLayout();
    header.setPadding(false);
    header.setSpacing(false);
    header.setAlignItems(Alignment.CENTER);
    header.addClassNames(Margin.Bottom.MEDIUM);

    H2 title = new H2("Backstage Area");
    title.addClassNames(Margin.Top.NONE, Margin.Bottom.XSMALL);
    header.add(title);

    Span subtitle = new Span("Take up to 2 actions each day to prepare for your next match.");
    subtitle.addClassNames(FontSize.SMALL, TextColor.SECONDARY);
    header.add(subtitle);
    add(header);

    HorizontalLayout mainContent = new HorizontalLayout();
    mainContent.setWidthFull();
    mainContent.setMaxWidth("1200px");
    mainContent.addClassNames(FlexWrap.WRAP, Gap.MEDIUM, JustifyContent.CENTER);

    // Left: Stats & Status
    VerticalLayout leftCol = new VerticalLayout();
    leftCol.setPadding(false);
    leftCol.setWidth("auto");
    leftCol.setMinWidth("300px");
    leftCol.setFlexGrow(1);

    DashboardCard statusCard = new DashboardCard("Your Status");
    statusCard.add(new WrestlerSummaryCard(wrestler, wrestlerService, true));

    Span actionsCount = new Span("Actions taken today: " + state.getActionsTaken() + " / 2");
    actionsCount.addClassNames(
        FontSize.MEDIUM,
        FontWeight.BOLD,
        state.getActionsTaken() >= 2 ? TextColor.ERROR : TextColor.SUCCESS,
        Margin.Top.MEDIUM);
    statusCard.add(actionsCount);
    leftCol.add(statusCard);

    // Right: Actions
    VerticalLayout rightCol = new VerticalLayout();
    rightCol.setPadding(false);
    rightCol.setWidth("auto");
    rightCol.setMinWidth("400px");
    rightCol.setFlexGrow(2);

    DashboardCard actionsCard = new DashboardCard("Available Actions");
    VerticalLayout actionsList = new VerticalLayout();
    actionsList.setPadding(false);
    actionsList.setSpacing(true);

    boolean actionsAvailable = state.getActionsTaken() < 2;
    boolean needsRecovery = wrestler.getBumps() > 0 || !wrestler.getActiveInjuries().isEmpty();

    actionsList.add(
        createActionButton(
            "💪 Training",
            "Improve your conditioning. Roll 1d6 + Drive vs Difficulty 4.",
            BackstageActionType.TRAINING,
            wrestler.getDrive(),
            actionsAvailable));

    actionsList.add(
        createActionButton(
            "🏥 Recovery",
            needsRecovery
                ? "Heal your minor bumps. Roll 1d6 + Resilience vs Difficulty 4."
                : "You don't have any bumps or injuries to recover from.",
            BackstageActionType.RECOVERY,
            wrestler.getResilience(),
            actionsAvailable && needsRecovery));

    if (state.isPromoUnlocked()) {
      actionsList.add(
          createActionButton(
              "🎤 Promo",
              "Build your hype. Roll 1d6 + Charisma vs Difficulty 4.",
              BackstageActionType.PROMO,
              wrestler.getCharisma(),
              actionsAvailable));
    }

    if (state.isAttackUnlocked()) {
      actionsList.add(
          createActionButton(
              "👊 Backstage Attack",
              "Intimidate your rivals. Roll 1d6 + Brawl vs Difficulty 4.",
              BackstageActionType.ATTACK,
              wrestler.getBrawl(),
              actionsAvailable));
    }

    actionsCard.add(actionsList);
    rightCol.add(actionsCard);

    // Bottom: How it Works
    DashboardCard mechanicsCard = new DashboardCard("How Backstage Actions Work");
    mechanicsCard.setMaxWidth("1200px");
    VerticalLayout mechContent = new VerticalLayout();
    mechContent.setPadding(false);
    mechContent.setSpacing(false);

    mechContent.add(
        new Paragraph("Every day before continuing your story, you can take two actions."));
    mechContent.add(
        new Span(
            "• Each action uses one of your primary attributes (Drive, Resilience, Charisma,"
                + " Brawl)."));
    mechContent.add(
        new Span("• A digital 6-sided die (1d6) is rolled and added to your attribute value."));
    mechContent.add(new Span("• If the total is 4 or higher, the action is successful!"));
    mechContent.add(
        new Span("• Successes grant Skill Tokens, heal bumps, or provide other unique bonuses."));

    mechContent
        .getChildren()
        .forEach(
            c -> {
              if (c instanceof Span) {
                ((Span) c).addClassNames(FontSize.SMALL, Margin.Bottom.XSMALL);
              }
            });

    mechanicsCard.add(mechContent);

    mainContent.add(leftCol, rightCol);
    add(mainContent, mechanicsCard);

    Button backBtn = new Button("Back to Dashboard", e -> UI.getCurrent().navigate("campaign"));
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    add(backBtn);
  }

  private HorizontalLayout createActionButton(
      String label, String description, BackstageActionType type, int attrValue, boolean enabled) {
    HorizontalLayout row = new HorizontalLayout();
    row.setWidthFull();
    row.setAlignItems(Alignment.CENTER);
    row.addClassNames(Padding.SMALL, Border.BOTTOM, BorderColor.CONTRAST_10);

    VerticalLayout textPart = new VerticalLayout();
    textPart.setPadding(false);
    textPart.setSpacing(false);

    Span name = new Span(label);
    name.addClassNames(FontWeight.BOLD);

    Span desc = new Span(description);
    desc.addClassNames(FontSize.XSMALL, TextColor.SECONDARY);

    textPart.add(name, desc);

    Button actionBtn = new Button("Perform");
    actionBtn.setId("action-button-" + type.name());
    actionBtn.setEnabled(enabled);
    actionBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    actionBtn.addClickListener(
        e -> {
          if (type == BackstageActionType.PROMO) {
            UI.getCurrent().navigate(PromoView.class);
            return;
          }
          var outcome = backstageActionService.performAction(currentCampaign, type, attrValue);
          if (outcome.successes() > 0) {
            Notification.show("Success! " + outcome.description())
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } else {
            Notification.show("Failure... " + outcome.description())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
          refreshUI();
        });

    row.add(textPart, actionBtn);
    row.expand(textPart);
    return row;
  }

  private void refreshUI() {
    removeAll();
    loadCampaign();
    initUI();
  }
}
