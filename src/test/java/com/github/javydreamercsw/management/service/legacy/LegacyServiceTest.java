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

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementCategory;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

class LegacyServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private AchievementRepository achievementRepository;
  @Mock private com.github.javydreamercsw.management.domain.title.TitleRepository titleRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private LegacyService legacyService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testUpdateLegacyScore_Fans() {
    Account account = new Account();
    account.setId(1L);
    account.setUsername("testuser");

    com.github.javydreamercsw.management.domain.universe.Universe universe =
        new com.github.javydreamercsw.management.domain.universe.Universe();
    universe.setId(1L);

    Wrestler w1 = new Wrestler();
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState s1 =
        new com.github.javydreamercsw.management.domain.wrestler.WrestlerState();
    s1.setWrestler(w1);
    s1.setUniverse(universe);
    s1.setFans(5000L);
    w1.setWrestlerStates(new java.util.ArrayList<>(List.of(s1)));

    Wrestler w2 = new Wrestler();
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState s2 =
        new com.github.javydreamercsw.management.domain.wrestler.WrestlerState();
    s2.setWrestler(w2);
    s2.setUniverse(universe);
    s2.setFans(2500L);
    w2.setWrestlerStates(new java.util.ArrayList<>(List.of(s2)));

    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(w1, w2));

    legacyService.updateLegacyScore(account);

    // Total fans = 7500. Score = 7500 / 1000 = 7.
    verify(accountRepository).save(argThat(a -> a.getLegacyScore() == 7));
  }

  @Test
  void testAchievementUnlocking() {
    Account account = new Account();
    account.setId(1L);
    account.setUsername("testuser");

    com.github.javydreamercsw.management.domain.universe.Universe universe =
        new com.github.javydreamercsw.management.domain.universe.Universe();
    universe.setId(1L);

    Wrestler w1 = new Wrestler();
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState s1 =
        new com.github.javydreamercsw.management.domain.wrestler.WrestlerState();
    s1.setWrestler(w1);
    s1.setUniverse(universe);
    s1.setFans(10000L);
    w1.setWrestlerStates(new java.util.ArrayList<>(List.of(s1)));

    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(w1));

    Achievement achievement = new Achievement();
    achievement.setKey("CROWD_PLEASER");
    achievement.setName("Crowd Pleaser");
    achievement.setXpValue(100);
    achievement.setCategory(AchievementCategory.FANS);

    when(achievementRepository.findByKey("CROWD_PLEASER")).thenReturn(Optional.of(achievement));

    legacyService.updateLegacyScore(account);

    // Should unlock Crowd Pleaser
    verify(accountRepository, atLeastOnce())
        .save(argThat(a -> a.getAchievements().contains(achievement) && a.getPrestige() == 100));
  }
}
