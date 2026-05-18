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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link CampaignController}. Each test uses a real DB and a standalone
 * MockMvc to avoid Vaadin servlet interference.
 */
@DisplayName("CampaignController Integration Tests")
@Transactional
class CampaignControllerIT extends AbstractRestControllerIT {

  @Autowired private CampaignUpgradeService upgradeService;

  private Wrestler testWrestler;
  private Campaign testCampaign;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new CampaignController(
                    campaignService, campaignRepository, wrestlerRepository, upgradeService))
            .build();

    testWrestler = createTestWrestler("Campaign IT Wrestler");
    testCampaign = campaignService.startCampaign(testWrestler);
  }

  // -------------------------------------------------------------------------
  // GET /{wrestlerId}/state — 200 when wrestler + active campaign exist
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("GET /{wrestlerId}/state returns 200 with campaign state for a valid wrestler")
  void getCampaignState_validWrestler_returns200() throws Exception {
    mockMvc
        .perform(get("/api/campaign/{id}/state", testWrestler.getId()))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // GET /{wrestlerId}/state — 404 when wrestler does not exist
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("GET /{wrestlerId}/state returns 404 when the wrestler ID does not exist")
  void getCampaignState_unknownWrestler_returns404() throws Exception {
    mockMvc
        .perform(get("/api/campaign/{id}/state", Long.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  // -------------------------------------------------------------------------
  // GET /{wrestlerId}/state — 404 when wrestler exists but has no active campaign
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("GET /{wrestlerId}/state returns 404 when the wrestler has no active campaign")
  void getCampaignState_noActiveCampaign_returns404() throws Exception {
    // Create a fresh wrestler with no campaign
    Wrestler noCampaignWrestler = createTestWrestler("No Campaign Wrestler");

    mockMvc
        .perform(get("/api/campaign/{id}/state", noCampaignWrestler.getId()))
        .andExpect(status().isNotFound());
  }

  // -------------------------------------------------------------------------
  // GET /upgrades — 200 and returns array
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("GET /upgrades returns 200 with an array of available upgrades")
  void getAllUpgrades_returns200WithList() throws Exception {
    mockMvc
        .perform(get("/api/campaign/upgrades"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  // -------------------------------------------------------------------------
  // POST /{wrestlerId}/test/advance-chapter — 200 when campaign exists
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("POST /{wrestlerId}/test/advance-chapter returns 200 for a valid active campaign")
  void advanceChapter_validCampaign_returns200() throws Exception {
    mockMvc
        .perform(post("/api/campaign/{id}/test/advance-chapter", testWrestler.getId()))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // POST /{wrestlerId}/test/advance-chapter — 404 when wrestler ID is unknown
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("POST /{wrestlerId}/test/advance-chapter returns 404 for an unknown wrestler")
  void advanceChapter_unknownWrestler_returns404() throws Exception {
    mockMvc
        .perform(post("/api/campaign/{id}/test/advance-chapter", Long.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  // -------------------------------------------------------------------------
  // POST /{wrestlerId}/test/process-match — 200 for a win
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("POST /{wrestlerId}/test/process-match returns 200 when recording a win")
  void processMatchResult_win_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/campaign/{id}/test/process-match", testWrestler.getId())
                .param("won", "true"))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // POST /{wrestlerId}/test/skip-to-show — 200 when campaign exists
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("POST /{wrestlerId}/test/skip-to-show returns 200 for a valid active campaign")
  void skipToShow_validCampaign_returns200() throws Exception {
    mockMvc
        .perform(post("/api/campaign/{id}/test/skip-to-show", testWrestler.getId()))
        .andExpect(status().isOk());
  }

  // -------------------------------------------------------------------------
  // POST /{wrestlerId}/upgrades/purchase — 404 when wrestler ID is unknown
  // -------------------------------------------------------------------------
  @Test
  @DisplayName(
      "POST /{wrestlerId}/upgrades/purchase returns 404 when the wrestler ID does not exist")
  void purchaseUpgrade_unknownWrestler_returns404() throws Exception {
    mockMvc
        .perform(
            post("/api/campaign/{id}/upgrades/purchase", Long.MAX_VALUE).param("upgradeId", "1"))
        .andExpect(status().isNotFound());
  }
}
