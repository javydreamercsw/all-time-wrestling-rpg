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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
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

  private Wrestler testWrestler;

  @BeforeEach
  void setupData() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    testWrestler =
        Wrestler.builder().name("API Tester").startingHealth(100).startingStamina(100).build();
    testWrestler = wrestlerRepository.save(testWrestler);
  }

  @Test
  void testTournamentExit_FailedToQualify() throws Exception {
    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);
    Campaign campaign = campaignRepository.findActiveByWrestler(testWrestler).get();

    // 2. Force Chapter 2
    CampaignState state = campaign.getState();
    state.setCurrentChapterId("ch2_tournament");
    campaignRepository.save(campaign);

    // 3. Lose 4 qualifying matches (assuming 4 matches, min 3 wins to qualify)
    for (int i = 0; i < 4; i++) {
      mockMvc
          .perform(
              post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                  .param("won", "false"))
          .andExpect(status().isOk());
    }

    // 4. Verify State
    String responseJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    CampaignState finalState = objectMapper.readValue(responseJson, CampaignState.class);
    assertThat(finalState.isFailedToQualify()).isTrue();
    assertThat(finalState.isFinalsPhase()).isFalse();
  }

  @Test
  void testTournamentExit_TournamentWinner() throws Exception {
    // 1. Start Campaign
    campaignService.startCampaign(testWrestler);
    Campaign campaign = campaignRepository.findActiveByWrestler(testWrestler).get();

    // 2. Force Chapter 2
    CampaignState state = campaign.getState();
    state.setCurrentChapterId("ch2_tournament");
    campaignRepository.save(campaign);

    // 3. Win 4 qualifying matches to reach finals
    for (int i = 0; i < 4; i++) {
      mockMvc
          .perform(
              post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                  .param("won", "true"))
          .andExpect(status().isOk());
    }

    // Check if in finals phase
    String midStateJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    CampaignState midState = objectMapper.readValue(midStateJson, CampaignState.class);
    assertThat(midState.isFinalsPhase()).isTrue();

    // 4. Win 2 Finals matches (totalFinalsMatches: 2)
    for (int i = 0; i < 2; i++) {
      mockMvc
          .perform(
              post("/api/campaign/" + testWrestler.getId() + "/test/process-match")
                  .param("won", "true"))
          .andExpect(status().isOk());
    }

    // 5. Verify Tournament Winner
    String finalStateJson =
        mockMvc
            .perform(get("/api/campaign/" + testWrestler.getId() + "/state"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    CampaignState finalState = objectMapper.readValue(finalStateJson, CampaignState.class);
    assertThat(finalState.isTournamentWinner()).isTrue();
  }
}
