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
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.TitleDTO;
import com.github.javydreamercsw.management.service.title.TitleReignRepairService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
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
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "championship";

  @Autowired
  public ChampionshipSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final TitleService titleService,
      final WrestlerRepository wrestlerRepository,
      final UniverseRepository universeRepository,
      final TitleReignRepairService titleReignRepairService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.titleService = titleService;
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
    this.titleReignRepairService = titleReignRepairService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
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

  /**
   * Version-change detection pass (seed-update review feature): classifies existing titles
   * field-by-field through {@link SeedCollisionDetector} instead of reconciling directly — tier-1
   * fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields
   * stay untouched. Only meaningful in PROTECT/VERIFY mode with existing rows.
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("championships.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<TitleDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      List<Title> toSave = new ArrayList<>();
      for (TitleDTO dto : dtos) {
        Title title = titleService.findByName(dto.getName()).orElse(null);
        if (title == null) {
          continue; // user-created rows have no seed match; missing rows are top-up's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(title.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(title, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getName()),
                dto.getName(),
                rowHasStoredStamp,
                title.getSeedFingerprint(),
                observations,
                this::titleValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(title, dto, result.silentUpdateFields());
          toSave.add(title);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(title.getSeedFingerprint())) {
          title.setSeedFingerprint(stamp);
          if (!toSave.contains(title)) {
            toSave.add(title);
          }
        }
      }
      if (!toSave.isEmpty()) {
        titleService.saveAll(toSave);
      }
      log.info(
          "Seed review detection (championship) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during championship seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one title row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      Title title, TitleDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, title.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "tier",
            null,
            title.getTier() == null ? null : title.getTier().name(),
            dto.getTier() == null ? null : dto.getTier().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "championshipType",
            null,
            title.getChampionshipType() == null ? null : title.getChampionshipType().name(),
            dto.getChampionshipType() == null ? null : dto.getChampionshipType().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "effectScript", null, title.getEffectScript(), dto.getEffectScript()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "gender",
            null,
            title.getGender() == null ? null : title.getGender().name(),
            dto.getGender() == null ? null : dto.getGender().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "imageUrl", null, title.getImageUrl(), dto.getImageUrl()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expansionCode",
            null,
            title.getExpansionCode(),
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME"));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "includeInRankings",
            null,
            String.valueOf(title.getIncludeInRankings()),
            String.valueOf(
                dto.getIncludeInRankings() != null ? dto.getIncludeInRankings() : Boolean.TRUE)));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(TitleDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getTier() == null ? null : dto.getTier().name(),
        dto.getChampionshipType() == null ? null : dto.getChampionshipType().name(),
        dto.getEffectScript(),
        dto.getGender() == null ? null : dto.getGender().name(),
        dto.getImageUrl(),
        dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
        dto.getIncludeInRankings() != null ? dto.getIncludeInRankings() : Boolean.TRUE);
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #titleFingerprint(Title)} exactly.
   */
  private String titleValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0),
        values.get(1) == null ? null : WrestlerTier.valueOf(values.get(1)),
        values.get(2) == null ? null : ChampionshipType.valueOf(values.get(2)),
        values.get(3),
        values.get(4) == null ? null : Gender.valueOf(values.get(4)),
        values.get(5),
        values.get(6),
        values.get(7) == null ? null : Boolean.valueOf(values.get(7)));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a championship name — shared with {@code ChampionshipSeedApplier}
   * so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "championship:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@link #applySeedFields(Title, TitleDTO)}.
   */
  private void applySilentSeedFields(Title title, TitleDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> title.setDescription(dto.getDescription());
        case "tier" -> {
          if (dto.getTier() != null) {
            title.setTier(dto.getTier());
          }
        }
        case "championshipType" -> title.setChampionshipType(dto.getChampionshipType());
        case "effectScript" -> title.setEffectScript(dto.getEffectScript());
        case "gender" -> title.setGender(dto.getGender());
        case "imageUrl" -> title.setImageUrl(dto.getImageUrl());
        case "expansionCode" ->
            title.setExpansionCode(
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        case "includeInRankings" -> {
          if (dto.getIncludeInRankings() != null) {
            title.setIncludeInRankings(dto.getIncludeInRankings());
          }
        }
        default ->
            throw new IllegalStateException(
                "Unknown silent-update championship field '" + field + "'");
      }
    }
  }

  /**
   * Canonical seed-owned field values for a title row, in fingerprint order. Public for the
   * seed-provenance manifest generator, which re-hashes fresh seed rows through the owning sync's
   * own method.
   */
  public String titleFingerprint(Title title) {
    return syncSupport.fingerprint(
        title.getDescription(),
        title.getTier() == null ? null : title.getTier().name(),
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
    return Objects.equals(title.getDescription(), dto.getDescription())
        && Objects.equals(title.getTier(), dto.getTier())
        && Objects.equals(
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
    title.setDescription(dto.getDescription());
    if (dto.getTier() != null) {
      title.setTier(dto.getTier());
    }
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
