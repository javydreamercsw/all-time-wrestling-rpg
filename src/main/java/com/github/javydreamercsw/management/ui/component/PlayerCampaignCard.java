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

  private final Div card;
  private boolean isFlipped = false;

  public PlayerCampaignCard(@NonNull Campaign campaign) {
    getContent().addClassName("player-card-container");

    card = new Div();
    card.addClassName("player-card");
    card.addClickListener(e -> toggleFlip());

    // Front Side
    card.add(createCardFront(campaign));

    // Back Side
    card.add(createCardBack(campaign));

    getContent().add(card);
  }

  private void toggleFlip() {
    isFlipped = !isFlipped;
    if (isFlipped) {
      card.addClassName("flipped");
    } else {
      card.removeClassName("flipped");
    }
  }

  private Div createCardFront(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment = wrestler.getAlignment();

    Div face = new Div();
    face.addClassName("player-card-face");
    face.addClassName("player-card-front");

    // Header
    Div header = new Div();
    header.addClassName("player-card-header");
    header.addClassName("wrestler-tier-" + wrestler.getTier().name().toLowerCase());
    header.add(new Span(wrestler.getName()));

    Icon alignmentIcon;
    if (alignment != null && alignment.getAlignmentType() == AlignmentType.HEEL) {
      alignmentIcon = VaadinIcon.THUMBS_DOWN.create();
    } else {
      alignmentIcon = VaadinIcon.THUMBS_UP.create();
    }
    alignmentIcon.addClassName("set-icon");
    header.add(alignmentIcon);
    face.add(header);

    // Content
    Div content = new Div();
    content.addClassName("player-card-content");

    // Image Placeholder
    Div image = new Div();
    image.addClassName("player-card-image-placeholder");
    if (wrestler.getImageUrl() != null && !wrestler.getImageUrl().isEmpty()) {
      image.getStyle().set("background-image", "url('" + wrestler.getImageUrl() + "')");
    } else {
      image.setText(wrestler.getName());
    }
    content.add(image);

    // Status Bars
    content.add(
        createStatusBar(
            "Health",
            String.valueOf(wrestler.getCurrentHealthWithPenalties()),
            wrestler.getCurrentHealthWithPenalties(),
            20,
            wrestler.getLowHealth(),
            "health"));
    content.add(
        createStatusBar(
            "Stamina",
            String.valueOf(wrestler.getEffectiveStartingStamina()),
            wrestler.getEffectiveStartingStamina(),
            20,
            wrestler.getLowStamina(),
            "stamina"));

    // VP
    content.add(
        createStatRow("Victory Points", String.valueOf(campaign.getState().getVictoryPoints())));

    face.add(content);

    // Flip Hint
    Span hint = new Span("Click to flip");
    hint.addClassName("flip-hint");
    face.add(hint);

    return face;
  }

  private Div createCardBack(Campaign campaign) {
    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();

    Div face = new Div();
    face.addClassName("player-card-face");
    face.addClassName("player-card-back");

    // Header
    Div header = new Div();
    header.addClassName("player-card-header");
    header.getStyle().set("background-color", "var(--lumo-contrast-80pct)");
    header.add(new Span("Stats & Skills"));
    face.add(header);

    // Content
    Div content = new Div();
    content.addClassName("player-card-content");

    // Detailed Stats
    content.add(createGroupTitle("Status"));
    content.add(createStatRow("Momentum", "+" + state.getMomentumBonus()));

    // Bumps
    Div bumpRow = new Div();
    bumpRow.addClassName("stat-row");
    bumpRow.add(new Span("Bumps"));
    Div bumpIcons = new Div();
    bumpIcons.addClassName("icon-row");
    for (int i = 0; i < wrestler.getBumps(); i++) {
      Icon bumpIcon = VaadinIcon.CIRCLE.create();
      bumpIcon.addClassName("bump-icon");
      bumpIcons.add(bumpIcon);
    }
    if (wrestler.getBumps() == 0) {
      Span none = new Span("-");
      none.addClassName(LumoUtility.TextColor.SECONDARY);
      bumpIcons.add(none);
    }
    bumpRow.add(bumpIcons);
    content.add(bumpRow);

    // Injuries
    Div injuryRow = new Div();
    injuryRow.addClassName("stat-row");
    injuryRow.add(new Span("Active Injuries"));
    Div injuryIcons = new Div();
    injuryIcons.addClassName("icon-row");
    int injuryCount = wrestler.getActiveInjuries().size();
    for (int i = 0; i < injuryCount; i++) {
      Icon cross = VaadinIcon.PLUS.create();
      cross.addClassName("injury-icon");
      injuryIcons.add(cross);
    }
    if (injuryCount == 0) {
      Span none = new Span("-");
      none.addClassName(LumoUtility.TextColor.SECONDARY);
      injuryIcons.add(none);
    }
    injuryRow.add(injuryIcons);
    content.add(injuryRow);

    // Penalties
    content.add(createGroupTitle("Penalties"));
    content.add(createStatRow("HP Penalty", "-" + state.getHealthPenalty()));
    content.add(createStatRow("Stam Penalty", "-" + state.getStaminaPenalty()));
    content.add(createStatRow("Hand Size Penalty", "-" + state.getHandSizePenalty()));

    // Skills
    content.add(createGroupTitle("Purchased Skills"));
    Div skillsContainer = new Div();
    skillsContainer.addClassName("skill-list");
    state
        .getUpgrades()
        .forEach(
            upgrade -> {
              Span skill = new Span(upgrade.getName());
              skill.addClassName("skill-tag");
              skill.setTitle(upgrade.getDescription());
              skillsContainer.add(skill);
            });
    if (state.getUpgrades().isEmpty()) {
      Span none = new Span("None");
      none.addClassName(LumoUtility.FontSize.SMALL);
      none.addClassName(LumoUtility.TextColor.SECONDARY);
      skillsContainer.add(none);
    }
    content.add(skillsContainer);

    face.add(content);
    return face;
  }

  private Div createStatusBar(
      String label, String valueText, int current, int max, int lowLimit, String styleClass) {
    Div container = new Div();
    container.addClassName("status-bar-container");

    Div labels = new Div();
    labels.addClassName("status-bar-label");
    labels.add(new Span(label));
    labels.add(new Span(valueText)); // Add numeric value
    container.add(labels);

    Div track = new Div();
    track.addClassName("status-bar-track");

    Div fill = new Div();
    fill.addClassName("status-bar-fill");
    fill.addClassName(styleClass);

    double percent = Math.min(100.0, Math.max(0.0, (double) current / max * 100.0));
    fill.setWidth(percent + "%");
    track.add(fill);

    // Visual Scale
    Div scale = new Div();
    scale.addClassName("status-bar-scale");
    for (int i = 1; i < 10; i++) {
      Div tick = new Div();
      tick.addClassName("status-bar-tick");
      if (i == 5) {
        tick.addClassName("major");
      }
      scale.add(tick);
    }
    track.add(scale);

    // Low Limit Marker
    if (lowLimit > 0 && lowLimit < max) {
      Div marker = new Div();
      marker.addClassName("status-bar-limit-marker");
      double limitPercent = (double) lowLimit / max * 100.0;
      marker.getStyle().set("left", limitPercent + "%");
      track.add(marker);
    }

    container.add(track);
    return container;
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
    div.addClassName(LumoUtility.FontSize.XSMALL);
    div.addClassName(LumoUtility.FontWeight.BOLD);
    div.addClassName(LumoUtility.TextColor.SECONDARY);
    div.addClassName(LumoUtility.Margin.Top.MEDIUM);
    div.addClassName(LumoUtility.Border.BOTTOM);
    div.getStyle().set("text-transform", "uppercase");
    return div;
  }
}
