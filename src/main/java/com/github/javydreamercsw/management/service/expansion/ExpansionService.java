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
package com.github.javydreamercsw.management.service.expansion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpansionService {

  public static final String SET_ENABLED_PREFIX = "set_enabled_";
  private final GameSettingService gameSettingService;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public List<Expansion> getExpansions() {
    List<Expansion> expansions = new ArrayList<>();
    ClassPathResource resource = new ClassPathResource("expansions.json");
    if (resource.exists()) {
      try (var is = resource.getInputStream()) {
        expansions = objectMapper.readValue(is, new TypeReference<List<Expansion>>() {});
        for (Expansion expansion : expansions) {
          expansion.setEnabled(isExpansionEnabled(expansion.getCode()));
        }
      } catch (IOException e) {
        log.error("Error loading expansions from file", e);
      }
    }
    return expansions;
  }

  public boolean isExpansionEnabled(String expansionCode) {
    String key = SET_ENABLED_PREFIX + expansionCode;
    return gameSettingService
        .findById(key)
        .map(setting -> Boolean.parseBoolean(setting.getValue()))
        .orElse(true); // Default to enabled
  }

  public void setExpansionEnabled(String expansionCode, boolean enabled) {
    String key = SET_ENABLED_PREFIX + expansionCode;
    gameSettingService.save(key, String.valueOf(enabled));
    eventPublisher.publishEvent(new ExpansionToggledEvent(this, expansionCode, enabled));
  }

  public List<String> getEnabledExpansionCodes() {
    return getExpansions().stream()
        .filter(Expansion::isEnabled)
        .map(Expansion::getCode)
        .collect(Collectors.toList());
  }
}
