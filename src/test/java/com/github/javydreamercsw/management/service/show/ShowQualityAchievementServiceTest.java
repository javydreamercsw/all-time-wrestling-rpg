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
package com.github.javydreamercsw.management.service.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowQualityAchievementServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private ShowRepository showRepository;
  @Mock private PromoBookingService promoBookingService;
  @Mock private LegacyService legacyService;
  @Mock private SecurityUtils securityUtils;

  private ShowQualityAchievementService service;

  private Account bookerAccount;
  private Show show;
  private Universe universe;

  @BeforeEach
  void setUp() {
    service =
        new ShowQualityAchievementService(
            segmentRepository, showRepository, promoBookingService, legacyService, securityUtils);

    bookerAccount = new Account();
    bookerAccount.setId(1L);

    CustomUserDetails userDetails = new CustomUserDetails(bookerAccount);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));

    universe = new Universe();
    universe.setId(1L);

    show = new Show();
    show.setId(10L);
    show.setName("Test Show");
    show.setUniverse(universe);

    when(showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any()))
        .thenReturn(List.of());
  }

  // ---------- null quality score guard ----------

  @Test
  void nullQualityScore_skipsAllProcessing() {
    show.setQualityScore(null);

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(segmentRepository, never()).findByShow(any());
    verify(legacyService, never()).unlockAchievement(any(), any());
  }

  // ---------- FIVE_STAR_CLASSIC ----------

  @Test
  void segmentRating100_unlocksFiveStarClassic() {
    show.setQualityScore(5.0);
    Segment segment = makeMatchSegment(100, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("FIVE_STAR_CLASSIC"));
  }

  @Test
  void segmentRating99_doesNotUnlockFiveStarClassic() {
    show.setQualityScore(4.5);
    Segment segment = makeMatchSegment(99, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService, never()).unlockAchievement(any(), eq("FIVE_STAR_CLASSIC"));
  }

  // ---------- SEGMENT_QUALITY_GOOD / GREAT ----------

  @Test
  void rating85_unlocksSegmentQualityGreat() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(85, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("SEGMENT_QUALITY_GREAT"));
  }

  @Test
  void rating70_unlocksSegmentQualityGood() {
    show.setQualityScore(3.5);
    Segment segment = makeMatchSegment(70, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("SEGMENT_QUALITY_GOOD"));
  }

  // ---------- SEGMENT_QUALITY_TITLE_CLASSIC ----------

  @Test
  void rating80TitleMatch_unlocksTitleClassic() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(80, true, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService)
        .unlockAchievement(any(Account.class), eq("SEGMENT_QUALITY_TITLE_CLASSIC"));
  }

  @Test
  void rating80NonTitleMatch_doesNotUnlockTitleClassic() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(80, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService, never()).unlockAchievement(any(), eq("SEGMENT_QUALITY_TITLE_CLASSIC"));
  }

  // ---------- TV_GOLD / PLE_SHOWSTOPPER ----------

  @Test
  void rating75WeeklyShow_unlocksTvGold() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(75, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));
    // show.isPremiumLiveEvent() = false (no template set)

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("TV_GOLD"));
  }

  @Test
  void rating90PleShow_unlocksShowstopper() {
    show.setQualityScore(4.5);
    show.setTemplate(makePleTemplate());
    Segment segment = makeMatchSegment(90, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("PLE_SHOWSTOPPER"));
  }

  @Test
  void rating90WeeklyShow_doesNotUnlockShowstopper() {
    show.setQualityScore(4.5);
    Segment segment = makeMatchSegment(90, false, false, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService, never()).unlockAchievement(any(), eq("PLE_SHOWSTOPPER"));
  }

  // ---------- STIPULATION_SPECIALIST ----------

  @Test
  void rating75WithStipulation_unlocksStipulationSpecialist() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(75, false, false, true);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("STIPULATION_SPECIALIST"));
  }

  // ---------- MAIN_EVENT_CLASSIC ----------

  @Test
  void rating85MainEvent_unlocksMainEventClassic() {
    show.setQualityScore(4.0);
    Segment segment = makeMatchSegment(85, false, true, false);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(segment));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(any(Account.class), eq("MAIN_EVENT_CLASSIC"));
  }

  // ---------- Show quality achievements ----------

  @Test
  void qualityScore40_unlocksShowQualityGreat() {
    show.setQualityScore(4.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(eq(bookerAccount), eq("SHOW_QUALITY_GREAT"));
  }

  @Test
  void qualityScore35_unlocksShowQualityGood() {
    show.setQualityScore(3.5);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(eq(bookerAccount), eq("SHOW_QUALITY_GOOD"));
    verify(legacyService, never()).unlockAchievement(eq(bookerAccount), eq("SHOW_QUALITY_GREAT"));
  }

  @Test
  void qualityScore50_unlocksShowQualityElite() {
    show.setQualityScore(5.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());
    when(showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any()))
        .thenReturn(List.of());

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(eq(bookerAccount), eq("SHOW_QUALITY_ELITE"));
  }

  // ---------- Streak achievements ----------

  @Test
  void threeConsecutiveShows_unlocksStreak3() {
    show.setQualityScore(4.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    Show prev1 = showWithScore(4.0);
    Show prev2 = showWithScore(3.5);
    when(showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any()))
        .thenReturn(List.of(show, prev1, prev2));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(eq(bookerAccount), eq("QUALITY_STREAK_3"));
    verify(legacyService, never()).unlockAchievement(eq(bookerAccount), eq("QUALITY_STREAK_5"));
  }

  @Test
  void fiveConsecutiveShows_unlocksStreak5() {
    show.setQualityScore(4.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    List<Show> streak =
        List.of(
            show, showWithScore(4.0), showWithScore(3.5), showWithScore(4.5), showWithScore(3.5));
    when(showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any()))
        .thenReturn(streak);

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService).unlockAchievement(eq(bookerAccount), eq("QUALITY_STREAK_5"));
  }

  @Test
  void streakBrokenByLowScore_doesNotUnlock() {
    show.setQualityScore(4.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    Show lowScore = showWithScore(2.0);
    Show highScore = showWithScore(4.0);
    when(showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any()))
        .thenReturn(List.of(show, lowScore, highScore));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService, never()).unlockAchievement(eq(bookerAccount), eq("QUALITY_STREAK_3"));
  }

  @Test
  void showQualityBelowStreakThreshold_skipsStreakCheck() {
    show.setQualityScore(3.0);
    when(segmentRepository.findByShow(show)).thenReturn(List.of());

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(showRepository, never())
        .findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(any(), any());
  }

  @Test
  void promoSegment_skippedForSegmentAchievements() {
    show.setQualityScore(4.0);
    Segment promo = makeMatchSegment(100, false, false, false);
    when(promoBookingService.isPromoSegment(promo)).thenReturn(true);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(promo));

    service.onApplicationEvent(new AdjudicationCompletedEvent(this, show));

    verify(legacyService, never()).unlockAchievement(any(), eq("FIVE_STAR_CLASSIC"));
  }

  // ---------- helpers ----------

  private Segment makeMatchSegment(
      final int rating,
      final boolean isTitle,
      final boolean isMainEvent,
      final boolean hasStipulation) {

    SegmentType type = new SegmentType();
    type.setName("One on One");

    Segment segment = new Segment();
    segment.setId((long) (Math.random() * 10_000));
    segment.setSegmentType(type);
    segment.setSegmentRating(rating);
    segment.setIsTitleSegment(isTitle);
    segment.setMainEvent(isMainEvent);

    when(promoBookingService.isPromoSegment(segment)).thenReturn(false);

    Account wrestlerAccount = new Account();
    wrestlerAccount.setId(100L + (long) (Math.random() * 1000));

    Wrestler w = new Wrestler();
    w.setId(200L + (long) (Math.random() * 1000));
    w.setName("Test Wrestler");
    w.setAccount(wrestlerAccount);

    if (hasStipulation) {
      var rule = new SegmentRule();
      rule.setName("Ladder Match");
      segment.setSegmentRules(new HashSet<>(Set.of(rule)));
    } else {
      segment.setSegmentRules(new HashSet<>());
    }

    var participant = new SegmentParticipant();
    participant.setWrestler(w);
    segment.setParticipants(new HashSet<>(Set.of(participant)));

    return segment;
  }

  private Show showWithScore(final double score) {
    Show s = new Show();
    s.setId((long) (Math.random() * 10_000));
    s.setName("Past Show");
    s.setUniverse(universe);
    s.setQualityScore(score);
    return s;
  }

  private ShowTemplate makePleTemplate() {
    var showType = new ShowType();
    showType.setName("Premium Live Event (PLE)");
    showType.setCategory(ShowCategory.PLE);
    var template = new ShowTemplate();
    template.setShowType(showType);
    return template;
  }
}
