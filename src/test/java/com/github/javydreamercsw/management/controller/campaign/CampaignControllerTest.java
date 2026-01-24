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
package com.github.javydreamercsw.management.controller.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
class CampaignControllerTest extends AbstractIntegrationTest {

  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private CampaignService campaignService;
  @Autowired private CampaignEncounterService campaignEncounterService;
  @Autowired private CampaignUpgradeService upgradeService;
  @Autowired private WrestlerAlignmentRepository alignmentRepository;

  @Autowired private WrestlerRepository wrestlerRepository;

  private Wrestler testWrestler;

  @BeforeEach
  void setupData() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    testWrestler =
        Wrestler.builder().name("API Tester").startingHealth(100).startingStamina(100).build();
    testWrestler = wrestlerRepository.save(testWrestler);

    // Populate enough wrestlers for tournament (16 total needed)
    for (int i = 0; i < 15; i++) {
      wrestlerRepository.save(
          Wrestler.builder()
              .name("Wrestler " + i)
              .startingHealth(100)
              .startingStamina(100)
              .build());
    }
  }

  @Test
  void testUpgradeRestriction() throws Exception {
    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);
    Campaign campaign = campaignRepository.findActiveByWrestler(testWrestler).get();
    CampaignState state = campaign.getState();

    // 2. Give enough tokens for 2 upgrades
    state.setSkillTokens(20);
    campaignRepository.save(campaign);

    List<CampaignUpgrade> allUpgrades = upgradeService.getAllUpgrades();
    // Find two upgrades of the same type (e.g., DAMAGE)
    List<CampaignUpgrade> damageUpgrades =
        allUpgrades.stream().filter(u -> "DAMAGE".equals(u.getType())).toList();

    assertThat(damageUpgrades.size()).isGreaterThanOrEqualTo(2);

    CampaignUpgrade first = damageUpgrades.get(0);
    CampaignUpgrade second = damageUpgrades.get(1);

    // 3. Purchase first
    upgradeService.purchaseUpgrade(campaign, first.getId());
    assertThat(state.getUpgrades()).contains(first);
    assertThat(state.getSkillTokens()).isEqualTo(12);

    // 4. Attempt to purchase second of same type (should fail)
    try {
      upgradeService.purchaseUpgrade(campaign, second.getId());
      fail("Should have thrown IllegalStateException for duplicate upgrade type");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("already have a permanent upgrade of type: DAMAGE");
    }

    // 5. Verify VP and tokens unchanged after failure
    assertThat(state.getSkillTokens()).isEqualTo(12);
    assertThat(state.getUpgrades()).hasSize(1);
  }

  @Test
  void testBackstageActionsUnlocking() throws Exception {
    // 1. Start Campaign (Initial state: Locked)
    campaignService.startCampaign(testWrestler);
    String responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    CampaignState state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.isPromoUnlocked()).isFalse();
    assertThat(state.isAttackUnlocked()).isFalse();

    // 2. Set Alignment to FACE and finish first match
    WrestlerAlignment alignment = alignmentRepository.findByWrestler(testWrestler).get();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignmentRepository.save(alignment);

    mockMvc
        .perform(
            post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                .param("won", "true"))
        .andExpect(status().isOk());

    responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.isPromoUnlocked()).isTrue();
    assertThat(state.isAttackUnlocked()).isFalse();

    // 3. Create a HEEL wrestler and finish first match
    Wrestler heelWrestler =
        Wrestler.builder().name("Heel Tester").startingHealth(100).startingStamina(100).build();
    heelWrestler = wrestlerRepository.save(heelWrestler);
    campaignService.startCampaign(heelWrestler);

    WrestlerAlignment heelAlignment = alignmentRepository.findByWrestler(heelWrestler).get();
    heelAlignment.setAlignmentType(AlignmentType.HEEL);
    alignmentRepository.save(heelAlignment);

    mockMvc
        .perform(
            post("/api/campaign/" + heelWrestler.getId() + "/test/process-match")
                .param("won", "true"))
        .andExpect(status().isOk());

    responseJson =
        mockMvc
            .perform(get("/api/campaign/" + heelWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.isPromoUnlocked()).isTrue();
    assertThat(state.isAttackUnlocked()).isTrue();
  }

  @Test
  void testNoVpWithoutMatch() throws Exception {
    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);

    // 2. Simulate a narrative choice that DOES NOT lead to a match
    CampaignEncounterResponseDTO.Choice noMatchChoice =
        CampaignEncounterResponseDTO.Choice.builder()
            .text("Refuse the challenge")
            .alignmentShift(1)
            .vpReward(50) // High VP that should be ignored
            .nextPhase("BACKSTAGE")
            .build();

    // We need to record this choice. Since recordEncounterChoice needs an existing encounter:
    // Generate one first
    campaignEncounterService.generateEncounter(
        campaignRepository.findActiveByWrestler(testWrestler).get());

    campaignEncounterService.recordEncounterChoice(
        campaignRepository.findActiveByWrestler(testWrestler).get(), noMatchChoice);

    // 3. Verify VP is still 0
    String responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    CampaignState finalState = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(finalState.getVictoryPoints()).isEqualTo(0);
  }

  @Test
  void testMatchVpRewards() throws Exception {
    // 1. Start Campaign (Starts at Chapter 1)
    campaignService.startCampaign(testWrestler);

    // Initial VP should be 0
    String responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    CampaignState state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.getVictoryPoints()).isEqualTo(0);

    // 2. Win a match (Chapter 1 win VP = 2)
    mockMvc
        .perform(
            post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                .param("won", "true"))
        .andExpect(status().isOk());

    responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.getVictoryPoints()).isEqualTo(2);

    // 3. Lose a match (Chapter 1 loss VP = -1)
    mockMvc
        .perform(
            post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                .param("won", "false"))
        .andExpect(status().isOk());

    responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    state = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(state.getVictoryPoints()).isEqualTo(1); // 2 - 1 = 1
  }

  @Test
  void testTournamentExit_Eliminated() throws Exception {

    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);
    Campaign campaign = campaignRepository.findActiveByWrestler(testWrestler).get();

    // 2. Force Chapter 2
    CampaignState state = campaign.getState();
    state.setCurrentChapterId("tournament");
    campaignRepository.save(campaign);

    // Create a match so processMatchResult has context
    campaignService.createMatchForEncounter(campaign, "Wrestler 1", "Tournament R1", "One on One");

    // 3. Lose Round 1 match (Immediate bracket entry)
    mockMvc
        .perform(
            post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                .param("won", "false"))
        .andExpect(status().isOk());

    // 4. Verify State
    String responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    CampaignState finalState = objectMapper.readValue(responseJson, CampaignState.class);
    // Should be in finals phase (bracket) but lost
    assertThat(finalState.isFinalsPhase()).isTrue();
    // Should not be marked as "failed to qualify" because qualifying phase is skipped
    assertThat(finalState.isFailedToQualify()).isFalse();
    // Should not be winner
    assertThat(finalState.isTournamentWinner()).isFalse();
    // Should have 1 loss
    assertThat(finalState.getLosses()).isEqualTo(1);
  }

  @Test
  void testTournamentExit_TournamentWinner() throws Exception {
    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);
    Campaign campaign = campaignRepository.findActiveByWrestler(testWrestler).get();

    // 2. Force Chapter 2
    CampaignState state = campaign.getState();
    state.setCurrentChapterId("tournament");
    campaignRepository.save(campaign);

    // 3. (Qualifying skipped) - Directly Win 4 Finals matches (R16, QF, SF, F)
    // 16 participants -> 4 rounds.
    for (int i = 0; i < 4; i++) {
      campaignService.createMatchForEncounter(
          campaign, "Wrestler " + (i + 2), "Round " + (i + 1), "One on One");
      mockMvc
          .perform(
              post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                  .param("won", "true"))
          .andExpect(status().isOk());

      // Advance post-match to clear currentMatch for next iteration?
      // createMatchForEncounter requires BACKSTAGE or similar?
      // processMatchResult sets POST_MATCH.
      // createMatchForEncounter doesn't strictly check phase, but sets it to MATCH.
      // However, processMatchResult logic requires currentMatch.
      // After processMatch, currentMatch is still set (but adjudicated).
      // If we call createMatch, it overwrites currentMatch.
      // So this should work.
    }

    // 4. Verify Tournament Winner
    String finalStateJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    CampaignState finalState = objectMapper.readValue(finalStateJson, CampaignState.class);
    assertThat(finalState.isFinalsPhase()).isTrue();
    assertThat(finalState.isTournamentWinner()).isTrue();
  }
}
