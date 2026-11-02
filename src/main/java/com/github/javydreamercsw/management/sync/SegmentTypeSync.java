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
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.io.IOException;
import java.util.ArrayList;
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

  private final SeedSyncSupport syncSupport;
  private final SegmentTypeService segmentTypeService;
  private final ObjectMapper objectMapper;

  @Autowired
  public SegmentTypeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final SegmentTypeService segmentTypeService,
      final ObjectMapper objectMapper) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.segmentTypeService = segmentTypeService;
    this.objectMapper = objectMapper;
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
  private String segmentTypeFingerprint(SegmentType st) {
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
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (com.fasterxml.jackson.core.JsonProcessingException
        | java.security.NoSuchAlgorithmException e) {
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
}
