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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.NonNull;

public class PlayerCampaignCard extends Composite<Div> {

  public PlayerCampaignCard(@NonNull Campaign campaign) {
    getContent().addClassName("player-card-container");
    getContent()
        .addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.LARGE, LumoUtility.FlexWrap.WRAP);

    // Front Side
    getContent().add(createCardFront(campaign));

    // Back Side (Stats)
    getContent().add(createCardBack(campaign));
  }

  private Div createCardFront(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment = wrestler.getAlignment();

    Div card = new Div();
    card.addClassName("player-card");

    // Header
    Div header = new Div();
    header.addClassName("player-card-header");
    header.addClassName(
        "wrestler-tier-" + wrestler.getTier().name().toLowerCase()); // Dynamic color
    header.add(new Span(wrestler.getName()));

    // Alignment Icon (Placeholder for "Set" or configurable icon)
    Icon alignmentIcon;
    if (alignment != null && alignment.getAlignmentType() == AlignmentType.HEEL) {
      alignmentIcon = VaadinIcon.THUMBS_DOWN.create();
    } else {
      alignmentIcon = VaadinIcon.THUMBS_UP.create();
    }
    alignmentIcon.addClassName("set-icon");
    header.add(alignmentIcon);
    card.add(header);

    // Content
    Div content = new Div();
    content.addClassName("player-card-content");

    // Image Placeholder
    Div image = new Div();
    image.addClassName("player-card-image-placeholder");
    if (wrestler.getImageUrl() != null && !wrestler.getImageUrl().isEmpty()) {
      // TODO: Actual Image Implementation
      image.setText("Image: " + wrestler.getName());
    } else {
      image.setText(wrestler.getName());
    }
    content.add(image);

    // Vitals
    content.add(
        createStatRow(
            "Health",
            wrestler.getCurrentHealthWithPenalties() + " / " + wrestler.getStartingHealth()));
    content.add(createStatRow("VP", String.valueOf(campaign.getState().getVictoryPoints())));

    card.add(content);

    // Footer
    Div footer = new Div();
    footer.addClassName("player-card-footer");
    Span tierBadge = new Span(wrestler.getTier().name());
    tierBadge.addClassName("wrestler-tier-badge");
    tierBadge.addClassName("wrestler-tier-" + wrestler.getTier().name().toLowerCase());
    footer.add(tierBadge);
    card.add(footer);

    return card;
  }

  private Div createCardBack(Campaign campaign) {
    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();

    Div card = new Div();
    card.addClassName("player-card");

    // Header
    Div header = new Div();
    header.addClassName("player-card-header");
    header.getStyle().set("background-color", "var(--lumo-contrast-80pct)");
    header.add(new Span("Campaign Stats"));
    card.add(header);

    // Content
    Div content = new Div();
    content.addClassName("player-card-content");

    // General Stats
    content.add(createGroupTitle("Progress"));
    content.add(createStatRow("Chapter", state.getCurrentChapterId()));
    if (state.getCurrentGameDate() != null) {
      content.add(
          createStatRow(
              "Date",
              state
                  .getCurrentGameDate()
                  .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))));
    }
    content.add(createStatRow("Bumps", String.valueOf(state.getBumps())));
    content.add(createStatRow("Gender", wrestler.getGender().name()));

    // Mechanics
    content.add(createGroupTitle("Mechanics"));
    content.add(createStatRow("Momentum", "+" + state.getMomentumBonus()));
    content.add(createStatRow("HP Penalty", "-" + state.getHealthPenalty()));
    content.add(createStatRow("Stam Penalty", "-" + state.getStaminaPenalty()));
    content.add(createStatRow("Hand Penalty", "-" + state.getHandSizePenalty()));

    // Skills
    content.add(createGroupTitle("Skills"));
    Div skillsContainer = new Div();
    state
        .getUpgrades()
        .forEach(
            upgrade -> {
              Span skill = new Span(upgrade.getName());
              skill.addClassName("skill-keyword");
              skill.setTitle(upgrade.getDescription()); // Tooltip
              skillsContainer.add(skill);
            });
    if (state.getUpgrades().isEmpty()) {
      Span none = new Span("None");
      none.addClassName(LumoUtility.FontSize.XSMALL);
      none.addClassName(LumoUtility.TextColor.SECONDARY);
      skillsContainer.add(none);
    }
    content.add(skillsContainer);

    card.add(content);
    return card;
  }

  private Div createStatRow(String label, String value) {
    Div row = new Div();
    row.addClassName("stat-row");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("stat-label");
    Span valueSpan = new Span(value);
    valueSpan.addClassName("stat-value");
    row.add(labelSpan, valueSpan);
    return row;
  }

  private Div createGroupTitle(String title) {
    Div div = new Div();
    div.setText(title);
    div.addClassName("stat-group-title");
    return div;
  }
}
