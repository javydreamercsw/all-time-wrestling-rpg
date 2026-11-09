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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(50)
public class SegmentTypeSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "segment_type";

  private final SeedSyncSupport syncSupport;
  private final SegmentTypeService segmentTypeService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public SegmentTypeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final SegmentTypeService segmentTypeService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.segmentTypeService = segmentTypeService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && segmentTypeService.count() > 0) {
      topUpMissingSegmentTypes();
      if (syncSupport.isVerify()) {
        reconcileVerifiedSegmentTypes();
      }
      return;
    }
    loadSegmentTypes(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still create seed entries that don't exist yet, so
   * catalogs added in later releases reach flag-protected installs (e.g. the 2.12.0 Trios segment
   * type). Existing rows are never passed to the service.
   */
  private void topUpMissingSegmentTypes() {
    List<String> existingNames =
        segmentTypeService.findAll().stream().map(SegmentType::getName).toList();
    List<SegmentTypeDTO> dtos = parseSeedFile();
    for (SegmentTypeDTO dto : dtos) {
      if (!existingNames.contains(dto.getName())) {
        log.info("Segment type top-up: inserting missing seed entry '{}'.", dto.getName());
        SegmentType created =
            segmentTypeService.createOrUpdateSegmentType(
                dto.getName(),
                dto.getDescription(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
                dto.getGuide(),
                dto.getCode(),
                dto.getEventOnly());
        if (created != null) {
          created.setSeedFingerprint(segmentTypeFingerprint(created));
          segmentTypeService.saveSegmentType(created);
        }
      }
    }
  }

  private void loadSegmentTypes(boolean updateExisting) {
    List<SegmentTypeDTO> dtos = parseSeedFile();
    for (SegmentTypeDTO dto : dtos) {
      SegmentType segmentType =
          segmentTypeService.createOrUpdateSegmentType(
              dto.getName(),
              dto.getDescription(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
              dto.getGuide(),
              dto.getCode(),
              dto.getEventOnly());
      if (segmentType != null
          && SeedSyncSupport.isBlankFingerprint(segmentType.getSeedFingerprint())) {
        // The service save leaves the stamp unset (same as the top-up branch): stamp the freshly
        // seeded row so verify mode's stored-side check can match it against the manifest.
        segmentType.setSeedFingerprint(segmentTypeFingerprint(segmentType));
        segmentTypeService.saveSegmentType(segmentType);
      }
      log.debug(
          "Loaded segment type: {} (Players: {})",
          segmentType.getName(),
          dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
    }
    log.debug("Segment type loading completed");
  }

  private List<SegmentTypeDTO> parseSeedFile() {
    ClassPathResource resource = new ClassPathResource("segment_types.json");
    if (!resource.exists()) {
      log.warn("Segment types file not found: {}", resource.getPath());
      return List.of();
    }
    log.debug("Loading segment types from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      return objectMapper.readValue(is, new TypeReference<>() {});
    } catch (IOException e) {
      log.error("Error loading segment types from file", e);
      return List.of();
    }
  }

  /**
   * Canonical seed-owned field values for a segment type row, in fingerprint order: description,
   * expansionCode, guideHash, code, eventOnly. Mirrors the seed write set of {@code
   * createOrUpdateSegmentType}; {@code active} is admin-managed and excluded.
   */
  public String segmentTypeFingerprint(SegmentType st) {
    return syncSupport.fingerprint(
        st.getDescription(),
        st.getExpansionCode(),
        st.getGuideHash(),
        st.getCode(),
        st.isEventOnly());
  }

  /** Guide hash the seed write would compute for a DTO (mirrors the service's private method). */
  private String seedGuideHash(SegmentTypeDTO dto) {
    if (dto.getGuide() == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(dto.getGuide());
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("Failed to compute seed guide hash for segment type", e);
      return null;
    }
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing segment types. The stored fingerprint is
   * the hash of the row's seed-owned fields as the last seed write left them; re-hashing the row's
   * current fields detects hand edits. A row that still hashes to its stamp is clean and adopts
   * seed changes; a row whose fields drifted is user-edited — warned and preserved. Segment types
   * with no seed entry are never considered.
   */
  private void reconcileVerifiedSegmentTypes() {
    long start = System.currentTimeMillis();
    List<SegmentTypeDTO> dtos = parseSeedFile();
    if (dtos.isEmpty()) {
      return;
    }
    Map<String, SegmentType> existingByName =
        segmentTypeService.findAll().stream()
            .collect(Collectors.toMap(SegmentType::getName, st -> st, (a, b) -> a));
    int reconciled = 0;
    int preserved = 0;
    List<SegmentType> toSave = new ArrayList<>();
    for (SegmentTypeDTO dto : dtos) {
      SegmentType st = existingByName.get(dto.getName());
      if (st == null) {
        continue; // user-created rows have no seed match and are never touched
      }
      String current = segmentTypeFingerprint(st);
      String seedHash = seedGuideHash(dto);
      if (SeedSyncSupport.isBlankFingerprint(st.getSeedFingerprint())) {
        // Legacy row: adopt only if its seed-owned fields already equal the seed.
        if (Objects.equals(st.getDescription(), dto.getDescription())
            && Objects.equals(
                st.getExpansionCode(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME")
            && Objects.equals(st.getGuideHash(), seedHash)
            && Objects.equals(st.getCode(), dto.getCode())
            && st.isEventOnly() == Boolean.TRUE.equals(dto.getEventOnly())) {
          st.setSeedFingerprint(current);
          toSave.add(st);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: segment type '{}' has hand-edited seed-owned fields (legacy row, "
                  + "no fingerprint); preserving user values.",
              st.getName());
          preserved++;
        }
      } else if (current.equals(st.getSeedFingerprint())) {
        if (Objects.equals(st.getDescription(), dto.getDescription())
            && Objects.equals(
                st.getExpansionCode(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME")
            && Objects.equals(st.getGuideHash(), seedHash)
            && Objects.equals(st.getCode(), dto.getCode())
            && st.isEventOnly() == Boolean.TRUE.equals(dto.getEventOnly())) {
          continue; // already in sync
        }
        // Clean row: refresh through the same service path the seed write uses, then persist
        // the stamp — the service save leaves it unset and the returned row is detached.
        SegmentType refreshed =
            segmentTypeService.createOrUpdateSegmentType(
                dto.getName(),
                dto.getDescription(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
                dto.getGuide(),
                dto.getCode(),
                dto.getEventOnly());
        refreshed.setSeedFingerprint(segmentTypeFingerprint(refreshed));
        segmentTypeService.saveSegmentType(refreshed);
        reconciled++;
      } else {
        log.warn(
            "Seed verify: segment type '{}' was modified after seeding (fingerprint mismatch); "
                + "preserving user values.",
            st.getName());
        preserved++;
      }
    }
    if (!toSave.isEmpty()) {
      toSave.forEach(segmentTypeService::createSegmentType);
    }
    log.info(
        "{} ({} ms)",
        syncSupport.reconcileLog("segment type", reconciled, preserved),
        System.currentTimeMillis() - start);
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing segment types are classified field-by-field
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
    if (syncSupport.mode() == SeedSyncMode.FULL || segmentTypeService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    List<SegmentTypeDTO> dtos = parseSeedFile();
    if (dtos.isEmpty()) {
      return;
    }
    Map<String, SegmentType> existingByName =
        segmentTypeService.findAllForAdmin().stream()
            .collect(Collectors.toMap(SegmentType::getName, st -> st, (a, b) -> a));
    List<SegmentType> toSave = new ArrayList<>();
    for (SegmentTypeDTO dto : dtos) {
      SegmentType st = existingByName.get(dto.getName());
      if (st == null) {
        continue; // user-created rows have no seed match; missing rows are top-up's job
      }
      String newFingerprint =
          SeedReviewDetectionSupport.incomingFingerprint(
              provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
      boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(st.getSeedFingerprint());
      List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(st, dto);
      SeedReviewDetectionSupport.DetectionResult result =
          SeedReviewDetectionSupport.detectRow(
              DOMAIN_KEY,
              entityKey(dto.getName()),
              dto.getName(),
              rowHasStoredStamp,
              st.getSeedFingerprint(),
              observations,
              this::segmentTypeValueListFingerprint,
              newFingerprint,
              seedReviewService);
      String stamp =
          SeedReviewDetectionSupport.stampForResolvedRow(result.provenanceExact(), newFingerprint);
      if (!result.silentUpdateFields().isEmpty()) {
        // Tier-1 write set: ONLY the silent-update fields, written in place on the row — the
        // service's createOrUpdateSegmentType would apply the full seed write-set and clobber a
        // tier-3-preserved admin edit on the same row.
        applySilentSeedFields(st, dto, result.silentUpdateFields());
        toSave.add(st);
      }
      // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
      // comparing after the set reads the NEW value and the save is silently skipped forever
      // (the row never converges).
      if (stamp != null && !stamp.equals(st.getSeedFingerprint())) {
        st.setSeedFingerprint(stamp);
        if (!toSave.contains(st)) {
          toSave.add(st);
        }
      }
    }
    if (!toSave.isEmpty()) {
      for (SegmentType st : toSave) {
        segmentTypeService.saveSegmentType(st);
      }
    }
    log.info(
        "Seed review detection (segment type) complete ({} ms).",
        System.currentTimeMillis() - start);
  }

  /** The detection-time per-field observations for one segment type row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      SegmentType st, SegmentTypeDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, st.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expansionCode",
            null,
            st.getExpansionCode(),
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME"));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "guideHash", null, st.getGuideHash(), seedGuideHash(dto)));
    obs.add(new SeedCollisionDetector.FieldObservation("code", null, st.getCode(), dto.getCode()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "eventOnly",
            null,
            String.valueOf(st.isEventOnly()),
            String.valueOf(Boolean.TRUE.equals(dto.getEventOnly()))));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(SegmentTypeDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
        seedGuideHash(dto),
        dto.getCode(),
        Boolean.TRUE.equals(dto.getEventOnly()));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #segmentTypeFingerprint(SegmentType)} exactly.
   */
  private String segmentTypeValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so the observed string
    // forms hash identically to the row fingerprint's typed values (see WrestlerSync's shape).
    return syncSupport.fingerprint(values.toArray());
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a segment type name — shared with {@code SegmentTypeSeedApplier}
   * so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "segment_type:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@code createOrUpdateSegmentType}'s write set (the guide hash is
   * recomputed from the DTO and written with the guide, mirroring the service's private
   * computeGuideHash).
   */
  private void applySilentSeedFields(
      SegmentType st, SegmentTypeDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> st.setDescription(dto.getDescription());
        case "expansionCode" ->
            st.setExpansionCode(
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        case "guideHash" -> {
          st.setGuide(dto.getGuide());
          st.setGuideHash(seedGuideHash(dto));
        }
        case "code" -> st.setCode(dto.getCode());
        case "eventOnly" -> st.setEventOnly(Boolean.TRUE.equals(dto.getEventOnly()));
        default ->
            throw new IllegalStateException(
                "Unknown silent-update segment type field '" + field + "'");
      }
    }
  }
}
