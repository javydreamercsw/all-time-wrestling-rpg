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
package com.github.javydreamercsw.management.ui.view.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the effective stats logic displayed on the Player Dashboard. Tests the Wrestler
 * domain model calculations that the view renders, exercising the same paths as
 * createEffectiveStats Section() in PlayerView without needing the full Spring/Vaadin context.
 */
public class PlayerViewTest {

  private Wrestler wrestler;

  @BeforeEach
  public void setUp() {
    wrestler =
        Wrestler.builder()
            .name("Dashboard Wrestler")
            .tier(WrestlerTier.MIDCARDER)
            .startingHealth(15)
            .startingStamina(15)
            .bumps(0)
            .build();
  }

  @Test
  public void testBaseEffectiveStats() {
    assertEquals(15, wrestler.getEffectiveStartingHealth());
    assertEquals(15, wrestler.getEffectiveStartingStamina());
    assertEquals(0, wrestler.getEffectiveStartingMomentum());
    assertEquals(5, wrestler.getEffectiveHandSize());
  }

  @Test
  public void testCampaignMomentumBonus() {
    CampaignState state = CampaignState.builder().momentumBonus(3).build();
    Campaign campaign = Campaign.builder().wrestler(wrestler).state(state).build();
    state.setCampaign(campaign);
    WrestlerAlignment alignment =
        WrestlerAlignment.builder()
            .wrestler(wrestler)
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .campaign(campaign)
            .build();
    wrestler.setAlignment(alignment);

    assertEquals(3, wrestler.getEffectiveStartingMomentum());
  }

  @Test
  public void testStatusCardModifiesMomentum() {
    StatusCard drawCard =
        StatusCard.builder()
            .key("status_draw_test")
            .level1Name("Draw")
            .level2Name("Main Eventer")
            .positive(true)
            .level1Effect("momentum: +4")
            .level2Effect("momentum: +4")
            .build();

    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(drawCard).level(1).build();
    wrestler.setStatuses(List.of(status));

    assertEquals(4, wrestler.getEffectiveStartingMomentum());
  }

  @Test
  public void testNegativeStatusCardReducesHandSize() {
    StatusCard lostConfidence =
        StatusCard.builder()
            .key("status_lost_confidence_test")
            .level1Name("Lost Confidence")
            .level2Name("Humiliated")
            .positive(false)
            .level1Effect("handSize: -2")
            .level2Effect("momentum: -7")
            .build();

    WrestlerStatus status =
        WrestlerStatus.builder().wrestler(wrestler).statusCard(lostConfidence).level(1).build();
    wrestler.setStatuses(List.of(status));

    assertEquals(3, wrestler.getEffectiveHandSize());
  }

  @Test
  public void testCampaignHandSizePenalty() {
    CampaignState state = CampaignState.builder().handSizePenalty(2).build();
    Campaign campaign = Campaign.builder().wrestler(wrestler).state(state).build();
    state.setCampaign(campaign);
    WrestlerAlignment alignment =
        WrestlerAlignment.builder()
            .wrestler(wrestler)
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .campaign(campaign)
            .build();
    wrestler.setAlignment(alignment);

    assertEquals(3, wrestler.getEffectiveHandSize());
  }
}
