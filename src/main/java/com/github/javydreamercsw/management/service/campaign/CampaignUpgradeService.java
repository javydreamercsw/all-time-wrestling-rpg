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

  @PostConstruct
  public void init() {
    loadUpgrades();
  }

  public void loadUpgrades() {
    if (upgradeRepository.count() > 0) {
      log.info("Campaign upgrades already loaded.");
      return;
    }

    try (InputStream is = getClass().getResourceAsStream("/campaign_upgrades.json")) {
      if (is == null) {
        log.error("campaign_upgrades.json not found in resources.");
        return;
      }
      List<CampaignUpgrade> upgrades = objectMapper.readValue(is, new TypeReference<List<CampaignUpgrade>>() {});
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

    CampaignUpgrade upgrade = upgradeRepository.findById(upgradeId)
        .orElseThrow(() -> new IllegalArgumentException("Upgrade not found."));

    if (state.getUpgrades().contains(upgrade)) {
      throw new IllegalStateException("Upgrade already purchased.");
    }

    state.setSkillTokens(state.getSkillTokens() - 8);
    state.getUpgrades().add(upgrade);
    stateRepository.save(state);
    
    log.info("Wrestler {} purchased upgrade: {}", campaign.getWrestler().getName(), upgrade.getName());
  }
}
