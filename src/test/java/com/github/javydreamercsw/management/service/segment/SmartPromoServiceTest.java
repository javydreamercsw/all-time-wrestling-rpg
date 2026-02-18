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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.segment.promo.PromoHookDTO;
import com.github.javydreamercsw.management.dto.segment.promo.PromoOutcomeDTO;
import com.github.javydreamercsw.management.dto.segment.promo.SmartPromoResponseDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmartPromoServiceTest {

  private SmartPromoService smartPromoService;
  private SegmentNarrationService aiService;
  private ObjectMapper objectMapper;
  private CampaignService campaignService;
  private CampaignStateRepository campaignStateRepository;
  private BackstageActionHistoryRepository actionHistoryRepository;
  private SegmentRuleRepository segmentRuleRepository;
  private com.github.javydreamercsw.management.service.rivalry.RivalryService rivalryService;
  private com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService feudService;

  @BeforeEach
  void setUp() {
    SegmentNarrationServiceFactory aiFactory = mock(SegmentNarrationServiceFactory.class);
    aiService = mock(SegmentNarrationService.class);
    when(aiFactory.getBestAvailableService()).thenReturn(aiService);
    when(aiService.isAvailable()).thenReturn(true);

    objectMapper = new ObjectMapper();
    campaignService = mock(CampaignService.class);
    campaignStateRepository = mock(CampaignStateRepository.class);
    actionHistoryRepository = mock(BackstageActionHistoryRepository.class);
    segmentRuleRepository = mock(SegmentRuleRepository.class);
    rivalryService =
        mock(com.github.javydreamercsw.management.service.rivalry.RivalryService.class);
    feudService =
        mock(com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService.class);

    smartPromoService =
        new SmartPromoService(
            aiFactory,
            objectMapper,
            campaignService,
            campaignStateRepository,
            actionHistoryRepository,
            segmentRuleRepository,
            rivalryService,
            feudService);
  }

  @Test
  void testGeneratePromoContext() throws Exception {
    Campaign campaign = new Campaign();
    campaign.setState(new CampaignState());
    Wrestler player = new Wrestler();
    player.setName("Player One");
    player.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment.builder()
            .alignmentType(com.github.javydreamercsw.management.domain.campaign.AlignmentType.FACE)
            .build());
    campaign.setWrestler(player);

    Wrestler opponent = new Wrestler();
    opponent.setName("The Heel");
    opponent.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment.builder()
            .alignmentType(com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL)
            .build());

    String aiJsonResponse =
        """
        {
          "opener": "You walk out to a chorus of boos.",
          "hooks": [
            { "hook": "Insult local team", "label": "Cheap Heat", "text": "This city's team is a joke!", "alignmentShift": -1, "difficulty": 4 },
            { "hook": "Challenge honor", "label": "Stand Tall", "text": "I'm the best there is.", "alignmentShift": 1, "difficulty": 5 }
          ],
          "opponentName": "The Heel"
        }
        """;

    when(aiService.generateText(anyString())).thenReturn(aiJsonResponse);

    SmartPromoResponseDTO result =
        smartPromoService.generatePromoContext(player, opponent, campaign);

    assertNotNull(result);
    assertEquals("You walk out to a chorus of boos.", result.getOpener());
    assertEquals(2, result.getHooks().size());
    assertEquals("Cheap Heat", result.getHooks().get(0).getLabel());
  }

  @Test
  void testGeneratePromoContextWithRivalry() throws Exception {
    Campaign campaign = new Campaign();
    campaign.setState(new CampaignState());
    Wrestler player = new Wrestler();
    player.setId(1L);
    player.setName("Player One");
    campaign.setWrestler(player);

    Wrestler opponent = new Wrestler();
    opponent.setId(2L);
    opponent.setName("The Heel");

    var rivalry = new com.github.javydreamercsw.management.domain.rivalry.Rivalry();
    rivalry.setHeat(25);
    rivalry.setStorylineNotes("You stole my title!");

    when(rivalryService.getRivalryBetweenWrestlers(1L, 2L)).thenReturn(Optional.of(rivalry));
    when(feudService.getActiveFeudsForWrestler(1L)).thenReturn(List.of());

    String aiJsonResponse =
        """
        {
          "opener": "The tension is thick.",
          "hooks": [
            { "hook": "Demand rematch", "label": "Rematch", "text": "I want my title back!", "alignmentShift": 1, "difficulty": 5 }
          ],
          "opponentName": "The Heel"
        }
        """;

    when(aiService.generateText(anyString())).thenReturn(aiJsonResponse);

    smartPromoService.generatePromoContext(player, opponent, campaign);

    // Verify prompt contains rivalry info
    verify(aiService)
        .generateText(
            org.mockito.ArgumentMatchers.argThat(
                prompt ->
                    prompt.contains("ACTIVE RIVALRY:")
                        && prompt.contains("Heat Level: 25")
                        && prompt.contains("You stole my title!")));
  }

  @Test
  void testProcessPromoHook() throws Exception {
    Campaign campaign = new Campaign();
    campaign.setState(new CampaignState());
    Wrestler player = new Wrestler();
    player.setName("Player One");
    player.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment.builder()
            .alignmentType(com.github.javydreamercsw.management.domain.campaign.AlignmentType.FACE)
            .build());
    campaign.setWrestler(player);

    Wrestler opponent = new Wrestler();
    opponent.setName("The Heel");
    opponent.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment.builder()
            .alignmentType(com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL)
            .build());

    PromoHookDTO chosenHook =
        PromoHookDTO.builder()
            .hook("Insult local team")
            .label("Cheap Heat")
            .text("This city's team is a joke!")
            .alignmentShift(-1)
            .difficulty(4)
            .build();

    String aiJsonResponse =
        """
        {
          "retort": "How dare you speak about our team like that!",
          "crowdReaction": "The crowd is nuclear with heat.",
          "success": true,
          "alignmentShift": -1,
          "momentumBonus": 2,
          "finalNarration": "Player One insulted the city, drawing massive heat. The Heel responded with outrage."
        }
        """;

    when(aiService.generateText(anyString())).thenReturn(aiJsonResponse);
    when(campaignService.getOrCreateCampaignShow(any()))
        .thenReturn(new com.github.javydreamercsw.management.domain.show.Show());
    when(campaignService.getPromoSegmentType())
        .thenReturn(
            new com.github.javydreamercsw.management.domain.show.segment.type.SegmentType());

    PromoOutcomeDTO result =
        smartPromoService.processPromoHook(player, opponent, chosenHook, campaign);

    assertNotNull(result);
    assertEquals("How dare you speak about our team like that!", result.getRetort());
    assertEquals(2, result.getMomentumBonus());

    verify(campaignService).shiftAlignment(any(), eq(-1));
    verify(campaignStateRepository).save(any());
    verify(actionHistoryRepository).save(any());
  }
}
