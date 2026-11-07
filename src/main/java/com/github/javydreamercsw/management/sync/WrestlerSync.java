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
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "wrestler";

  private final SeedSyncSupport syncSupport;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerAbilityRepository wrestlerAbilityRepository;
  private final WrestlerService wrestlerService;
  private final UniverseRepository universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final NpcService npcService;
  private final TierRecalculationService tierRecalculationService;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public WrestlerSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final WrestlerRepository wrestlerRepository,
      final WrestlerAbilityRepository wrestlerAbilityRepository,
      final WrestlerService wrestlerService,
      final UniverseRepository universeRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final NpcService npcService,
      final TierRecalculationService tierRecalculationService,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerAbilityRepository = wrestlerAbilityRepository;
    this.wrestlerService = wrestlerService;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.npcService = npcService;
    this.tierRecalculationService = tierRecalculationService;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && wrestlerRepository.count() > 0) {
      topUpMissingWrestlers();
      if (syncSupport.isVerify()) {
        reconcileVerifiedWrestlers();
      }
      return;
    }
    loadWrestlers(true);
    wrestlerService.evictWrestlerCache();
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing wrestlers are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || wrestlerRepository.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers/*.json");
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAllWithAlignments().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      List<Wrestler> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<WrestlerImportDTO> wrestlersFromFile =
              objectMapper.readValue(is, new TypeReference<>() {});
          for (WrestlerImportDTO w : wrestlersFromFile) {
            Wrestler wrestler = wrestlersByName.get(w.getName());
            if (wrestler == null) {
              continue; // user-created rows have no seed match; missing rows are top-up's job
            }
            String newFingerprint =
                SeedReviewDetectionSupport.incomingFingerprint(
                    provenanceManifest, DOMAIN_KEY, w.getName(), seedValueFingerprint(w, wrestler));
            boolean rowHasStoredStamp =
                SeedReviewDetectionSupport.hasStamp(wrestler.getSeedFingerprint());
            List<SeedCollisionDetector.FieldObservation> observations =
                fieldObservations(wrestler, w);
            SeedReviewDetectionSupport.DetectionResult result =
                SeedReviewDetectionSupport.detectRow(
                    DOMAIN_KEY,
                    entityKey(wrestler.getName()),
                    wrestler.getName(),
                    rowHasStoredStamp,
                    wrestler.getSeedFingerprint(),
                    observations,
                    values -> syncSupport.fingerprint(values.toArray()),
                    newFingerprint,
                    seedReviewService);
            String stamp =
                SeedReviewDetectionSupport.stampForResolvedRow(
                    result.provenanceExact(), newFingerprint);
            if (!result.silentUpdateFields().isEmpty()) {
              // Tier-1 write set: ONLY the silent-update fields — an admin edit the pass decided
              // to preserve (tier 3) must never be touched (per-field granularity, spec "Tier 2
              // granularity").
              applySilentSeedFields(wrestler, w, result.silentUpdateFields());
              toSave.add(wrestler);
            }
            if (stamp != null && !stamp.equals(wrestler.getSeedFingerprint())) {
              wrestler.setSeedFingerprint(stamp);
              if (!toSave.contains(wrestler)) {
                toSave.add(wrestler);
              }
            }
          }
        } catch (IOException e) {
          log.error("Error during wrestler seed-review detection", e);
        }
      }
      if (!toSave.isEmpty()) {
        wrestlerRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (wrestler) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving wrestler resources for seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one wrestler row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Wrestler wrestler, WrestlerImportDTO w) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "deckSize",
            null,
            wrestler.getDeckSize() == null ? null : wrestler.getDeckSize().toString(),
            w.getDeckSize() == null ? null : w.getDeckSize().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "startingHealth",
            null,
            wrestler.getStartingHealth() == null ? null : wrestler.getStartingHealth().toString(),
            w.getStartingHealth() == null ? null : w.getStartingHealth().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "lowHealth",
            null,
            wrestler.getLowHealth() == null ? null : wrestler.getLowHealth().toString(),
            w.getLowHealth() == null ? null : w.getLowHealth().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "startingStamina",
            null,
            wrestler.getStartingStamina() == null ? null : wrestler.getStartingStamina().toString(),
            w.getStartingStamina() == null ? null : w.getStartingStamina().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "lowStamina",
            null,
            wrestler.getLowStamina() == null ? null : wrestler.getLowStamina().toString(),
            w.getLowStamina() == null ? null : w.getLowStamina().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, wrestler.getDescription(), w.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "gender",
            null,
            wrestler.getGender() == null ? null : wrestler.getGender().name(),
            w.getGender() == null ? null : w.getGender().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "imageUrl", null, wrestler.getImageUrl(), w.getImageUrl()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "heritageTag", null, wrestler.getHeritageTag(), w.getHeritageTag()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expansionCode", null, wrestler.getExpansionCode(), w.getExpansionCode()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "drive",
            null,
            wrestler.getDrive() == null ? null : wrestler.getDrive().toString(),
            w.getDrive() == null ? null : w.getDrive().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "resilience",
            null,
            wrestler.getResilience() == null ? null : wrestler.getResilience().toString(),
            w.getResilience() == null ? null : w.getResilience().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "charisma",
            null,
            wrestler.getCharisma() == null ? null : wrestler.getCharisma().toString(),
            w.getCharisma() == null ? null : w.getCharisma().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "brawl",
            null,
            wrestler.getBrawl() == null ? null : wrestler.getBrawl().toString(),
            w.getBrawl() == null ? null : w.getBrawl().toString()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "alignment",
            null,
            wrestler.getAlignment() == null
                ? null
                : wrestler.getAlignment().getAlignmentType().name(),
            w.getAlignment() == null
                ? null
                : AlignmentType.valueOf(w.getAlignment().toUpperCase()).name()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(WrestlerImportDTO w, Wrestler wrestler) {
    return syncSupport.fingerprint(
        w.getDeckSize(),
        w.getStartingHealth(),
        w.getLowHealth(),
        w.getStartingStamina(),
        w.getLowStamina(),
        w.getDescription(),
        w.getGender(),
        w.getImageUrl(),
        w.getHeritageTag(),
        w.getExpansionCode() == null ? wrestler.getExpansionCode() : w.getExpansionCode(),
        w.getDrive(),
        w.getResilience(),
        w.getCharisma(),
        w.getBrawl(),
        w.getAlignment() == null
            ? (wrestler.getAlignment() == null ? null : wrestler.getAlignment().getAlignmentType())
            : AlignmentType.valueOf(w.getAlignment().toUpperCase()));
  }

  private static String entityKey(String name) {
    return "wrestler:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@link #applySeedFields(Wrestler, WrestlerImportDTO)}.
   */
  private void applySilentSeedFields(
      Wrestler wrestler, WrestlerImportDTO w, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "deckSize" -> wrestler.setDeckSize(w.getDeckSize());
        case "startingHealth" -> wrestler.setStartingHealth(w.getStartingHealth());
        case "lowHealth" -> wrestler.setLowHealth(w.getLowHealth());
        case "startingStamina" -> wrestler.setStartingStamina(w.getStartingStamina());
        case "lowStamina" -> wrestler.setLowStamina(w.getLowStamina());
        case "description" -> wrestler.setDescription(w.getDescription());
        case "gender" -> wrestler.setGender(w.getGender());
        case "imageUrl" -> {
          if (w.getImageUrl() != null) {
            wrestler.setImageUrl(w.getImageUrl());
          }
        }
        case "heritageTag" -> {
          if (w.getHeritageTag() != null) {
            wrestler.setHeritageTag(w.getHeritageTag());
          }
        }
        case "expansionCode" -> wrestler.setExpansionCode(w.getExpansionCode());
        case "drive" -> {
          if (w.getDrive() != null) {
            wrestler.setDrive(w.getDrive());
          }
        }
        case "resilience" -> {
          if (w.getResilience() != null) {
            wrestler.setResilience(w.getResilience());
          }
        }
        case "charisma" -> {
          if (w.getCharisma() != null) {
            wrestler.setCharisma(w.getCharisma());
          }
        }
        case "brawl" -> {
          if (w.getBrawl() != null) {
            wrestler.setBrawl(w.getBrawl());
          }
        }
        case "alignment" -> {
          if (w.getAlignment() != null) {
            try {
              AlignmentType seedType = AlignmentType.valueOf(w.getAlignment().toUpperCase());
              WrestlerAlignment current = wrestler.getAlignment();
              if (current != null) {
                current.setAlignmentType(seedType);
              } else {
                wrestler.setAlignment(
                    WrestlerAlignment.builder()
                        .wrestler(wrestler)
                        .universe(universeRepository.findAll().stream().findFirst().orElse(null))
                        .alignmentType(seedType)
                        .level(0)
                        .build());
              }
            } catch (IllegalArgumentException e) {
              log.warn(
                  "Invalid alignment '{}' for wrestler '{}'",
                  LogSanitizer.sanitize(w.getAlignment()),
                  LogSanitizer.sanitize(wrestler.getName()));
            }
          }
        }
        default ->
            throw new IllegalStateException("Unknown silent-update wrestler field '" + field + "'");
      }
    }
  }

  /**
   * Skip-gate branch: wrestlers exist and the {@code skip-if-not-empty} flag protects existing rows
   * from seed-driven updates. Still insert seed wrestlers that don't exist yet, so expansions added
   * in later releases reach flag-protected installs. Existing rows are never re-saved; abilities of
   * existing wrestlers keep syncing (pre-existing skip-branch behavior).
   */
  private void topUpMissingWrestlers() {
    loadWrestlers(false);
    syncAbilitiesForExistingWrestlers();
    wrestlerService.evictWrestlerCache();
  }

  /**
   * Loads wrestlers from {@code classpath*:wrestlers/*.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only wrestlers missing from the database are created and existing rows are left
   *     untouched.
   */
  private void loadWrestlers(boolean updateExisting) {
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers/*.json");
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
              if (!updateExisting) {
                // Top-up mode: never re-save an existing wrestler.
                continue;
              }
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
                existing.setActive(!Boolean.FALSE.equals(w.getActive()));
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
              newWrestler.setActive(!Boolean.FALSE.equals(w.getActive()));
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
              stampSeedFingerprint(newWrestler);
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

          List<WrestlerAbilityDTO> universalAbilities = loadUniversalAbilities();
          for (Wrestler wrestler : allWrestlers) {
            // In top-up mode allWrestlers holds only newly inserted wrestlers (existing ones
            // were skipped above), so state/ability seeding below only ever touches new rows.
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

            // Merge: universal first, wrestler-specific overrides by name
            Map<String, WrestlerAbilityDTO> merged = new LinkedHashMap<>();
            universalAbilities.forEach(a -> merged.put(a.getName(), a));
            w.getAbilities().forEach(a -> merged.put(a.getName(), a));
            List<WrestlerAbilityDTO> allAbilities = new ArrayList<>(merged.values());
            if (!allAbilities.isEmpty()) {
              syncAbilities(wrestler, allAbilities);
            }
          }
        } catch (IOException e) {
          log.error("Error loading wrestlers from file", e);
        }
      }
    } catch (IOException e) {
      log.error("Error resolving wrestler resources", e);
    }
  }

  private void syncAbilitiesForExistingWrestlers() {
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers/*.json");
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAll().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      List<WrestlerAbilityDTO> universalAbilities = loadUniversalAbilities();

      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<WrestlerImportDTO> wrestlersFromFile =
              objectMapper.readValue(is, new TypeReference<>() {});
          for (WrestlerImportDTO dto : wrestlersFromFile) {
            Wrestler wrestler = wrestlersByName.get(dto.getName());
            if (wrestler == null) {
              continue;
            }

            Map<String, WrestlerAbilityDTO> merged = new LinkedHashMap<>();
            universalAbilities.forEach(a -> merged.put(a.getName(), a));
            dto.getAbilities().forEach(a -> merged.put(a.getName(), a));
            if (!merged.isEmpty()) {
              syncAbilities(wrestler, new ArrayList<>(merged.values()));
            }
          }
        }
      }
      log.debug("Synchronized wrestler abilities for existing wrestlers");
    } catch (IOException e) {
      log.error("Error synchronizing wrestler abilities for existing wrestlers", e);
    }
  }

  /** Stamps the seed fingerprint onto a newly inserted wrestler from its own field values. */
  private void stampSeedFingerprint(Wrestler wrestler) {
    wrestler.setSeedFingerprint(wrestlerFingerprint(wrestler));
  }

  /**
   * Canonical seed-owned field values for a wrestler row, in fingerprint order — the composite
   * {@link WrestlerSeedFields#canonical(Wrestler)} set. Public for the seed-provenance manifest
   * generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String wrestlerFingerprint(Wrestler wrestler) {
    return syncSupport.fingerprint(WrestlerSeedFields.canonical(wrestler));
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing wrestlers. The stored fingerprint is the
   * hash of the row's seed-owned fields as the last seed write left them; re-hashing the row's
   * current fields detects hand edits. A row that still hashes to its stored fingerprint is clean
   * and adopts seed changes; a row whose fields drifted is user-edited — warned and preserved.
   * User-created wrestlers (no seed match) are never considered.
   */
  private void reconcileVerifiedWrestlers() {
    long start = System.currentTimeMillis();
    try {
      Resource[] resources = resourcePatternResolver.getResources("classpath*:wrestlers/*.json");
      Map<String, Wrestler> wrestlersByName =
          wrestlerRepository.findAllWithAlignments().stream()
              .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      List<Wrestler> toSave = new ArrayList<>();
      for (Resource resource : resources) {
        if (!resource.exists()) {
          continue;
        }
        try (var is = resource.getInputStream()) {
          List<WrestlerImportDTO> wrestlersFromFile =
              objectMapper.readValue(is, new TypeReference<>() {});
          for (WrestlerImportDTO w : wrestlersFromFile) {
            Wrestler wrestler = wrestlersByName.get(w.getName());
            if (wrestler == null) {
              continue; // user-created rows have no seed match and are never touched
            }
            String current = syncSupport.fingerprint(WrestlerSeedFields.canonical(wrestler));
            if (SeedSyncSupport.isBlankFingerprint(wrestler.getSeedFingerprint())) {
              // Legacy row: adopt only if its seed-owned fields already equal the seed.
              if (matchesSeed(wrestler, w)) {
                wrestler.setSeedFingerprint(current);
                toSave.add(wrestler);
                reconciled++;
              } else {
                log.warn(
                    "Seed verify: wrestler '{}' has hand-edited seed-owned fields "
                        + "(legacy row, no fingerprint); preserving user values.",
                    LogSanitizer.sanitize(wrestler.getName()));
                preserved++;
              }
            } else if (current.equals(wrestler.getSeedFingerprint())) {
              if (matchesSeed(wrestler, w)) {
                continue; // already in sync
              }
              applySeedFields(wrestler, w);
              wrestler.setSeedFingerprint(
                  syncSupport.fingerprint(WrestlerSeedFields.canonical(wrestler)));
              toSave.add(wrestler);
              reconciled++;
            } else {
              log.warn(
                  "Seed verify: wrestler '{}' was modified after seeding (fingerprint mismatch); "
                      + "preserving user values.",
                  LogSanitizer.sanitize(wrestler.getName()));
              preserved++;
            }
          }
        } catch (IOException e) {
          log.error("Error during wrestler verify reconcile", e);
        }
      }
      if (!toSave.isEmpty()) {
        wrestlerRepository.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("wrestler", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving wrestler resources for verify reconcile", e);
    }
  }

  private boolean matchesSeed(Wrestler wrestler, WrestlerImportDTO w) {
    // Null-guarded fields mirror applySeedFields: the seed write leaves an existing value
    // untouched when the seed omits the field, so the row may legitimately carry a value the
    // current seed no longer ships.
    return Objects.equals(wrestler.getDeckSize(), w.getDeckSize())
        && Objects.equals(wrestler.getStartingHealth(), w.getStartingHealth())
        && Objects.equals(wrestler.getLowHealth(), w.getLowHealth())
        && Objects.equals(wrestler.getStartingStamina(), w.getStartingStamina())
        && Objects.equals(wrestler.getLowStamina(), w.getLowStamina())
        && Objects.equals(wrestler.getDescription(), w.getDescription())
        && Objects.equals(wrestler.getGender(), w.getGender())
        && (w.getImageUrl() == null || Objects.equals(wrestler.getImageUrl(), w.getImageUrl()))
        && (w.getHeritageTag() == null
            || Objects.equals(wrestler.getHeritageTag(), w.getHeritageTag()))
        && (w.getExpansionCode() == null
            || Objects.equals(wrestler.getExpansionCode(), w.getExpansionCode()))
        && (w.getDrive() == null || Objects.equals(wrestler.getDrive(), w.getDrive()))
        && (w.getResilience() == null
            || Objects.equals(wrestler.getResilience(), w.getResilience()))
        && (w.getCharisma() == null || Objects.equals(wrestler.getCharisma(), w.getCharisma()))
        && (w.getBrawl() == null || Objects.equals(wrestler.getBrawl(), w.getBrawl()))
        && Objects.equals(
            wrestler.getAlignment() == null ? null : wrestler.getAlignment().getAlignmentType(),
            w.getAlignment() == null
                ? null
                : AlignmentType.valueOf(w.getAlignment().toUpperCase()));
  }

  private void applySeedFields(Wrestler wrestler, WrestlerImportDTO w) {
    wrestler.setDeckSize(w.getDeckSize());
    wrestler.setStartingHealth(w.getStartingHealth());
    wrestler.setLowHealth(w.getLowHealth());
    wrestler.setStartingStamina(w.getStartingStamina());
    wrestler.setLowStamina(w.getLowStamina());
    wrestler.setDescription(w.getDescription());
    wrestler.setGender(w.getGender());
    if (w.getImageUrl() != null) {
      wrestler.setImageUrl(w.getImageUrl());
    }
    if (w.getHeritageTag() != null) {
      wrestler.setHeritageTag(w.getHeritageTag());
    }
    wrestler.setExpansionCode(w.getExpansionCode());
    if (w.getDrive() != null) {
      wrestler.setDrive(w.getDrive());
    }
    if (w.getResilience() != null) {
      wrestler.setResilience(w.getResilience());
    }
    if (w.getCharisma() != null) {
      wrestler.setCharisma(w.getCharisma());
    }
    if (w.getBrawl() != null) {
      wrestler.setBrawl(w.getBrawl());
    }
    if (w.getAlignment() != null) {
      try {
        AlignmentType seedType = AlignmentType.valueOf(w.getAlignment().toUpperCase());
        WrestlerAlignment current = wrestler.getAlignment();
        if (current != null) {
          // Replace in place — setAlignment appends, which would leave the stale alignment
          // first in the set and the derived getter never reflecting the seed change.
          current.setAlignmentType(seedType);
        } else {
          wrestler.setAlignment(
              WrestlerAlignment.builder()
                  .wrestler(wrestler)
                  .universe(universeRepository.findAll().stream().findFirst().orElse(null))
                  .alignmentType(seedType)
                  .level(0)
                  .build());
        }
      } catch (IllegalArgumentException e) {
        log.warn(
            "Invalid alignment '{}' for wrestler '{}'",
            LogSanitizer.sanitize(w.getAlignment()),
            LogSanitizer.sanitize(wrestler.getName()));
      }
    }
  }

  private List<WrestlerAbilityDTO> loadUniversalAbilities() {
    try {
      Resource[] resources =
          resourcePatternResolver.getResources("classpath*:wrestler_abilities_universal.json");
      for (Resource r : resources) {
        if (r.exists()) {
          return objectMapper.readValue(r.getInputStream(), new TypeReference<>() {});
        }
      }
    } catch (IOException e) {
      log.warn("Could not load universal wrestler abilities", e);
    }
    return List.of();
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
