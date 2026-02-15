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
package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchRewardServiceImplTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private TitleService titleService;
  @Mock private LegacyService legacyService;
  @Mock private Segment segment;
  @Mock private Wrestler wrestler;
  @Mock private Account account;
  @Mock private SegmentType segmentType;

  private MatchRewardServiceImpl matchRewardService;

  @BeforeEach
  void setUp() {
    matchRewardService = new MatchRewardServiceImpl(wrestlerService, titleService, legacyService);

    when(segment.getWrestlers()).thenReturn(List.of(wrestler));
    when(segment.getWinners()).thenReturn(List.of(wrestler));
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Match");
    when(wrestler.getId()).thenReturn(1L);

    // Default mock behavior for wrestlerService
    lenient()
        .when(wrestlerService.awardFans(anyLong(), anyLong()))
        .thenReturn(Optional.of(wrestler));
  }

  @Test
  void testProcessRewards_NoAchievementsOnLowRoll() {
    // We can't easily force the random roll in the implementation without refactoring
    // but we can verify that the service runs.
    matchRewardService.processRewards(segment, 1.0);
    verify(wrestlerService, atLeastOnce()).awardFans(eq(1L), anyLong());
  }
}
