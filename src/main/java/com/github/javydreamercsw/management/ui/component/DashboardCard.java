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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import lombok.NonNull;

/** A reusable card component for dashboard-style layouts. */
public class DashboardCard extends Composite<Div> {

  private final Div content;

  public DashboardCard(@NonNull String title) {
    getContent()
        .addClassNames(
            Display.FLEX,
            FlexDirection.COLUMN,
            Padding.MEDIUM,
            Background.BASE,
            Border.ALL,
            BorderRadius.MEDIUM,
            BoxShadow.SMALL,
            Width.FULL);

    H3 header = new H3(title);
    header.addClassNames(Margin.Top.NONE, Margin.Bottom.MEDIUM, FontSize.MEDIUM, TextColor.PRIMARY);
    getContent().add(header);

    content = new Div();
    content.addClassNames(Display.FLEX, FlexDirection.COLUMN, Width.FULL);
    getContent().add(content);
  }

  public DashboardCard(Campaign campaign) {
    this("Player Campaign Card");
    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment = wrestler.getAlignment();

    HorizontalLayout layout = new HorizontalLayout();
    layout.setWidthFull();
    layout.addClassNames(FlexWrap.WRAP, Gap.MEDIUM);

    // Stats
    layout.add(createStatCard("Chapter", state.getCurrentChapterId()));
    if (state.getCurrentGameDate() != null) {
      layout.add(
          createStatCard(
              "Game Date",
              state
                  .getCurrentGameDate()
                  .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))));
    }
    layout.add(createStatCard("Victory Points", String.valueOf(state.getVictoryPoints())));
    layout.add(createStatCard("Skill Tokens", String.valueOf(state.getSkillTokens())));
    if (state.getMomentumBonus() > 0) {
      layout.add(createStatCard("Next Match Momentum", "+" + state.getMomentumBonus()));
    }
    layout.add(createStatCard("Bumps", String.valueOf(state.getBumps())));

    if (alignment != null) {
      layout.add(
          createStatCard(
              "Alignment",
              (alignment.getAlignmentType() == AlignmentType.NEUTRAL
                      ? "NEUTRAL"
                      : alignment.getAlignmentType().name())
                  + " (Lvl "
                  + alignment.getLevel()
                  + ")"));
    }

    // Health & Penalties
    layout.add(
        createStatCard(
            "Health",
            wrestler.getCurrentHealthWithPenalties() + " / " + wrestler.getStartingHealth()));
    layout.add(
        createStatCard(
            "Penalties",
            "HP: -"
                + state.getHealthPenalty()
                + ", Stam: -"
                + state.getStaminaPenalty()
                + ", Hand: -"
                + state.getHandSizePenalty()));

    content.add(layout);
  }

  public void add(Component... components) {
    content.add(components);
  }

  public void setMaxWidth(String maxWidth) {
    getContent().setMaxWidth(maxWidth);
  }

  private VerticalLayout createStatCard(@NonNull String label, @NonNull String value) {
    VerticalLayout card = new VerticalLayout();
    card.setPadding(true);
    card.setSpacing(false);
    card.setWidth("200px");
    card.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.BoxShadow.XSMALL,
        LumoUtility.AlignItems.CENTER);

    Span labelSpan = new Span(label);
    labelSpan.addClassNames(
        LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.FontWeight.MEDIUM);

    Span valueSpan = new Span(value);
    valueSpan.addClassNames(
        LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);

    card.add(labelSpan, valueSpan);
    return card;
  }
}
