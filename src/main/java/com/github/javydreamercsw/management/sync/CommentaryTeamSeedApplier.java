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

import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.service.commentator.CommentaryService;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to commentary team rows (seed-update review feature).
 * The entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code commentary_team:NAME-slug} via {@link
 * CommentaryTeamSync#entityKeyFor}) — the name is the domain's stable identity, and key-based
 * reminting keeps resolution rename-proof if seed naming ever moves; the {@code
 * entity_display_name} stays the exact seed name for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class CommentaryTeamSeedApplier implements SeedDomainApplier {

  private final CommentaryTeamRepository commentaryTeamRepository;
  private final CommentaryService commentaryService;
  private final ShowRepository showRepository;
  private final ShowTemplateRepository showTemplateRepository;

  public CommentaryTeamSeedApplier(
      final CommentaryTeamRepository commentaryTeamRepository,
      final CommentaryService commentaryService,
      final ShowRepository showRepository,
      final ShowTemplateRepository showTemplateRepository) {
    this.commentaryTeamRepository = commentaryTeamRepository;
    this.commentaryService = commentaryService;
    this.showRepository = showRepository;
    this.showTemplateRepository = showTemplateRepository;
  }

  @Override
  public String domainKey() {
    return CommentaryTeamSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findTeam(decision)
        .ifPresentOrElse(
            team -> stamp(team, decision),
            () ->
                log.warn(
                    "Seed decision {}: commentary team '{}' not found — nothing to keep",
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
                    "Seed decision {}: commentary team '{}' not found — nothing to apply",
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
              commentaryTeamRepository.save(team);
            },
            () ->
                log.warn(
                    "Seed decision {}: commentary team '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (shows and show
    // templates fielding the team). The fail-safe direction re-checks at delete time: a team a
    // show or template still fields is kept as custom data rather than blowing the FK mid-apply.
    findTeam(decision)
        .ifPresentOrElse(
            team -> {
              if (hasDependents(team)) {
                log.warn(
                    "Seed decision {}: commentary team '{}' still has shows or templates — the"
                        + " row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                commentaryTeamRepository.delete(team);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: commentary team '{}' not found — nothing to remove",
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
   * The commentary team's REAL persisted FK relationships (verified against the entity model):
   * shows ({@code Show.commentaryTeam}) and show templates ({@code ShowTemplate.commentaryTeam}).
   * The team→commentator M2M join is the team's own content, not a dependent.
   */
  private boolean hasDependents(CommentaryTeam team) {
    return showRepository.existsByCommentaryTeam(team)
        || showTemplateRepository.existsByCommentaryTeam(team);
  }

  /** Resolves the decision's commentary team by reminting its entity key. */
  private Optional<CommentaryTeam> findTeam(SeedUpdateDecision decision) {
    return findTeamByKey(decision.getEntityKey());
  }

  private Optional<CommentaryTeam> findTeamByKey(String entityKey) {
    return commentaryTeamRepository.findAll().stream()
        .filter(t -> entityKey.equals(CommentaryTeamSync.entityKeyFor(t.getName())))
        .findFirst();
  }

  /**
   * Writes the decision's incoming seed value to the matching seed-owned field. The membership
   * write goes through {@code CommentaryService.createOrUpdateTeam} so member resolution stays in
   * one place; the team's name — the resolution identity — never changes here.
   */
  private void applyFieldValue(CommentaryTeam team, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "members" -> {
        List<String> names =
            value == null || value.isBlank()
                ? List.of()
                : Arrays.stream(value.split("\n")).map(String::trim).toList();
        commentaryService.createOrUpdateTeam(team.getName(), names);
      }
      default ->
          throw new IllegalStateException(
              "Commentary team field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
  }

  private void stamp(CommentaryTeam team, SeedUpdateDecision decision) {
    team.setSeedFingerprint(decision.getNewFingerprint());
    commentaryTeamRepository.save(team);
  }
}
