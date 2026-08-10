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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.service.title.TitleReignRepairService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(140)
public class ChampionshipSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final TitleService titleService;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseRepository universeRepository;
  private final TitleReignRepairService titleReignRepairService;
  private final ObjectMapper objectMapper;

  @Autowired
  public ChampionshipSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final TitleService titleService,
      final WrestlerRepository wrestlerRepository,
      final UniverseRepository universeRepository,
      final TitleReignRepairService titleReignRepairService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.titleService = titleService;
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
    this.titleReignRepairService = titleReignRepairService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && titleService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("championships.json");
    if (!resource.exists()) {
      return;
    }
    log.debug("Loading championships from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<TitleDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Long universeId =
          universeRepository.findAll().stream()
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No universe found"))
              .getId();
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      List<Title> toSave = new ArrayList<>();
      for (TitleDTO dto : dtos) {
        Optional<Title> existingTitle = titleService.findByName(dto.getName());
        Title title;
        if (existingTitle.isEmpty()) {
          title =
              titleService.createTitle(
                  dto.getName(),
                  dto.getDescription(),
                  dto.getTier(),
                  dto.getChampionshipType(),
                  dto.getGender(),
                  universeId);
          log.debug(
              "Created new title: {} with type: {}", dto.getName(), dto.getChampionshipType());
        } else {
          title = existingTitle.get();
          log.debug("Title {} already exists, updating.", dto.getName());
        }
        title.setChampionshipType(dto.getChampionshipType());
        title.setEffectScript(dto.getEffectScript());
        title.setGender(dto.getGender());
        title.setImageUrl(dto.getImageUrl());
        title.setExpansionCode(
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        if (dto.getIncludeInRankings() != null) {
          title.setIncludeInRankings(dto.getIncludeInRankings());
        }
        toSave.add(title);
      }
      titleService.saveAll(toSave);

      for (TitleDTO dto : dtos) {
        if (dto.getCurrentChampionName() != null
            && !dto.getCurrentChampionName().trim().isEmpty()) {
          Title title = titleService.findByName(dto.getName()).orElse(null);
          if (title == null) {
            continue;
          }
          String[] championNames = dto.getCurrentChampionName().split(",");
          List<Wrestler> champions = new ArrayList<>();
          for (String name : championNames) {
            Optional.ofNullable(wrestlersByName.get(name.trim())).ifPresent(champions::add);
          }
          if (!champions.isEmpty()) {
            boolean matchesCurrent =
                title.getCurrentChampions().size() == champions.size()
                    && new HashSet<>(title.getCurrentChampions()).containsAll(champions);
            if (!matchesCurrent) {
              titleService.awardTitleTo(title, champions);
              log.debug(
                  "Awarded title {} to champions {}",
                  title.getName(),
                  dto.getCurrentChampionName());
            }
          }
        } else {
          log.debug("Leaving title {} as is in the database.", dto.getName());
        }
      }
    } catch (IOException e) {
      log.error("Error loading championships from file", e);
    }

    titleReignRepairService.repairIfNeeded();
  }
}
