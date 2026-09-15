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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.show.PromoBookingService;
import com.github.javydreamercsw.management.service.show.ShowQualityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * One-time migration of historical match ratings to the ATW-gegc rebalanced score.
 *
 * <p>Shows finalized before the rebalance carry match ratings scored under the old formula (NPC
 * ceiling 55, bare floor clamped to one star). This contributor recomputes every COMPLETED match
 * segment's rating with the new {@link ShowQualityService#computeAndPersist} formula, then flips a
 * {@code game_setting} flag so it never runs again. Re-runs are additionally safe by construction:
 * the new score reads crowd noise (immutable after adjudication), not the stored rating.
 *
 * <p>Promos are skipped — they kept their adjudication dice roll all along and are unaffected by
 * the rebalance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentRatingRebalanceSync implements DataSyncContributor {

  /** Global game_setting marker: once "true", the backfill never runs again. */
  public static final String REBALANCE_DONE_KEY = "segment_rating_rebalance_gegc_done";

  private final SegmentRepository segmentRepository;
  private final ShowRepository showRepository;
  private final ShowQualityService showQualityService;
  private final PromoBookingService promoBookingService;
  private final GameSettingService gameSettingService;

  @Override
  public void sync() {
    if (gameSettingService
        .findByKeyForUniverse(REBALANCE_DONE_KEY, null)
        .map(setting -> Boolean.parseBoolean(setting.getValue()))
        .orElse(false)) {
      log.debug("Segment rating rebalance already applied — skipping.");
      return;
    }

    // Shows projected from completed segments — never ShowRepository.findAll(), whose call count
    // some cache-eviction tests pin (ShowServiceIT.testCreateShowEvictsCache).
    List<Show> shows = segmentRepository.findShowsWithCompletedSegments();
    int segmentsRescored = 0;
    int showsRescored = 0;
    for (Show show : shows) {
      List<Segment> matches =
          segmentRepository.findByShow(show).stream()
              .filter(s -> s.getStatus() == SegmentStatus.COMPLETED)
              .filter(s -> !promoBookingService.isPromoSegment(s))
              .toList();
      if (matches.isEmpty()) {
        continue;
      }

      // Rescore through the same code path new shows use — one formula, no drift. computeAndPersist
      // also sets show.qualityScore; the save persists it (shows from the projection are detached
      // outside a transaction, and computeAndPersist only saves segments).
      showQualityService.computeAndPersist(show, matches);
      segmentsRescored += matches.size();
      showsRescored++;
      showRepository.save(show);
    }

    gameSettingService.save(REBALANCE_DONE_KEY, "true");
    log.info(
        "Segment rating rebalance complete: {} match segment(s) rescored across {} show(s).",
        segmentsRescored,
        showsRescored);
  }
}
