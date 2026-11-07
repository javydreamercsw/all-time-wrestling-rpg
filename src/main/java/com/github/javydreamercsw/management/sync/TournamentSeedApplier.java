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

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to catalog tournament rows (seed-update review
 * feature). The entity is resolved through the decision row's {@code entity_key}, reminted exactly
 * the way detection minted it ({@code tournament:CODE} via {@link TournamentSync#entityKeyFor}) —
 * the code is the catalog's stable identity and the name is seed-owned content (a collision
 * target), so name-based lookup can never resolve a tier-2 name collision; the {@code
 * entity_display_name} stays the exact seed name for the review UI.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting. Lifecycle state (status, entries,
 * rounds) is never touched by any verb.
 */
@Slf4j
@Component
public class TournamentSeedApplier implements SeedDomainApplier {

  private final TournamentRepository tournamentRepository;
  private final SegmentRuleRepository segmentRuleRepository;

  public TournamentSeedApplier(
      final TournamentRepository tournamentRepository,
      final SegmentRuleRepository segmentRuleRepository) {
    this.tournamentRepository = tournamentRepository;
    this.segmentRuleRepository = segmentRuleRepository;
  }

  @Override
  public String domainKey() {
    return TournamentSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findTournament(decision)
        .ifPresentOrElse(
            tournament -> stamp(tournament, decision),
            () ->
                log.warn(
                    "Seed decision {}: tournament '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findTournament(decision)
        .ifPresentOrElse(
            tournament -> {
              applyFieldValue(tournament, decision);
              stamp(tournament, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: tournament '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findTournament(decision)
        .ifPresentOrElse(
            tournament -> {
              tournament.setSeedFingerprint(null);
              tournamentRepository.save(tournament);
            },
            () ->
                log.warn(
                    "Seed decision {}: tournament '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (entries, rounds, template
    // assignments, briefcase opportunities). The fail-safe direction re-checks at delete time: a
    // tournament with lifecycle state is kept as custom data rather than blowing the FK mid-apply.
    findTournament(decision)
        .ifPresentOrElse(
            tournament -> {
              if (hasLifecycleState(tournament)) {
                log.warn(
                    "Seed decision {}: tournament '{}' has entries, rounds or other lifecycle"
                        + " state — the row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                tournamentRepository.delete(tournament);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: tournament '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findTournamentByKey(entityKey).map(this::hasLifecycleState).orElse(false);
  }

  /**
   * Resolves the decision's tournament by reminting its entity key — the code survives the admin
   * rename a tier-2 NAME collision parks around (see class doc).
   */
  private Optional<Tournament> findTournament(SeedUpdateDecision decision) {
    return findTournamentByKey(decision.getEntityKey());
  }

  private Optional<Tournament> findTournamentByKey(String entityKey) {
    // The rules collection is read by the apply verbs (allowedRules write), so rows load with it
    // eagerly; entries/rounds stay lazy (the lifecycle check uses only isEmpty on the proxies,
    // guarded inside the apply service's transaction).
    return tournamentRepository.findAllWithRules().stream()
        .filter(
            t -> t.getCode() != null && entityKey.equals(TournamentSync.entityKeyFor(t.getCode())))
        .findFirst();
  }

  /**
   * Whether the tournament carries gameplay state a catalog removal must not destroy: bracket
   * entries, rounds, a host-show pairing, or a briefcase opportunity earned from it.
   */
  private boolean hasLifecycleState(Tournament tournament) {
    return !tournament.getEntries().isEmpty()
        || !tournament.getRounds().isEmpty()
        || tournament.getPayoffShow() != null;
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Tournament tournament, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "name" -> tournament.setName(value);
      case "formatId" -> {
        // The format is locked once a bracket exists (the service's createOrUpdateTournament rule)
        // — a running tournament keeps its format; the decision's row still reaches its terminal
        // status through the apply service.
        if (tournament.getEntries().isEmpty()) {
          tournament.setFormatId(value);
        } else {
          log.warn(
              "Seed decision {}: tournament '{}' has entries — locked format '{}' kept",
              decision.getId(),
              tournament.getCode(),
              tournament.getFormatId());
        }
      }
      case "defaultEntrantCount" ->
          tournament.setDefaultEntrantCount(value == null ? null : Integer.valueOf(value));
      case "allowedRules" -> tournament.setAllowedRules(new ArrayList<>(resolveRules(value)));
      default ->
          throw new IllegalStateException(
              "Tournament field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    tournamentRepository.save(tournament);
  }

  /** Resolves a newline-joined rule-name pool back to rule entities (unknown names skipped). */
  private List<SegmentRule> resolveRules(String value) {
    List<SegmentRule> rules = new ArrayList<>();
    if (value == null || value.isBlank()) {
      return rules;
    }
    for (String line : value.split("\n")) {
      if (!line.isBlank()) {
        segmentRuleRepository.findByName(line).ifPresent(rules::add);
      }
    }
    return rules;
  }

  private void stamp(Tournament tournament, SeedUpdateDecision decision) {
    tournament.setSeedFingerprint(decision.getNewFingerprint());
    tournamentRepository.save(tournament);
  }
}
