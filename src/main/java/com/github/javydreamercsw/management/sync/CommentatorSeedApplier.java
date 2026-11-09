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

import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.Commentator;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to commentator rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code commentator:NPC-NAME-slug} via {@link CommentatorSync#entityKeyFor})
 * — the NPC name is the domain's stable identity, and key-based reminting keeps resolution
 * rename-proof if seed naming ever moves; the {@code entity_display_name} stays the exact seed name
 * for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CommentatorSeedApplier implements SeedDomainApplier {

  private final CommentatorRepository commentatorRepository;
  private final CommentaryTeamRepository commentaryTeamRepository;

  public CommentatorSeedApplier(
      final CommentatorRepository commentatorRepository,
      final CommentaryTeamRepository commentaryTeamRepository) {
    this.commentatorRepository = commentatorRepository;
    this.commentaryTeamRepository = commentaryTeamRepository;
  }

  @Override
  public String domainKey() {
    return CommentatorSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findCommentator(decision)
        .ifPresentOrElse(
            commentator -> stamp(commentator, decision),
            () ->
                log.warn(
                    "Seed decision {}: commentator '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findCommentator(decision)
        .ifPresentOrElse(
            commentator -> {
              applyFieldValue(commentator, decision);
              stamp(commentator, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: commentator '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findCommentator(decision)
        .ifPresentOrElse(
            commentator -> {
              commentator.setSeedFingerprint(null);
              commentatorRepository.save(commentator);
            },
            () ->
                log.warn(
                    "Seed decision {}: commentator '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (commentary teams
    // fielding the commentator). The fail-safe direction re-checks at delete time: a commentator
    // on a team is kept as custom data rather than blowing the FK mid-apply.
    findCommentator(decision)
        .ifPresentOrElse(
            commentator -> {
              if (hasDependents(commentator)) {
                log.warn(
                    "Seed decision {}: commentator '{}' is still on a commentary team — the row"
                        + " is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                commentatorRepository.delete(commentator);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: commentator '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findCommentatorByKey(entityKey).map(this::hasDependents).orElse(false);
  }

  /**
   * The commentator's REAL persisted FK relationship (verified against the entity model): the
   * {@code commentary_team_members} join table ({@code CommentaryTeam.commentators}). The NPC
   * back-reference is the row's own identity, not a dependent.
   */
  private boolean hasDependents(Commentator commentator) {
    return commentaryTeamRepository.findAll().stream()
        .anyMatch(team -> team.getCommentators().contains(commentator));
  }

  /** Resolves the decision's commentator by reminting its entity key. */
  private Optional<Commentator> findCommentator(SeedUpdateDecision decision) {
    return findCommentatorByKey(decision.getEntityKey());
  }

  private Optional<Commentator> findCommentatorByKey(String entityKey) {
    return commentatorRepository.findAll().stream()
        .filter(
            c ->
                c.getNpc() != null
                    && entityKey.equals(CommentatorSync.entityKeyFor(c.getNpc().getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Commentator commentator, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "style" -> commentator.setStyle(value);
      case "catchphrase" -> commentator.setCatchphrase(value);
      case "personaDescription" -> commentator.setPersonaDescription(value);
      default ->
          throw new IllegalStateException(
              "Commentator field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    commentatorRepository.save(commentator);
  }

  private void stamp(Commentator commentator, SeedUpdateDecision decision) {
    commentator.setSeedFingerprint(decision.getNewFingerprint());
    commentatorRepository.save(commentator);
  }
}
