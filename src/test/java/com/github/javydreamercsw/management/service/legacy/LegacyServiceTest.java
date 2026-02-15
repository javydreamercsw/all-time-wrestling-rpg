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
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.domain.account.AchievementType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LegacyServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private AchievementRepository achievementRepository;

  @InjectMocks private LegacyService legacyService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testUpdateLegacyScore_Fans() {
    Account account = new Account();
    account.setUsername("testuser");

    Wrestler w1 = new Wrestler();
    w1.setFans(5000L);
    Wrestler w2 = new Wrestler();
    w2.setFans(2500L);

    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(w1, w2));

    legacyService.updateLegacyScore(account);

    // Total fans = 7500. Score = 7500 / 1000 = 7.
    verify(accountRepository).save(argThat(a -> a.getLegacyScore() == 7));
  }

  @Test
  void testAchievementUnlocking() {
    Account account = new Account();
    account.setUsername("testuser");

    Wrestler w1 = new Wrestler();
    w1.setFans(10000L);

    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(w1));

    Achievement achievement = new Achievement();

    achievement.setType(AchievementType.CROWD_PLEASER);

    when(achievementRepository.findByType(AchievementType.CROWD_PLEASER))
        .thenReturn(Optional.of(achievement));

    legacyService.updateLegacyScore(account);

    // Should unlock Crowd Pleaser
    verify(accountRepository, atLeastOnce())
        .save(argThat(a -> a.getAchievements().contains(achievement) && a.getPrestige() == 100));
  }
}
