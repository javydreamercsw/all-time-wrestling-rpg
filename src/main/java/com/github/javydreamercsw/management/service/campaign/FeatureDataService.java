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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Central service for reading and writing the JSON feature-data blob on {@link CampaignState}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureDataService {

  private final ObjectMapper objectMapper;
  private final CampaignStateRepository campaignStateRepository;

  public Map<String, Object> getFeatureData(final CampaignState state) {
    if (state.getFeatureData() == null) {
      return new HashMap<>();
    }
    try {
      return objectMapper.readValue(
          state.getFeatureData(), new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      log.error("Error parsing feature data", e);
      return new HashMap<>();
    }
  }

  public void saveFeatureData(final CampaignState state, final Map<String, Object> data) {
    try {
      state.setFeatureData(objectMapper.writeValueAsString(data));
    } catch (JsonProcessingException e) {
      log.error("Error serializing feature data", e);
    }
  }

  public <T> T getFeatureValue(
      final CampaignState state, final String key, final Class<T> type, final T defaultValue) {
    Object value = getFeatureData(state).get(key);
    if (value == null) {
      return defaultValue;
    }
    return objectMapper.convertValue(value, type);
  }

  public void setFeatureValue(final CampaignState state, final String key, final Object value) {
    Map<String, Object> data = getFeatureData(state);
    data.put(key, value);
    saveFeatureData(state, data);
  }

  /** Reads the int stored under {@code key}, removes it, persists state, and returns the value. */
  public int consumeFeatureInt(final CampaignState state, final String key) {
    if (state.getFeatureData() == null) {
      return 0;
    }
    try {
      Map<String, Object> data =
          objectMapper.readValue(state.getFeatureData(), new TypeReference<>() {});
      Object value = data.remove(key);
      if (value == null) {
        return 0;
      }
      int result = ((Number) value).intValue();
      state.setFeatureData(objectMapper.writeValueAsString(data));
      campaignStateRepository.save(state);
      return result;
    } catch (Exception e) {
      log.warn("Failed to consume featureData key '{}': {}", key, e.getMessage());
      return 0;
    }
  }
}
