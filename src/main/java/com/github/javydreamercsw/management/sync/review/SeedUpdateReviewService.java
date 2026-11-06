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
package com.github.javydreamercsw.management.sync.review;

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.migration.DataMigrationHistory;
import com.github.javydreamercsw.management.migration.DataMigrationHistoryRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * Decides when seed-update review detection runs and parks detected seed-data collisions as pending
 * {@link SeedUpdateDecision} rows (seed-update review feature).
 *
 * <h2>When detection runs</h2>
 *
 * {@link #shouldRunDetection()} is true exactly when:
 *
 * <ul>
 *   <li><b>Version changed</b> — a previous app version is recorded in {@code
 *       data_migration_history} and differs from the current boot's version (resolved through the
 *       same {@link BuildProperties} source {@code DataMigrationRunner.resolveAppVersion()} reads).
 *       {@code DataInitializer.performInit()} runs the syncs <em>before</em> the migrations, so on
 *       an upgrade boot the sync phase still sees the previous boot's recorded version and the
 *       trigger fires exactly when the detector needs it.
 *   <li><b>Pending decisions exist</b> — undecided rows keep detection alive across same-version
 *       restarts, so leftover collisions are re-offered until an administrator resolves them.
 * </ul>
 *
 * <p><b>Fresh installs never detect:</b> an empty {@code data_migration_history} means no previous
 * version exists; the comparison treats "no previous version + any current version" as NOT a
 * version change. A fresh database matches the incoming seed exactly, so there is nothing to
 * collide — treating absence as "changed" would raise the gate on every first boot. The
 * version-change trigger only fires when a previous version exists and differs.
 *
 * <h2>Parking collisions</h2>
 *
 * {@link #parkFieldConflict} and {@link #parkRemoval} are idempotent on the {@code (domain_key,
 * entity_key, field_name)} PENDING coordinate: when an open decision already exists, no second row
 * is created and only the incoming-seed snapshot ({@code new_value}, {@code new_fingerprint}) is
 * refreshed to the latest values — so a later Apply writes the current release's values. The
 * database-side snapshot ({@code current_value}) and the original detection metadata stay as first
 * captured. Parking deliberately has no version-change precondition: the detector calls it whenever
 * it finds a collision, including on boots where no version history exists yet.
 *
 * <p>There is intentionally no DB-level unique constraint behind this dedupe — H2 and MySQL lack
 * partial indexes — see {@code SeedUpdateDecision}'s class javadoc.
 */
@Slf4j
@Service
public class SeedUpdateReviewService {

  /** Version recorded when {@link BuildProperties} is unavailable, mirroring the runner. */
  static final String UNKNOWN_VERSION = "unknown";

  private final SeedUpdateDecisionRepository decisionRepository;
  private final DataMigrationHistoryRepository historyRepository;
  private final Optional<BuildProperties> buildProperties;
  private final SeedReviewGate gate;

  @Autowired
  public SeedUpdateReviewService(
      final SeedUpdateDecisionRepository decisionRepository,
      final DataMigrationHistoryRepository historyRepository,
      final Optional<BuildProperties> buildProperties,
      final SeedReviewGate gate) {
    this.decisionRepository = decisionRepository;
    this.historyRepository = historyRepository;
    this.buildProperties = buildProperties;
    this.gate = gate;
  }

  /**
   * @return true when a version change is pending or undecided seed decisions exist
   */
  public boolean shouldRunDetection() {
    if (decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING) > 0) {
      log.debug("Pending seed-update decisions exist — seed review detection must run.");
      return true;
    }
    String current = resolveAppVersion();
    Optional<DataMigrationHistory> lastRecorded =
        historyRepository.findTopByOrderByExecutedAtDesc();
    if (lastRecorded.isEmpty()) {
      // Fresh install: no previous version to compare against — never treat that as a change.
      log.debug("No data migration history recorded yet — not a version change.");
      return false;
    }
    String previous = lastRecorded.get().getAppVersion();
    boolean changed = !previous.equals(current);
    if (changed) {
      log.info(
          "App version changed ({} -> {}) — seed review detection will run.", previous, current);
    }
    return changed;
  }

  /**
   * Parks a field-level seed collision as (or refreshes) a PENDING {@code FIELD_CONFLICT} decision.
   *
   * @param domainKey seed domain the colliding entity belongs to (e.g. {@code "wrestler"})
   * @param entityKey stable business key of the entity within its domain
   * @param displayName human-readable entity name for the review UI
   * @param fieldName the colliding field
   * @param currentValue the database's current value at detection time
   * @param newValue the incoming seed value at detection time
   * @param newFingerprint fingerprint of the incoming seed data, for pre-apply re-verification
   */
  public void parkFieldConflict(
      final String domainKey,
      final String entityKey,
      final String displayName,
      final String fieldName,
      final String currentValue,
      final String newValue,
      final String newFingerprint) {
    Optional<SeedUpdateDecision> existing =
        decisionRepository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            domainKey, entityKey, fieldName, SeedUpdateDecision.STATUS_PENDING);
    if (existing.isPresent()) {
      refreshIncomingSnapshot(existing.get(), newValue, newFingerprint);
      return;
    }
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(fieldName);
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    decision.setCurrentValue(currentValue);
    decision.setNewValue(newValue);
    decision.setNewFingerprint(newFingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion(resolveAppVersion());
    decisionRepository.save(decision);
    gate.refresh(); // raise the gate — an undecided decision now exists
    log.info(
        "Parked seed field conflict: {}.{} [{}] (version {})",
        domainKey,
        entityKey,
        fieldName,
        decision.getAppVersion());
  }

  /**
   * Parks a seed-entity removal as (or refreshes) a PENDING {@code REMOVAL} decision. Idempotent on
   * the same PENDING coordinate as {@link #parkFieldConflict}, with a {@code null} {@code
   * field_name} — the derived query must translate that to {@code IS NULL}.
   *
   * @param domainKey seed domain the removed entity belongs to
   * @param entityKey stable business key of the removed entity
   * @param displayName human-readable entity name for the review UI
   * @param newFingerprint fingerprint of the incoming seed data, for pre-apply re-verification
   */
  public void parkRemoval(
      final String domainKey,
      final String entityKey,
      final String displayName,
      final String newFingerprint) {
    Optional<SeedUpdateDecision> existing =
        decisionRepository.findByDomainKeyAndEntityKeyAndFieldNameAndStatus(
            domainKey, entityKey, null, SeedUpdateDecision.STATUS_PENDING);
    if (existing.isPresent()) {
      refreshIncomingSnapshot(existing.get(), null, newFingerprint);
      return;
    }
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(null); // NULL for removal decisions
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    decision.setCurrentValue(null); // NULL for removals
    decision.setNewValue(null); // NULL for removals
    decision.setNewFingerprint(newFingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion(resolveAppVersion());
    decisionRepository.save(decision);
    gate.refresh(); // raise the gate — an undecided decision now exists
    log.info(
        "Parked seed removal: {}.{} (version {})", domainKey, entityKey, decision.getAppVersion());
  }

  /**
   * @return the number of undecided seed decisions
   */
  public long pendingCount() {
    return decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING);
  }

  /**
   * @return the gate state bean — raised automatically by the {@code park*} methods; call {@link
   *     SeedReviewGate#refresh()} after applying decisions to lower it
   */
  public SeedReviewGate gate() {
    return gate;
  }

  /**
   * Refreshes the incoming-seed snapshot on an existing PENDING decision: {@code new_value} and
   * {@code new_fingerprint} move to the latest detected values (so Apply writes current-release
   * values); {@code current_value} and the original detection metadata stay as first captured.
   */
  private void refreshIncomingSnapshot(
      SeedUpdateDecision existing, String newValue, String newFingerprint) {
    existing.setNewValue(newValue);
    existing.setNewFingerprint(newFingerprint);
    decisionRepository.save(existing);
    log.debug(
        "Refreshed incoming snapshot on existing PENDING decision {}.{} [{}]",
        existing.getDomainKey(),
        existing.getEntityKey(),
        existing.getFieldName());
  }

  /**
   * @return the current app version from the same source {@code
   *     DataMigrationRunner.resolveAppVersion()} reads
   */
  private String resolveAppVersion() {
    return buildProperties.map(BuildProperties::getVersion).orElse(UNKNOWN_VERSION);
  }
}
