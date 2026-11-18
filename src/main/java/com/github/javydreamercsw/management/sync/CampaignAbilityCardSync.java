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
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.dto.CampaignAbilityCardDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignAbilityCardService;
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
@Order(180)
public class CampaignAbilityCardSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "campaign_ability_card";

  private final SeedSyncSupport syncSupport;
  private final CampaignAbilityCardService campaignAbilityCardService;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CampaignAbilityCardSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CampaignAbilityCardService campaignAbilityCardService,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.campaignAbilityCardService = campaignAbilityCardService;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    if (syncSupport.mode() != SeedSyncMode.FULL && campaignAbilityCardService.count() > 0) {
      topUpMissingCards();
      if (syncSupport.isVerify()) {
        reconcileVerifiedCards();
      }
      return;
    }
    loadCards();
  }

  /**
   * Skip-gate branch: cards exist and the flag protects them. Still insert seed cards that don't
   * exist yet. Existing rows are never re-saved.
   */
  private void topUpMissingCards() {
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignAbilityCard> existingByName =
          campaignAbilityCardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel(),
                      c -> c,
                      (a, b) -> a));
      int inserted = 0;
      for (CampaignAbilityCardDTO dto : dtos) {
        String key = dto.getName() + "|" + dto.getAlignmentType() + "|" + dto.getLevel();
        if (existingByName.containsKey(key)) {
          continue;
        }
        CampaignAbilityCard created = applyDto(dto);
        created.setSeedFingerprint(cardFingerprint(created));
        campaignAbilityCardService.save(created);
        inserted++;
      }
      if (inserted > 0) {
        log.info("Campaign ability card top-up: inserting {} missing seed card(s).", inserted);
      }
    } catch (IOException e) {
      log.error("Error loading campaign ability cards from file", e);
    }
  }

  private void loadCards() {
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (resource.exists()) {
      log.debug("Loading campaign ability cards from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (CampaignAbilityCardDTO dto : dtos) {
          CampaignAbilityCard card = applyDto(dto);
          card.setSeedFingerprint(cardFingerprint(card));
          campaignAbilityCardService.save(card);
          log.debug("Loaded campaign ability card: {}", dto.getName());
        }
        log.debug("Campaign ability card loading completed - {} cards loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading campaign ability cards from file", e);
      }
    } else {
      log.warn("Campaign ability cards file not found: {}", resource.getPath());
    }
  }

  /** Creates or updates through the service (which saves) and returns the row. */
  private CampaignAbilityCard applyDto(CampaignAbilityCardDTO dto) {
    return campaignAbilityCardService.createOrUpdateCard(
        dto.getName(),
        dto.getDescription(),
        dto.getAlignmentType(),
        dto.getLevel(),
        dto.isOneTimeUse(),
        dto.getTiming(),
        dto.getEffectScript(),
        dto.getSecondaryEffectScript(),
        dto.isSecondaryOneTimeUse(),
        dto.getSecondaryTiming());
  }

  /**
   * Canonical seed-owned field values for an ability card row, in fingerprint order. The lookup key
   * (name, alignmentType, level) is excluded; the rest mirrors {@code createOrUpdateCard}'s write
   * set. Public for the seed-provenance manifest generator, which re-hashes fresh seed rows through
   * the owning sync's own method.
   */
  public String cardFingerprint(CampaignAbilityCard card) {
    return syncSupport.fingerprint(
        card.getDescription(),
        card.isOneTimeUse(),
        card.getTiming() == null ? null : card.getTiming().name(),
        card.getEffectScript(),
        card.getSecondaryEffectScript(),
        card.isSecondaryOneTimeUse(),
        card.getSecondaryTiming() == null ? null : card.getSecondaryTiming().name());
  }

  /**
   * Verify mode: reconcile seed-owned fields of existing ability cards. The stored fingerprint is
   * the hash of the row's seed-owned fields as the last seed write left them; re-hashing the row's
   * current fields detects hand edits. A row that still hashes to its stamp is clean and adopts
   * seed changes; a row whose fields drifted is user-edited — warned and preserved. Cards with no
   * seed entry (keyed by name|alignment|level) are never considered.
   */
  private void reconcileVerifiedCards() {
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignAbilityCard> existingByName =
          campaignAbilityCardService.findAll().stream()
              .collect(
                  Collectors.toMap(
                      c -> c.getName() + "|" + c.getAlignmentType() + "|" + c.getLevel(),
                      c -> c,
                      (a, b) -> a));
      int reconciled = 0;
      int preserved = 0;
      for (CampaignAbilityCardDTO dto : dtos) {
        String key = dto.getName() + "|" + dto.getAlignmentType() + "|" + dto.getLevel();
        CampaignAbilityCard card = existingByName.get(key);
        if (card == null) {
          continue; // user-created rows have no seed match and are never touched
        }
        String current = cardFingerprint(card);
        boolean matchesSeed =
            Objects.equals(card.getDescription(), dto.getDescription())
                && card.isOneTimeUse() == dto.isOneTimeUse()
                && Objects.equals(
                    card.getTiming() == null ? null : card.getTiming().name(),
                    dto.getTiming() == null ? null : dto.getTiming().name())
                && Objects.equals(card.getEffectScript(), dto.getEffectScript())
                && Objects.equals(card.getSecondaryEffectScript(), dto.getSecondaryEffectScript())
                && card.isSecondaryOneTimeUse() == dto.isSecondaryOneTimeUse()
                && Objects.equals(
                    card.getSecondaryTiming() == null ? null : card.getSecondaryTiming().name(),
                    dto.getSecondaryTiming() == null ? null : dto.getSecondaryTiming().name());
        if (SeedSyncSupport.isBlankFingerprint(card.getSeedFingerprint())) {
          // Legacy row: adopt only if its seed-owned fields already equal the seed.
          if (matchesSeed) {
            card.setSeedFingerprint(current);
            campaignAbilityCardService.save(card);
            reconciled++;
          } else {
            log.warn(
                "Seed verify: campaign ability card '{}' has hand-edited seed-owned fields "
                    + "(legacy row, no fingerprint); preserving user values.",
                card.getName());
            preserved++;
          }
        } else if (current.equals(card.getSeedFingerprint())) {
          if (matchesSeed) {
            continue; // already in sync
          }
          CampaignAbilityCard refreshed = applyDto(dto);
          refreshed.setSeedFingerprint(cardFingerprint(refreshed));
          campaignAbilityCardService.save(refreshed);
          reconciled++;
        } else {
          log.warn(
              "Seed verify: campaign ability card '{}' was modified after seeding (fingerprint "
                  + "mismatch); preserving user values.",
              card.getName());
          preserved++;
        }
      }
      log.info(
          "{} ({} ms)",
          syncSupport.reconcileLog("campaign ability card", reconciled, preserved),
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during campaign ability card verify reconcile", e);
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing ability cards are classified field-by-field
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
    if (syncSupport.mode() == SeedSyncMode.FULL || campaignAbilityCardService.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    ClassPathResource resource = new ClassPathResource("campaign_ability_cards.json");
    if (!resource.exists()) {
      return;
    }
    try (var is = resource.getInputStream()) {
      List<CampaignAbilityCardDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      List<CampaignAbilityCard> toSave = new ArrayList<>();
      for (CampaignAbilityCardDTO dto : dtos) {
        CampaignAbilityCard card =
            campaignAbilityCardService
                .findByNameAndAlignmentAndLevel(
                    dto.getName(), dto.getAlignmentType(), dto.getLevel())
                .orElse(null);
        if (card == null) {
          continue; // user-created rows have no seed match; missing rows are top-up's job
        }
        String newFingerprint =
            SeedReviewDetectionSupport.incomingFingerprint(
                provenanceManifest, DOMAIN_KEY, seedKeyOf(dto), seedValueFingerprint(dto));
        boolean rowHasStoredStamp = SeedReviewDetectionSupport.hasStamp(card.getSeedFingerprint());
        List<SeedCollisionDetector.FieldObservation> observations = fieldObservations(card, dto);
        SeedReviewDetectionSupport.DetectionResult result =
            SeedReviewDetectionSupport.detectRow(
                DOMAIN_KEY,
                entityKeyFor(seedKeyOf(dto)),
                // The display name is the seed's name portion of the composite key — captured
                // verbatim for the review UI; the applier resolves through the entity key (names
                // are not unique across alignment/level rows).
                dto.getName(),
                rowHasStoredStamp,
                card.getSeedFingerprint(),
                observations,
                this::cardValueListFingerprint,
                newFingerprint,
                seedReviewService);
        String stamp =
            SeedReviewDetectionSupport.stampForResolvedRow(
                result.provenanceExact(), newFingerprint);
        if (!result.silentUpdateFields().isEmpty()) {
          // Tier-1 write set: ONLY the silent-update fields — a tier-3-preserved admin edit on
          // the same row must never be touched (per-field granularity, spec "Tier 2 granularity").
          applySilentSeedFields(card, dto, result.silentUpdateFields());
          toSave.add(card);
        }
        // Stamp-only rows must persist the re-stamp too: capture the change BEFORE setting it —
        // comparing after the set reads the NEW value and the save is silently skipped forever
        // (the row never converges).
        if (stamp != null && !stamp.equals(card.getSeedFingerprint())) {
          card.setSeedFingerprint(stamp);
          if (!toSave.contains(card)) {
            toSave.add(card);
          }
        }
      }
      if (!toSave.isEmpty()) {
        for (CampaignAbilityCard card : toSave) {
          campaignAbilityCardService.save(card);
        }
      }
      log.info(
          "Seed review detection (campaign ability card) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during campaign ability card seed-review detection", e);
    }
  }

  /** The composite seed key of an ability card DTO (the manifest's lookup key). */
  private static String seedKeyOf(CampaignAbilityCardDTO dto) {
    return dto.getName() + "|" + dto.getAlignmentType() + "|" + dto.getLevel();
  }

  /** The detection-time per-field observations for one ability card row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      CampaignAbilityCard card, CampaignAbilityCardDTO dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, card.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "oneTimeUse",
            null,
            String.valueOf(card.isOneTimeUse()),
            String.valueOf(dto.isOneTimeUse())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "timing",
            null,
            card.getTiming() == null ? null : card.getTiming().name(),
            dto.getTiming() == null ? null : dto.getTiming().name()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "effectScript", null, card.getEffectScript(), dto.getEffectScript()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "secondaryEffectScript",
            null,
            card.getSecondaryEffectScript(),
            dto.getSecondaryEffectScript()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "secondaryOneTimeUse",
            null,
            String.valueOf(card.isSecondaryOneTimeUse()),
            String.valueOf(dto.isSecondaryOneTimeUse())));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "secondaryTiming",
            null,
            card.getSecondaryTiming() == null ? null : card.getSecondaryTiming().name(),
            dto.getSecondaryTiming() == null ? null : dto.getSecondaryTiming().name()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(CampaignAbilityCardDTO dto) {
    return syncSupport.fingerprint(
        dto.getDescription(),
        dto.isOneTimeUse(),
        dto.getTiming() == null ? null : dto.getTiming().name(),
        dto.getEffectScript(),
        dto.getSecondaryEffectScript(),
        dto.isSecondaryOneTimeUse(),
        dto.getSecondaryTiming() == null ? null : dto.getSecondaryTiming().name());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #cardFingerprint(CampaignAbilityCard)} exactly.
   */
  private String cardValueListFingerprint(List<String> values) {
    // Raw pass-through: fingerprint serializes each value via toString, so the observed string
    // forms hash identically to the row fingerprint's typed values (see WrestlerSync's shape).
    return syncSupport.fingerprint(values.toArray());
  }

  /**
   * The canonical entity key for an ability card's composite seed key — shared with {@code
   * CampaignAbilityCardSeedApplier} so apply-side resolution derives keys exactly as detection
   * minted them.
   */
  static String entityKeyFor(String seedKey) {
    return "campaign_ability_card:" + seedKey.toLowerCase();
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries; the write
   * semantics mirror {@code createOrUpdateCard}'s write set.
   */
  private void applySilentSeedFields(
      CampaignAbilityCard card, CampaignAbilityCardDTO dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> card.setDescription(dto.getDescription());
        case "oneTimeUse" -> card.setOneTimeUse(dto.isOneTimeUse());
        case "timing" -> card.setTiming(dto.getTiming());
        case "effectScript" -> card.setEffectScript(dto.getEffectScript());
        case "secondaryEffectScript" ->
            card.setSecondaryEffectScript(dto.getSecondaryEffectScript());
        case "secondaryOneTimeUse" -> card.setSecondaryOneTimeUse(dto.isSecondaryOneTimeUse());
        case "secondaryTiming" -> card.setSecondaryTiming(dto.getSecondaryTiming());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update campaign ability card field '" + field + "'");
      }
    }
  }
}
