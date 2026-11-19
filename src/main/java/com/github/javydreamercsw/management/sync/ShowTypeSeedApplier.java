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
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to show type rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code show_type:NAME-slug} via {@link ShowTypeSync#entityKeyFor}) — the
 * name is the domain's stable identity, and key-based reminting keeps resolution rename-proof if
 * seed naming ever moves; the {@code entity_display_name} stays the exact seed name for the review
 * UI and is never slug-reversed.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class ShowTypeSeedApplier implements SeedDomainApplier {

  private final ShowTypeRepository showTypeRepository;
  private final ShowRepository showRepository;
  private final ShowTemplateRepository showTemplateRepository;

  public ShowTypeSeedApplier(
      final ShowTypeRepository showTypeRepository,
      final ShowRepository showRepository,
      final ShowTemplateRepository showTemplateRepository) {
    this.showTypeRepository = showTypeRepository;
    this.showRepository = showRepository;
    this.showTemplateRepository = showTemplateRepository;
  }

  @Override
  public String domainKey() {
    return ShowTypeSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findShowType(decision)
        .ifPresentOrElse(
            showType -> stamp(showType, decision),
            () ->
                log.warn(
                    "Seed decision {}: show type '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findShowType(decision)
        .ifPresentOrElse(
            showType -> {
              applyFieldValue(showType, decision);
              stamp(showType, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: show type '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findShowType(decision)
        .ifPresentOrElse(
            showType -> {
              showType.setSeedFingerprint(null);
              showTypeRepository.save(showType);
            },
            () ->
                log.warn(
                    "Seed decision {}: show type '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (shows booked on the
    // type, templates referencing it). The fail-safe direction re-checks at delete time: a type
    // still referenced is kept as custom data rather than blowing the FK mid-apply.
    findShowType(decision)
        .ifPresentOrElse(
            showType -> {
              if (showRepository.existsByType(showType)
                  || !showTemplateRepository.findByShowType(showType).isEmpty()) {
                log.warn(
                    "Seed decision {}: show type '{}' still has shows or templates — the row is"
                        + " kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                showTypeRepository.delete(showType);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: show type '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findShowTypeByKey(entityKey)
        .map(
            showType ->
                showRepository.existsByType(showType)
                    || !showTemplateRepository.findByShowType(showType).isEmpty())
        .orElse(false);
  }

  /**
   * Resolves the decision's show type by reminting its entity key — the key survives the admin
   * rename a tier-2 NAME collision parks around (see class doc).
   */
  private Optional<ShowType> findShowType(SeedUpdateDecision decision) {
    return findShowTypeByKey(decision.getEntityKey());
  }

  private Optional<ShowType> findShowTypeByKey(String entityKey) {
    return showTypeRepository.findAll().stream()
        .filter(st -> entityKey.equals(ShowTypeSync.entityKeyFor(st.getName())))
        .findFirst();
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(ShowType showType, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> showType.setDescription(value);
      case "expectedMatches" ->
          showType.setExpectedMatches(value == null ? 0 : Integer.parseInt(value));
      case "expectedPromos" ->
          showType.setExpectedPromos(value == null ? 0 : Integer.parseInt(value));
      case "category" ->
          showType.setCategory(value == null ? ShowCategory.OTHER : ShowCategory.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Show type field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    showTypeRepository.save(showType);
  }

  private void stamp(ShowType showType, SeedUpdateDecision decision) {
    showType.setSeedFingerprint(decision.getNewFingerprint());
    showTypeRepository.save(showType);
  }
}
