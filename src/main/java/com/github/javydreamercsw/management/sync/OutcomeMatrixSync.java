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
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.dto.OutcomeMatrixEntryImportDTO;
import com.github.javydreamercsw.management.dto.OutcomeMatrixImportDTO;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(250)
public class OutcomeMatrixSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final OutcomeMatrixService outcomeMatrixService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;

  @Autowired
  public OutcomeMatrixSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final OutcomeMatrixService outcomeMatrixService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.outcomeMatrixService = outcomeMatrixService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && outcomeMatrixService.count() > 0) {
      return;
    }
    try {
      Resource[] resources =
          resourcePatternResolver.getResources("classpath*:outcome_matrices/*.json");

      // First pass: create or update all matrices (without redirect FKs)
      for (Resource res : resources) {
        if (!res.exists()) {
          continue;
        }
        log.debug("Loading outcome matrix from file: {}", res.getFilename());
        try (var is = res.getInputStream()) {
          OutcomeMatrixImportDTO dto =
              objectMapper.readValue(is, new TypeReference<OutcomeMatrixImportDTO>() {});
          OutcomeMatrix matrix =
              outcomeMatrixService.getByName(dto.getName()).orElseGet(OutcomeMatrix::new);
          matrix.setName(dto.getName());
          matrix.setDescription(dto.getDescription());
          try {
            matrix.setCategory(OutcomeMatrixCategory.valueOf(dto.getCategory()));
          } catch (IllegalArgumentException e) {
            log.warn(
                "Unknown OutcomeMatrixCategory '{}' in file {}",
                dto.getCategory(),
                res.getFilename());
            continue;
          }
          matrix = outcomeMatrixService.createMatrix(matrix);

          if (dto.getEntries() != null) {
            Map<Integer, OutcomeMatrixEntry> existingByRoll =
                outcomeMatrixService.getEntries(matrix.getId()).stream()
                    .collect(Collectors.toMap(OutcomeMatrixEntry::getDiceRoll, e -> e));
            for (OutcomeMatrixEntryImportDTO entryDto : dto.getEntries()) {
              OutcomeMatrixEntry entry =
                  existingByRoll.getOrDefault(entryDto.getDiceRoll(), new OutcomeMatrixEntry());
              entry.setDiceRoll(entryDto.getDiceRoll());
              entry.setTemplateText(entryDto.getTemplateText());
              entry.setHeatDelta(entryDto.getHeatDelta());
              entry.setFanDelta(entryDto.getFanDelta());
              entry.setTvGradeDelta(entryDto.getTvGradeDelta());
              entry.setGrudgeGradeDelta(entryDto.getGrudgeGradeDelta());
              entry.setInjuryCaused(entryDto.isInjuryCaused());
              if (entry.getId() == null) {
                outcomeMatrixService.addEntry(matrix.getId(), entry);
              } else {
                outcomeMatrixService.updateEntry(entry);
              }
            }
          }
        } catch (IOException e) {
          log.error("Error loading outcome matrix from file {}", res.getFilename(), e);
        }
      }

      // Second pass: wire redirect FKs by name
      for (Resource res : resources) {
        if (!res.exists()) {
          continue;
        }
        try (var is = res.getInputStream()) {
          OutcomeMatrixImportDTO dto =
              objectMapper.readValue(is, new TypeReference<OutcomeMatrixImportDTO>() {});
          if (dto.getEntries() == null) {
            continue;
          }
          OutcomeMatrix matrix = outcomeMatrixService.getByName(dto.getName()).orElse(null);
          if (matrix == null) {
            continue;
          }
          for (OutcomeMatrixEntryImportDTO entryDto : dto.getEntries()) {
            if (entryDto.getRedirectToMatrix() == null) {
              continue;
            }
            outcomeMatrixService.getEntries(matrix.getId()).stream()
                .filter(e -> e.getDiceRoll() == entryDto.getDiceRoll())
                .findFirst()
                .ifPresent(
                    entry -> {
                      outcomeMatrixService
                          .getByName(entryDto.getRedirectToMatrix())
                          .ifPresentOrElse(
                              target -> {
                                entry.setRedirectToMatrix(target);
                                outcomeMatrixService.updateEntry(entry);
                              },
                              () ->
                                  log.warn(
                                      "Redirect matrix '{}' not found for entry roll={} in '{}'",
                                      entryDto.getRedirectToMatrix(),
                                      entryDto.getDiceRoll(),
                                      dto.getName()));
                    });
          }
        } catch (IOException e) {
          log.error("Error wiring redirects for file {}", res.getFilename(), e);
        }
      }
      log.debug("Outcome matrix loading complete.");
    } catch (IOException e) {
      log.error("Error resolving outcome_matrices resources", e);
    }
  }
}
