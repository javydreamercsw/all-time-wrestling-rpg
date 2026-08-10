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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
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
@Order(40)
public class ShowTypeSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final ShowTypeService showTypeService;
  private final ObjectMapper objectMapper;

  @Autowired
  public ShowTypeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final ShowTypeService showTypeService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.showTypeService = showTypeService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && showTypeService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("show_types.json");
    if (resource.exists()) {
      log.debug("Loading show types from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ShowType> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (ShowType st : dtos) {
          showTypeService.createOrUpdateShowType(
              st.getName(), st.getDescription(), st.getExpectedMatches(), st.getExpectedPromos());
        }
        log.debug("Show type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading show types from file", e);
      }
    } else {
      log.warn("Show types file not found: {}", resource.getPath());
    }
  }
}
