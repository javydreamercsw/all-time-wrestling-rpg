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

import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntryRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to outcome matrix rows — both row families (matrix
 * headers keyed {@code matrix:NAME-slug} and their entries keyed {@code entry:NAME-slug#roll}) —
 * for the seed-update review feature. Each entity is resolved through the decision row's {@code
 * entity_key}, reminted exactly the way detection minted it (via {@link
 * OutcomeMatrixSync#matrixEntityKeyFor} / {@link OutcomeMatrixSync#entryEntityKeyFor}) — the name
 * plus roll is the entry's stable identity, and key-based reminting keeps resolution rename-proof
 * if seed naming ever moves; the {@code entity_display_name} stays the exact seed name for the
 * review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class OutcomeMatrixSeedApplier implements SeedDomainApplier {

  private final OutcomeMatrixRepository matrixRepository;
  private final OutcomeMatrixEntryRepository entryRepository;

  public OutcomeMatrixSeedApplier(
      final OutcomeMatrixRepository matrixRepository,
      final OutcomeMatrixEntryRepository entryRepository) {
    this.matrixRepository = matrixRepository;
    this.entryRepository = entryRepository;
  }

  @Override
  public String domainKey() {
    return OutcomeMatrixSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findMatrix(decision)
        .ifPresentOrElse(
            matrix -> stamp(matrix, decision),
            () ->
                findEntry(decision)
                    .ifPresentOrElse(
                        entry -> stamp(entry, decision),
                        () ->
                            log.warn(
                                "Seed decision {}: outcome matrix row '{}' not found — nothing to"
                                    + " keep",
                                decision.getId(),
                                decision.getEntityDisplayName())));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findMatrix(decision)
        .ifPresentOrElse(
            matrix -> {
              applyMatrixFieldValue(matrix, decision);
              stamp(matrix, decision);
            },
            () ->
                findEntry(decision)
                    .ifPresentOrElse(
                        entry -> {
                          applyEntryFieldValue(entry, decision);
                          stamp(entry, decision);
                        },
                        () ->
                            log.warn(
                                "Seed decision {}: outcome matrix row '{}' not found — nothing to"
                                    + " apply",
                                decision.getId(),
                                decision.getEntityDisplayName())));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findMatrix(decision)
        .ifPresentOrElse(
            matrix -> {
              matrix.setSeedFingerprint(null);
              matrixRepository.save(matrix);
            },
            () ->
                findEntry(decision)
                    .ifPresentOrElse(
                        entry -> {
                          entry.setSeedFingerprint(null);
                          entryRepository.save(entry);
                        },
                        () ->
                            log.warn(
                                "Seed decision {}: outcome matrix row '{}' not found — nothing to"
                                    + " mark custom",
                                decision.getId(),
                                decision.getEntityDisplayName())));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (a matrix other entries
    // still redirect to; an entry row is leaf content). The fail-safe direction re-checks at
    // delete time: a redirect-target matrix is kept as custom data rather than blowing the FK
    // mid-apply.
    if (decision.getEntityKey().startsWith("matrix:")) {
      findMatrix(decision)
          .ifPresentOrElse(
              matrix -> {
                if (hasDependents(decision.getEntityKey())) {
                  log.warn(
                      "Seed decision {}: outcome matrix '{}' still has redirecting entries — the"
                          + " row is kept as custom data",
                      decision.getId(),
                      decision.getEntityDisplayName());
                } else {
                  matrixRepository.delete(matrix);
                }
              },
              () ->
                  log.warn(
                      "Seed decision {}: outcome matrix '{}' not found — nothing to remove",
                      decision.getId(),
                      decision.getEntityDisplayName()));
      return;
    }
    findEntry(decision)
        .ifPresentOrElse(
            entryRepository::delete,
            () ->
                log.warn(
                    "Seed decision {}: outcome matrix entry '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it. Entry rows are leaf content — no other row references them.
    if (!entityKey.startsWith("matrix:")) {
      return false;
    }
    return findMatrixByKey(entityKey)
        .map(
            matrix ->
                entryRepository.findAllWithMatrixAndRedirect().stream()
                    .anyMatch(e -> e.isRedirect() && matrix.equals(e.getRedirectToMatrix())))
        .orElse(false);
  }

  /** Resolves the decision's matrix row by reminting its entity key. */
  private Optional<OutcomeMatrix> findMatrix(SeedUpdateDecision decision) {
    if (!decision.getEntityKey().startsWith("matrix:")) {
      return Optional.empty();
    }
    return findMatrixByKey(decision.getEntityKey());
  }

  private Optional<OutcomeMatrix> findMatrixByKey(String entityKey) {
    return matrixRepository.findAll().stream()
        .filter(m -> entityKey.equals(OutcomeMatrixSync.matrixEntityKeyFor(m.getName())))
        .findFirst();
  }

  /** Resolves the decision's entry row by reminting its entity key. */
  private Optional<OutcomeMatrixEntry> findEntry(SeedUpdateDecision decision) {
    if (decision.getEntityKey().startsWith("matrix:")) {
      return Optional.empty();
    }
    return findEntryByKey(decision.getEntityKey());
  }

  private Optional<OutcomeMatrixEntry> findEntryByKey(String entityKey) {
    // Entry keys slug the owning matrix's name (entry:NAME-slug#roll) — match against every
    // entry's reminted key.
    return entryRepository.findAllWithMatrixAndRedirect().stream()
        .filter(
            e ->
                entityKey.equals(
                    OutcomeMatrixSync.entryEntityKeyFor(e.getMatrix().getName(), e.getDiceRoll())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching matrix-header field. */
  private void applyMatrixFieldValue(OutcomeMatrix matrix, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> matrix.setDescription(value);
      case "category" -> matrix.setCategory(OutcomeMatrixCategory.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Outcome matrix field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    matrixRepository.save(matrix);
  }

  /** Writes the decision's incoming seed value to the matching entry field. */
  private void applyEntryFieldValue(OutcomeMatrixEntry entry, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "templateText" -> entry.setTemplateText(value);
      case "heatDelta" -> entry.setHeatDelta(value == null ? null : Integer.valueOf(value));
      case "fanDelta" -> entry.setFanDelta(value == null ? null : Long.valueOf(value));
      case "tvGradeDelta" -> entry.setTvGradeDelta(value == null ? null : Integer.valueOf(value));
      case "grudgeGradeDelta" ->
          entry.setGrudgeGradeDelta(value == null ? null : Integer.valueOf(value));
      case "injuryCaused" -> entry.setInjuryCaused(Boolean.parseBoolean(value));
      case "redirectToMatrix" -> applyRedirect(entry, value);
      default ->
          throw new IllegalStateException(
              "Outcome matrix entry field '"
                  + decision.getFieldName()
                  + "' is not a seed-owned field");
    }
    entryRepository.save(entry);
  }

  /** Resolves the redirect target by seed name, mirroring the FULL path's second pass. */
  private void applyRedirect(OutcomeMatrixEntry entry, String value) {
    if (value == null || value.isBlank()) {
      entry.setRedirectToMatrix(null);
      return;
    }
    matrixRepository
        .findByNameIgnoreCase(value)
        .ifPresentOrElse(
            entry::setRedirectToMatrix,
            () ->
                log.warn(
                    "Seed decision apply: redirect matrix '{}' not found — redirect not changed",
                    value));
  }

  private void stamp(OutcomeMatrix matrix, SeedUpdateDecision decision) {
    matrix.setSeedFingerprint(decision.getNewFingerprint());
    matrixRepository.save(matrix);
  }

  private void stamp(OutcomeMatrixEntry entry, SeedUpdateDecision decision) {
    entry.setSeedFingerprint(decision.getNewFingerprint());
    entryRepository.save(entry);
  }
}
