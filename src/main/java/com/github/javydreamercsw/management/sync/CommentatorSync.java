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
import com.github.javydreamercsw.management.dto.commentator.CommentatorImportDTO;
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
@Order(210)
public class CommentatorSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final CommentaryService commentaryService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CommentatorSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final CommentaryService commentaryService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.commentaryService = commentaryService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && commentaryService.countCommentators() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("commentators.json");
    if (resource.exists()) {
      log.debug("Loading commentators from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CommentatorImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CommentatorImportDTO dto : dtos) {
          commentaryService.createOrUpdateCommentator(
              dto.getNpcName(),
              dto.getGender(),
              dto.getAlignment(),
              dto.getDescription(),
              dto.getStyle(),
              dto.getCatchphrase(),
              dto.getPersonaDescription(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          log.debug("Loaded commentator: {}", dto.getNpcName());
        }
        log.debug("Commentator loading completed - {} commentators loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading commentators from file", e);
      }
    } else {
      log.warn("Commentators file not found: {}", resource.getPath());
    }
  }
}
