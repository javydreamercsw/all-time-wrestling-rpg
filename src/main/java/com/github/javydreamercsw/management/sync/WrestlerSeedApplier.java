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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to wrestler rows (seed-update review feature). Runs
 * inside {@code SeedUpdateDecisionService#apply}'s transaction; entity keys are {@code
 * wrestler:<slug>} as produced by detection.
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

  public WrestlerSeedApplier(final WrestlerRepository wrestlerRepository) {
    this.wrestlerRepository = wrestlerRepository;
  }

  @Override
  public String domainKey() {
    return WrestlerSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(entityName(decision.getEntityKey()))
        .ifPresentOrElse(
            wrestler -> stamp(wrestler, decision),
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityKey()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(entityName(decision.getEntityKey()))
        .ifPresentOrElse(
            wrestler -> {
              applyFieldValue(wrestler, decision);
              stamp(wrestler, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityKey()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp so future detection
    // passes treat the row as user-owned content (unstamped rows are only content-compared).
    wrestlerRepository
        .findByName(entityName(decision.getEntityKey()))
        .ifPresentOrElse(
            wrestler -> {
              wrestler.setSeedFingerprint(null);
              wrestlerRepository.save(wrestler);
            },
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityKey()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    wrestlerRepository
        .findByName(entityName(decision.getEntityKey()))
        .ifPresentOrElse(
            wrestlerRepository::delete,
            () ->
                log.warn(
                    "Seed decision {}: wrestler '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityKey()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    return wrestlerRepository
        .findByName(entityName(entityKey))
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
      case "gender" ->
          wrestler.setGender(
              value == null
                  ? null
                  : com.github.javydreamercsw.base.domain.wrestler.Gender.valueOf(value));
      case "imageUrl" -> wrestler.setImageUrl(value);
      case "heritageTag" -> wrestler.setHeritageTag(value);
      case "expansionCode" -> wrestler.setExpansionCode(value);
      case "drive" -> wrestler.setDrive(value == null ? null : Integer.valueOf(value));
      case "resilience" -> wrestler.setResilience(value == null ? null : Integer.valueOf(value));
      case "charisma" -> wrestler.setCharisma(value == null ? null : Integer.valueOf(value));
      case "brawl" -> wrestler.setBrawl(value == null ? null : Integer.valueOf(value));
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

  /** Reverses {@code WrestlerSync#entityKey}: {@code wrestler:bret-hart} → {@code Bret Hart}. */
  static String entityName(String entityKey) {
    String slug = entityKey.substring(entityKey.indexOf(':') + 1);
    String[] words = slug.split("-");
    StringBuilder name = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      if (name.length() > 0) {
        name.append(' ');
      }
      name.append(
          Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT));
    }
    return name.toString();
  }
}
