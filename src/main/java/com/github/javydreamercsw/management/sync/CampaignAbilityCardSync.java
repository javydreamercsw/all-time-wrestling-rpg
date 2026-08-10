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
package com.github.javydreamercsw.management.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(180)
public class CampaignAbilityCardSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final CampaignAbilityCardService campaignAbilityCardService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CampaignAbilityCardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final CampaignAbilityCardService campaignAbilityCardService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.campaignAbilityCardService = campaignAbilityCardService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && campaignAbilityCardService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (resource.exists()) {
      log.debug("Loading campaign ability cards from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CampaignAbilityCardDTO dto : dtos) {
          campaignAbilityCardService.createOrUpdateCard(
              dto.getName(),
              dto.getDescription(),
              dto.getAlignmentType(),
              dto.getLevel(),
              dto.isOneTimeUse(),
              dto.getTiming(),
              dto.getEffectScript(),
              dto.getSecondaryEffectScript(),
              dto.isSecondaryOneTimeUse(),
              dto.getSecondaryTiming());
          log.debug("Loaded campaign ability card: {}", dto.getName());
        }
        log.debug("Campaign ability card loading completed - {} cards loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading campaign ability cards from file", e);
      }
    } else {
      log.warn("Campaign ability cards file not found: {}", resource.getPath());
    }
  }
}
