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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementCategory;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptedAchievementEvaluatorTest {

  @Mock private AchievementDefinitionService definitionService;
  @Mock private AchievementScriptService scriptService;

  private ScriptedAchievementEvaluator evaluator;
  private Account account;

  @BeforeEach
  void setUp() {
    evaluator = new ScriptedAchievementEvaluator(definitionService, scriptService);
    account = new Account();
    account.setId(1L);
  }

  private static Achievement scriptedAchievement(final String key, final String condition) {
    Achievement achievement = new Achievement();
    achievement.setKey(key);
    achievement.setName(key);
    achievement.setDescription(key);
    achievement.setXpValue(10);
    achievement.setCategory(AchievementCategory.SPECIAL_EVENT);
    achievement.setUnlockCondition(condition);
    return achievement;
  }

  @Test
  @DisplayName(
      "Achievement already held by the account is skipped before its script is compiled/evaluated")
  void testAlreadyHeldAchievementSkipsScriptEvaluation() {
    Achievement held = scriptedAchievement("ALREADY_HELD", "true");
    account.setAchievements(new HashSet<>(List.of(held)));

    when(definitionService.getScriptedAchievements()).thenReturn(List.of(held));

    List<String> result = evaluator.resolveNewlyUnlockedKeys(account, Map.of());

    assertThat(result).isEmpty();
    verify(scriptService, never()).evaluate(anyString(), any());
  }

  @Test
  @DisplayName("Unheld achievement whose script evaluates true is returned")
  void testUnheldAchievementScriptTrueIsUnlocked() {
    Achievement candidate = scriptedAchievement("NEW_ACHIEVEMENT", "wrestlers.size() >= 5");
    when(definitionService.getScriptedAchievements()).thenReturn(List.of(candidate));
    when(scriptService.evaluate(eq("wrestlers.size() >= 5"), any())).thenReturn(true);

    List<String> result =
        evaluator.resolveNewlyUnlockedKeys(account, Map.of("wrestlers", List.of()));

    assertThat(result).containsExactly("NEW_ACHIEVEMENT");
  }

  @Test
  @DisplayName("Unheld achievement whose script evaluates false is not returned")
  void testUnheldAchievementScriptFalseIsNotUnlocked() {
    Achievement candidate = scriptedAchievement("NEW_ACHIEVEMENT", "false");
    when(definitionService.getScriptedAchievements()).thenReturn(List.of(candidate));
    when(scriptService.evaluate(eq("false"), any())).thenReturn(false);

    List<String> result = evaluator.resolveNewlyUnlockedKeys(account, Map.of());

    assertThat(result).isEmpty();
  }
}
