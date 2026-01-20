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
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignEncounterService;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "campaign/narrative", layout = MainLayout.class)
@PageTitle("Story Narrative")
@PermitAll
@Slf4j
public class CampaignNarrativeView extends VerticalLayout {

  private final CampaignRepository campaignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final CampaignEncounterService encounterService;
  private final CampaignService campaignService;
  private final SecurityUtils securityUtils;

  private Campaign currentCampaign;
  private VerticalLayout narrativeContainer;
  private HorizontalLayout choicesContainer;
  private ProgressBar progressBar;

  @Autowired
  public CampaignNarrativeView(
      CampaignRepository campaignRepository,
      WrestlerRepository wrestlerRepository,
      CampaignEncounterService encounterService,
      CampaignService campaignService,
      SecurityUtils securityUtils) {
    this.campaignRepository = campaignRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.encounterService = encounterService;
    this.campaignService = campaignService;
    this.securityUtils = securityUtils;

    setSpacing(true);
    setPadding(true);
    setAlignItems(FlexComponent.Alignment.CENTER);

    loadCampaign();
    initUI();
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    if (currentCampaign != null && narrativeContainer.getComponentCount() == 0) {
      generateNextEncounter();
    }
  }

  private void loadCampaign() {
    securityUtils
        .getAuthenticatedUser()
        .flatMap(
            user ->
                wrestlerRepository
                    .findByAccount(user.getAccount())
                    .flatMap(campaignRepository::findActiveByWrestler))
        .ifPresent(campaign -> currentCampaign = campaign);
  }

  private void initUI() {
    if (currentCampaign == null) {
      add(new H2("Story Narrative"));
      add(new Span("No active campaign found."));
      return;
    }

    add(new H2("Story: " + campaignService.getCurrentChapter(currentCampaign).getTitle()));

    narrativeContainer = new VerticalLayout();
    narrativeContainer.setWidthFull();
    narrativeContainer.setMaxWidth("800px");
    narrativeContainer.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.MEDIUM);

    choicesContainer = new HorizontalLayout();
    choicesContainer.setSpacing(true);
    choicesContainer.setPadding(true);
    choicesContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    progressBar.setWidth("300px");

    add(narrativeContainer, progressBar, choicesContainer);
  }

  private void generateNextEncounter() {
    showLoading(true);
    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    // Use a background thread for AI generation to keep UI responsive
    UI ui = UI.getCurrent();
    SecurityContext context = SecurityContextHolder.getContext();

    Runnable backgroundTask =
        () -> {
          try {
            CampaignEncounterResponseDTO encounter =
                encounterService.generateEncounter(currentCampaign);
            ui.access(
                () -> {
                  displayEncounter(encounter);
                  showLoading(false);
                });
          } catch (Exception e) {
            log.error("Failed to generate encounter", e);
            ui.access(
                () -> {
                  Notification.show("Failed to connect to the Story Director. Please try again.");
                  showLoading(false);
                  addRetryButton();
                });
          }
        };

    // Wrap the task to propagate the security context
    new Thread(new DelegatingSecurityContextRunnable(backgroundTask, context)).start();
  }

  private void displayEncounter(CampaignEncounterResponseDTO encounter) {
    Paragraph p = new Paragraph(encounter.getNarrative());
    p.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.LineHeight.MEDIUM);
    narrativeContainer.add(p);

    for (CampaignEncounterResponseDTO.Choice choice : encounter.getChoices()) {
      Button choiceBtn = new Button(choice.getLabel());
      choiceBtn.setTooltipText(choice.getText());
      choiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      choiceBtn.addClickListener(e -> handleChoice(encounter, choice));
      choicesContainer.add(choiceBtn);
    }
  }

  private void handleChoice(
      CampaignEncounterResponseDTO response, CampaignEncounterResponseDTO.Choice choice) {
    encounterService.recordEncounterChoice(currentCampaign, choice);

    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    Paragraph p = new Paragraph("You chose: " + choice.getText());
    p.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);
    narrativeContainer.add(p);

    if ("MATCH".equals(choice.getNextPhase())) {
      try {
        com.github.javydreamercsw.management.domain.show.segment.Segment match =
            campaignService.createMatchForEncounter(
                currentCampaign,
                choice.getForcedOpponentName(),
                response.getNarrative(),
                choice.getMatchType());

        Button startMatchBtn =
            new Button("Proceed to Match", e -> UI.getCurrent().navigate("match/" + match.getId()));
        startMatchBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        choicesContainer.add(startMatchBtn);
      } catch (Exception e) {
        log.error("Failed to create match segment", e);
        Notification.show("Error setting up match. Returning to dashboard.");
        Button dashboardBtn =
            new Button("Back to Dashboard", e2 -> UI.getCurrent().navigate("campaign"));
        choicesContainer.add(dashboardBtn);
      }
    } else if ("BACKSTAGE".equals(choice.getNextPhase())) {
      campaignService.completePostMatch(currentCampaign);
      Button dashboardBtn =
          new Button("Return to Management", e -> UI.getCurrent().navigate("campaign"));
      dashboardBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      choicesContainer.add(dashboardBtn);
    } else {
      // POST_MATCH or other narrative continuation
      Button continueBtn = new Button("Continue Story", e -> generateNextEncounter());
      choicesContainer.add(continueBtn);
    }
  }

  private void showLoading(boolean loading) {
    progressBar.setVisible(loading);
    choicesContainer.setVisible(!loading);
  }

  private void addRetryButton() {
    Button retryBtn = new Button("Retry Story Director", e -> generateNextEncounter());
    choicesContainer.add(retryBtn);
  }
}
