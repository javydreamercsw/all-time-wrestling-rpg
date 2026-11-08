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
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import com.github.javydreamercsw.management.dto.StatusCardDTO;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(190)
public class StatusCardSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "status_card";

  private final SeedSyncSupport syncSupport;
  private final StatusCardService statusCardService;
  private final StatusCardRepository statusCardRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public StatusCardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final StatusCardService statusCardService,
      final StatusCardRepository statusCardRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.statusCardService = statusCardService;
    this.statusCardRepository = statusCardRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    // Non-FULL boots skip the file import once any card exists: the seed write-set would clobber
    // admin edits on real cards (the version-change detection pass owns seed drift instead).
    // FULL mode (first boot / empty table) applies the seed everywhere.
    if (syncSupport.mode() != SeedSyncMode.FULL && statusCardRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("status_cards.json");
    if (resource.exists()) {
      log.debug("Loading status cards from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<StatusCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (statusCardService.findAll().size() == dtos.size()) {
          log.debug("Status cards already loaded ({} cards), skipping sync.", dtos.size());
          return;
        }
        for (StatusCardDTO dto : dtos) {
          StatusCard saved =
              statusCardService.createOrUpdateCard(
                  dto.getKey(),
                  dto.getLevel1Name(),
                  dto.getLevel2Name(),
                  dto.getDescription(),
                  dto.isPositive(),
                  dto.getLevel1Effect(),
                  dto.getLevel2Effect(),
                  dto.getFlipUpCondition(),
                  dto.getFlipDownCondition(),
                  dto.getDiscardCondition());
          if (SeedSyncSupport.isBlankFingerprint(saved.getSeedFingerprint())) {
            // Stamp the freshly seeded row so verify mode's stored-side check and the
            // version-change detection pass can match it against the manifest. The service's
            // save leaves the stamp unset.
            saved.setSeedFingerprint(statusCardFingerprint(saved));
            statusCardService.save(saved);
          }
          log.debug("Loaded status card: {}", dto.getKey());
        }
        log.debug("Status card loading completed - {} cards loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading status cards from file", e);
      }
    } else {
      log.warn("Status cards file not found: {}", resource.getPath());
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing status cards are classified field-by-field
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
    if (syncSupport.mode() == SeedSyncMode.FULL || statusCardRepository.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("status_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<StatusCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      List<StatusCard> toSave = new ArrayList<>();
      for (StatusCardDTO dto : dtos) {
        // Status cards resolve by key — the seed's stable identity. The level names are seed-owned
        // content (collision targets), so they can never serve as the resolution handle (same
        // contract as the card domain's set#number key).
        StatusCard existing = statusCardRepository.findByKey(dto.getKey()).orElse(null);
        if (existing == null) {
          continue; // user-created rows have no seed match; missing rows are the FULL load's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, dto.getKey(), seedValueFingerprint(dto));
        boolean rowHasStoredStamp =
            SeedReviewDetectionSupport.hasStamp(existing.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations =
            fieldObservations(existing, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKey(dto.getKey()),
                dto.getKey(),
                rowHasStoredStamp,
                existing.getSeedFingerprint(),
                observations,
                this::statusCardValueListFingerprint,
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
        for (StatusCard card : toSave) {
          statusCardService.save(card);
        }
      }
      log.info(
          "Seed review detection (status card) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during status card seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one status card row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      StatusCard existing, StatusCardDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "level1Name", null, existing.getLevel1Name(), dto.getLevel1Name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "level2Name", null, existing.getLevel2Name(), dto.getLevel2Name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "positive",
            null,
            String.valueOf(existing.isPositive()),
            String.valueOf(dto.isPositive())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "level1Effect", null, existing.getLevel1Effect(), dto.getLevel1Effect()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "level2Effect", null, existing.getLevel2Effect(), dto.getLevel2Effect()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "flipUpCondition", null, existing.getFlipUpCondition(), dto.getFlipUpCondition()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "flipDownCondition",
            null,
            existing.getFlipDownCondition(),
            dto.getFlipDownCondition()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "discardCondition", null, existing.getDiscardCondition(), dto.getDiscardCondition()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(StatusCardDTO dto) {
    return syncSupport.fingerprint(
        dto.getLevel1Name(),
        dto.getLevel2Name(),
        dto.getDescription(),
        dto.isPositive(),
        dto.getLevel1Effect(),
        dto.getLevel2Effect(),
        dto.getFlipUpCondition(),
        dto.getFlipDownCondition(),
        dto.getDiscardCondition());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #statusCardFingerprint(StatusCard)} exactly.
   */
  private String statusCardValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so the observed string
    // forms hash identically to the row fingerprint's typed values (see WrestlerSync's shape).
    return syncSupport.fingerprint(values.toArray());
  }

  private static String entityKey(String key) {
    return entityKeyFor(key);
  }

  /**
   * The canonical entity key for a status card key — shared with {@code StatusCardSeedApplier} so
   * apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String key) {
    return "status_card:" + key.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for a status card row, in fingerprint order: level1Name,
   * level2Name, description, positive, level1Effect, level2Effect, flipUpCondition,
   * flipDownCondition, discardCondition. The lookup key is excluded. Public for the seed-provenance
   * manifest generator, which re-hashes fresh seed rows through the owning sync's own method.
   */
  public String statusCardFingerprint(StatusCard card) {
    return syncSupport.fingerprint(
        card.getLevel1Name(),
        card.getLevel2Name(),
        card.getDescription(),
        card.isPositive(),
        card.getLevel1Effect(),
        card.getLevel2Effect(),
        card.getFlipUpCondition(),
        card.getFlipDownCondition(),
        card.getDiscardCondition());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the write
   * semantics mirror {@code createOrUpdateCard}'s write set.
   */
  private void applySilentSeedFields(
      StatusCard existing, StatusCardDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "level1Name" -> existing.setLevel1Name(dto.getLevel1Name());
        case "level2Name" -> existing.setLevel2Name(dto.getLevel2Name());
        case "description" -> existing.setDescription(dto.getDescription());
        case "positive" -> existing.setPositive(dto.isPositive());
        case "level1Effect" -> existing.setLevel1Effect(dto.getLevel1Effect());
        case "level2Effect" -> existing.setLevel2Effect(dto.getLevel2Effect());
        case "flipUpCondition" -> existing.setFlipUpCondition(dto.getFlipUpCondition());
        case "flipDownCondition" -> existing.setFlipDownCondition(dto.getFlipDownCondition());
        case "discardCondition" -> existing.setDiscardCondition(dto.getDiscardCondition());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update status card field '" + field + "'");
      }
    }
  }
}
