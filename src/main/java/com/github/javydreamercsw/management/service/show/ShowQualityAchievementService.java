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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShowQualityAchievementService
    implements ApplicationListener<AdjudicationCompletedEvent> {

  private static final double STREAK_THRESHOLD = 3.5;
  private static final int STREAK_WINDOW = 5;

  private final SegmentRepository segmentRepository;
  private final ShowRepository showRepository;
  private final PromoBookingService promoBookingService;
  private final LegacyService legacyService;
  private final SecurityUtils securityUtils;

  @Override
  @Transactional
  public void onApplicationEvent(@NonNull final AdjudicationCompletedEvent event) {
    Show show = event.getShow();
    Double qualityScore = show.getQualityScore();
    if (qualityScore == null) {
      return;
    }

    List<Segment> segments = segmentRepository.findByShow(show);
    boolean isPle = show.isPremiumLiveEvent();

    for (Segment segment : segments) {
      if (promoBookingService.isPromoSegment(segment)) {
        continue;
      }
      Integer rating = segment.getSegmentRating();
      if (rating == null) {
        continue;
      }
      boolean hasStipulation = !segment.getSegmentRules().isEmpty();
      boolean isMainEvent = segment.isMainEvent();
      boolean isTitleMatch = Boolean.TRUE.equals(segment.getIsTitleSegment());

      for (var wrestler : segment.getWrestlers()) {
        Account account = wrestler.getAccount();
        if (account == null) {
          continue;
        }
        unlockSegmentAchievements(
            account, rating, isPle, isMainEvent, isTitleMatch, hasStipulation);
      }
    }

    securityUtils
        .getAuthenticatedUser()
        .map(details -> details.getAccount())
        .ifPresent(
            account -> {
              unlockShowAchievements(account, qualityScore);
              checkStreakAchievements(account, show, qualityScore);
            });
  }

  private void unlockSegmentAchievements(
      final Account account,
      final int rating,
      final boolean isPle,
      final boolean isMainEvent,
      final boolean isTitleMatch,
      final boolean hasStipulation) {

    if (rating == 100) {
      legacyService.unlockAchievement(account, "FIVE_STAR_CLASSIC");
    }
    if (rating >= 85) {
      legacyService.unlockAchievement(account, "SEGMENT_QUALITY_GREAT");
      if (isMainEvent) {
        legacyService.unlockAchievement(account, "MAIN_EVENT_CLASSIC");
      }
    }
    if (rating >= 80 && isTitleMatch) {
      legacyService.unlockAchievement(account, "SEGMENT_QUALITY_TITLE_CLASSIC");
    }
    if (rating >= 70) {
      legacyService.unlockAchievement(account, "SEGMENT_QUALITY_GOOD");
    }
    if (rating >= 75) {
      if (!isPle) {
        legacyService.unlockAchievement(account, "TV_GOLD");
      }
      if (hasStipulation) {
        legacyService.unlockAchievement(account, "STIPULATION_SPECIALIST");
      }
    }
    if (rating >= 90 && isPle) {
      legacyService.unlockAchievement(account, "PLE_SHOWSTOPPER");
    }
  }

  private void unlockShowAchievements(final Account account, final double qualityScore) {
    if (qualityScore >= 4.0) {
      legacyService.unlockAchievement(account, "SHOW_QUALITY_GREAT");
    }
    if (qualityScore >= 3.5) {
      legacyService.unlockAchievement(account, "SHOW_QUALITY_GOOD");
    }
    if (qualityScore >= 2.5) {
      legacyService.unlockAchievement(account, "SHOW_QUALITY_SOLID");
    }
    if (qualityScore == 5.0) {
      legacyService.unlockAchievement(account, "SHOW_QUALITY_ELITE");
    }
  }

  private void checkStreakAchievements(
      final Account account, final Show show, final double qualityScore) {
    if (show.getUniverse() == null || qualityScore < STREAK_THRESHOLD) {
      return;
    }

    List<Show> recentRated =
        showRepository.findByUniverseAndQualityScoreIsNotNullOrderByShowDateDesc(
            show.getUniverse(), PageRequest.of(0, STREAK_WINDOW));

    long consecutive =
        recentRated.stream()
            .takeWhile(s -> s.getQualityScore() != null && s.getQualityScore() >= STREAK_THRESHOLD)
            .count();

    if (consecutive >= 5) {
      legacyService.unlockAchievement(account, "QUALITY_STREAK_5");
    }
    if (consecutive >= 3) {
      legacyService.unlockAchievement(account, "QUALITY_STREAK_3");
    }
  }
}
