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
import com.github.javydreamercsw.management.dto.StatusCardDTO;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(190)
public class StatusCardSync implements DataSyncContributor {

  private final StatusCardService statusCardService;
  private final ObjectMapper objectMapper;

  @Autowired
  public StatusCardSync(
      final StatusCardService statusCardService, final ObjectMapper objectMapper) {
    this.statusCardService = statusCardService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    ClassPathResource resource = new ClassPathResource("status_cards.json");
    if (resource.exists()) {
      log.debug("Loading status cards from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<StatusCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (statusCardService.findAll().size() == dtos.size()) {
          log.debug("Status cards already loaded ({} cards), skipping sync.", dtos.size());
          return;
        }
        for (StatusCardDTO dto : dtos) {
          statusCardService.createOrUpdateCard(
              dto.getKey(),
              dto.getLevel1Name(),
              dto.getLevel2Name(),
              dto.getDescription(),
              dto.isPositive(),
              dto.getLevel1Effect(),
              dto.getLevel2Effect(),
              dto.getFlipUpCondition(),
              dto.getFlipDownCondition(),
              dto.getDiscardCondition());
          log.debug("Loaded status card: {}", dto.getKey());
        }
        log.debug("Status card loading completed - {} cards loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading status cards from file", e);
      }
    } else {
      log.warn("Status cards file not found: {}", resource.getPath());
    }
  }
}
