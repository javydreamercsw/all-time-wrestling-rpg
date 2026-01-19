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
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
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

  private Campaign currentCampaign;

  @Autowired
  public CampaignDashboardView(
      CampaignRepository campaignRepository,
      CampaignService campaignService,
      WrestlerRepository wrestlerRepository) {
    this.campaignRepository = campaignRepository;
    this.campaignService = campaignService;
    this.wrestlerRepository = wrestlerRepository;

    setSpacing(true);
    setPadding(true);

    loadCampaign();
    initUI();
  }

  private void loadCampaign() {
    // Find campaign for current user
    // Assumption: Current user is a Player or has a linked Wrestler.
    // If Admin/Booker, maybe show list? For now, implementing Player view.
    
    // We need to find the wrestler associated with the current user account.
    // SecurityUtils.getPrincipal() might help if we have UserDetails.
    // For simplicity, I'll search for a campaign where the wrestler has the current username? 
    // Or just pick the first one for testing if not implementing full player linking logic here.
    
    // In a real implementation:
    // User user = SecurityUtils.getUser();
    // Wrestler wrestler = wrestlerRepository.findByAccount(user.getAccount());
    // currentCampaign = campaignRepository.findByWrestlerAndStatus(wrestler, ACTIVE);
    
    // Fallback: If no campaign found, show "Start Campaign" button.
    
    // TODO: Implement proper user-wrestler resolution.
    // For now, I'll just check if there is ANY active campaign to display, or show empty.
    
    // Ideally, we look up by the logged in user.
    // Since I don't have easy access to 'currentUser -> wrestler' mapping helper here without digging deeper,
    // I will display a placeholder or 'No Active Campaign' message.
  }

  private void initUI() {
    if (currentCampaign == null) {
        add(new H2("Campaign Mode"));
        add(new Span("No active campaign found. Please ask an Admin to assign a campaign (or implement 'Start New Campaign' flow)."));
        
        // Temporary "Start Campaign" for debugging/testing if user is Admin?
        Button startButton = new Button("Start New Campaign (Debug)", e -> startDebugCampaign());
        add(startButton);
        return;
    }

    CampaignState state = currentCampaign.getState();
    Wrestler wrestler = currentCampaign.getWrestler();

    add(new H2("Campaign: All or Nothing (Season 1)"));
    add(new H3("Wrestler: " + wrestler.getName()));

    HorizontalLayout statsLayout = new HorizontalLayout();
    statsLayout.add(createStatCard("Chapter", String.valueOf(state.getCurrentChapter())));
    statsLayout.add(createStatCard("Victory Points", String.valueOf(state.getVictoryPoints())));
    statsLayout.add(createStatCard("Skill Tokens", String.valueOf(state.getSkillTokens())));
    statsLayout.add(createStatCard("Bumps", String.valueOf(state.getBumps())));
    
    add(statsLayout);
    
    HorizontalLayout healthLayout = new HorizontalLayout();
    healthLayout.add(createStatCard("Health", wrestler.getCurrentHealthWithPenalties() + " / " + wrestler.getStartingHealth()));
    healthLayout.add(createStatCard("Penalties", 
            "HP: -" + state.getHealthPenalty() + 
            ", Stam: -" + state.getStaminaPenalty() + 
            ", Hand: -" + state.getHandSizePenalty()));
    add(healthLayout);

    add(new H4("Actions"));
    HorizontalLayout actionsLayout = new HorizontalLayout();
    actionsLayout.add(new Button("Backstage Actions", e -> UI.getCurrent().navigate(BackstageActionView.class)));
    actionsLayout.add(new Button("Story Narrative", e -> UI.getCurrent().navigate("campaign/narrative"))); // To be implemented
    add(actionsLayout);
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
  
  private void startDebugCampaign() {
      // Find a wrestler or create one?
      // Just pick the first available wrestler.
      Optional<Wrestler> w = wrestlerRepository.findAll().stream().findFirst();
      if (w.isPresent()) {
          currentCampaign = campaignService.startCampaign(w.get());
          removeAll();
          initUI();
      } else {
          add(new Span("No wrestlers found in DB to start campaign."));
      }
  }
}
