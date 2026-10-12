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
package com.github.javydreamercsw.management.migration;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.domain.news.NewsRepository;
import com.github.javydreamercsw.management.domain.rivalry.HeatEvent;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.rivalry.heatevent.HeatEventRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.GameClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-anchors wall-clock timestamps in game-domain tables to in-game (kayfabe) dates (ATW-2f8d).
 * Before the game-anchored {@link GameClock} (ATW-07hw) shipped, several services stamped {@code
 * Instant.now()}-derived timestamps into game tables, so rows carry real-world dates that drift out
 * of kayfabe (a rivalry "started" months after its own feud played out).
 *
 * <p>Two passes:
 *
 * <ol>
 *   <li><b>Segment normalization</b> — every segment's {@code segment_date} is set to its show's
 *       kayfabe show-date at start of day (the {@code ShowPlanningService} convention). Shows are
 *       correctly dated, so this is safe and fixes both future-dated and historically mismatched
 *       segments in one rule. Segments on unscheduled shows keep their date.
 *   <li><b>Event re-anchoring</b> — rows dated after the current game date are moved onto the game
 *       date. Traceable rows (heat events whose reason names a segment) anchor to the rivalry's
 *       latest corrected segment date; everything else (NPC generation batches, heat decay,
 *       drama-event rolls, NPC injuries, news) anchors to the game date with id-based staggering so
 *       timestamp chains keep their ordering. Duration-derived rivalry ends recompute from their
 *       corrected start so the kayfabe duration is preserved.
 * </ol>
 *
 * <p>Rows already at or before the game date are left untouched — the game date has been stable
 * across the whole contamination window, so nothing else needs interpolation.
 */
@Component
@Order(20)
@Slf4j
@RequiredArgsConstructor
public class GameDateReAnchorMigration implements DataMigration {

  /** Heat-event reasons that name a segment, making the row traceable to a show date. */
  private static final String FROM_SEGMENT_PREFIX = "From segment:";

  private static final String AI_BOOKED_PREFIX = "AI-booked segment:";

  private final SegmentRepository segmentRepository;
  private final RivalryRepository rivalryRepository;
  private final HeatEventRepository heatEventRepository;
  private final DramaEventRepository dramaEventRepository;
  private final InjuryRepository injuryRepository;
  private final NewsRepository newsRepository;
  private final GameClock gameClock;

  @Override
  public String id() {
    return "re-anchor-wall-clock-game-dates";
  }

  @Override
  @Transactional
  public void migrate() {
    Instant gameNow = gameClock.gameNow();
    log.info("Re-anchoring game-domain dates to the current game date {}", gameClock.gameToday());

    // Pass A — segments normalize to their show's kayfabe date; builds the rivalry traceability
    // map (rivalryId -> latest corrected segment date) while we're there.
    Map<Long, Instant> rivalryAnchors = normalizeSegments();
    rivalryAnchors.forEach(
        (rivalryId, anchor) -> log.debug("Rivalry {} anchor: {}", rivalryId, anchor));

    // Pass B — event rows dated after the game date move onto the game timeline.
    reAnchorHeatEvents(gameNow, rivalryAnchors);
    reAnchorRivalries(gameNow);
    reAnchorDramaEvents(gameNow);
    reAnchorInjuries(gameNow);
    reAnchorNews(gameNow);
  }

  /** Segment dates normalize to their show's date; returns rivalryId -> latest segment anchor. */
  private Map<Long, Instant> normalizeSegments() {
    Map<Long, Instant> rivalryAnchors = new HashMap<>();
    List<Segment> changed = new ArrayList<>();
    for (Segment segment : segmentRepository.findAllWithShow()) {
      Show show = segment.getShow();
      if (show == null || show.getShowDate() == null) {
        continue; // no anchor available — leave the date alone
      }
      Instant corrected = gameClock.gameNowOn(show.getShowDate());
      rivalryAnchors.merge(
          segment.getRivalryId(),
          corrected,
          (a, b) -> a.isAfter(b) ? a : b); // latest segment date wins
      if (!corrected.equals(segment.getSegmentDate())) {
        segment.setSegmentDate(corrected);
        changed.add(segment);
      }
    }
    segmentRepository.saveAll(changed);
    log.info("Normalized {} segment dates to their shows' kayfabe dates", changed.size());
    return rivalryAnchors;
  }

  private void reAnchorHeatEvents(Instant gameNow, Map<Long, Instant> rivalryAnchors) {
    List<HeatEvent> changed = new ArrayList<>();
    for (HeatEvent event : heatEventRepository.findAll()) {
      if (event.getEventDate() != null && !event.getEventDate().isAfter(gameNow)) {
        continue; // already on the game timeline
      }
      Instant anchor = rivalryAnchors.get(event.getRivalry().getId());
      if (anchor != null && isSegmentSourced(event.getReason())) {
        event.setEventDate(anchor);
      } else {
        event.setEventDate(gameClock.gameNowStaggered());
      }
      event.setCreationDate(event.getEventDate());
      changed.add(event);
    }
    heatEventRepository.saveAll(changed);
    log.info("Re-anchored {} heat events", changed.size());
  }

  private boolean isSegmentSourced(String reason) {
    return reason != null
        && (reason.startsWith(FROM_SEGMENT_PREFIX) || reason.startsWith(AI_BOOKED_PREFIX));
  }

  private void reAnchorRivalries(Instant gameNow) {
    List<Rivalry> changed = new ArrayList<>();
    for (Rivalry rivalry : rivalryRepository.findAll()) {
      if (rivalry.getStartedDate() != null && rivalry.getStartedDate().isAfter(gameNow)) {
        Duration duration = null;
        if (rivalry.getEndedDate() != null) {
          duration = Duration.between(rivalry.getStartedDate(), rivalry.getEndedDate());
        }
        rivalry.setStartedDate(gameClock.gameNowStaggered());
        if (duration != null) {
          // Preserve the kayfabe duration when moving the start.
          rivalry.setEndedDate(rivalry.getStartedDate().plus(duration));
        }
        changed.add(rivalry);
      } else if (rivalry.getEndedDate() != null
          && rivalry.getEndedDate().isAfter(gameNow)
          && rivalry.getStartedDate() != null
          && !rivalry.getStartedDate().isAfter(gameNow)) {
        rivalry.setEndedDate(gameClock.gameNowStaggered());
        changed.add(rivalry);
      }
    }
    rivalryRepository.saveAll(changed);
    log.info("Re-anchored {} rivalries", changed.size());
  }

  private void reAnchorDramaEvents(Instant gameNow) {
    List<DramaEvent> changed = new ArrayList<>();
    for (DramaEvent event : dramaEventRepository.findAll()) {
      if (event.getEventDate() != null && event.getEventDate().isAfter(gameNow)) {
        event.setEventDate(gameClock.gameNowStaggered());
        if (event.getProcessedDate() != null && event.getProcessedDate().isAfter(gameNow)) {
          event.setProcessedDate(event.getEventDate());
        }
        changed.add(event);
      }
    }
    dramaEventRepository.saveAll(changed);
    log.info("Re-anchored {} drama events", changed.size());
  }

  private void reAnchorInjuries(Instant gameNow) {
    List<Injury> changed = new ArrayList<>();
    for (Injury injury : injuryRepository.findAll()) {
      if (injury.getInjuryDate() != null && injury.getInjuryDate().isAfter(gameNow)) {
        Duration healing = null;
        if (injury.getHealedDate() != null) {
          healing = Duration.between(injury.getInjuryDate(), injury.getHealedDate());
        }
        injury.setInjuryDate(gameClock.gameNowStaggered());
        if (healing != null) {
          injury.setHealedDate(injury.getInjuryDate().plus(healing));
        }
        changed.add(injury);
      }
    }
    injuryRepository.saveAll(changed);
    log.info("Re-anchored {} injuries", changed.size());
  }

  private void reAnchorNews(Instant gameNow) {
    List<NewsItem> changed = new ArrayList<>();
    for (NewsItem item : newsRepository.findAll()) {
      if (item.getPublishDate() != null && item.getPublishDate().isAfter(gameNow)) {
        item.setPublishDate(gameClock.gameNowStaggered());
        changed.add(item);
      }
    }
    newsRepository.saveAll(changed);
    log.info("Re-anchored {} news items", changed.size());
  }
}
