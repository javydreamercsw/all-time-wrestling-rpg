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
import com.github.javydreamercsw.management.event.SeedUpdateReviewPendingEvent;
import com.github.javydreamercsw.management.migration.DataMigrationHistory;
import com.github.javydreamercsw.management.migration.DataMigrationHistoryRepository;
import com.github.javydreamercsw.management.migration.DataMigrationRunner;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
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
 *
 * <h2>Batched pending notification</h2>
 *
 * {@code park*} calls arm an internal flag; {@link #notifyPending()} publishes one {@link
 * SeedUpdateReviewPendingEvent} for the whole phase when anything was parked (never per row), which
 * the inbox listener turns into a single administrator notification. PR 4's detection loop calls
 * {@code notifyPending()} once after parking completes.
 */
@Slf4j
@Service
public class SeedUpdateReviewService {

  private final SeedUpdateDecisionRepository decisionRepository;
  private final DataMigrationHistoryRepository historyRepository;
  private final Optional<BuildProperties> buildProperties;
  private final SeedReviewGate gate;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Set by the first {@code park*} call of this bean's lifetime and consumed by {@link
   * #notifyPending()}: the once-per-boot batched inbox notification fires only when the detection
   * phase actually parked (or re-parked) something — not on every boot, and never per row.
   */
  private boolean parkingHappened;

  @Autowired
  public SeedUpdateReviewService(
      final SeedUpdateDecisionRepository decisionRepository,
      final DataMigrationHistoryRepository historyRepository,
      final Optional<BuildProperties> buildProperties,
      final SeedReviewGate gate,
      final ApplicationEventPublisher eventPublisher) {
    this.decisionRepository = decisionRepository;
    this.historyRepository = historyRepository;
    this.buildProperties = buildProperties;
    this.gate = gate;
    this.eventPublisher = eventPublisher;
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
    parkingHappened = true;
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
    parkingHappened = true;
    gate.refresh(); // raise the gate — an undecided decision now exists
    log.info(
        "Parked seed removal: {}.{} (version {})", domainKey, entityKey, decision.getAppVersion());
  }

  /**
   * Closes the detection/parking phase of a boot: publishes exactly one {@link
   * SeedUpdateReviewPendingEvent} when this boot's phase parked (or re-parked) at least one
   * collision — the inbox listener turns it into a single batched administrator notification. A
   * boot whose phase parked nothing publishes nothing.
   *
   * <p>The once-per-boot guarantee is structural: the caller invokes this exactly once, after the
   * detection loop finishes (PR 4 wires it there). Repeated invocations are additionally inert for
   * the same bean instance — the flag is consumed on first publish — and park* calls arm only their
   * own service instance, so a restarted context starts with a clean flag.
   */
  public void notifyPending() {
    if (!parkingHappened) {
      log.debug("No seed collisions parked this phase — no pending-review notification.");
      return;
    }
    parkingHappened = false;
    int count = (int) pendingCount();
    eventPublisher.publishEvent(new SeedUpdateReviewPendingEvent(this, count, resolveAppVersion()));
    log.info("Published seed-update review pending event: {} decision(s) pending.", count);
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
   * Operator escape hatch (invoked by {@code SeedReviewConfig.forceResolveIfConfigured()} from
   * {@code DataInitializer.performInit()} when {@code seed.review.force-resolve=keep-all}):
   * force-decides every PENDING decision as keep-mine and lowers the gate, logged WARN.
   *
   * <p><b>This is an operator escape, not a real apply.</b> Keep-mine semantics are a no-op on
   * entity data — nothing in the database changes — and unlike the review UI's apply flow, this
   * hatch deliberately does NOT re-stamp the rows' seed fingerprints. The force-decided rows
   * therefore re-collide on the next upgrade boot (fail-safe direction: the operator is asked
   * again, no seed value is ever silently adopted).
   *
   * <p>Only PENDING rows are touched: rows already in an {@code APPLIED_*} state keep the outcome
   * the administrator chose, including their snapshots.
   */
  public void forceResolvePendingAsKeepMine() {
    List<SeedUpdateDecision> pending =
        decisionRepository.findByStatusOrderByIdAsc(SeedUpdateDecision.STATUS_PENDING);
    if (pending.isEmpty()) {
      log.info("seed.review.force-resolve=keep-all — no pending seed decisions to force-resolve.");
      return;
    }
    log.warn(
        "FORCING {} pending seed decision(s) to keep-mine (seed.review.force-resolve=keep-all)."
            + " This is an operator escape: values are NOT applied and seed fingerprints are NOT"
            + " re-stamped, so these collisions will re-appear on the next upgrade.",
        pending.size());
    for (SeedUpdateDecision decision : pending) {
      decision.setStatus(SeedUpdateDecision.STATUS_APPLIED_KEEP);
      decisionRepository.save(decision);
      log.warn(
          "Force-resolved seed decision {}.{} [{}] as keep-mine",
          decision.getDomainKey(),
          decision.getEntityKey(),
          decision.getFieldName());
    }
    gate.refresh(); // lower the gate — no undecided decisions remain
  }

  /**
   * Refreshes the incoming-seed snapshot on an existing PENDING decision: {@code new_value} and
   * {@code new_fingerprint} move to the latest detected values (so Apply writes current-release
   * values); {@code current_value} and {@code entity_display_name} deliberately stay as first
   * captured — they are first-detection audit records (the review UI shows the state at the moment
   * the collision was found), and the Apply write path reads live entity state, so re-park
   * staleness there never affects what gets written.
   */
  private void refreshIncomingSnapshot(
      SeedUpdateDecision existing, String newValue, String newFingerprint) {
    existing.setNewValue(newValue);
    existing.setNewFingerprint(newFingerprint);
    decisionRepository.save(existing);
    gate.refresh(); // a re-park means an undecided decision exists — the gate must end up raised
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
    return buildProperties
        .map(BuildProperties::getVersion)
        .orElse(DataMigrationRunner.UNKNOWN_VERSION);
  }
}
