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
package com.github.javydreamercsw.management.ui.view.match;

import static com.github.mvysny.kaributesting.v10.LocatorJ._assertOne;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.NarrationParserService;
import com.github.javydreamercsw.management.service.segment.PromoService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchPromoUITest extends AbstractViewTest {

  @Mock private SegmentService segmentService;
  @Mock private WrestlerService wrestlerService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private CampaignRepository campaignRepository;
  @Mock private SegmentNarrationServiceFactory narrationServiceFactory;
  @Mock private NpcService npcService;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private MatchFulfillmentService matchFulfillmentService;
  @Mock private PromoService promoService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private NarrationParserService narrationParserService;

  @Mock
  private com.github.javydreamercsw.management.service.interference.InterferenceService
      interferenceService;

  @Mock
  private com.github.javydreamercsw.management.service.interference.InterferenceAiService
      interferenceAiService;

  private MatchView matchView;

  @BeforeEach
  public void setup() {
    matchView =
        new MatchView(
            segmentService,
            wrestlerService,
            securityUtils,
            campaignService,
            campaignRepository,
            narrationServiceFactory,
            npcService,
            segmentAdjudicationService,
            matchFulfillmentRepository,
            matchFulfillmentService,
            promoService,
            commentaryTeamRepository,
            narrationParserService,
            interferenceService,
            interferenceAiService);
  }

  @Test
  void testPromoOptionsDisplayedForParticipatingPlayer() {
    // 1. Setup Segment
    Segment segment = new Segment();
    segment.setId(1L);
    Show show = new Show();
    show.setName("Test Show");
    segment.setShow(show);
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    segment.setSegmentType(promoType);

    // 2. Setup User and Wrestler
    Account account = new Account();
    account.setId(100L);

    Wrestler playerWrestler = new Wrestler();
    playerWrestler.setId(1L);
    playerWrestler.setName("Player Wrestler");
    playerWrestler.setAccount(account);

    Wrestler opponent = new Wrestler();
    opponent.setId(2L);
    opponent.setName("Opponent");

    segment.addParticipant(playerWrestler);
    segment.addParticipant(opponent);

    // 3. Setup Campaign and State
    Campaign campaign = new Campaign();
    campaign.setId(50L);
    campaign.setWrestler(playerWrestler);

    com.github.javydreamercsw.management.domain.campaign.CampaignState state =
        new com.github.javydreamercsw.management.domain.campaign.CampaignState();
    state.setCurrentMatch(segment);
    campaign.setState(state);

    // 4. Mock Security and Repositories
    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(100L));
    when(userDetails.getWrestler()).thenReturn(playerWrestler);

    when(segmentService.findByIdWithShow(1L)).thenReturn(Optional.of(segment));
    when(campaignRepository.findActiveByWrestler(playerWrestler)).thenReturn(Optional.of(campaign));
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());
    // 5. Navigate to View
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "1"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    // 6. Verify Buttons
    _assertOne(Button.class, spec -> spec.withId("ai-generate-narration-button"));
    _assertOne(Button.class, spec -> spec.withId("go-smart-promo-hooks-button"));
    _assertOne(Button.class, spec -> spec.withId("go-interactive-promo-button"));

    Button autoGenBtn = _get(Button.class, spec -> spec.withId("ai-generate-narration-button"));
    org.junit.jupiter.api.Assertions.assertEquals("Auto-Generate Promo (AI)", autoGenBtn.getText());
  }

  @Test
  void testPromoOptionsDisplayedForPlayerWithoutCampaign() {
    // 1. Setup Segment
    Segment segment = new Segment();
    segment.setId(2L);
    Show show = new Show();
    show.setName("League Show");
    segment.setShow(show);
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    segment.setSegmentType(promoType);

    // 2. Setup User and Wrestler
    Account account = new Account();
    account.setId(101L);

    Wrestler playerWrestler = new Wrestler();
    playerWrestler.setId(3L);
    playerWrestler.setName("League Player");
    playerWrestler.setAccount(account);

    segment.addParticipant(playerWrestler);

    // 4. Mock Security and Repositories
    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(101L));
    when(userDetails.getWrestler()).thenReturn(playerWrestler);

    when(segmentService.findByIdWithShow(2L)).thenReturn(Optional.of(segment));
    // No campaign
    when(campaignRepository.findActiveByWrestler(playerWrestler)).thenReturn(Optional.empty());
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());

    // 5. Navigate to View
    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "2"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    // 6. Verify Buttons still visible
    _assertOne(Button.class, spec -> spec.withId("go-smart-promo-hooks-button"));
    _assertOne(Button.class, spec -> spec.withId("go-interactive-promo-button"));
  }
}
