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
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to show template rows (seed-update review feature).
 * The entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code show_template:NAME-slug} via {@link ShowTemplateSync#entityKeyFor}) —
 * the name is the domain's stable identity (unique-constrained), and key-based reminting keeps
 * resolution rename-proof if seed naming ever moves; the {@code entity_display_name} stays the
 * exact seed name for the review UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class ShowTemplateSeedApplier implements SeedDomainApplier {

  private final ShowTemplateRepository showTemplateRepository;
  private final ShowRepository showRepository;
  private final ShowTypeRepository showTypeRepository;
  private final CommentaryTeamRepository commentaryTeamRepository;

  public ShowTemplateSeedApplier(
      final ShowTemplateRepository showTemplateRepository,
      final ShowRepository showRepository,
      final ShowTypeRepository showTypeRepository,
      final CommentaryTeamRepository commentaryTeamRepository) {
    this.showTemplateRepository = showTemplateRepository;
    this.showRepository = showRepository;
    this.showTypeRepository = showTypeRepository;
    this.commentaryTeamRepository = commentaryTeamRepository;
  }

  @Override
  public String domainKey() {
    return ShowTemplateSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findTemplate(decision)
        .ifPresentOrElse(
            template -> stamp(template, decision),
            () ->
                log.warn(
                    "Seed decision {}: show template '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findTemplate(decision)
        .ifPresentOrElse(
            template -> {
              applyFieldValue(template, decision);
              stamp(template, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: show template '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findTemplate(decision)
        .ifPresentOrElse(
            template -> {
              template.setSeedFingerprint(null);
              showTemplateRepository.save(template);
            },
            () ->
                log.warn(
                    "Seed decision {}: show template '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (shows generated from
    // the template). The fail-safe direction re-checks at delete time: a template with shows is
    // kept as custom data rather than blowing the FK mid-apply.
    findTemplate(decision)
        .ifPresentOrElse(
            template -> {
              if (hasShows(template)) {
                log.warn(
                    "Seed decision {}: show template '{}' still has shows — the row is kept as"
                        + " custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                showTemplateRepository.delete(template);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: show template '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findTemplateByKey(entityKey).map(this::hasShows).orElse(false);
  }

  /**
   * Resolves the decision's template by reminting its entity key — the key survives the admin
   * rename a tier-2 NAME collision parks around (see class doc).
   */
  private Optional<ShowTemplate> findTemplate(SeedUpdateDecision decision) {
    return findTemplateByKey(decision.getEntityKey());
  }

  private Optional<ShowTemplate> findTemplateByKey(String entityKey) {
    return showTemplateRepository.findAll().stream()
        .filter(t -> entityKey.equals(ShowTemplateSync.entityKeyFor(t.getName())))
        .findFirst();
  }

  /** Whether any show was generated from this template (the domain's real persisted FK). */
  private boolean hasShows(ShowTemplate template) {
    return template.getId() != null && showRepository.existsByTemplateId(template.getId());
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(ShowTemplate template, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> template.setDescription(value);
      case "showTypeName" ->
          showTypeRepository
              .findByName(value)
              .ifPresentOrElse(
                  template::setShowType,
                  () ->
                      log.warn(
                          "Seed decision {}: show type '{}' not found — show type not changed",
                          decision.getId(),
                          value));
      case "commentaryTeamName" ->
          commentaryTeamRepository.findAll().stream()
              .filter(t -> t.getName().equals(value))
              .findFirst()
              .ifPresentOrElse(
                  template::setCommentaryTeam,
                  () ->
                      log.warn(
                          "Seed decision {}: commentary team '{}' not found — team not changed",
                          decision.getId(),
                          value));
      case "expectedMatches" ->
          template.setExpectedMatches(value == null ? null : Integer.valueOf(value));
      case "expectedPromos" ->
          template.setExpectedPromos(value == null ? null : Integer.valueOf(value));
      case "durationDays" -> template.setDurationDays(value == null ? 1 : Integer.valueOf(value));
      case "recurrenceType" ->
          template.setRecurrenceType(
              value == null
                  ? com.github.javydreamercsw.management.domain.show.template.RecurrenceType.NONE
                  : com.github.javydreamercsw.management.domain.show.template.RecurrenceType
                      .valueOf(value));
      case "dayOfWeek" ->
          template.setDayOfWeek(value == null ? null : java.time.DayOfWeek.valueOf(value));
      case "dayOfMonth" -> template.setDayOfMonth(value == null ? null : Integer.valueOf(value));
      case "weekOfMonth" -> template.setWeekOfMonth(value == null ? null : Integer.valueOf(value));
      case "month" -> template.setMonth(value == null ? null : java.time.Month.valueOf(value));
      case "genderConstraint" ->
          template.setGenderConstraint(
              value == null
                  ? null
                  : com.github.javydreamercsw.base.domain.wrestler.Gender.valueOf(value));
      case "requiredExpansions" -> {
        template.getRequiredExpansions().clear();
        if (value != null && !value.isBlank()) {
          template.getRequiredExpansions().addAll(List.of(value.split("\n")));
        }
      }
      default ->
          throw new IllegalStateException(
              "Show template field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    showTemplateRepository.save(template);
  }

  private void stamp(ShowTemplate template, SeedUpdateDecision decision) {
    template.setSeedFingerprint(decision.getNewFingerprint());
    showTemplateRepository.save(template);
  }
}
