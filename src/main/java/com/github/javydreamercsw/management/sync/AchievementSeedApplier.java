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

import com.github.javydreamercsw.base.domain.account.AccountAchievementRepository;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.sync.review.SeedDomainApplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies administrator seed-update decisions to achievement rows (seed-update review feature). The
 * entity is resolved through the decision row's {@code entity_display_name} — the exact seed KEY
 * captured at detection time (achievement keys are the domain's stable identity; the human-readable
 * name field is itself seed-owned content, so it cannot be the resolution handle).
 *
 * <p>Stamp obligations: {@code USE_NEW} and {@code KEEP_MINE} both stamp the decision's {@code
 * new_fingerprint} on the row — keep-mine stamps while keeping the admin's value, so a later seed
 * change to that field re-parks instead of silently overwriting.
 */
@Slf4j
@Component
public class AchievementSeedApplier implements SeedDomainApplier {

  private final AchievementRepository achievementRepository;
  private final AccountAchievementRepository accountAchievementRepository;

  public AchievementSeedApplier(
      final AchievementRepository achievementRepository,
      final AccountAchievementRepository accountAchievementRepository) {
    this.achievementRepository = achievementRepository;
    this.accountAchievementRepository = accountAchievementRepository;
  }

  @Override
  public String domainKey() {
    return AchievementSync.DOMAIN_KEY;
  }

  @Override
  public void applyKeep(SeedUpdateDecision decision) {
    achievementRepository
        .findByKey(decision.getEntityDisplayName())
        .ifPresentOrElse(
            achievement -> stamp(achievement, decision),
            () ->
                log.warn(
                    "Seed decision {}: achievement '{}' not found — nothing to keep",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyNew(SeedUpdateDecision decision) {
    achievementRepository
        .findByKey(decision.getEntityDisplayName())
        .ifPresentOrElse(
            achievement -> {
              applyFieldValue(achievement, decision);
              stamp(achievement, decision);
            },
            () ->
                log.warn(
                    "Seed decision {}: achievement '{}' not found — nothing to apply",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyKeepAsCustom(SeedUpdateDecision decision) {
    // Keep the value, mark it no longer seed-owned: clear the row's stamp.
    achievementRepository
        .findByKey(decision.getEntityDisplayName())
        .ifPresentOrElse(
            achievement -> {
              achievement.setSeedFingerprint(null);
              achievementRepository.save(achievement);
            },
            () ->
                log.warn(
                    "Seed decision {}: achievement '{}' not found — nothing to mark custom",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public void applyRemove(SeedUpdateDecision decision) {
    // Dependents were verified in the apply service's pre-dispatch phase (account achievement
    // progress). A still-earned achievement would fail its FK on delete, so the fail-safe
    // direction re-checks at delete time: an earned achievement is kept as custom data.
    achievementRepository
        .findByKey(decision.getEntityDisplayName())
        .ifPresentOrElse(
            achievement -> {
              if (accountAchievementRepository.findByAchievement(achievement).isEmpty()) {
                achievementRepository.delete(achievement);
              } else {
                log.warn(
                    "Seed decision {}: achievement '{}' has account progress — the row is kept as"
                        + " custom data",
                    decision.getId(),
                    decision.getEntityDisplayName());
              }
            },
            () ->
                log.warn(
                    "Seed decision {}: achievement '{}' not found — nothing to remove",
                    decision.getId(),
                    decision.getEntityDisplayName()));
  }

  @Override
  public boolean hasDependents(String entityKey) {
    // The SPI passes the entity key only; a slug round-trip is ambiguous, so match the key exactly
    // the way detection minted it (achievement keys are lowercased into the slug).
    return achievementRepository.findAll().stream()
        .filter(a -> entityKey.equals(AchievementSync.entityKeyFor(a.getKey())))
        .findFirst()
        .map(achievement -> !accountAchievementRepository.findByAchievement(achievement).isEmpty())
        .orElse(false);
  }

  /** Writes the decision's incoming seed value to the matching seed-owned field. */
  private void applyFieldValue(Achievement achievement, SeedUpdateDecision decision) {
    String value = decision.getNewValue();
    switch (decision.getFieldName()) {
      case "name" -> achievement.setName(value);
      case "description" -> achievement.setDescription(value);
      case "xpValue" -> achievement.setXpValue(value == null ? null : Integer.valueOf(value));
      case "category" ->
          achievement.setCategory(
              value == null
                  ? null
                  : com.github.javydreamercsw.base.domain.account.AchievementCategory.valueOf(
                      value));
      case "unlockCondition" -> achievement.setUnlockCondition(value);
      default ->
          throw new IllegalStateException(
              "Achievement field '" + decision.getFieldName() + "' is not a seed-owned field");
    }
    achievementRepository.save(achievement);
  }

  private void stamp(Achievement achievement, SeedUpdateDecision decision) {
    achievement.setSeedFingerprint(decision.getNewFingerprint());
    achievementRepository.save(achievement);
  }
}
