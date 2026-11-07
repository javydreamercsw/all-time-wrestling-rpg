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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to championship (title) rows (seed-update review
 * feature). The entity is resolved through the decision row's {@code entity_display_name} — the
 * exact seed name captured at detection time (slugs are lossy: "ATW World" uppercases to "ATW
 * WORLD" in a slug round-trip, and the repository lookups are case-sensitive). Current champions
 * are gameplay state and are never touched by any decision.
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class ChampionshipSeedApplier implements SeedDomainApplier {

  private final TitleServiceBridge titleService;
  private final TitleReignRepository titleReignRepository;
  private final SegmentRepository segmentRepository;

  /** Minimal write surface the applier needs from {@code TitleService}. */
  public interface TitleServiceBridge {
    Optional<Title> findByName(String name);

    java.util.List<Title> findAllUnfiltered();

    Title save(Title title);

    boolean deleteTitle(Long id);
  }

  public ChampionshipSeedApplier(
      final com.github.javydreamercsw.management.service.title.TitleService titleService,
      final TitleReignRepository titleReignRepository,
      final SegmentRepository segmentRepository) {
    this.titleService =
        new TitleServiceBridge() {
          @Override
          public Optional<Title> findByName(String name) {
            return titleService.findByName(name);
          }

          @Override
          public java.util.List<Title> findAllUnfiltered() {
            return titleService.findAllUnfiltered();
          }

          @Override
          public Title save(Title title) {
            return titleService.save(title);
          }

          @Override
          public boolean deleteTitle(Long id) {
            return titleService.deleteTitle(id);
          }
        };
    this.titleReignRepository = titleReignRepository;
    this.segmentRepository = segmentRepository;
  }

  @Override
  public String domainKey() {
    return ChampionshipSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    titleService
        .findByName(decision.getEntityDisplayName())
        .ifPresent(title -> stamp(title, decision));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    titleService
        .findByName(decision.getEntityDisplayName())
        .ifPresent(
            title -> {
              applyFieldValue(title, decision);
              stamp(title, decision);
            });
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    titleService
        .findByName(decision.getEntityDisplayName())
        .ifPresent(
            title -> {
              title.setSeedFingerprint(null);
              titleService.save(title);
            });
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (reigns, title
    // segments). deleteTitle only removes vacant, inactive titles — a title with gameplay state
    // left behind logs and stays (fail-safe direction: gameplay history is never destroyed).
    titleService
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            title -> {
              if (!titleService.deleteTitle(title.getId())) {
                log.warn(
                    "Seed decision {}: championship '{}' could not be deleted (not vacant and"
                        + " inactive) — the row is kept as custom data",
                    decision.getId(),
                    title.getName());
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: championship '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous (existing hyphens in a
    // seed name are indistinguishable from slug-converted spaces), so match the key exactly the
    // way detection minted it.
    return titleService.findAllUnfiltered().stream()
        .filter(t -> entityKey.equals(ChampionshipSync.entityKeyFor(t.getName())))
        .findFirst()
        .map(
            title ->
                !titleReignRepository.findByTitle(title).isEmpty()
                    || !segmentRepository.findByTitle(title).isEmpty())
        .orElse(false);
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Title title, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "description" -> title.setDescription(value);
      case "tier" ->
          title.setTier(
              value == null
                  ? null
                  : com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.valueOf(value));
      case "championshipType" ->
          title.setChampionshipType(
              value == null
                  ? null
                  : com.github.javydreamercsw.management.domain.title.ChampionshipType.valueOf(
                      value));
      case "effectScript" -> title.setEffectScript(value);
      case "gender" ->
          title.setGender(
              value == null
                  ? null
                  : com.github.javydreamercsw.base.domain.wrestler.Gender.valueOf(value));
      case "imageUrl" -> title.setImageUrl(value);
      case "expansionCode" -> title.setExpansionCode(value);
      case "includeInRankings" ->
          title.setIncludeInRankings(value == null ? null : Boolean.valueOf(value));
      default ->
          throw new IllegalStateException(
              "Championship field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    titleService.save(title);
  }

  private void stamp(Title title, SeedUpdateDecision decision) {
    title.setSeedFingerprint(decision.getNewFingerprint());
    titleService.save(title);
  }
}
