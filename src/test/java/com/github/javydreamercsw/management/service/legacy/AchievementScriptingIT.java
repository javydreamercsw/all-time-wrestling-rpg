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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the full data-driven achievement path end-to-end with no mocks: achievements.json → {@code
 * AchievementSync} → DB → {@code AchievementDefinitionService} (cache) → {@code
 * AchievementScriptService} (Groovy) → {@code ScriptedAchievementEvaluator} → {@code
 * LegacyService.unlockAchievement} → {@code AchievementUnlockedEvent}.
 */
@Transactional
class AchievementScriptingIT extends AbstractMockUserIntegrationTest {

  @Autowired private LegacyService legacyService;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void updateLegacyScore_rosterOf20WithThreeTitles_unlocksEliteScout() {
    Account account = new Account("elite_scout_it_user", "P@ssword123", "elite_scout@example.com");
    account = accountRepository.save(account);

    List<Wrestler> roster = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      Wrestler wrestler = Wrestler.builder().build();
      wrestler.setName("Elite Scout Wrestler " + i);
      wrestler.setAccount(account);
      roster.add(wrestlerRepository.save(wrestler));
    }

    for (int i = 0; i < 3; i++) {
      Title title =
          titleService.createTitle(
              "Elite Scout Title " + i,
              "Test title",
              WrestlerTier.MAIN_EVENTER,
              ChampionshipType.SINGLE,
              defaultUniverse.getId());
      title.awardTitleTo(List.of(roster.get(i)), Instant.now());
      titleRepository.save(title);
    }

    // Flush + clear so the account/wrestler instances re-fetched inside updateLegacyScore are
    // loaded fresh from the DB rather than returned from this transaction's identity map — the
    // in-memory Wrestler.reigns collection (the inverse, mappedBy side of the awardTitleTo
    // association) is never auto-synced on the already-managed instances above.
    entityManager.flush();
    entityManager.clear();

    legacyService.updateLegacyScore(accountRepository.findById(account.getId()).orElseThrow());

    Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
    assertThat(reloaded.getAchievements())
        .as("ELITE_SCOUT is a script-only achievement (achievements.json), no Java trigger exists")
        .anyMatch(a -> "ELITE_SCOUT".equals(a.getKey()));
    assertThat(achievementRepository.findByKey("ELITE_SCOUT")).isPresent();
  }
}
