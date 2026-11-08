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
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.dto.OutcomeMatrixEntryImportDTO;
import com.github.javydreamercsw.management.dto.OutcomeMatrixImportDTO;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "outcome_matrix";

  private final SeedSyncSupport syncSupport;
  private final OutcomeMatrixService outcomeMatrixService;
  private final OutcomeMatrixRepository matrixRepository;
  private final OutcomeMatrixEntryRepository entryRepository;
  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public OutcomeMatrixSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final OutcomeMatrixService outcomeMatrixService,
      final OutcomeMatrixRepository matrixRepository,
      final OutcomeMatrixEntryRepository entryRepository,
      final ResourcePatternResolver resourcePatternResolver,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.outcomeMatrixService = outcomeMatrixService;
    this.matrixRepository = matrixRepository;
    this.entryRepository = entryRepository;
    this.resourcePatternResolver = resourcePatternResolver;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && outcomeMatrixService.count() > 0) {
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
              // Stamp the freshly seeded row so verify-mode-style checks and the version-change
              // detection pass can match it against the manifest. Both row families (the matrix
              // header and its entries) carry their own stamps; only a blank stamp is written —
              // an already-stamped row keeps its stamp.
              if (SeedSyncSupport.isBlankFingerprint(matrix.getSeedFingerprint())) {
                matrix.setSeedFingerprint(matrixFingerprint(matrix));
                outcomeMatrixService.updateMatrix(matrix);
              }
              if (SeedSyncSupport.isBlankFingerprint(entry.getSeedFingerprint())) {
                entry.setSeedFingerprint(entryFingerprint(entry));
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
                                // The redirect target's name is part of the entry fingerprint —
                                // re-stamp when the second pass just wired a blank-stamped entry.
                                if (SeedSyncSupport.isBlankFingerprint(
                                    entry.getSeedFingerprint())) {
                                  entry.setSeedFingerprint(entryFingerprint(entry));
                                  outcomeMatrixService.updateEntry(entry);
                                }
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

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing outcome matrices and entries are classified
   * field-by-field through {@link SeedCollisionDetector} instead of being reconciled directly —
   * tier-1 fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3
   * fields stay untouched. Runs only in PROTECT/VERIFY mode with existing rows.
   *
   * <p>The domain carries two row families: the matrix header (name = lookup key; description and
   * category reviewable) keyed {@code matrix:NAME-slug}, and one entry per dice roll keyed {@code
   * entry:NAME-slug#roll} (templateText and the four mechanical deltas + injuryCaused reviewable;
   * the redirect target's name participates in the fingerprint as the target matrix's exact seed
   * name). Display names are the seed names at detection time: {@code NAME} for matrix rows, the
   * human-readable {@code NAME (roll N)} for entry rows.
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   */
  void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || outcomeMatrixService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    try {
      Resource[] resources =
          resourcePatternResolver.getResources("classpath*:outcome_matrices/*.json");
      List<OutcomeMatrixEntry> entriesToSave = new ArrayList<>();
      for (Resource res : resources) {
        if (!res.exists()) {
          continue;
        }
        try (var is = res.getInputStream()) {
          OutcomeMatrixImportDTO dto =
              objectMapper.readValue(is, new TypeReference<OutcomeMatrixImportDTO>() {});
          Optional<OutcomeMatrix> existing = outcomeMatrixService.getByName(dto.getName());
          if (existing.isEmpty()) {
            continue; // user-created rows have no seed match; missing rows are the FULL load's job
          }
          OutcomeMatrix matrix = existing.get();
          detectMatrixRow(matrix, dto);
          if (dto.getEntries() != null) {
            Map<Integer, OutcomeMatrixEntry> existingByRoll =
                entryRepository.findAllWithMatrixAndRedirect().stream()
                    .filter(e -> e.getMatrix().getId().equals(matrix.getId()))
                    .collect(Collectors.toMap(OutcomeMatrixEntry::getDiceRoll, e -> e));
            for (OutcomeMatrixEntryImportDTO entryDto : dto.getEntries()) {
              OutcomeMatrixEntry entry = existingByRoll.get(entryDto.getDiceRoll());
              if (entry == null) {
                continue; // missing rows are the FULL load's job; the orchestrator owns removals
              }
              detectEntryRow(entry, matrix, dto.getName(), entryDto, entriesToSave);
            }
          }
        } catch (IOException e) {
          log.error(
              "Error during outcome matrix seed-review detection for {}", res.getFilename(), e);
        }
      }
      if (!entriesToSave.isEmpty()) {
        entryRepository.saveAll(entriesToSave);
      }
      log.info(
          "Seed review detection (outcome matrix) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error resolving outcome_matrices resources during seed-review detection", e);
    }
  }

  /** Runs one matrix header row through the shared per-row detection driver. */
  private void detectMatrixRow(OutcomeMatrix matrix, OutcomeMatrixImportDTO dto) {
    String newFingerprint =
        SeedReviewDetectionSupport.incomingFingerprint(
            provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
    boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(matrix.getSeedFingerprint());
    List<SeedCollisionDetector.FieldObservation> observations = matrixObservations(matrix, dto);
    SeedReviewDetectionSupport.DetectionResult result =
        SeedReviewDetectionSupport.detectRow(
            DOMAIN_KEY,
            matrixEntityKey(dto.getName()),
            dto.getName(),
            rowHasStoredStamp,
            matrix.getSeedFingerprint(),
            observations,
            this::matrixValueListFingerprint,
            newFingerprint,
            seedReviewService);
    String stamp =
        SeedReviewDetectionSupport.stampForResolvedRow(result.provenanceExact(), newFingerprint);
    if (!result.silentUpdateFields().isEmpty()) {
      // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on the
      // same row must never be touched (per-field granularity, spec "Tier 2 granularity").
      applySilentSeedFields(matrix, dto, result.silentUpdateFields());
      outcomeMatrixService.updateMatrix(matrix);
    }
    // Stamp-only rows must persist the re-stamp too — capture the change BEFORE setting it.
    if (stamp != null && !stamp.equals(matrix.getSeedFingerprint())) {
      matrix.setSeedFingerprint(stamp);
      outcomeMatrixService.updateMatrix(matrix);
    }
  }

  /** Runs one entry row through the shared per-row detection driver. */
  private void detectEntryRow(
      OutcomeMatrixEntry entry,
      OutcomeMatrix matrix,
      String matrixName,
      OutcomeMatrixEntryImportDTO entryDto,
      List<OutcomeMatrixEntry> entriesToSave) {
    String seedKey = entrySeedKey(matrixName, entryDto.getDiceRoll());
    String newFingerprint =
        SeedReviewDetectionSupport.incomingFingerprint(
            provenanceManifest, DOMAIN_KEY, seedKey, entrySeedValueFingerprint(entryDto));
    boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(entry.getSeedFingerprint());
    List<SeedCollisionDetector.FieldObservation> observations = entryObservations(entry, entryDto);
    SeedReviewDetectionSupport.DetectionResult result =
        SeedReviewDetectionSupport.detectRow(
            DOMAIN_KEY,
            entryEntityKey(matrixName, entryDto.getDiceRoll()),
            entryDisplayName(matrixName, entryDto.getDiceRoll()),
            rowHasStoredStamp,
            entry.getSeedFingerprint(),
            observations,
            this::entryValueListFingerprint,
            newFingerprint,
            seedReviewService);
    String stamp =
        SeedReviewDetectionSupport.stampForResolvedRow(result.provenanceExact(), newFingerprint);
    if (!result.silentUpdateFields().isEmpty()) {
      // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on the
      // same row must never be touched (per-field granularity, spec "Tier 2 granularity").
      applySilentSeedFields(entry, entryDto, result.silentUpdateFields());
      entriesToSave.add(entry);
    }
    // Stamp-only rows must persist the re-stamp too — capture the change BEFORE setting it.
    if (stamp != null && !stamp.equals(entry.getSeedFingerprint())) {
      entry.setSeedFingerprint(stamp);
      if (!entriesToSave.contains(entry)) {
        entriesToSave.add(entry);
      }
    }
  }

  /** The detection-time per-field observations for one matrix header row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> matrixObservations(
      OutcomeMatrix existing, OutcomeMatrixImportDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "category", null, existing.getCategory().name(), dto.getCategory()));
    return obs;
  }

  /** The detection-time per-field observations for one entry row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> entryObservations(
      OutcomeMatrixEntry existing, OutcomeMatrixEntryImportDTO entryDto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "templateText", null, existing.getTemplateText(), entryDto.getTemplateText()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "heatDelta", null, boxed(existing.getHeatDelta()), boxed(entryDto.getHeatDelta())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "fanDelta", null, boxed(existing.getFanDelta()), boxed(entryDto.getFanDelta())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "tvGradeDelta",
            null,
            boxed(existing.getTvGradeDelta()),
            boxed(entryDto.getTvGradeDelta())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "grudgeGradeDelta",
            null,
            boxed(existing.getGrudgeGradeDelta()),
            boxed(entryDto.getGrudgeGradeDelta())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "injuryCaused",
            null,
            Boolean.toString(existing.isInjuryCaused()),
            Boolean.toString(entryDto.isInjuryCaused())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "redirectToMatrix", null, redirectName(existing), entryDto.getRedirectToMatrix()));
    return obs;
  }

  /** Fingerprint the incoming matrix header values would produce, in the canonical field order. */
  private String seedValueFingerprint(OutcomeMatrixImportDTO dto) {
    return syncSupport.fingerprint(dto.getDescription(), dto.getCategory());
  }

  /** Fingerprint the incoming entry values would produce, in the canonical field order. */
  private String entrySeedValueFingerprint(OutcomeMatrixEntryImportDTO entryDto) {
    return syncSupport.fingerprint(
        entryDto.getTemplateText(),
        boxed(entryDto.getHeatDelta()),
        boxed(entryDto.getFanDelta()),
        boxed(entryDto.getTvGradeDelta()),
        boxed(entryDto.getGrudgeGradeDelta()),
        Boolean.toString(entryDto.isInjuryCaused()),
        entryDto.getRedirectToMatrix());
  }

  /**
   * Re-hashes a field-value list (the {@link #matrixObservations} order) back into a row
   * fingerprint — the hash-function half of the clean-row/single-edit provenance reconstruction.
   * Serialization mirrors {@link #matrixFingerprint(OutcomeMatrix)} exactly.
   */
  private String matrixValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1));
  }

  /**
   * Re-hashes a field-value list (the {@link #entryObservations} order) back into a row
   * fingerprint. Serialization mirrors {@link #entryFingerprint(OutcomeMatrixEntry)} exactly.
   */
  private String entryValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0),
        values.get(1),
        values.get(2),
        values.get(3),
        values.get(4),
        values.get(5),
        values.get(6));
  }

  private static String boxed(Integer value) {
    return value == null ? null : Integer.toString(value);
  }

  private static String boxed(Long value) {
    return value == null ? null : Long.toString(value);
  }

  /** The redirect target's name for fingerprinting; null when the entry is not a redirect. */
  private static String redirectName(OutcomeMatrixEntry entry) {
    if (entry.getRedirectToMatrix() == null) {
      return null;
    }
    // LAZY proxy: reading the name through the proxy works inside the sync's session; the
    // manifest generator passes eager-fetched rows.
    return entry.getRedirectToMatrix().getName();
  }

  private static String matrixEntityKey(String name) {
    return matrixEntityKeyFor(name);
  }

  private static String entryEntityKey(String matrixName, int diceRoll) {
    return entryEntityKeyFor(matrixName, diceRoll);
  }

  /** Human-readable display name for an entry row: the matrix's seed name plus its roll. */
  private static String entryDisplayName(String matrixName, int diceRoll) {
    return matrixName + " (roll " + diceRoll + ")";
  }

  /** The manifest seed key for an entry row: the matrix's seed name plus its roll. */
  private static String entrySeedKey(String matrixName, int diceRoll) {
    return entrySeedKeyFor(matrixName, diceRoll);
  }

  /**
   * The manifest seed key for an entry row — the matrix's exact seed name plus its roll. Public
   * shared with the seed-provenance manifest generator, which keys entries identically.
   */
  public static String entrySeedKeyFor(String matrixName, int diceRoll) {
    return matrixName + " (roll " + diceRoll + ")";
  }

  /**
   * The canonical entity key for a matrix name — shared with {@code OutcomeMatrixSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String matrixEntityKeyFor(String name) {
    return "matrix:" + name.toLowerCase().replace(' ', '-');
  }

  /** The canonical entity key for an entry row: the owning matrix's slug plus the dice roll. */
  static String entryEntityKeyFor(String matrixName, int diceRoll) {
    return "entry:" + matrixEntityKeyFor(matrixName).substring("matrix:".length()) + "#" + diceRoll;
  }

  /**
   * Canonical seed-owned field values for a matrix header row, in fingerprint order: description,
   * category. The name is the lookup key. Public for the seed-provenance manifest generator, which
   * re-hashes fresh seed rows through the owning sync's own method.
   */
  public String matrixFingerprint(OutcomeMatrix matrix) {
    return syncSupport.fingerprint(matrix.getDescription(), matrix.getCategory().name());
  }

  /**
   * Canonical seed-owned field values for an entry row, in fingerprint order: templateText, the
   * four deltas, injuryCaused, and the redirect target's name (null when not a redirect). Public
   * for the seed-provenance manifest generator, which re-hashes fresh seed rows through the owning
   * sync's own method.
   */
  public String entryFingerprint(OutcomeMatrixEntry entry) {
    return syncSupport.fingerprint(
        entry.getTemplateText(),
        boxed(entry.getHeatDelta()),
        boxed(entry.getFanDelta()),
        boxed(entry.getTvGradeDelta()),
        boxed(entry.getGrudgeGradeDelta()),
        Boolean.toString(entry.isInjuryCaused()),
        entry.getRedirectToMatrix() == null ? null : entry.getRedirectToMatrix().getName());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #matrixObservations} entries.
   */
  private void applySilentSeedFields(
      OutcomeMatrix existing, OutcomeMatrixImportDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "category" -> existing.setCategory(OutcomeMatrixCategory.valueOf(dto.getCategory()));
        default ->
            throw new IllegalStateException(
                "Unknown silent-update outcome matrix field '" + field + "'");
      }
    }
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the entry row — the tier-1
   * write set of the version-change pass. Field names match {@link #entryObservations} entries; the
   * redirect write resolves by seed name at apply time, mirroring the FULL path's second pass.
   */
  private void applySilentSeedFields(
      OutcomeMatrixEntry existing,
      OutcomeMatrixEntryImportDTO entryDto,
      List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "templateText" -> existing.setTemplateText(entryDto.getTemplateText());
        case "heatDelta" -> existing.setHeatDelta(entryDto.getHeatDelta());
        case "fanDelta" -> existing.setFanDelta(entryDto.getFanDelta());
        case "tvGradeDelta" -> existing.setTvGradeDelta(entryDto.getTvGradeDelta());
        case "grudgeGradeDelta" -> existing.setGrudgeGradeDelta(entryDto.getGrudgeGradeDelta());
        case "injuryCaused" -> existing.setInjuryCaused(entryDto.isInjuryCaused());
        case "redirectToMatrix" -> {
          if (entryDto.getRedirectToMatrix() == null) {
            existing.setRedirectToMatrix(null);
          } else {
            outcomeMatrixService
                .getByName(entryDto.getRedirectToMatrix())
                .ifPresentOrElse(
                    existing::setRedirectToMatrix,
                    () ->
                        log.warn(
                            "Redirect matrix '{}' not found — redirect not changed on entry"
                                + " roll {}",
                            entryDto.getRedirectToMatrix(),
                            entryDto.getDiceRoll()));
          }
        }
        default ->
            throw new IllegalStateException(
                "Unknown silent-update outcome matrix entry field '" + field + "'");
      }
    }
  }
}
