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
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "campaign/narrative", layout = MainLayout.class)
@PageTitle("Story Narrative")
@PermitAll
@Slf4j
public class NarrativeStoryView extends VerticalLayout {

  private final CampaignRepository campaignRepository;
  private Campaign currentCampaign;

  @Autowired
  public NarrativeStoryView(CampaignRepository campaignRepository) {
    this.campaignRepository = campaignRepository;

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
    add(new H2("Story Narrative"));

    if (currentCampaign == null) {
      add(new Paragraph("No active campaign found."));
      add(new Button("Back", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
      return;
    }

    // In a real app, we would fetch the latest narrative segment or generate one.
    // For now, placeholder content.

    String chapterTitle = "Chapter " + currentCampaign.getState().getCurrentChapter();
    add(new H2(chapterTitle));

    Paragraph narrativeText = new Paragraph();
    narrativeText.setText(
        "You stand backstage, the roar of the crowd muting your thoughts. "
            + "This is your moment. The path to the All Time Championship starts here. "
            + "Will you be a hero, or a villain? Your actions today will define your legacy.");
    narrativeText.getStyle().set("font-size", "1.2em");
    narrativeText.getStyle().set("font-style", "italic");
    narrativeText.getStyle().set("padding", "20px");
    narrativeText.getStyle().set("background-color", "#f5f5f5"); // Or dark mode compatible color
    narrativeText.getStyle().set("border-left", "5px solid #333");

    add(narrativeText);

    add(
        new Button(
            "Back to Dashboard", e -> UI.getCurrent().navigate(CampaignDashboardView.class)));
  }
}
