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

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import com.github.javydreamercsw.management.service.campaign.BackstageEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign/backstage-situation", layout = MainLayout.class)
@PageTitle("Backstage Situation")
@PermitAll
@Slf4j
public class BackstageEncounterView extends VerticalLayout {

  private final BackstageEncounterService backstageEncounterService;
  private final CampaignRepository campaignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final SecurityUtils securityUtils;
  private final CampaignService campaignService;

  private Campaign currentCampaign;
  private Wrestler playerWrestler;

  private VerticalLayout narrativeContainer;
  private HorizontalLayout choicesContainer;
  private ProgressBar progressBar;

  @Autowired
  public BackstageEncounterView(
      BackstageEncounterService backstageEncounterService,
      CampaignRepository campaignRepository,
      WrestlerRepository wrestlerRepository,
      SecurityUtils securityUtils,
      CampaignService campaignService) {
    this.backstageEncounterService = backstageEncounterService;
    this.campaignRepository = campaignRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.securityUtils = securityUtils;
    this.campaignService = campaignService;

    setSpacing(true);
    setPadding(true);
    setAlignItems(FlexComponent.Alignment.CENTER);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              com.github.javydreamercsw.base.domain.account.Account account = user.getAccount();
              java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
              playerWrestler =
                  wrestlers.stream()
                      .filter(w -> w.getId().equals(account.getActiveWrestlerId()))
                      .findFirst()
                      .orElse(wrestlers.isEmpty() ? null : wrestlers.get(0));

              if (playerWrestler != null) {
                campaignService
                    .getCampaignForWrestler(playerWrestler)
                    .ifPresent(c -> currentCampaign = c);
              }
            });
  }

  private void initUI() {
    H2 title = new H2("Backstage Situation");
    title.setId("backstage-situation-title");
    add(title);

    narrativeContainer = new VerticalLayout();
    narrativeContainer.setId("narrative-container");
    narrativeContainer.setWidthFull();
    narrativeContainer.setMaxWidth("800px");
    narrativeContainer.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.MEDIUM);

    choicesContainer = new HorizontalLayout();
    choicesContainer.setId("choices-container");
    choicesContainer.setSpacing(true);
    choicesContainer.setPadding(true);
    choicesContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    progressBar = new ProgressBar();
    progressBar.setId("backstage-situation-progress-bar");
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    progressBar.setWidth("300px");

    add(narrativeContainer, progressBar, choicesContainer);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    if (currentCampaign != null && narrativeContainer.getComponentCount() == 0) {
      startSituation();
    }
  }

  private void startSituation() {
    showLoading(true);
    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    GeneralSecurityUtils.runAsAdmin(
        () -> {
          try {
            log.info("Generating backstage situation synchronously");
            CampaignEncounterResponseDTO encounter =
                backstageEncounterService.generateBackstageEncounter(currentCampaign);
            displaySituation(encounter);
          } catch (Exception e) {
            log.error("Failed to start backstage situation", e);
            Notification.show("Failed to connect to the Backstage Director.")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            addBackButton();
          } finally {
            showLoading(false);
          }
        });
  }

  private void displaySituation(@NonNull CampaignEncounterResponseDTO context) {
    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    Paragraph p = new Paragraph(context.getNarrative());
    p.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.LineHeight.MEDIUM);
    narrativeContainer.add(p);

    for (CampaignEncounterResponseDTO.Choice choice : context.getChoices()) {
      Button choiceBtn = new Button(choice.getLabel());
      choiceBtn.setId("backstage-choice-" + choice.getLabel().replace(" ", "-").toLowerCase());
      choiceBtn.setTooltipText(choice.getText());
      choiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      choiceBtn.addClickListener(e -> handleChoice(choice));
      choicesContainer.add(choiceBtn);
    }
  }

  private void handleChoice(@NonNull CampaignEncounterResponseDTO.Choice choice) {
    log.info("Backstage choice chosen: {}", choice.getLabel());
    showLoading(true);
    choicesContainer.removeAll();
    narrativeContainer.removeAll();

    GeneralSecurityUtils.runAsAdmin(
        () -> {
          try {
            log.info("Processing backstage choice synchronously: {}", choice.getLabel());
            backstageEncounterService.recordBackstageChoice(currentCampaign, choice);
            displayOutcome(choice);
          } catch (Exception e) {
            log.error("Failed to process backstage choice synchronously", e);
            Notification.show("Failed to resolve the situation: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            addBackButton();
          } finally {
            showLoading(false);
          }
        });
  }

  private void displayOutcome(@NonNull CampaignEncounterResponseDTO.Choice choice) {
    log.info("Displaying backstage situation outcome");

    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    VerticalLayout resultLayout = new VerticalLayout();
    resultLayout.setPadding(false);
    resultLayout.setSpacing(true);

    Span chosenText = new Span("You decided: '" + choice.getText() + "'");
    chosenText.getStyle().set("font-style", "italic");
    chosenText.addClassNames(LumoUtility.TextColor.PRIMARY);
    resultLayout.add(chosenText);

    if (choice.getOutcomeText() != null && !choice.getOutcomeText().isBlank()) {
      Paragraph outcome = new Paragraph(choice.getOutcomeText());
      outcome.addClassNames(LumoUtility.LineHeight.MEDIUM);
      resultLayout.add(new Span("Outcome:"), outcome);
    }

    if (choice.getMomentumBonus() != 0) {
      String bonusStr = (choice.getMomentumBonus() > 0 ? "+" : "") + choice.getMomentumBonus();
      Span momentumSpan = new Span("Momentum Bonus for next match: " + bonusStr);
      momentumSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
      resultLayout.add(momentumSpan);
    }

    if (choice.getAlignmentShift() != 0) {
      String shiftStr = (choice.getAlignmentShift() > 0 ? "Face (+)" : "Heel (-)") + " shift";
      Span alignmentSpan = new Span("Alignment Effect: " + shiftStr);
      alignmentSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      resultLayout.add(alignmentSpan);
    }

    narrativeContainer.add(resultLayout);

    Button finishBtn =
        new Button("Finish Interaction", e -> UI.getCurrent().navigate("campaign/actions"));
    finishBtn.setId("finish-backstage-situation-button");
    finishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    choicesContainer.add(finishBtn);
    log.info("Backstage situation outcome display complete");
  }

  private void showLoading(boolean loading) {
    progressBar.setVisible(loading);
    choicesContainer.setVisible(!loading);
  }

  private void addBackButton() {
    Button backBtn =
        new Button("Back to Actions", e -> UI.getCurrent().navigate("campaign/actions"));
    choicesContainer.add(backBtn);
  }
}
