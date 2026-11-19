/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(40)
public class ShowTypeSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "show_type";

  private final SeedSyncSupport syncSupport;
  private final ShowTypeService showTypeService;
  private final ShowTypeRepository showTypeRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public ShowTypeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final ShowTypeService showTypeService,
      final ShowTypeRepository showTypeRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.showTypeService = showTypeService;
    this.showTypeRepository = showTypeRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && showTypeService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("show_types.json");
    if (resource.exists()) {
      log.debug("Loading show types from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ShowType> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (ShowType st : dtos) {
          ShowType saved =
              showTypeService.createOrUpdateShowType(
                  st.getName(),
                  st.getDescription(),
                  st.getExpectedMatches(),
                  st.getExpectedPromos(),
                  st.getCategory());
          if (SeedSyncSupport.isBlankFingerprint(saved.getSeedFingerprint())) {
            // Stamp the freshly seeded row so verify mode's stored-side check and the
            // version-change detection pass can match it against the manifest. The service's
            // save leaves the stamp unset; a same-content second boot re-derives the identical
            // hash, so re-stamping is a harmless no-op.
            saved.setSeedFingerprint(showTypeFingerprint(saved));
            showTypeService.save(saved);
          }
        }
        log.debug("Show type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading show types from file", e);
      }
    } else {
      log.warn("Show types file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing show types are classified field-by-field
   * through {@link SeedCollisionDetector} instead of being reconciled directly — tier-1 fields
   * update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3 fields stay
   * untouched. Runs only in PROTECT/VERIFY mode with existing rows (FULL mode's load already
   * applies the seed everywhere; an empty DB has nothing to collide).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || showTypeService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("show_types.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<ShowType> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, ShowType> existingByName =
          showTypeRepository.findAll().stream()
              .collect(Collectors.toMap(ShowType::getName, st -> st, (a, b) -> a));
      List<ShowType> toSave = new ArrayList<>();
      for (ShowType dto : dtos) {
        ShowType existing = existingByName.get(dto.getName());
        if (existing == null) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(existing, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getName()),
                dto.getName(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::showTypeValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(existing, dto, result.silentUpdateFields());
          toSave.add(existing);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(existing.getSeedFingerprint())) {
          existing.setSeedFingerprint(stamp);
          if (!toSave.contains(existing)) {
            toSave.add(existing);
          }
        }
      }
      if (!toSave.isEmpty()) {
        for (ShowType st : toSave) {
          showTypeService.save(st);
        }
      }
      log.info(
          "Seed review detection (show type) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during show type seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one show type row against its seed row. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      ShowType existing, ShowType dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expectedMatches",
            null,
            Integer.toString(existing.getExpectedMatches()),
            Integer.toString(dto.getExpectedMatches())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expectedPromos",
            null,
            Integer.toString(existing.getExpectedPromos()),
            Integer.toString(dto.getExpectedPromos())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "category", null, existing.getCategory().name(), dto.getCategory().name()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(ShowType dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getExpectedMatches(),
        dto.getExpectedPromos(),
        dto.getCategory().name());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #showTypeFingerprint(ShowType)} exactly.
   */
  private String showTypeValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so a string "5" hashes
    // identically to the integer 5 the row fingerprint used (null serializes as "null" both
    // ways) — this mirrors WrestlerSync's row-hash shape.
    return syncSupport.fingerprint(values.toArray());
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a show type name — shared with {@code ShowTypeSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "show_type:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a show type row, in fingerprint order: description,
   * expectedMatches, expectedPromos, category. Mirrors the seed write set of {@code
   * createOrUpdateShowType} minus the name key; {@code active} is admin-managed and excluded.
   * Public for the seed-provenance manifest generator, which re-hashes fresh seed rows through the
   * owning sync's own method.
   */
  public String showTypeFingerprint(ShowType st) {
    return syncSupport.fingerprint(
        st.getDescription(),
        st.getExpectedMatches(),
        st.getExpectedPromos(),
        st.getCategory() == null ? null : st.getCategory().name());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed row onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror the {@code createOrUpdateShowType} write set.
   */
  private void applySilentSeedFields(ShowType existing, ShowType dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "expectedMatches" -> existing.setExpectedMatches(dto.getExpectedMatches());
        case "expectedPromos" -> existing.setExpectedPromos(dto.getExpectedPromos());
        case "category" -> existing.setCategory(dto.getCategory());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update show type field '" + field + "'");
      }
    }
  }
}
