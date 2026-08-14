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
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.util.LogSanitizer;
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.AbilityCategory;
import com.github.javydreamercsw.management.domain.wrestler.AbilityType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.WrestlerAbilityDTO;
import com.github.javydreamercsw.management.dto.WrestlerImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
@Order(120)
public class WrestlerSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerAbilityRepository wrestlerAbilityRepository;
  private final WrestlerService wrestlerService;
  private final UniverseRepository universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final NpcService npcService;
  private final TierRecalculationService tierRecalculationService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;

  @Autowired
  public WrestlerSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final WrestlerRepository wrestlerRepository,
      final WrestlerAbilityRepository wrestlerAbilityRepository,
      final WrestlerService wrestlerService,
      final UniverseRepository universeRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final NpcService npcService,
      final TierRecalculationService tierRecalculationService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerAbilityRepository = wrestlerAbilityRepository;
    this.wrestlerService = wrestlerService;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.npcService = npcService;
    this.tierRecalculationService = tierRecalculationService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && wrestlerRepository.count() > 0) {
      return;
    }
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers*.json");
      Universe universe =
          universeRepository.findAll().stream()
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No universe found"));
      Long leagueId = universe.getId();

      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAllWithAlignments().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      Map<String, Npc> npcByName = new HashMap<>();

      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        log.debug("Loading wrestlers from file: {}", resource.getFilename());
        try (var is = resource.getInputStream()) {
          List<WrestlerImportDTO> wrestlersFromFile =
              objectMapper.readValue(is, new TypeReference<>() {});

          Map<String, WrestlerImportDTO> dtoMap = new HashMap<>();
          List<Wrestler> toSaveBulk = new ArrayList<>();
          List<Wrestler> allWrestlers = new ArrayList<>();

          for (WrestlerImportDTO w : wrestlersFromFile) {
            Wrestler existing = wrestlersByName.get(w.getName());
            Wrestler wrestlerToSave;

            if (existing != null) {
              boolean changed = false;
              if (!Objects.equals(existing.getDeckSize(), w.getDeckSize())) {
                existing.setDeckSize(w.getDeckSize());
                changed = true;
              }
              if (!Objects.equals(existing.getStartingHealth(), w.getStartingHealth())) {
                existing.setStartingHealth(w.getStartingHealth());
                changed = true;
              }
              if (!Objects.equals(existing.getLowHealth(), w.getLowHealth())) {
                existing.setLowHealth(w.getLowHealth());
                changed = true;
              }
              if (!Objects.equals(existing.getStartingStamina(), w.getStartingStamina())) {
                existing.setStartingStamina(w.getStartingStamina());
                changed = true;
              }
              if (!Objects.equals(existing.getLowStamina(), w.getLowStamina())) {
                existing.setLowStamina(w.getLowStamina());
                changed = true;
              }
              if (!Objects.equals(existing.getDescription(), w.getDescription())) {
                existing.setDescription(w.getDescription());
                changed = true;
              }
              if (!Objects.equals(existing.getGender(), w.getGender())) {
                existing.setGender(w.getGender());
                changed = true;
              }
              if (w.getImageUrl() != null
                  && !Objects.equals(existing.getImageUrl(), w.getImageUrl())) {
                existing.setImageUrl(w.getImageUrl());
                changed = true;
              }
              if (w.getHeritageTag() != null
                  && !Objects.equals(existing.getHeritageTag(), w.getHeritageTag())) {
                existing.setHeritageTag(w.getHeritageTag());
                changed = true;
              }
              if (w.getExpansionCode() != null
                  && !Objects.equals(existing.getExpansionCode(), w.getExpansionCode())) {
                existing.setExpansionCode(w.getExpansionCode());
                changed = true;
              }
              if (w.getDrive() != null && !Objects.equals(existing.getDrive(), w.getDrive())) {
                existing.setDrive(w.getDrive());
                changed = true;
              }
              if (w.getResilience() != null
                  && !Objects.equals(existing.getResilience(), w.getResilience())) {
                existing.setResilience(w.getResilience());
                changed = true;
              }
              if (w.getCharisma() != null
                  && !Objects.equals(existing.getCharisma(), w.getCharisma())) {
                existing.setCharisma(w.getCharisma());
                changed = true;
              }
              if (w.getBrawl() != null && !Objects.equals(existing.getBrawl(), w.getBrawl())) {
                existing.setBrawl(w.getBrawl());
                changed = true;
              }

              if (w.getAlignment() != null) {
                if (existing.getAlignment() == null
                    || existing.getAlignment().getAlignmentType() == null) {
                  try {
                    AlignmentType at = AlignmentType.valueOf(w.getAlignment().toUpperCase());
                    existing.setAlignment(
                        WrestlerAlignment.builder()
                            .wrestler(existing)
                            .universe(universe)
                            .alignmentType(at)
                            .level(0)
                            .build());
                    log.debug(
                        "Initialized alignment for existing wrestler {}: {}",
                        existing.getName(),
                        at);
                    changed = true;
                  } catch (IllegalArgumentException e) {
                    log.warn(
                        "Invalid alignment '{}' for wrestler '{}'",
                        LogSanitizer.sanitize(w.getAlignment()),
                        LogSanitizer.sanitize(w.getName()));
                  }
                }
              }

              if (existing.getActive() == null) {
                existing.setActive(true);
                changed = true;
              }
              if (existing.getIsPlayer() == null) {
                existing.setIsPlayer(false);
                changed = true;
              }

              wrestlerToSave = existing;
              if (changed) {
                toSaveBulk.add(wrestlerToSave);
                log.debug(
                    "Wrestler changed, will save: {}", LogSanitizer.sanitize(existing.getName()));
              } else {
                log.debug(
                    "Wrestler unchanged, skipping save: {}",
                    LogSanitizer.sanitize(existing.getName()));
              }
            } else {
              Wrestler newWrestler = new Wrestler();
              newWrestler.setName(w.getName());
              newWrestler.setDeckSize(w.getDeckSize());
              newWrestler.setStartingHealth(w.getStartingHealth());
              newWrestler.setLowHealth(w.getLowHealth());
              newWrestler.setStartingStamina(w.getStartingStamina());
              newWrestler.setLowStamina(w.getLowStamina());
              newWrestler.setDescription(w.getDescription());
              newWrestler.setGender(w.getGender());
              newWrestler.setActive(true);
              newWrestler.setIsPlayer(false);
              newWrestler.setImageUrl(w.getImageUrl());
              newWrestler.setHeritageTag(w.getHeritageTag());
              if (w.getExpansionCode() != null) {
                newWrestler.setExpansionCode(w.getExpansionCode());
              }
              if (w.getDrive() != null) {
                newWrestler.setDrive(w.getDrive());
              }
              if (w.getResilience() != null) {
                newWrestler.setResilience(w.getResilience());
              }
              if (w.getCharisma() != null) {
                newWrestler.setCharisma(w.getCharisma());
              }
              if (w.getBrawl() != null) {
                newWrestler.setBrawl(w.getBrawl());
              }

              if (w.getAlignment() != null) {
                try {
                  AlignmentType at = AlignmentType.valueOf(w.getAlignment().toUpperCase());
                  newWrestler.setAlignment(
                      WrestlerAlignment.builder()
                          .wrestler(newWrestler)
                          .alignmentType(at)
                          .level(0)
                          .build());
                } catch (IllegalArgumentException e) {
                  log.warn(
                      "Invalid alignment '{}' for wrestler '{}'",
                      LogSanitizer.sanitize(w.getAlignment()),
                      LogSanitizer.sanitize(w.getName()));
                }
              }

              wrestlerToSave = newWrestler;
              toSaveBulk.add(wrestlerToSave);
            }

            allWrestlers.add(wrestlerToSave);
            dtoMap.put(wrestlerToSave.getName(), w);
          }

          if (!toSaveBulk.isEmpty()) {
            log.debug(
                "Saving {}/{} wrestlers with changes", toSaveBulk.size(), allWrestlers.size());
            wrestlerRepository.saveAll(toSaveBulk);
            wrestlerRepository.flush();
          } else {
            log.debug("All {} wrestlers unchanged — skipping saveAll", allWrestlers.size());
          }

          toSaveBulk.forEach(saved -> wrestlersByName.put(saved.getName(), saved));

          for (Wrestler wrestler : allWrestlers) {
            WrestlerImportDTO w = dtoMap.get(wrestler.getName());
            WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), leagueId);
            boolean stateChanged = state.getId() == null;

            if (w.getFans() != null && w.getFans() > state.getFans()) {
              state.setFans(w.getFans());
              stateChanged = true;
            }
            if (w.getBumps() != null && w.getBumps() > state.getBumps()) {
              state.setBumps(w.getBumps());
              stateChanged = true;
            }
            if (stateChanged) {
              state.setTier(WrestlerTier.fromFanCount(state.getFans()));
              tierRecalculationService.recalculateTier(state);
            }

            if (w.getManager() != null) {
              if (!npcByName.containsKey(w.getManager())) {
                npcByName.put(w.getManager(), npcService.findByName(w.getManager()));
              }
              Npc manager = npcByName.get(w.getManager());
              if (manager != null && !Objects.equals(state.getManager(), manager)) {
                state.setManager(manager);
                stateChanged = true;
              }
            }
            if (stateChanged) {
              wrestlerStateRepository.save(state);
            }

            if (!w.getAbilities().isEmpty()) {
              syncAbilities(wrestler, w.getAbilities());
            }
          }
        } catch (IOException e) {
          log.error("Error loading wrestlers from file", e);
        }
      }
    } catch (IOException e) {
      log.error("Error resolving wrestler resources", e);
    }
    wrestlerService.evictWrestlerCache();
  }

  private void syncAbilities(final Wrestler wrestler, final List<WrestlerAbilityDTO> dtoAbilities) {
    Map<String, WrestlerAbility> existingByName =
        wrestlerAbilityRepository.findByWrestler(wrestler).stream()
            .collect(Collectors.toMap(WrestlerAbility::getName, a -> a, (a, b) -> a));

    Set<String> dtoNames = new HashSet<>();
    for (WrestlerAbilityDTO dto : dtoAbilities) {
      dtoNames.add(dto.getName());
      WrestlerAbility ability = existingByName.getOrDefault(dto.getName(), new WrestlerAbility());
      boolean isNew = ability.getId() == null;

      ability.setWrestler(wrestler);
      ability.setName(dto.getName());
      ability.setDescription(dto.getDescription());
      ability.setDefault(dto.isDefault());
      ability.setUnlockCondition(dto.getUnlockCondition());
      ability.setSwapCondition(dto.getSwapCondition());
      ability.setCostScript(dto.getCostScript());
      ability.setEffectScript(dto.getEffectScript());
      ability.setMaxUses(dto.getMaxUses());

      if (dto.getType() != null) {
        try {
          ability.setAbilityType(AbilityType.valueOf(dto.getType().toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Unknown ability type '{}' for wrestler '{}'", dto.getType(), wrestler.getName());
          ability.setAbilityType(AbilityType.ALWAYS_ON);
        }
      }

      if (dto.getCategory() != null) {
        try {
          ability.setCategory(AbilityCategory.valueOf(dto.getCategory().toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Unknown ability category '{}' for wrestler '{}'",
              dto.getCategory(),
              wrestler.getName());
        }
      }

      if (dto.getTiming() != null) {
        try {
          ability.setTiming(AbilityTiming.valueOf(dto.getTiming().toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Unknown ability timing '{}' for wrestler '{}'", dto.getTiming(), wrestler.getName());
        }
      }

      if (isNew) {
        log.debug("Adding ability '{}' to wrestler '{}'", dto.getName(), wrestler.getName());
      }
      wrestlerAbilityRepository.save(ability);
    }

    for (Map.Entry<String, WrestlerAbility> entry : existingByName.entrySet()) {
      if (!dtoNames.contains(entry.getKey())) {
        log.debug(
            "Removing stale ability '{}' from wrestler '{}'", entry.getKey(), wrestler.getName());
        wrestlerAbilityRepository.delete(entry.getValue());
      }
    }
  }
}
