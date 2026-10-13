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
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.dto.TournamentDefinitionDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Seeds the predefined tournament catalog from tournaments.json (ATW-vg16), mirroring the
 * SegmentTypeSync pattern. Runs after SegmentRuleSync (@Order 30) so the referenced rules exist.
 * Upserts by {@code code} — lifecycle state (status, entries, rounds) is never touched on re-sync,
 * so a consumed or completed seeded tournament stays consumed.
 */
@Slf4j
@Component
@Order(55)
public class TournamentSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final TournamentService tournamentService;
  private final ObjectMapper objectMapper;

  @Autowired
  public TournamentSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final TournamentService tournamentService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.tournamentService = tournamentService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && tournamentService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("tournaments.json");
    if (resource.exists()) {
      log.debug("Loading tournament definitions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<TournamentDefinitionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (TournamentDefinitionDTO dto : dtos) {
          Tournament tournament =
              tournamentService.createOrUpdateTournament(
                  dto.code(),
                  dto.name(),
                  dto.formatId(),
                  dto.defaultEntrantCount(),
                  dto.allowedRules());
          log.debug("Loaded tournament: {} ({})", tournament.getName(), tournament.getCode());
        }
        log.debug("Tournament catalog loading completed");
      } catch (IOException e) {
        log.error("Error loading tournaments.json", e);
      }
    } else {
      log.warn("Tournament catalog file not found: {}", resource.getPath());
    }
  }
}
