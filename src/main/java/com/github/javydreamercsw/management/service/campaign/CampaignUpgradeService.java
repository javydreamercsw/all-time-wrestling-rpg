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
package com.github.javydreamercsw.management.service.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgradeRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CampaignUpgradeService {

  private final ObjectMapper objectMapper;
  private final CampaignUpgradeRepository upgradeRepository;
  private final CampaignStateRepository stateRepository;
  private final com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository
      wrestlerRepository;
  private final com.github.javydreamercsw.management.service.wrestler.WrestlerService
      wrestlerService;
  private final com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository
      wrestlerStateRepository;

  @PostConstruct
  public void init() {
    loadUpgrades();
  }

  public void loadUpgrades() {
    log.info("Loading campaign upgrades...");
    try (InputStream is = getClass().getResourceAsStream("/campaign_upgrades.json")) {
      if (is == null) {
        log.error("campaign_upgrades.json not found in resources.");
        return;
      }
      List<CampaignUpgrade> upgrades =
          objectMapper.readValue(is, new TypeReference<List<CampaignUpgrade>>() {});

      // Clear existing upgrades before saving new ones to ensure idempotency
      upgradeRepository.deleteAllInBatch();
      upgradeRepository.saveAll(upgrades);
      log.info("Loaded {} campaign upgrades into database.", upgrades.size());
    } catch (IOException e) {
      log.error("Error loading campaign upgrades from JSON", e);
    }
  }

  public List<CampaignUpgrade> getAllUpgrades() {
    return upgradeRepository.findAll();
  }

  @Transactional
  public void purchaseUpgrade(Campaign campaign, Long upgradeId) {
    CampaignState state = campaign.getState();
    if (state.getSkillTokens() < 8) {
      throw new IllegalStateException("Not enough skill tokens. Need 8.");
    }

    CampaignUpgrade upgrade =
        upgradeRepository
            .findById(upgradeId)
            .orElseThrow(() -> new IllegalArgumentException("Upgrade not found."));

    // Check if an upgrade of the same type is already owned
    boolean alreadyHasType =
        state.getUpgrades().stream().anyMatch(u -> u.getType().equals(upgrade.getType()));

    if (alreadyHasType) {
      throw new IllegalStateException(
          "You already have a permanent upgrade of type: " + upgrade.getType());
    }

    state.setSkillTokens(state.getSkillTokens() - 8);
    state.getUpgrades().add(upgrade);
    stateRepository.save(state);

    log.info(
        "Wrestler {} purchased upgrade: {}", campaign.getWrestler().getName(), upgrade.getName());

    if ("HEALTH".equals(upgrade.getType())) {
      com.github.javydreamercsw.management.domain.wrestler.Wrestler wrestler =
          campaign.getWrestler();
      Long universeId = campaign.getUniverse() != null ? campaign.getUniverse().getId() : 1L;
      com.github.javydreamercsw.management.domain.wrestler.WrestlerState wrestlerState =
          wrestlerService.getOrCreateState(wrestler.getId(), universeId);

      // Manual health refresh logic
      int bonus = 0;
      int penalty = 0;
      if (wrestler.getAlignment() != null
          && wrestler.getAlignment().getCampaign() != null
          && wrestler.getAlignment().getCampaign().getState() != null) {
        bonus = wrestler.getAlignment().getCampaign().getState().getCampaignHealthBonus();
        penalty = wrestler.getAlignment().getCampaign().getState().getHealthPenalty();
      }

      int effective = wrestler.getStartingHealth() + bonus - penalty;
      wrestlerState.setCurrentHealth(Math.max(1, effective));
      wrestlerStateRepository.save(wrestlerState);

      log.info("Refreshed health for wrestler {} in universe {}", wrestler.getName(), universeId);
    }
  }
}
