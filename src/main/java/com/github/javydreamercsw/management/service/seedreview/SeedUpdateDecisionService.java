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
package com.github.javydreamercsw.management.service.seedreview;

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional core of the seed-update review feature: reads pending {@link SeedUpdateDecision}
 * rows for the review tree UI and applies an administrator's choices per domain through the
 * pluggable {@link SeedDomainApplier} SPI.
 *
 * <h2>Apply contract</h2>
 *
 * {@link #apply(Map)} is <b>all-or-nothing</b>: every PENDING row must appear in the decision map
 * before anything is dispatched or written — the review UI submits one complete batch. All applier
 * dispatch, status transitions, and saves happen inside one transaction ({@code @Transactional} on
 * the public method): a failure anywhere rolls back every decision write (appliers run inside the
 * transaction and write entities through the same transactional {@code EntityManager}).
 *
 * <h2>Keep-mine stamp contract</h2>
 *
 * On {@code KEEP_MINE} this service preserves the decision row's detection-time {@code
 * new_fingerprint} unchanged and hands the full row to the applier — the applier stamps that
 * fingerprint on the entity so a later boot recognizes the kept value as matching the release it
 * was detected against.
 *
 * <h2>Grouping contract</h2>
 *
 * {@link #pendingGrouped()} returns the pending rows ordered by id (id order = detection order =
 * domain grouping order). The review UI does its own domain/entity grouping on top of the ordered
 * list; this service's contract is just the stable, ordered pending queue.
 *
 * <h2>Authorization</h2>
 *
 * {@link #apply(Map)} and {@link #approveEverythingExceptRemovals()} carry
 * {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} as defense-in-depth behind the review view's
 * {@code @RolesAllowed("ADMIN")} and the navigation freeze, so the write path stays closed even if
 * a future caller bypasses the UI. The read methods ({@link #pendingGrouped()}, {@link
 * #pendingCount()}) are unannotated: they have no REST surface and their only consumer is the
 * already-{@code @RolesAllowed("ADMIN")} review view.
 */
@Slf4j
@Service
public class SeedUpdateDecisionService {

  private final SeedUpdateDecisionRepository decisionRepository;
  private final Map<String, SeedDomainApplier> appliersByDomain;
  private final SeedReviewGate gate;

  /**
   * Collects every registered {@link SeedDomainApplier} bean into a map keyed by {@link
   * SeedDomainApplier#domainKey()}. Empty until the follow-up PR registers per-domain appliers —
   * applying against an unregistered domain then fails loud via {@link #apply(Map)}.
   */
  public SeedUpdateDecisionService(
      final SeedUpdateDecisionRepository decisionRepository,
      final List<SeedDomainApplier> appliers,
      final SeedReviewGate gate) {
    this.decisionRepository = decisionRepository;
    this.appliersByDomain =
        appliers.stream()
            .collect(
                Collectors.toUnmodifiableMap(SeedDomainApplier::domainKey, Function.identity()));
    this.gate = gate;
  }

  /**
   * @return all PENDING decisions in id order — id order = detection order, so the review UI's
   *     domain/entity grouping is stable across calls. See the class javadoc's grouping contract.
   */
  public List<SeedUpdateDecision> pendingGrouped() {
    return decisionRepository.findByStatusOrderByIdAsc(SeedUpdateDecision.STATUS_PENDING);
  }

  /**
   * @return the number of undecided seed decisions
   */
  public long pendingCount() {
    return decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING);
  }

  /**
   * Applies an administrator's decisions to the parked seed collisions. All-or-nothing: every
   * PENDING row must be covered by {@code decisions} or nothing is applied, and every row must
   * resolve to an applier (with removable removals) in the validation phase BEFORE any dispatch —
   * so a batch containing an unresolvable row fails without writing anything, instead of failing
   * mid-dispatch on top of earlier appliers' writes (rollback still covers the mid-dispatch case,
   * but failing before any write is cheaper and keeps appliers side-effect-free on failure).
   * Removal decisions are refused when the applier reports dependents — the decision stays PENDING
   * so the administrator can choose again. After all rows are applied the review gate re-reads its
   * snapshot (lowering the gate once no undecided decisions remain).
   *
   * @param decisions pending-decision id to chosen outcome; must cover every PENDING row
   * @throws IllegalStateException when a pending id is missing from the map (nothing applied), the
   *     decision's domain has no registered applier, or a removal has dependents
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Transactional
  public void apply(Map<Long, SeedFieldDecision> decisions) {
    List<SeedUpdateDecision> pending = pendingGrouped();
    // All-or-nothing validation before any dispatch or write: the review UI submits one complete
    // batch, and a partial batch must not leave the queue half-applied.
    List<Long> missing =
        pending.stream()
            .map(SeedUpdateDecision::getId)
            .filter(id -> !decisions.containsKey(id))
            .toList();
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "All-or-nothing apply violated: pending seed decision(s) missing from the decision map: "
              + missing
              + ". Every pending decision must be decided before applying.");
    }
    // Second validation pass: resolve every applier and pre-check removals BEFORE dispatching
    // anything, so an unresolvable row never leaves earlier appliers' writes behind (the
    // transaction would roll back, but pre-validation keeps appliers side-effect-free on
    // refusal).
    Map<SeedUpdateDecision, SeedDomainApplier> resolved = new LinkedHashMap<>();
    for (SeedUpdateDecision decision : pending) {
      SeedDomainApplier applier = appliersByDomain.get(decision.getDomainKey());
      if (applier == null) {
        throw new IllegalStateException(
            "No SeedDomainApplier registered for seed domain '"
                + decision.getDomainKey()
                + "' (decision "
                + decision.getId()
                + ") — cannot apply the administrator's choice.");
      }
      SeedFieldDecision fieldDecision = decisions.get(decision.getId());
      if (fieldDecision == SeedFieldDecision.REMOVE
          && applier.hasDependents(decision.getEntityKey())) {
        throw new IllegalStateException(
            "Cannot remove '"
                + decision.getEntityKey()
                + "': other data still references it. Resolve or remove its dependents first, then "
                + "re-apply (the decision stays pending).");
      }
      resolved.put(decision, applier);
    }
    for (Map.Entry<SeedUpdateDecision, SeedDomainApplier> entry : resolved.entrySet()) {
      dispatch(entry.getValue(), entry.getKey(), decisions.get(entry.getKey().getId()));
    }
    // The appliers may have written entities and/or stamps; the gate must re-read its snapshot.
    gate.refresh();
  }

  /**
   * Builds the decisions map for an approve-everything gesture: every PENDING {@code
   * FIELD_CONFLICT} row maps to {@code USE_NEW}; {@code REMOVAL} rows are excluded — removing data
   * the seed no longer carries requires explicit per-entity confirmation and can never ride on an
   * approve-all. The map is returned for the UI to pass back to {@link #apply(Map)} — the Apply
   * button stays the single write point; nothing is dispatched here.
   *
   * @return pending-decision id to {@code USE_NEW} for every pending field conflict (removals
   *     excluded, so a subsequent {@link #apply(Map)} still refuses until they are decided)
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public Map<Long, SeedFieldDecision> approveEverythingExceptRemovals() {
    Map<Long, SeedFieldDecision> decisions =
        pendingGrouped().stream()
            .filter(
                decision ->
                    SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT.equals(
                        decision.getDecisionType()))
            .collect(
                Collectors.toMap(SeedUpdateDecision::getId, decision -> SeedFieldDecision.USE_NEW));
    log.info(
        "Approve-everything prepared {} seed field conflict(s) as use-new (removals excluded, "
            + "still pending explicit per-entity choices)",
        decisions.size());
    return decisions;
  }

  /**
   * Dispatches one pre-validated row to its domain applier, moves the row to the matching terminal
   * status, and saves it. Validation (applier resolution, removal dependents) already happened in
   * {@link #apply(Map)}'s pre-dispatch phase — this method never refuses.
   */
  private void dispatch(
      SeedDomainApplier applier, SeedUpdateDecision decision, SeedFieldDecision fieldDecision) {
    switch (fieldDecision) {
      case KEEP_MINE -> applier.applyKeep(decision);
      case USE_NEW -> applier.applyNew(decision);
      case KEEP_AS_CUSTOM -> applier.applyKeepAsCustom(decision);
      case REMOVE -> applier.applyRemove(decision);
    }
    decision.setStatus(terminalStatus(fieldDecision));
    decisionRepository.save(decision);
    log.info(
        "Applied seed decision {}.{} [{}] as {} ({} -> {})",
        decision.getDomainKey(),
        decision.getEntityKey(),
        decision.getFieldName(),
        fieldDecision,
        decision.getCurrentValue(),
        decision.getNewValue());
  }

  /**
   * @return the terminal {@code APPLIED_*} status for the chosen outcome
   */
  private String terminalStatus(SeedFieldDecision fieldDecision) {
    return switch (fieldDecision) {
      case KEEP_MINE -> SeedUpdateDecision.STATUS_APPLIED_KEEP;
      case USE_NEW -> SeedUpdateDecision.STATUS_APPLIED_NEW;
      case KEEP_AS_CUSTOM -> SeedUpdateDecision.STATUS_APPLIED_KEEP_AS_CUSTOM;
      case REMOVE -> SeedUpdateDecision.STATUS_APPLIED_REMOVE;
    };
  }
}
