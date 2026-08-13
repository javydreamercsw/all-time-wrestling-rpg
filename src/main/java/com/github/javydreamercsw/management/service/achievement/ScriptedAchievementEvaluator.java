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
package com.github.javydreamercsw.management.service.achievement;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Achievement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves which data-defined (scripted) achievements newly qualify for an account, given a context
 * map of the variables available at a given unlock-check call site. Does not perform the unlock
 * itself — callers pass the returned keys to {@code LegacyService.unlockAchievement}, the existing
 * generic unlock sink, so XP award, per-account dedup, legacy score recalculation, and event
 * publishing stay unchanged.
 *
 * <p>Deliberately has no dependency on {@code LegacyService} to avoid a bean cycle, since {@code
 * LegacyService} depends on this class.
 */
@Service
@RequiredArgsConstructor
public class ScriptedAchievementEvaluator {

  private final AchievementDefinitionService definitionService;
  private final AchievementScriptService scriptService;

  /**
   * Returns the keys of scripted achievements the account newly qualifies for. Achievements the
   * account already holds are skipped before their script is compiled or evaluated.
   */
  public List<String> resolveNewlyUnlockedKeys(
      @NonNull final Account account, @NonNull final Map<String, Object> context) {
    Set<String> alreadyUnlocked =
        account.getAchievements().stream().map(Achievement::getKey).collect(Collectors.toSet());
    List<String> newlyUnlocked = new ArrayList<>();
    for (Achievement candidate : definitionService.getScriptedAchievements()) {
      if (alreadyUnlocked.contains(candidate.getKey())) {
        continue;
      }
      if (scriptService.evaluate(candidate.getUnlockCondition(), context)) {
        newlyUnlocked.add(candidate.getKey());
      }
    }
    return newlyUnlocked;
  }
}
