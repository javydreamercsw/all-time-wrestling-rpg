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
package com.github.javydreamercsw.management.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.ShowQualityService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentRatingRebalanceSyncTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private ShowRepository showRepository;
  @Mock private ShowQualityService showQualityService;
  @Mock private PromoBookingService promoBookingService;
  @Mock private GameSettingService gameSettingService;

  private SegmentRatingRebalanceSync sync;

  private Show show;
  private Segment match;
  private Segment promo;

  @BeforeEach
  void setUp() {
    sync =
        new SegmentRatingRebalanceSync(
            segmentRepository,
            showRepository,
            showQualityService,
            promoBookingService,
            gameSettingService);

    show = new Show();
    show.setId(1L);
    show.setName("Old Show");
    show.setQualityScore(1.5);

    match = new Segment();
    match.setId(10L);
    match.setStatus(SegmentStatus.COMPLETED);
    match.setCrowdNoiseLevel(80);

    promo = new Segment();
    promo.setId(11L);
    promo.setStatus(SegmentStatus.COMPLETED);
    promo.setCrowdNoiseLevel(80);

    when(showRepository.findAll()).thenReturn(List.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(List.of(match, promo));
    when(promoBookingService.isPromoSegment(promo)).thenReturn(true);
    when(promoBookingService.isPromoSegment(match)).thenReturn(false);
  }

  @Test
  void firstRun_rescoresMatchesAndSetsFlag() {
    when(gameSettingService.findByKeyForUniverse(
            SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, null))
        .thenReturn(Optional.empty());

    sync.sync();

    // Matches rescored through the shared computeAndPersist path; promos untouched.
    verify(showQualityService).computeAndPersist(show, List.of(match));
    verify(gameSettingService).save(SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, "true");
  }

  @Test
  void flagAlreadySet_neverRunsAgain() {
    GameSetting done = new GameSetting();
    done.setSettingKey(SegmentRatingRebalanceSync.REBALANCE_DONE_KEY);
    done.setValue("true");
    when(gameSettingService.findByKeyForUniverse(
            SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, null))
        .thenReturn(Optional.of(done));

    sync.sync();

    verify(showQualityService, never()).computeAndPersist(any(), any());
    verify(gameSettingService, never()).save(any(), any());
  }

  @Test
  void showLevelQualityRecomputedFromRescoredSegments() {
    when(gameSettingService.findByKeyForUniverse(
            SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, null))
        .thenReturn(Optional.empty());

    // computeAndPersist's contract: mutate segment ratings in place AND set show.qualityScore.
    when(showQualityService.computeAndPersist(any(), any()))
        .thenAnswer(
            inv -> {
              Show s = inv.getArgument(0);
              s.setQualityScore(3.5);
              return 3.5;
            });
    match.setSegmentRating(70); // what computeAndPersist would persist

    sync.sync();

    assertThat(show.getQualityScore()).isEqualTo(3.5);
    verify(showRepository).save(show);
  }

  @Test
  void noCompletedMatches_stillSetsFlagWithoutScoring() {
    when(gameSettingService.findByKeyForUniverse(
            SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, null))
        .thenReturn(Optional.empty());
    match.setStatus(SegmentStatus.BOOKED);

    sync.sync();

    verify(showQualityService, never()).computeAndPersist(any(), any());
    verify(gameSettingService).save(SegmentRatingRebalanceSync.REBALANCE_DONE_KEY, "true");
  }
}
