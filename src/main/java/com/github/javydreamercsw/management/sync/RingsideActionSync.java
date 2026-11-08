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
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.dto.RingsideActionDTO;
import com.github.javydreamercsw.management.dto.RingsideActionTypeDTO;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
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
@Order(240)
public class RingsideActionSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "ringside_action";

  private final SeedSyncSupport syncSupport;
  private final RingsideActionDataService ringsideActionDataService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public RingsideActionSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final RingsideActionDataService ringsideActionDataService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.ringsideActionDataService = ringsideActionDataService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    // Types always flow through their own skip gate inside syncTypes() (FULL re-syncs them).
    syncTypes();
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countActions() > 0) {
      topUpMissingActions();
      if (syncSupport.isVerify()) {
        reconcileVerifiedActions();
      }
      return;
    }
    syncActions();
  }

  private void syncTypes() {
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countTypes() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_action_types.json");
    if (resource.exists()) {
      log.debug("Loading ringside action types from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionTypeDTO> dtos =
            objectMapper.readValue(is, new TypeReference<List<RingsideActionTypeDTO>>() {});
        for (RingsideActionTypeDTO dto : dtos) {
          ringsideActionDataService.createOrUpdateType(
              dto.getName(),
              dto.isIncreasesAwareness(),
              dto.isCanCauseDq(),
              dto.getBaseRiskMultiplier());
          log.debug("Loaded ringside action type: {}", dto.getName());
        }
        log.debug("Ringside action type loading completed - {} types loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside action types from file", e);
      }
    }
  }

  /**
   * Skip-gate branch: actions exist and the flag protects them. Still insert seed actions that
   * don't exist yet. Existing rows are never re-saved.
   */
  private void topUpMissingActions() {
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, RingsideAction> existingByName =
          ringsideActionDataService.findAllActionsUnfiltered().stream()
              .collect(Collectors.toMap(RingsideAction::getName, a -> a, (a, b) -> a));
      int inserted = 0;
      for (RingsideActionDTO dto : dtos) {
        if (existingByName.containsKey(dto.getName())) {
          continue;
        }
        RingsideAction created =
            ringsideActionDataService.createOrUpdateAction(
                dto.getName(),
                dto.getType(),
                dto.getDescription(),
                dto.getImpact(),
                dto.getRisk(),
                dto.getAlignment(),
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        created.setSeedFingerprint(actionFingerprint(created));
        ringsideActionDataService.saveAction(created);
        inserted++;
      }
      if (inserted > 0) {
        log.info("Ringside action top-up: inserting {} missing seed action(s).", inserted);
      }
    } catch (IOException e) {
      log.error("Error loading ringside actions from file", e);
    }
  }

  private void syncActions() {
    if (syncSupport.mode() != SeedSyncMode.FULL && ringsideActionDataService.countActions() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (resource.exists()) {
      log.debug("Loading ringside actions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (RingsideActionDTO dto : dtos) {
          RingsideAction action =
              ringsideActionDataService.createOrUpdateAction(
                  dto.getName(),
                  dto.getType(),
                  dto.getDescription(),
                  dto.getImpact(),
                  dto.getRisk(),
                  dto.getAlignment(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          action.setSeedFingerprint(actionFingerprint(action));
          ringsideActionDataService.saveAction(action);
          log.debug("Loaded ringside action: {}", dto.getName());
        }
        log.debug("Ringside action loading completed - {} actions loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading ringside actions from file", e);
      }
    }
  }

  /**
   * Canonical seed-owned field values for a ringside action row, in fingerprint order: type name,
   * description, impact, risk, alignment, expansionCode. Mirrors the write set of {@code
   * createOrUpdateAction} minus the name key. Public for the seed-provenance manifest generator,
   * which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String actionFingerprint(RingsideAction action) {
    return syncSupport.fingerprint(
        action.getType() == null ? null : action.getType().getName(),
        action.getDescription(),
        action.getImpact(),
        action.getRisk(),
        action.getAlignment() == null ? null : action.getAlignment().name(),
        action.getExpansionCode());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing ringside actions. The stored fingerprint
   * is the hash of the row's seed-owned fields as the last seed write left them; re-hashing the
   * row's current fields detects hand edits. A row that still hashes to its stamp is clean and
   * adopts seed changes; a row whose fields drifted is user-edited — warned and preserved. Actions
   * with no seed entry are never considered.
   */
  private void reconcileVerifiedActions() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, RingsideAction> existingByName =
          ringsideActionDataService.findAllActionsUnfiltered().stream()
              .collect(Collectors.toMap(RingsideAction::getName, a -> a, (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      List<RingsideAction> toSave = new ArrayList<>();
      for (RingsideActionDTO dto : dtos) {
        RingsideAction action = existingByName.get(dto.getName());
        if (action == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = actionFingerprint(action);
        boolean matchesSeed =
            Objects.equals(
                    action.getType() == null ? null : action.getType().getName(), dto.getType())
                && Objects.equals(action.getDescription(), dto.getDescription())
                && action.getImpact() == dto.getImpact()
                && action.getRisk() == dto.getRisk()
                && Objects.equals(
                    action.getAlignment() == null ? null : action.getAlignment().name(),
                    dto.getAlignment() == null ? "NEUTRAL" : dto.getAlignment().name())
                && Objects.equals(
                    action.getExpansionCode(),
                    dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        if (SeedSyncSupport.isBlankFingerprint(action.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (matchesSeed) {
            action.setSeedFingerprint(current);
            toSave.add(action);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: ringside action '{}' has hand-edited seed-owned fields (legacy "
                    + "row, no fingerprint); preserving user values.",
                action.getName());
            preserved++;
          }
        } else if (current.equals(action.getSeedFingerprint())) {
          if (matchesSeed) {
            continue; // already in sync
          }
          RingsideAction refreshed =
              ringsideActionDataService.createOrUpdateAction(
                  dto.getName(),
                  dto.getType(),
                  dto.getDescription(),
                  dto.getImpact(),
                  dto.getRisk(),
                  dto.getAlignment(),
                  dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
          refreshed.setSeedFingerprint(actionFingerprint(refreshed));
          ringsideActionDataService.saveAction(refreshed);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: ringside action '{}' was modified after seeding (fingerprint "
                  + "mismatch); preserving user values.",
              action.getName());
          preserved++;
        }
      }
      for (RingsideAction action : toSave) {
        ringsideActionDataService.saveAction(action);
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("ringside action", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during ringside action verify reconcile", e);
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): classifies existing ringside
   * actions field-by-field through {@link SeedCollisionDetector} instead of reconciling directly —
   * tier-1 fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3
   * fields stay untouched. Only meaningful in PROTECT/VERIFY mode with existing rows.
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      return; // FULL mode applies the seed everywhere
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("ringside_actions.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<RingsideActionDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, RingsideAction> existingByName =
          ringsideActionDataService.findAllActionsUnfiltered().stream()
              .collect(Collectors.toMap(RingsideAction::getName, a -> a, (a, b) -> a));
      List<RingsideAction> toSave = new ArrayList<>();
      for (RingsideActionDTO dto : dtos) {
        RingsideAction action = existingByName.get(dto.getName());
        if (action == null) {
          continue; // user-created rows have no seed match; missing rows are top-up's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getName(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(action.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(action, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getName()),
                dto.getName(),
                rowHasStoredStamp,
                action.getSeedFingerprint(),
                observations,
                this::actionValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(action, dto, result.silentUpdateFields());
          toSave.add(action);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(action.getSeedFingerprint())) {
          action.setSeedFingerprint(stamp);
          if (!toSave.contains(action)) {
            toSave.add(action);
          }
        }
      }
      for (RingsideAction action : toSave) {
        ringsideActionDataService.saveAction(action);
      }
      log.info(
          "Seed review detection (ringside action) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during ringside action seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one ringside action row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      RingsideAction action, RingsideActionDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "type",
            null,
            action.getType() == null ? null : action.getType().getName(),
            dto.getType()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, action.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "impact",
            null,
            Integer.toString(action.getImpact()),
            Integer.toString(dto.getImpact())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "risk", null, Integer.toString(action.getRisk()), Integer.toString(dto.getRisk())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "alignment",
            null,
            action.getAlignment() == null ? null : action.getAlignment().name(),
            dto.getAlignment() == null ? "NEUTRAL" : dto.getAlignment().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "expansionCode",
            null,
            action.getExpansionCode(),
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME"));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(RingsideActionDTO dto) {
    return syncSupport.fingerprint(
        dto.getType(),
        dto.getDescription(),
        dto.getImpact(),
        dto.getRisk(),
        dto.getAlignment() == null ? "NEUTRAL" : dto.getAlignment().name(),
        dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #actionFingerprint(RingsideAction)} exactly.
   */
  private String actionValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(
        values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for a ringside action name — shared with {@code
   * RingsideActionSeedApplier} so apply-side resolution derives keys exactly as detection minted
   * them.
   */
  static String entityKeyFor(String name) {
    return "ringside_action:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the field
   * write semantics mirror the {@code createOrUpdateAction} write set.
   */
  private void applySilentSeedFields(
      RingsideAction action, RingsideActionDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "type" ->
            ringsideActionDataService.findAllTypes().stream()
                .filter(t -> dto.getType().equals(t.getName()))
                .findFirst()
                .ifPresent(action::setType);
        case "description" -> action.setDescription(dto.getDescription());
        case "impact" -> action.setImpact(dto.getImpact());
        case "risk" -> action.setRisk(dto.getRisk());
        case "alignment" ->
            action.setAlignment(
                dto.getAlignment() == null ? AlignmentType.NEUTRAL : dto.getAlignment());
        case "expansionCode" ->
            action.setExpansionCode(
                dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME");
        default ->
            throw new IllegalStateException(
                "Unknown silent-update ringside action field '" + field + "'");
      }
    }
  }
}
