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

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign/abilities", layout = MainLayout.class)
@PageTitle("Ability Tree")
@PermitAll
@Slf4j
public class AbilityTreeView extends VerticalLayout {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;

  private Campaign currentCampaign;
  private Span tokenDisplay;

  @Autowired
  public AbilityTreeView(
      CampaignRepository campaignRepository, CampaignStateRepository campaignStateRepository) {
    this.campaignRepository = campaignRepository;
    this.campaignStateRepository = campaignStateRepository;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    // Simplistic loading: Pick first active campaign.
    Optional<Campaign> c = campaignRepository.findAll().stream().findFirst();
    c.ifPresent(campaign -> currentCampaign = campaign);
  }

  private void initUI() {
    add(new H2("Ability Tree"));

    if (currentCampaign == null) {
      add(new Paragraph("No active campaign found."));
      add(new Button("Back", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
      return;
    }

    tokenDisplay =
        new Span("Available Skill Tokens: " + currentCampaign.getState().getSkillTokens());
    tokenDisplay.getStyle().set("font-weight", "bold");
    add(tokenDisplay);

    add(new Paragraph("Spend tokens to unlock abilities."));

    HorizontalLayout abilitiesLayout = new HorizontalLayout();

    abilitiesLayout.add(
        createAbilityCard("Iron Man", "Reduces stamina cost of all moves by 1.", 8));
    abilitiesLayout.add(createAbilityCard("High Flyer", "Increases agility rolls by +1.", 8));
    abilitiesLayout.add(createAbilityCard("Hardcore Legend", "Weapon attacks deal +2 damage.", 8));

    add(abilitiesLayout);
    add(
        new Button(
            "Back to Dashboard", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
  }

  private VerticalLayout createAbilityCard(String name, String description, int cost) {
    VerticalLayout card = new VerticalLayout();
    card.addClassName("ability-card");
    card.setPadding(true);
    card.getStyle().set("border", "1px solid #ccc");
    card.getStyle().set("border-radius", "5px");
    card.setWidth("300px");

    card.add(new Span(name));
    card.add(new Paragraph(description));
    card.add(new Span("Cost: " + cost + " Tokens"));

    Button buyButton = new Button("Unlock", e -> purchaseAbility(name, cost));
    buyButton.setEnabled(currentCampaign.getState().getSkillTokens() >= cost);
    card.add(buyButton);

    return card;
  }

  private void purchaseAbility(String abilityName, int cost) {
    CampaignState state = currentCampaign.getState();
    if (state.getSkillTokens() >= cost) {
      state.setSkillTokens(state.getSkillTokens() - cost);
      campaignStateRepository.save(state);

      Notification.show("Unlocked " + abilityName + "!");

      // Refresh UI
      tokenDisplay.setText("Available Skill Tokens: " + state.getSkillTokens());
      // Ideally re-render buttons to update enabled state
    } else {
      Notification.show("Not enough tokens!");
    }
  }
}
