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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.html.Span;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WrestlerSummaryCardTest extends AbstractViewTest {

  private WrestlerService wrestlerService;
  private Wrestler wrestler;

  @BeforeEach
  public void setUp() {
    wrestlerService = mock(WrestlerService.class);

    wrestler =
        Wrestler.builder()
            .id(1L)
            .name("Test Wrestler")
            .tier(WrestlerTier.MIDCARDER)
            .startingHealth(15)
            .startingStamina(15)
            .bumps(0)
            .drive(1)
            .resilience(2)
            .charisma(3)
            .brawl(4)
            .build();

    CampaignState state = CampaignState.builder().build();
    Campaign campaign = Campaign.builder().wrestler(wrestler).state(state).build();

    WrestlerAlignment alignment =
        WrestlerAlignment.builder()
            .wrestler(wrestler)
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .campaign(campaign)
            .build();
    wrestler.setAlignment(alignment);

    when(wrestlerService.getWrestlerStats(1L)).thenReturn(Optional.of(new WrestlerStats(10, 5, 1)));
    when(wrestlerService.findByIdWithInjuries(1L)).thenReturn(Optional.of(wrestler));
  }

  @Test
  public void testSummaryContent() {
    WrestlerSummaryCard card = new WrestlerSummaryCard(wrestler, wrestlerService, true);

    // Verify Name
    _get(
        card,
        com.vaadin.flow.component.html.H4.class,
        spec -> spec.withText("Test Wrestler (YOU)"));

    // Verify Stats
    _get(card, Span.class, spec -> spec.withText("Wins: 10"));
    _get(card, Span.class, spec -> spec.withText("Losses: 5"));

    // Verify Campaign Attributes
    _get(card, Span.class, spec -> spec.withText("DRV: 1"));
    _get(card, Span.class, spec -> spec.withText("RES: 2"));
    _get(card, Span.class, spec -> spec.withText("CHA: 3"));
    _get(card, Span.class, spec -> spec.withText("BRL: 4"));
  }
}
