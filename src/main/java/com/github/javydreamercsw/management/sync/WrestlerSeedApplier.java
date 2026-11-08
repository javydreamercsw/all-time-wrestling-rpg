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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.util.LogSanitizer;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to wrestler rows (seed-update review feature). Runs
 * inside {@code SeedUpdateDecisionService#apply}'s transaction; the entity is resolved through the
 * decision row's {@code entity_display_name} — the exact seed name captured at detection time
 * (slugs are lossy: "Jessie McKay"/"André the Giant"/"OMZ" do not survive a slug round-trip, and
 * the repository lookups are case-sensitive).
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} (the incoming release's manifest hash) on the row — keep-mine stamps while
 * keeping the admin's value, so a later seed change to that field re-parks instead of silently
 * overwriting.
 */
@Slf4j
@Component
public class WrestlerSeedApplier implements SeedDomainApplier {

  private final WrestlerRepository wrestlerRepository;
  private final UniverseRepository universeRepository;

  public WrestlerSeedApplier(
      final WrestlerRepository wrestlerRepository, final UniverseRepository universeRepository) {
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
  }

  @Override
  public String domainKey() {
    return WrestlerSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            wrestler -> stamp(wrestler, decision),
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            wrestler -> {
              applyFieldValue(wrestler, decision);
              stamp(wrestler, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp so future detection
    // passes treat the row as user-owned content (unstamped rows are only content-compared).
    wrestlerRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            wrestler -> {
              wrestler.setSeedFingerprint(null);
              wrestlerRepository.save(wrestler);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(decision.getEntityDisplayName())
        .ifPresentOrElse(
            wrestlerRepository::delete,
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; the slug round-trip is lossy, so match on the seed-name
    // portion the way detection derived it (wrestler names are unique, so a name-suffix match on
    // the display name is not available here — resolve by the slug's name part case-insensitively
    // and rely on the apply service passing the same key detection minted).
    return wrestlerRepository.findAll().stream()
        .filter(w -> entityKey.equals(WrestlerSync.entityKeyFor(w.getName())))
        .findFirst()
        .map(
            wrestler ->
                (wrestler.getDecks() != null && !wrestler.getDecks().isEmpty())
                    || (wrestler.getRivalriesAsWrestler1() != null
                        && !wrestler.getRivalriesAsWrestler1().isEmpty())
                    || (wrestler.getRivalriesAsWrestler2() != null
                        && !wrestler.getRivalriesAsWrestler2().isEmpty()))
        .orElse(false);
  }

  /**
   * Writes the decision's incoming seed value to the matching seed-owned field and stamps the row's
   * fingerprint with the decision's {@code new_fingerprint} (the apply-time stamp obligation).
   */
  private void applyFieldValue(Wrestler wrestler, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "deckSize" -> wrestler.setDeckSize(value == null ? null : Integer.valueOf(value));
      case "startingHealth" ->
          wrestler.setStartingHealth(value == null ? null : Integer.valueOf(value));
      case "lowHealth" -> wrestler.setLowHealth(value == null ? null : Integer.valueOf(value));
      case "startingStamina" ->
          wrestler.setStartingStamina(value == null ? null : Integer.valueOf(value));
      case "lowStamina" -> wrestler.setLowStamina(value == null ? null : Integer.valueOf(value));
      case "description" -> wrestler.setDescription(value);
      case "gender" -> wrestler.setGender(value == null ? null : Gender.valueOf(value));
      case "imageUrl" -> wrestler.setImageUrl(value);
      case "heritageTag" -> wrestler.setHeritageTag(value);
      case "expansionCode" -> wrestler.setExpansionCode(value);
      case "drive" -> wrestler.setDrive(value == null ? null : Integer.valueOf(value));
      case "resilience" -> wrestler.setResilience(value == null ? null : Integer.valueOf(value));
      case "charisma" -> wrestler.setCharisma(value == null ? null : Integer.valueOf(value));
      case "brawl" -> wrestler.setBrawl(value == null ? null : Integer.valueOf(value));
      case "alignment" -> {
        // Same write semantics as the silent-update writer (WrestlerSync.applySilentSeedFields):
        // replace the campaign-free alignment holder in place — setAlignment appends, which
        // would leave the stale alignment first in the set and the derived getter never
        // reflecting the seed change.
        if (value != null) {
          try {
            AlignmentType seedType = AlignmentType.valueOf(value);
            WrestlerAlignment current = wrestler.getAlignment();
            if (current != null) {
              current.setAlignmentType(seedType);
            } else {
              wrestler.setAlignment(
                  WrestlerAlignment.builder()
                      .wrestler(wrestler)
                      .universe(universeRepository.findAll().stream().findFirst().orElse(null))
                      .alignmentType(seedType)
                      .level(0)
                      .build());
            }
          } catch (IllegalArgumentException e) {
            log.warn(
                "Seed decision {}: invalid alignment '{}' for wrestler '{}'",
                decision.getId(),
                LogSanitizer.sanitize(value),
                LogSanitizer.sanitize(wrestler.getName()));
          }
        }
      }
      default ->
          throw new IllegalStateException(
              "Wrestler field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    wrestlerRepository.save(wrestler);
  }

  /**
   * Stamps the row's seed fingerprint with the decision's detection-time {@code new_fingerprint}.
   */
  private void stamp(Wrestler wrestler, SeedUpdateDecision decision) {
    wrestler.setSeedFingerprint(decision.getNewFingerprint());
    wrestlerRepository.save(wrestler);
  }
}
