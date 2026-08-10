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
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(60)
public class ShowTemplateSync implements DataSyncContributor {

  private final ShowTemplateService showTemplateService;
  private final ObjectMapper objectMapper;

  @Autowired
  public ShowTemplateSync(
      final ShowTemplateService showTemplateService, final ObjectMapper objectMapper) {
    this.showTemplateService = showTemplateService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    long existingCount = showTemplateService.count();
    if (existingCount > 0) {
      log.debug(
          "Show templates table already contains {} templates - skipping file import",
          existingCount);
      return;
    }

    ClassPathResource resource = new ClassPathResource("show_templates.json");
    if (resource.exists()) {
      log.debug(
          "Show templates table is empty - loading templates from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ShowTemplateDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (ShowTemplateDTO dto : dtos) {
          ShowTemplate template =
              showTemplateService.createOrUpdateTemplate(
                  dto.getName(),
                  dto.getDescription(),
                  dto.getShowTypeName(),
                  null,
                  dto.getCommentaryTeamName(),
                  dto.getExpectedMatches(),
                  dto.getExpectedPromos(),
                  dto.getDurationDays(),
                  dto.getRecurrenceType() != null
                      ? RecurrenceType.valueOf(dto.getRecurrenceType())
                      : null,
                  dto.getDayOfWeek() != null ? DayOfWeek.valueOf(dto.getDayOfWeek()) : null,
                  dto.getDayOfMonth(),
                  dto.getWeekOfMonth(),
                  dto.getMonth() != null ? Month.valueOf(dto.getMonth()) : null,
                  dto.getGenderConstraint() != null
                      ? Gender.valueOf(dto.getGenderConstraint())
                      : null);
          if (template != null) {
            log.debug(
                "Loaded show template: {} (Type: {})", template.getName(), dto.getShowTypeName());
          } else {
            log.warn(
                "Failed to load show template: {} - show type not found: {}",
                dto.getName(),
                dto.getShowTypeName());
          }
        }
      } catch (IOException e) {
        log.error("Error loading show templates from file", e);
      }
    } else {
      log.warn("Show templates file not found: {}", resource.getPath());
    }
  }
}
