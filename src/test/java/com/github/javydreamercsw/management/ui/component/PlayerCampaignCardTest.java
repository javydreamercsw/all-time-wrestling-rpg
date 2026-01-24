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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerCampaignCardTest extends AbstractViewTest {

  private Campaign campaign;
  private CampaignState state;
  private Wrestler wrestler;

  @BeforeEach
  public void setUp() {

    wrestler =
        Wrestler.builder()
            .name("Test Wrestler")
            .tier(WrestlerTier.MIDCARDER)
            .startingHealth(15)
            .startingStamina(15)
            .lowHealth(4)
            .lowStamina(4)
            .bumps(0)
            .injuries(new ArrayList<>())
            .build();

    WrestlerAlignment alignment =
        WrestlerAlignment.builder()
            .wrestler(wrestler)
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();

    wrestler.setAlignment(alignment);

    state = CampaignState.builder().skillTokens(5).momentumBonus(2).build();

    campaign = Campaign.builder().wrestler(wrestler).state(state).build();
  }

  @Test
  public void testCardFrontDisplay() {
    PlayerCampaignCard card = new PlayerCampaignCard(campaign);

    // Verify wrestler name is displayed
    _get(card, Span.class, spec -> spec.withText("Test Wrestler"));

    // Verify Health Label
    _get(card, Span.class, spec -> spec.withText("Health"));
    // Verify Health Value (15) - we expect 2 of them (Health and Stamina), so we need to be
    // specific or just ensure at least one exists
    // Better: Find the label "Health", get parent, find the value "15" sibling

    // Verify Stamina Label
    _get(card, Span.class, spec -> spec.withText("Stamina"));

    // Verify VP
    _get(card, Span.class, spec -> spec.withText("Victory Points"));
  }

  @Test
  public void testCardBackDisplay() {
    PlayerCampaignCard card = new PlayerCampaignCard(campaign);

    // Verify detailed stats on back
    _get(card, Span.class, spec -> spec.withText("Stats & Skills"));
    _get(card, Span.class, spec -> spec.withText("Momentum"));
    _get(card, Span.class, spec -> spec.withText("+2"));

    // Verify Skill Tokens are displayed
    _get(card, Span.class, spec -> spec.withText("Skill Tokens"));
    _get(card, Span.class, spec -> spec.withText("5"));
  }

  @Test
  public void testBumpsDisplay() {
    wrestler.setBumps(2);
    PlayerCampaignCard card = new PlayerCampaignCard(campaign);

    // Verify "Bumps" label exists
    _get(card, Span.class, spec -> spec.withText("Bumps"));

    // Verify we can find the bump icons.
    // The bumps are rendered as VaadinIcon.CIRCLE
    // We can just count the total number of CIRCLE icons on the card.
    // Since there are 2 bumps, we expect 2 CIRCLE icons.

    long bumpIcons =
        _get(card, Div.class, spec -> spec.withClasses("player-card-back"))
            .getChildren()
            .flatMap(Component::getChildren) // Content
            .flatMap(Component::getChildren) // Rows
            .flatMap(Component::getChildren) // Icon container
            .filter(c -> c instanceof Icon)
            .map(c -> (Icon) c)
            .filter(icon -> icon.getElement().getAttribute("icon").equals("vaadin:circle"))
            .count();

    assertThat(bumpIcons).isEqualTo(2);
  }

  @Test
  public void testInjuriesDisplay() {
    Injury injury = new Injury();
    injury.setName("Broken Arm");
    injury.setSeverity(InjurySeverity.SEVERE);
    injury.setIsActive(true);
    injury.setHealthPenalty(3);

    wrestler.getInjuries().add(injury);

    PlayerCampaignCard card = new PlayerCampaignCard(campaign);

    // Verify injury icon is present
    List<Icon> icons =
        _get(card, Div.class, spec -> spec.withClasses("player-card-back"))
            .getChildren()
            .flatMap(Component::getChildren) // Content
            .flatMap(Component::getChildren) // Rows
            .flatMap(Component::getChildren) // Icons
            .filter(c -> c instanceof Icon && c.getClassNames().contains("injury-icon"))
            .map(c -> (Icon) c)
            .toList();

    assertThat(icons).hasSize(1);
  }

  @Test
  public void testHealthCalculationWithPenalties() {
    wrestler.setBumps(1);

    Injury injury = new Injury();
    injury.setName("Minor Sprain");
    injury.setSeverity(InjurySeverity.MINOR);
    injury.setIsActive(true);
    injury.setHealthPenalty(1);

    wrestler.getInjuries().add(injury);

    // 15 - 1 (bump) - 1 (injury) = 13
    PlayerCampaignCard card = new PlayerCampaignCard(campaign);

    // Verify health bar shows 13
    _get(card, Span.class, spec -> spec.withText("13"));
  }
}
