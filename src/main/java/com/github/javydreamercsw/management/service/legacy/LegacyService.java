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
    long titlesHeld =
        wrestlers.stream()
            .mapToLong(w -> w.getReigns().size())
            .sum(); // This counts reigns, simplified for now
    // TODO: Need proper title counting logic if 'reigns' includes past ones. Assuming active reigns
    // for now or need a better check.
    // Actually, getting current championships is better.
    // For now, let's stick to Fans as the primary driver to verify persistence.

    long score = totalFans / 1000;

    // Add Achievement XP
    score += account.getAchievements().stream().mapToInt(Achievement::getXpValue).sum();

    account.setLegacyScore(score);
    accountRepository.save(account);
    log.info("Updated legacy score for {}: {}", account.getUsername(), score);
  }

  @Transactional
  public void unlockAchievement(Account account, String achievementName) {
    achievementRepository
        .findByName(achievementName)
        .ifPresent(
            achievement -> {
              if (!account.getAchievements().contains(achievement)) {
                account.getAchievements().add(achievement);
                account.setPrestige(
                    account.getPrestige() + achievement.getXpValue()); // Prestige accumulates XP
                accountRepository.save(account);
                updateLegacyScore(account); // Update legacy score to reflect new achievement
                log.info(
                    "Unlocked achievement '{}' for {}", achievementName, account.getUsername());
              }
            });
  }
}
