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
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to team rows (seed-update review feature). The entity
 * is resolved through the decision row's {@code entity_key}, reminted exactly the way detection
 * minted it ({@code team:NAME-slug} via {@link TeamSync#entityKeyFor}) — the name is the domain's
 * stable identity, and key-based reminting keeps resolution rename-proof if seed naming ever moves;
 * the {@code entity_display_name} stays the exact seed name for the review UI and is never
 * slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class TeamSeedApplier implements SeedDomainApplier {

  private final TeamRepository teamRepository;
  private final TournamentEntryRepository tournamentEntryRepository;
  private final TrophyReignRepository trophyReignRepository;
  private final TitleReignRepository titleReignRepository;

  public TeamSeedApplier(
      final TeamRepository teamRepository,
      final TournamentEntryRepository tournamentEntryRepository,
      final TrophyReignRepository trophyReignRepository,
      final TitleReignRepository titleReignRepository) {
    this.teamRepository = teamRepository;
    this.tournamentEntryRepository = tournamentEntryRepository;
    this.trophyReignRepository = trophyReignRepository;
    this.titleReignRepository = titleReignRepository;
  }

  @Override
  public String domainKey() {
    return TeamSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findTeam(decision)
        .ifPresentOrElse(
            team -> stamp(team, decision),
            () ->
                log.warn(
                    "Seed decision {}: team '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findTeam(decision)
        .ifPresentOrElse(
            team -> {
              applyFieldValue(team, decision);
              stamp(team, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: team '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findTeam(decision)
        .ifPresentOrElse(
            team -> {
              team.setSeedFingerprint(null);
              teamRepository.save(team);
            },
            () ->
                log.warn(
                    "Seed decision {}: team '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (bracket entries, trophy
    // reigns, team-title reigns). The fail-safe direction re-checks at delete time: a team with
    // any of them is kept as custom data rather than blowing the FK mid-apply.
    findTeam(decision)
        .ifPresentOrElse(
            team -> {
              if (hasDependents(team)) {
                log.warn(
                    "Seed decision {}: team '{}' still has entries or reigns — the row is kept as"
                        + " custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                teamRepository.delete(team);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: team '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findTeamByKey(entityKey).map(this::hasDependents).orElse(false);
  }

  /**
   * The team's REAL persisted FK relationships (verified against the entity model): bracket entries
   * ({@code TournamentEntry.team}), trophy reigns ({@code TrophyReign.winnerTeam}), and team-title
   * reigns ({@code TitleReign.team}). Membership is on the team's own member rows, not a dependent.
   */
  private boolean hasDependents(Team team) {
    return !tournamentEntryRepository.findByTeam(team).isEmpty()
        || !trophyReignRepository.findByWinnerTeam(team).isEmpty()
        || !titleReignRepository.findByTeam(team).isEmpty();
  }

  /** Resolves the decision's team by reminting its entity key. */
  private Optional<Team> findTeam(SeedUpdateDecision decision) {
    return findTeamByKey(decision.getEntityKey());
  }

  private Optional<Team> findTeamByKey(String entityKey) {
    return teamRepository.findAll().stream()
        .filter(t -> entityKey.equals(TeamSync.entityKeyFor(t.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Team team, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> team.setDescription(value);
      case "teamSize" -> team.setTeamSize(value == null ? TeamSize.DUO : TeamSize.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Team field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    teamRepository.save(team);
  }

  private void stamp(Team team, SeedUpdateDecision decision) {
    team.setSeedFingerprint(decision.getNewFingerprint());
    teamRepository.save(team);
  }
}
