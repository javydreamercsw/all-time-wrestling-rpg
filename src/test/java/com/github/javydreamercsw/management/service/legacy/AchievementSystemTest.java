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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AchievementUnlockedEvent;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.match.MatchRewardService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AchievementSystemTest {

  @Mock private AccountRepository accountRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private AchievementRepository achievementRepository;
  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private TitleService titleService;
  @Mock private MatchRewardService matchRewardService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private LegacyService legacyService;
  private SegmentAdjudicationService segmentAdjudicationService;

  private Account account;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    legacyService =
        new LegacyService(
            accountRepository,
            wrestlerRepository,
            achievementRepository,
            titleRepository,
            eventPublisher);
    segmentAdjudicationService =
        new SegmentAdjudicationService(
            rivalryService,
            wrestlerService,
            feudResolutionService,
            feudService,
            titleService,
            matchRewardService,
            matchFulfillmentRepository,
            leagueRosterRepository,
            legacyService,
            new Random());

    account = new Account();
    account.setUsername("testuser");
    account.setPrestige(0L);

    wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setAccount(account);
    wrestler.setFans(0L);

    // Mock achievement repository to return an achievement if found
    lenient()
        .when(achievementRepository.findByKey(anyString()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              Achievement a = new Achievement();
              a.setKey(key);
              a.setName(key.replace("_", " "));
              a.setXpValue(10);
              return Optional.of(a);
            });
  }

  @Test
  void testCollectionAchievements() {
    List<Wrestler> wrestlers = new ArrayList<>();
    when(wrestlerRepository.findByAccount(account)).thenReturn(wrestlers);

    // No wrestlers - should not unlock anything yet
    legacyService.updateLegacyScore(account);
    verify(achievementRepository, never()).findByKey("FIRST_WRESTLER");

    // 1 wrestler
    wrestlers.add(wrestler);
    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("FIRST_WRESTLER");
    verify(eventPublisher).publishEvent(any(AchievementUnlockedEvent.class));

    // 10 wrestlers
    for (int i = 0; i < 9; i++) {
      wrestlers.add(new Wrestler());
    }
    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("ROSTER_BUILDER");
    verify(eventPublisher, atLeastOnce()).publishEvent(any(AchievementUnlockedEvent.class));
  }

  @Test
  void testFanAchievements() {
    List<Wrestler> wrestlers = List.of(wrestler);
    when(wrestlerRepository.findByAccount(account)).thenReturn(wrestlers);

    // 10k fans
    wrestler.setFans(10_000L);
    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("CROWD_PLEASER");

    // 100k fans
    wrestler.setFans(100_000L);
    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("MAIN_EVENT_DRAW");

    // 1M fans
    wrestler.setFans(1_000_000L);
    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("GLOBAL_ICON");
  }

  @Test
  void testChampionshipAchievements() {
    List<Wrestler> wrestlers = List.of(wrestler);
    when(wrestlerRepository.findByAccount(account)).thenReturn(wrestlers);

    TitleReign reign = new TitleReign();
    reign.setEndDate(null);
    wrestler.setReigns(Collections.singletonList(reign));

    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("FIRST_CHAMPION");
  }

  @Test
  void testGrandSlamAchievement() {
    List<Wrestler> wrestlers = List.of(wrestler);
    when(wrestlerRepository.findByAccount(account)).thenReturn(wrestlers);

    com.github.javydreamercsw.management.domain.title.Title t1 =
        mock(com.github.javydreamercsw.management.domain.title.Title.class);
    com.github.javydreamercsw.management.domain.title.Title t2 =
        mock(com.github.javydreamercsw.management.domain.title.Title.class);
    when(t1.getId()).thenReturn(1L);
    when(t2.getId()).thenReturn(2L);
    when(titleRepository.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

    TitleReign reign1 = new TitleReign();
    reign1.setTitle(t1);
    reign1.setEndDate(null);

    // Only holding one title - no grand slam
    wrestler.setReigns(List.of(reign1));
    legacyService.updateLegacyScore(account);
    verify(achievementRepository, never()).findByKey("GRAND_SLAM");

    // Holding both titles
    TitleReign reign2 = new TitleReign();
    reign2.setTitle(t2);
    reign2.setEndDate(null);
    wrestler.setReigns(List.of(reign1, reign2));

    legacyService.updateLegacyScore(account);
    verify(achievementRepository).findByKey("GRAND_SLAM");
  }

  @Test
  void testBookingAchievements() {
    account.setShowsBooked(49);
    legacyService.incrementShowsBooked(account);
    // Should be 50 now
    verify(achievementRepository).findByKey("BOOKER_OF_THE_YEAR");
  }

  @Test
  void testMatchAdjudicationAchievements() {
    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    SegmentRule rule = mock(SegmentRule.class);
    Show show = mock(Show.class);

    when(segment.getWrestlers()).thenReturn(List.of(wrestler));
    when(segment.getWinners()).thenReturn(List.of(wrestler));
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.getSegmentRules()).thenReturn(List.of(rule));
    when(segment.getShow()).thenReturn(show);
    when(type.getName()).thenReturn("One on One");
    when(rule.getName()).thenReturn("Normal");
    when(show.isPremiumLiveEvent()).thenReturn(false);
    when(segment.isMainEvent()).thenReturn(true);

    segmentAdjudicationService.adjudicateMatch(segment);

    // Participation
    verify(achievementRepository).findByKey("PARTICIPATE_ONE_ON_ONE");
    verify(achievementRepository).findByKey("PARTICIPATE_NORMAL");

    // Wins
    verify(achievementRepository).findByKey("WIN_ONE_ON_ONE");
    verify(achievementRepository).findByKey("WIN_NORMAL");

    // Main Event
    verify(achievementRepository).findByKey("MAIN_EVENT");
  }

  @Test
  void testSpecialEventAchievements() {
    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    Show show = mock(Show.class);

    when(segment.getWrestlers()).thenReturn(List.of(wrestler));
    when(segment.getWinners()).thenReturn(List.of(wrestler));
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.getSegmentRules()).thenReturn(Collections.emptyList());
    when(segment.getShow()).thenReturn(show);
    when(type.getName()).thenReturn("Abu Dhabi Rumble");
    when(show.isPremiumLiveEvent()).thenReturn(true);
    when(segment.isMainEvent()).thenReturn(true);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(achievementRepository).findByKey("PARTICIPATE_ABU_DHABI_RUMBLE");
    verify(achievementRepository).findByKey("RUMBLE_WINNER");
    verify(achievementRepository).findByKey("MAIN_EVENT_PLE");
  }
}
