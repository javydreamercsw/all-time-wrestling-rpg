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
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
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
@Order(90)
public class NpcSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "npc";

  private final SeedSyncSupport syncSupport;
  private final NpcService npcService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public NpcSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final NpcService npcService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.npcService = npcService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && npcService.count() > 0) {
      topUpMissingNpcs();
      if (syncSupport.isVerify()) {
        reconcileVerifiedNpcs();
      }
      return;
    }
    loadNpcs(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still insert seed entries that don't exist yet, so
   * catalogs added in later releases reach flag-protected installs (e.g. the 2.12.0 referees).
   * Existing rows are never re-saved.
   */
  private void topUpMissingNpcs() {
    List<Npc> missing = loadNpcs(false);
    if (!missing.isEmpty()) {
      log.info("NPC top-up: inserting {} missing seed NPC(s).", missing.size());
      npcService.saveAll(missing);
    }
  }

  /**
   * Loads NPCs from {@code npcs.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only entries missing from the database are returned and existing rows are left
   *     untouched.
   * @return NPCs that were inserted or need saving.
   */
  private List<Npc> loadNpcs(boolean updateExisting) {
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (!resource.exists()) {
      log.warn("Npcs file not found: {}", resource.getPath());
      return List.of();
    }
    log.debug("Loading npcs from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<NpcDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Npc> existingNpcs =
          npcService.findAllUnfiltered().stream()
              .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
      List<Npc> toSave = new ArrayList<>();
      for (NpcDTO dto : dtos) {
        Npc npc = existingNpcs.get(dto.getName());
        if (npc == null) {
          Npc newNpc = fromDto(dto);
          newNpc.setSeedFingerprint(npcFingerprint(newNpc, dto.getAwareness()));
          toSave.add(newNpc);
        } else if (updateExisting) {
          boolean changed = false;
          if (!Objects.equals(npc.getDescription(), dto.getDescription())) {
            npc.setDescription(dto.getDescription());
            changed = true;
          }
          if (!Objects.equals(npc.getNpcType(), dto.getType())) {
            npc.setNpcType(dto.getType());
            changed = true;
          }
          if (dto.getExpansionCode() != null
              && !Objects.equals(npc.getExpansionCode(), dto.getExpansionCode())) {
            npc.setExpansionCode(dto.getExpansionCode());
            changed = true;
          }
          if (dto.getAlignment() != null) {
            try {
              AlignmentType at = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
              if (!Objects.equals(npc.getAlignment(), at)) {
                npc.setAlignment(at);
                changed = true;
              }
            } catch (IllegalArgumentException e) {
              log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
            }
          }
          if (dto.getAwareness() != null) {
            npcService.setAwareness(npc, dto.getAwareness());
          }
          if (changed) {
            npc.setSeedFingerprint(npcFingerprint(npc, dto.getAwareness()));
            toSave.add(npc);
          }
        }
      }
      if (!toSave.isEmpty() && updateExisting) {
        npcService.saveAll(toSave);
      }
      log.debug("Npc loading completed - {} npcs processed", dtos.size());
      return toSave;
    } catch (IOException e) {
      log.error("Error loading npcs from file", e);
      return List.of();
    }
  }

  /**
   * Canonical seed-owned field values for an NPC, in fingerprint order: description, type,
   * expansionCode, alignment, awareness. Mirrors the seed write set (NpcDTO fields). Public for the
   * seed-provenance manifest generator, which re-hashes fresh seed rows through the owning sync's
   * own method.
   */
  public String npcFingerprint(Npc npc, Integer seedAwareness) {
    Integer awareness;
    if (seedAwareness != null) {
      awareness = Math.max(0, Math.min(100, seedAwareness));
    } else {
      Object attr = npc.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS);
      awareness = attr instanceof Number number ? number.intValue() : null;
    }
    return syncSupport.fingerprint(
        npc.getDescription(),
        npc.getNpcType(),
        npc.getExpansionCode(),
        npc.getAlignment() == null ? null : npc.getAlignment().name(),
        awareness == null ? null : awareness.toString());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing NPCs. The stored fingerprint is the hash
   * of the row's seed-owned fields as the last seed write left them; re-hashing the row's current
   * fields detects hand edits. A row that still hashes to its stamp is clean and adopts seed
   * changes; a row whose fields drifted is user-edited — warned and preserved. NPCs with no seed
   * entry are never considered.
   */
  private void reconcileVerifiedNpcs() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<NpcDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Npc> existingNpcs =
          npcService.findAllUnfiltered().stream()
              .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      List<Npc> toSave = new ArrayList<>();
      for (NpcDTO dto : dtos) {
        Npc npc = existingNpcs.get(dto.getName());
        if (npc == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = npcFingerprint(npc, null);
        if (SeedSyncSupport.isBlankFingerprint(npc.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (npcMatchesSeed(npc, dto)) {
            npc.setSeedFingerprint(current);
            toSave.add(npc);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: NPC '{}' has hand-edited seed-owned fields (legacy row, no "
                    + "fingerprint); preserving user values.",
                npc.getName());
            preserved++;
          }
        } else if (current.equals(npc.getSeedFingerprint())) {
          if (npcMatchesSeed(npc, dto)) {
            continue; // already in sync
          }
          applySeedFields(npc, dto);
          npc.setSeedFingerprint(npcFingerprint(npc, dto.getAwareness()));
          toSave.add(npc);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: NPC '{}' was modified after seeding (fingerprint mismatch); "
                  + "preserving user values.",
              npc.getName());
          preserved++;
        }
      }
      if (!toSave.isEmpty()) {
        npcService.saveAll(toSave);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("npc", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during NPC verify reconcile", e);
    }
  }

  private boolean npcMatchesSeed(Npc npc, NpcDTO dto) {
    AlignmentType seedAlignment = null;
    if (dto.getAlignment() != null) {
      try {
        seedAlignment = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
      }
    }
    Integer seedAwareness = dto.getAwareness();
    Object attr = npc.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS);
    Integer npcAwareness = attr instanceof Number number ? number.intValue() : null;
    // When the seed omits alignment, the seed write leaves the entity default (NEUTRAL).
    AlignmentType effectiveSeedAlignment =
        seedAlignment != null ? seedAlignment : AlignmentType.NEUTRAL;
    return Objects.equals(npc.getDescription(), dto.getDescription())
        && Objects.equals(npc.getNpcType(), dto.getType())
        && Objects.equals(npc.getExpansionCode(), dto.getExpansionCode())
        && Objects.equals(npc.getAlignment(), effectiveSeedAlignment)
        && Objects.equals(
            npcAwareness, seedAwareness == null ? null : Math.max(0, Math.min(100, seedAwareness)));
  }

  private void applySeedFields(Npc npc, NpcDTO dto) {
    npc.setDescription(dto.getDescription());
    npc.setNpcType(dto.getType());
    if (dto.getExpansionCode() != null) {
      npc.setExpansionCode(dto.getExpansionCode());
    }
    if (dto.getAlignment() != null) {
      try {
        npc.setAlignment(AlignmentType.valueOf(dto.getAlignment().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
      }
    }
    if (dto.getAwareness() != null) {
      npcService.setAwareness(npc, dto.getAwareness());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing NPCs
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
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<NpcDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, Npc> existingNpcs =
          npcService.findAllUnfiltered().stream()
              .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
      List<Npc> toSave = new ArrayList<>();
      for (NpcDTO dto : dtos) {
        Npc npc = existingNpcs.get(dto.getName());
        if (npc == null) {
          continue; // user-created rows have no seed match; missing rows are top-up's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(npc, dto));
        boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(npc.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(npc, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getName()),
                dto.getName(),
                rowHasStoredStamp,
                npc.getSeedFingerprint(),
                observations,
                this::npcValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(npc, dto, result.silentUpdateFields());
          toSave.add(npc);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(npc.getSeedFingerprint())) {
          npc.setSeedFingerprint(stamp);
          if (!toSave.contains(npc)) {
            toSave.add(npc);
          }
        }
      }
      if (!toSave.isEmpty()) {
        npcService.saveAll(toSave);
      }
      log.info("Seed review detection (npc) complete ({} ms).", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during NPC seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one NPC row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(Npc npc, NpcDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, npc.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "npcType", null, npc.getNpcType(), dto.getType()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expansionCode", null, npc.getExpansionCode(), dto.getExpansionCode()));
    // When the seed omits alignment, the seed write leaves the entity default (NEUTRAL).
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "alignment",
            null,
            npc.getAlignment() == null ? null : npc.getAlignment().name(),
            dto.getAlignment() == null
                ? AlignmentType.NEUTRAL.name()
                : AlignmentType.valueOf(dto.getAlignment().toUpperCase()).name()));
    Object attr = npc.getAttributes().get(NpcService.ATTRIBUTE_AWARENESS);
    Integer npcAwareness = attr instanceof Number number ? number.intValue() : null;
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "awareness",
            null,
            npcAwareness == null ? null : npcAwareness.toString(),
            dto.getAwareness() == null
                ? null
                : Integer.toString(Math.max(0, Math.min(100, dto.getAwareness())))));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(Npc npc, NpcDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.getType(),
        dto.getExpansionCode(),
        dto.getAlignment() == null
            ? AlignmentType.NEUTRAL.name()
            : AlignmentType.valueOf(dto.getAlignment().toUpperCase()).name(),
        dto.getAwareness() == null
            ? null
            : Integer.toString(Math.max(0, Math.min(100, dto.getAwareness()))));
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #npcFingerprint(Npc, Integer)} exactly.
   */
  private String npcValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0), values.get(1), values.get(2), values.get(3), values.get(4));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for an NPC name — shared with {@code NpcSeedApplier} so apply-side
   * resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "npc:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror {@link #applySeedFields(Npc, NpcDTO)}.
   */
  private void applySilentSeedFields(Npc npc, NpcDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> npc.setDescription(dto.getDescription());
        case "npcType" -> npc.setNpcType(dto.getType());
        case "expansionCode" -> {
          if (dto.getExpansionCode() != null) {
            npc.setExpansionCode(dto.getExpansionCode());
          }
        }
        case "alignment" -> {
          if (dto.getAlignment() != null) {
            try {
              npc.setAlignment(AlignmentType.valueOf(dto.getAlignment().toUpperCase()));
            } catch (IllegalArgumentException e) {
              log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
            }
          }
        }
        case "awareness" -> {
          if (dto.getAwareness() != null) {
            npcService.setAwareness(npc, dto.getAwareness());
          }
        }
        default ->
            throw new IllegalStateException("Unknown silent-update npc field '" + field + "'");
      }
    }
  }

  private static Npc fromDto(NpcDTO dto) {
    Npc npc = new Npc();
    npc.setName(dto.getName());
    npc.setDescription(dto.getDescription());
    npc.setNpcType(dto.getType());
    if (dto.getExpansionCode() != null) {
      npc.setExpansionCode(dto.getExpansionCode());
    }
    if (dto.getAwareness() != null) {
      // NpcSync cannot call npcService here without coupling the mapping to the service;
      // awareness lands on the attributes map right after construction.
      Map<String, Object> attrs = npc.getAttributes();
      attrs.put(NpcService.ATTRIBUTE_AWARENESS, Math.max(0, Math.min(100, dto.getAwareness())));
      npc.setAttributes(attrs);
    }
    if (dto.getAlignment() != null) {
      try {
        npc.setAlignment(AlignmentType.valueOf(dto.getAlignment().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
      }
    }
    return npc;
  }
}
