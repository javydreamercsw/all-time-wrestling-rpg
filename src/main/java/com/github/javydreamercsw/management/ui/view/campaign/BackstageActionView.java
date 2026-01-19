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

import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.service.campaign.BackstageActionService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign/actions", layout = MainLayout.class)
@PageTitle("Backstage Actions")
@PermitAll
@Slf4j
public class BackstageActionView extends VerticalLayout {

  private final BackstageActionService backstageActionService;
  private final CampaignRepository campaignRepository;

  private Campaign currentCampaign;

  @Autowired
  public BackstageActionView(
      BackstageActionService backstageActionService, CampaignRepository campaignRepository) {
    this.backstageActionService = backstageActionService;
    this.campaignRepository = campaignRepository;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    // Simplistic loading: Pick first active campaign.
    // In real app, this would be context-aware.
    Optional<Campaign> c =
        campaignRepository.findAll().stream().findFirst(); // TODO: Filter by user
    c.ifPresent(campaign -> currentCampaign = campaign);
  }

  private void initUI() {
    add(new H2("Backstage Actions"));

    if (currentCampaign == null) {
      add(new Paragraph("No active campaign found."));
      add(new Button("Back", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
      return;
    }

    HorizontalLayout buttonLayout = new HorizontalLayout();

    buttonLayout.add(
        createActionButton(
            "Training (Drive)", BackstageActionType.TRAINING, 5)); // Hardcoded attribute for now
    buttonLayout.add(createActionButton("Recovery (Resilience)", BackstageActionType.RECOVERY, 5));
    buttonLayout.add(createActionButton("Promo (Charisma)", BackstageActionType.PROMO, 5));
    buttonLayout.add(
        createActionButton("Attack (Brawl)", BackstageActionType.ATTACK, 5)); // TODO: Heel check

    add(buttonLayout);
    add(
        new Button(
            "Back to Dashboard", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
  }

  private Button createActionButton(String label, BackstageActionType type, int attributeValue) {
    return new Button(
        label,
        e -> {
          BackstageActionService.ActionOutcome outcome =
              backstageActionService.performAction(currentCampaign, type, attributeValue);
          Notification.show(outcome.description());
        });
  }
}
