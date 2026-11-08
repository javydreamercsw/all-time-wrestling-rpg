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
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgradeRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.sync.review.SeedCollisionDetector;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionSupport;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(200)
public class CampaignUpgradeSync implements DataSyncContributor {

  /** Seed domain key (must match {@code SeedDomains.STAMPING_DOMAINS}). */
  public static final String DOMAIN_KEY = "campaign_upgrade";

  private final SeedSyncSupport syncSupport;
  private final CampaignUpgradeService campaignUpgradeService;
  private final CampaignUpgradeRepository upgradeRepository;
  private final ObjectMapper objectMapper;
  private final SeedUpdateReviewService seedReviewService;
  private final SeedProvenanceManifest provenanceManifest;

  @Autowired
  public CampaignUpgradeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final String skipIfNotEmptyRaw,
      final CampaignUpgradeService campaignUpgradeService,
      final CampaignUpgradeRepository upgradeRepository,
      final ObjectMapper objectMapper,
      final SeedUpdateReviewService seedReviewService,
      final SeedProvenanceManifest provenanceManifest) {
    this.syncSupport = new SeedSyncSupport(skipIfNotEmptyRaw);
    this.campaignUpgradeService = campaignUpgradeService;
    this.upgradeRepository = upgradeRepository;
    this.objectMapper = objectMapper;
    this.seedReviewService = seedReviewService;
    this.provenanceManifest = provenanceManifest;
  }

  @Override
  public void sync() {
    // The service's load is the seed write (deleteAll + saveAll); its @PostConstruct already
    // runs it unconditionally on every boot, so the sync only re-issues it in FULL mode (the
    // production first-boot shape) and always tops up the stamps afterwards — the service's
    // save leaves the stamp unset and the wipe re-creates rows every boot.
    if (syncSupport.mode() == SeedSyncMode.FULL) {
      campaignUpgradeService.loadUpgrades();
    }
    try (InputStream is = getClass().getResourceAsStream("/campaign_upgrades.json")) {
      if (is == null) {
        log.warn("campaign_upgrades.json not found in resources — upgrades left unstamped");
        return;
      }
      List<CampaignUpgrade> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignUpgrade> existingByName =
          upgradeRepository.findAll().stream()
              .collect(Collectors.toMap(CampaignUpgrade::getName, u -> u, (a, b) -> a));
      List<CampaignUpgrade> toSave = new ArrayList<>();
      for (CampaignUpgrade dto : dtos) {
        CampaignUpgrade existing = existingByName.get(dto.getName());
        if (existing == null) {
          continue;
        }
        if (SeedSyncSupport.isBlankFingerprint(existing.getSeedFingerprint())) {
          existing.setSeedFingerprint(upgradeFingerprint(existing));
          toSave.add(existing);
        }
      }
      if (!toSave.isEmpty()) {
        upgradeRepository.saveAll(toSave);
      }
    } catch (IOException e) {
      log.error("Error stamping campaign upgrades after load", e);
    }
  }

  /**
   * Version-change detection pass (seed-update review feature): on a boot where the app version
   * changed (or undecided decisions exist), existing campaign upgrades are classified
   * field-by-field through {@link SeedCollisionDetector} instead of being reconciled directly —
   * tier-1 fields update silently and re-stamp, tier-2 fields park as PENDING decisions, tier-3
   * fields stay untouched. Runs only in PROTECT/VERIFY mode with existing rows.
   *
   * <p>Deviation note: {@code loadUpgrades()} currently wipes and re-seeds the whole table on every
   * boot (its own idempotency contract, pre-dating the review feature) — so in PROTECT/VERIFY mode
   * the seed content itself is already re-applied before detection runs. This pass still matters:
   * it classifies the freshly-loaded rows against their stamps (NO_OP re-stamp on unchanged seed,
   * park/silent on a changed seed the load just applied) and keeps the domain's provenance manifest
   * entry honest. The service's boot-wipe behavior is a candidate to revisit once the review
   * pipeline is live end-to-end (see the domain's slice report).
   *
   * <p>Callers invoke this once per boot after the domain passes and only when {@link
   * SeedUpdateReviewService#shouldRunDetection()} is true; {@code markDetected()} is NOT called
   * here (the orchestrator stamps the version after all domains finish).
   *
   * <p>Public: the orchestrator entry point — invoked once per boot by {@code
   * SeedReviewDetectionOrchestrator} when {@code shouldRunDetection()} is true; no other callers.
   */
  public void detectVersionChangeCollisions() {
    if (syncSupport.mode() == SeedSyncMode.FULL || upgradeRepository.count() == 0) {
      return; // FULL mode applies the seed everywhere; an empty DB has nothing to collide
    }
    long start = System.currentTimeMillis();
    try (InputStream is = getClass().getResourceAsStream("/campaign_upgrades.json")) {
      if (is == null) {
        return;
      }
      List<CampaignUpgrade> dtos = objectMapper.readValue(is, new TypeReference<>() {});
      Map<String, CampaignUpgrade> existingByName =
          upgradeRepository.findAll().stream()
              .collect(Collectors.toMap(CampaignUpgrade::getName, u -> u, (a, b) -> a));
      List<CampaignUpgrade> toSave = new ArrayList<>();
      for (CampaignUpgrade dto : dtos) {
        CampaignUpgrade existing = existingByName.get(dto.getName());
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
                this::upgradeValueListFingerprint,
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
        upgradeRepository.saveAll(toSave);
      }
      log.info(
          "Seed review detection (campaign upgrade) complete ({} ms).",
          System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error("Error during campaign upgrade seed-review detection", e);
    }
  }

  /** The detection-time per-field observations for one upgrade row against its seed DTO. */
  private List<SeedCollisionDetector.FieldObservation> fieldObservations(
      CampaignUpgrade existing, CampaignUpgrade dto) {
    List<SeedCollisionDetector.FieldObservation> obs = new ArrayList<>();
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "description", null, existing.getDescription(), dto.getDescription()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "type", null, existing.getType(), dto.getType()));
    obs.add(
        new SeedCollisionDetector.FieldObservation(
            "subType", null, existing.getSubType(), dto.getSubType()));
    return obs;
  }

  /** Fingerprint the incoming seed values would produce, in the canonical field order. */
  private String seedValueFingerprint(CampaignUpgrade dto) {
    return syncSupport.fingerprint(dto.getDescription(), dto.getType(), dto.getSubType());
  }

  /**
   * Re-hashes a field-value list (the {@link #fieldObservations} order) back into a row fingerprint
   * — the hash-function half of the clean-row/single-edit provenance reconstruction. Serialization
   * mirrors {@link #upgradeFingerprint(CampaignUpgrade)} exactly (see WrestlerSync's raw
   * pass-through shape).
   */
  private String upgradeValueListFingerprint(List<String> values) {
    return syncSupport.fingerprint(values.get(0), values.get(1), values.get(2));
  }

  private static String entityKey(String name) {
    return entityKeyFor(name);
  }

  /**
   * The canonical entity key for an upgrade name — shared with {@code CampaignUpgradeSeedApplier}
   * so apply-side resolution derives keys exactly as detection minted them.
   */
  static String entityKeyFor(String name) {
    return "campaign_upgrade:" + name.toLowerCase().replace(' ', '-');
  }

  /**
   * Canonical seed-owned field values for an upgrade row, in fingerprint order: description, type,
   * subType. The name is the lookup key. Public for the seed-provenance manifest generator, which
   * re-hashes fresh seed rows through the owning sync's own method.
   */
  public String upgradeFingerprint(CampaignUpgrade upgrade) {
    return syncSupport.fingerprint(
        upgrade.getDescription(), upgrade.getType(), upgrade.getSubType());
  }

  /**
   * Writes ONLY the named silent-update fields from the seed DTO onto the row — the tier-1 write
   * set of the version-change pass. Field names match {@link #fieldObservations} entries.
   */
  private void applySilentSeedFields(
      CampaignUpgrade existing, CampaignUpgrade dto, List<String> silentFields) {
    for (String field : silentFields) {
      switch (field) {
        case "description" -> existing.setDescription(dto.getDescription());
        case "type" -> existing.setType(dto.getType());
        case "subType" -> existing.setSubType(dto.getSubType());
        default ->
            throw new IllegalStateException(
                "Unknown silent-update campaign upgrade field '" + field + "'");
      }
    }
  }
}
