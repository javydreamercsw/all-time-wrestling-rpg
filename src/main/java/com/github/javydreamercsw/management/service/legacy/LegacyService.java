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
package com.github.javydreamercsw.management.service.legacy;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyService {

  private final AccountRepository accountRepository;
  private final WrestlerRepository wrestlerRepository;
  private final AchievementRepository achievementRepository;

  /**
   * Recalculates the legacy score for an account based on their managed wrestlers. Formula: - 1
   * point per 1000 fans (across all wrestlers) - 50 points per Title held (current) - 100 points
   * per Legend wrestler
   *
   * @param account The account to update
   */
  @Transactional
  public void updateLegacyScore(Account account) {
    if (account == null) return;

    List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);

    long totalFans = wrestlers.stream().mapToLong(Wrestler::getFans).sum();
    long currentTitlesHeld =
        wrestlers.stream()
            .flatMap(w -> w.getReigns().stream())
            .filter(com.github.javydreamercsw.management.domain.title.TitleReign::isCurrentReign)
            .count();

    long score = totalFans / 1000;
    // Add 50 points per title currently held
    score += currentTitlesHeld * 50;

    // Add Achievement XP
    score += account.getAchievements().stream().mapToInt(Achievement::getXpValue).sum();

    account.setLegacyScore(score);
    accountRepository.save(account);
    log.info("Updated legacy score for {}: {}", account.getUsername(), score);

    checkAchievements(account, wrestlers, totalFans, currentTitlesHeld);
  }

  private void checkAchievements(
      @NonNull Account account,
      @NonNull List<Wrestler> wrestlers,
      long totalFans,
      long currentTitlesHeld) {
    if (!wrestlers.isEmpty()) {
      unlockAchievement(account, "FIRST_WRESTLER");
    }
    if (wrestlers.size() >= 10) {
      unlockAchievement(account, "ROSTER_BUILDER");
    }
    if (totalFans >= 10_000) {
      unlockAchievement(account, "CROWD_PLEASER");
    }
    if (totalFans >= 100_000) {
      unlockAchievement(account, "MAIN_EVENT_DRAW");
    }
    if (totalFans >= 1_000_000) {
      unlockAchievement(account, "GLOBAL_ICON");
    }
    if (currentTitlesHeld > 0) {
      unlockAchievement(account, "FIRST_CHAMPION");
    }
  }

  @Transactional
  public void incrementShowsBooked(@NonNull Account account) {
    account.setShowsBooked(account.getShowsBooked() + 1);
    accountRepository.save(account);
    log.info("Account {} has now booked {} shows", account.getUsername(), account.getShowsBooked());

    if (account.getShowsBooked() >= 50) {
      unlockAchievement(account, "BOOKER_OF_THE_YEAR");
    }
    updateLegacyScore(account);
  }

  @Transactional
  public void unlockAchievement(@NonNull Account account, @NonNull String key) {
    achievementRepository
        .findByKey(key)
        .ifPresent(
            achievement -> {
              if (!account.getAchievements().contains(achievement)) {
                account.getAchievements().add(achievement);
                account.setPrestige(
                    account.getPrestige() + achievement.getXpValue()); // Prestige accumulates XP
                accountRepository.save(account);
                updateLegacyScore(account);
                log.info(
                    "Unlocked achievement '{}' for {}",
                    achievement.getName(),
                    account.getUsername());
              }
            });
  }
}
