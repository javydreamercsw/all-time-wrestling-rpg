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
import com.github.javydreamercsw.management.dto.RingsideActionDTO;
import com.github.javydreamercsw.management.dto.RingsideActionTypeDTO;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
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
@Order(240)
public class RingsideActionSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final RingsideActionDataService ringsideActionDataService;
  private final ObjectMapper objectMapper;

  @Autowired
  public RingsideActionSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final RingsideActionDataService ringsideActionDataService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.ringsideActionDataService = ringsideActionDataService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    syncTypes();
    syncActions();
  }

  private void syncTypes() {
    if (skipIfNotEmpty && ringsideActionDataService.countTypes() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_action_types.json");
    if (resource.exists()) {
      log.debug("Loading ringside action types from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionTypeDTO> dtos =
            objectMapper.readValue(is, new TypeReference<List<RingsideActionTypeDTO>>() {});
        for (RingsideActionTypeDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateType(
              dto.getName(),
              dto.isIncreasesAwareness(),
              dto.isCanCauseDq(),
              dto.getBaseRiskMultiplier());
          log.debug("Loaded ringside action type: {}", dto.getName());
        }
        log.debug("Ringside action type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside action types from file", e);
      }
    }
  }

  private void syncActions() {
    if (skipIfNotEmpty && ringsideActionDataService.countActions() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (resource.exists()) {
      log.debug("Loading ringside actions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (RingsideActionDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateAction(
              dto.getName(),
              dto.getType(),
              dto.getDescription(),
              dto.getImpact(),
              dto.getRisk(),
              dto.getAlignment(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          log.debug("Loaded ringside action: {}", dto.getName());
        }
        log.debug("Ringside action loading completed - {} actions loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside actions from file", e);
      }
    }
  }
}
