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
import java.util.Objects;
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

  private final SeedSyncSupport syncSupport;
  private final TitleService titleService;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseRepository universeRepository;
  private final TitleReignRepairService titleReignRepairService;
  private final ObjectMapper objectMapper;

  @Autowired
  public ChampionshipSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final TitleService titleService,
      final WrestlerRepository wrestlerRepository,
      final UniverseRepository universeRepository,
      final TitleReignRepairService titleReignRepairService,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.titleService = titleService;
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
    this.titleReignRepairService = titleReignRepairService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && titleService.count() > 0) {
      topUpMissingTitles();
      if (syncSupport.isVerify()) {
        reconcileVerifiedTitles();
      }
      return;
    }
    loadChampionships(true);
    titleReignRepairService.repairIfNeeded();
  }

  /**
   * Skip-gate branch: titles exist and the {@code skip-if-not-empty} flag protects existing rows
   * from seed-driven updates. Still create seed titles that don't exist yet, so championships added
   * in later releases reach flag-protected installs. Existing rows are never re-saved; the
   * champion-award block is FULL-only (current holders are gameplay state, never seed-owned).
   */
  private void topUpMissingTitles() {
    loadChampionships(false);
  }

  /**
   * Loads titles from {@code championships.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync) and
   *     awards champions from {@code current_champion_name}; when false, only missing titles are
   *     created (without champion awards) and existing rows are left untouched.
   */
  private void loadChampionships(boolean updateExisting) {
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
      List<TitleDTO> created = new ArrayList<>();
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
          if (!updateExisting) {
            // Top-up mode: never modify an existing title.
            continue;
          }
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
        stampSeedFingerprint(title);
        toSave.add(title);
        created.add(dto);
      }
      titleService.saveAll(toSave);

      // Champion awards are gameplay state seeding, not content: in FULL mode the block runs for
      // every seed entry exactly as it always has; in top-up mode only titles this sync created
      // get their initial award. VERIFY never reaches here.
      for (TitleDTO dto : (updateExisting ? dtos : created)) {
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
  }

  /** Canonical seed-owned field values for a title row, in fingerprint order. */
  private String titleFingerprint(Title title) {
    return syncSupport.fingerprint(
        title.getChampionshipType() == null ? null : title.getChampionshipType().name(),
        title.getEffectScript(),
        title.getGender() == null ? null : title.getGender().name(),
        title.getImageUrl(),
        title.getExpansionCode(),
        title.getIncludeInRankings());
  }

  private void stampSeedFingerprint(Title title) {
    title.setSeedFingerprint(titleFingerprint(title));
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing titles. The stored fingerprint is the hash
   * of the row's seed-owned fields as the last seed write left them; re-hashing the row's current
   * fields detects hand edits. A row that still hashes to its stamp is clean and adopts seed
   * changes; a row whose fields drifted is user-edited — warned and preserved. Titles with no seed
   * entry are never considered; current holders are never touched.
   */
  private void reconcileVerifiedTitles() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("championships.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<TitleDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      int reconciled = 0;
      int preserved = 0;
      List<Title> toSave = new ArrayList<>();
      for (TitleDTO dto : dtos) {
        Title title = titleService.findByName(dto.getName()).orElse(null);
        if (title == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = titleFingerprint(title);
        if (SeedSyncSupport.isBlankFingerprint(title.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (titleMatchesSeed(title, dto)) {
            title.setSeedFingerprint(current);
            toSave.add(title);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: title '{}' has hand-edited seed-owned fields (legacy row, no "
                    + "fingerprint); preserving user values.",
                title.getName());
            preserved++;
          }
        } else if (current.equals(title.getSeedFingerprint())) {
          if (titleMatchesSeed(title, dto)) {
            continue; // already in sync
          }
          applySeedFields(title, dto);
          stampSeedFingerprint(title);
          toSave.add(title);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: title '{}' was modified after seeding (fingerprint mismatch); "
                  + "preserving user values.",
              title.getName());
          preserved++;
        }
      }
      if (!toSave.isEmpty()) {
        titleService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("title", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during title verify reconcile", e);
    }
  }

  private boolean titleMatchesSeed(Title title, TitleDTO dto) {
    return Objects.equals(
            title.getChampionshipType(),
            dto.getChampionshipType() != null ? dto.getChampionshipType() : null)
        && Objects.equals(title.getEffectScript(), dto.getEffectScript())
        && Objects.equals(title.getGender(), dto.getGender())
        && Objects.equals(title.getImageUrl(), dto.getImageUrl())
        && Objects.equals(
            title.getExpansionCode(),
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME")
        && Objects.equals(
            title.getIncludeInRankings(),
            dto.getIncludeInRankings() != null ? dto.getIncludeInRankings() : Boolean.TRUE);
  }

  private void applySeedFields(Title title, TitleDTO dto) {
    title.setChampionshipType(dto.getChampionshipType());
    title.setEffectScript(dto.getEffectScript());
    title.setGender(dto.getGender());
    title.setImageUrl(dto.getImageUrl());
    title.setExpansionCode(dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
    if (dto.getIncludeInRankings() != null) {
      title.setIncludeInRankings(dto.getIncludeInRankings());
    }
  }
}
