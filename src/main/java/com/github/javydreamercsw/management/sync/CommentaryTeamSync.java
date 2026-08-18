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
import com.github.javydreamercsw.management.dto.commentator.CommentaryTeamImportDTO;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
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
@Order(220)
public class CommentaryTeamSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final CommentaryService commentaryService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CommentaryTeamSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final CommentaryService commentaryService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.commentaryService = commentaryService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && commentaryService.countTeams() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentary_teams.json");
    if (resource.exists()) {
      log.debug("Loading commentary teams from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CommentaryTeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CommentaryTeamImportDTO dto : dtos) {
          commentaryService.createOrUpdateTeam(dto.getTeamName(), dto.getMemberNames());
          log.debug("Loaded commentary team: {}", dto.getTeamName());
        }
        log.debug("Commentary team loading completed - {} teams loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentary teams from file", e);
      }
    } else {
      log.warn("Commentary teams file not found: {}", resource.getPath());
    }
  }
}
