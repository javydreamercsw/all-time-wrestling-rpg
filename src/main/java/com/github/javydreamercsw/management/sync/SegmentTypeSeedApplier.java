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
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to segment type rows (seed-update review feature).
 * The entity is resolved through the decision row's {@code entity_key}, reminted exactly the way
 * detection minted it ({@code segment_type:NAME-slug} via {@link SegmentTypeSync#entityKeyFor}) —
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
public class SegmentTypeSeedApplier implements SeedDomainApplier {

  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRepository segmentRepository;
  private final ShowTemplateRepository showTemplateRepository;

  public SegmentTypeSeedApplier(
      final SegmentTypeRepository segmentTypeRepository,
      final SegmentRepository segmentRepository,
      final ShowTemplateRepository showTemplateRepository) {
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRepository = segmentRepository;
    this.showTemplateRepository = showTemplateRepository;
  }

  @Override
  public String domainKey() {
    return SegmentTypeSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    findSegmentType(decision)
        .ifPresentOrElse(
            segmentType -> stamp(segmentType, decision),
            () ->
                log.warn(
                    "Seed decision {}: segment type '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    findSegmentType(decision)
        .ifPresentOrElse(
            segmentType -> {
              applyFieldValue(segmentType, decision);
              stamp(segmentType, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: segment type '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    findSegmentType(decision)
        .ifPresentOrElse(
            segmentType -> {
              segmentType.setSeedFingerprint(null);
              segmentTypeRepository.save(segmentType);
            },
            () ->
                log.warn(
                    "Seed decision {}: segment type '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (segments booked on the
    // type, template assignment rows). The fail-safe direction re-checks at delete time: a type
    // still referenced is kept as custom data rather than blowing the NOT NULL FK mid-apply.
    findSegmentType(decision)
        .ifPresentOrElse(
            segmentType -> {
              if (hasSegments(segmentType) || hasAssignments(segmentType)) {
                log.warn(
                    "Seed decision {}: segment type '{}' still has segments or template"
                        + " assignments — the row is kept as custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              } else {
                segmentTypeRepository.delete(segmentType);
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: segment type '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it.
    return findSegmentTypeByKey(entityKey)
        .map(type -> hasSegments(type) || hasAssignments(type))
        .orElse(false);
  }

  /**
   * Resolves the decision's segment type by reminting its entity key — the key survives the admin
   * rename a tier-2 NAME collision parks around (see class doc).
   */
  private Optional<SegmentType> findSegmentType(SeedUpdateDecision decision) {
    return findSegmentTypeByKey(decision.getEntityKey());
  }

  private Optional<SegmentType> findSegmentTypeByKey(String entityKey) {
    return segmentTypeRepository.findAll().stream()
        .filter(st -> entityKey.equals(SegmentTypeSync.entityKeyFor(st.getName())))
        .findFirst();
  }

  /** Whether any booked segment uses this type (the real NOT NULL FK). */
  private boolean hasSegments(SegmentType segmentType) {
    return segmentType.getId() != null
        && segmentRepository.findAll().stream()
            .anyMatch(s -> s.getSegmentType().getId().equals(segmentType.getId()));
  }

  /** Whether any template assignment row targets this type (ShowTemplateSegmentAssignment FK). */
  private boolean hasAssignments(SegmentType segmentType) {
    return showTemplateRepository.findAll().stream()
        .flatMap(t -> t.getSegmentAssignments().stream())
        .filter(a -> a.getSegmentType() != null)
        .anyMatch(a -> a.getSegmentType().getId().equals(segmentType.getId()));
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(SegmentType segmentType, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> segmentType.setDescription(value);
      case "expansionCode" -> segmentType.setExpansionCode(value);
      case "guideHash" ->
          log.warn(
              "Seed decision {}: segment type guideHash is derived from the guide payload and"
                  + " cannot be applied directly — the guide hash field is guide-managed",
              decision.getId());
      case "code" -> segmentType.setCode(value);
      case "eventOnly" -> segmentType.setEventOnly(Boolean.parseBoolean(value));
      default ->
          throw new IllegalStateException(
              "Segment type field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    segmentTypeRepository.save(segmentType);
  }

  private void stamp(SegmentType segmentType, SeedUpdateDecision decision) {
    segmentType.setSeedFingerprint(decision.getNewFingerprint());
    segmentTypeRepository.save(segmentType);
  }
}
