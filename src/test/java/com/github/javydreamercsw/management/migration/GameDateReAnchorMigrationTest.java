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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.news.NewsRepository;
import com.github.javydreamercsw.management.domain.rivalry.HeatEvent;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.rivalry.heatevent.HeatEventRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.GameClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the wall-clock → kayfabe date re-anchor migration (ATW-2f8d): segment dates
 * normalize to their show's date; event rows re-anchor to the current game date with id-based
 * staggering; duration-derived rivalry ends recompute from corrected start dates.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameDateReAnchorMigrationTest {

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 7, 7);
  private static final LocalDate SHOW_DATE = LocalDate.of(2026, 5, 31);
  private static final Instant GAME_MIDNIGHT =
      GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant();

  @Mock private SegmentRepository segmentRepository;
  @Mock private ShowRepository showRepository;
  @Mock private RivalryRepository rivalryRepository;
  @Mock private HeatEventRepository heatEventRepository;
  @Mock private DramaEventRepository dramaEventRepository;
  @Mock private InjuryRepository injuryRepository;
  @Mock private NewsRepository newsRepository;
  @Mock private GameClock gameClock;

  private GameDateReAnchorMigration migration;

  @BeforeEach
  void setUp() {
    migration =
        new GameDateReAnchorMigration(
            segmentRepository,
            rivalryRepository,
            heatEventRepository,
            dramaEventRepository,
            injuryRepository,
            newsRepository,
            gameClock);
    when(gameClock.gameToday()).thenReturn(GAME_DATE);
    when(gameClock.gameNow())
        .thenReturn(GAME_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(gameClock.gameNowOn(any()))
        .thenAnswer(
            inv ->
                ((LocalDate) inv.getArgument(0)).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(gameClock.gameNowStaggered())
        .thenAnswer(
            inv -> {
              // Deterministic stagger: game midnight + 1s per call within the test.
              staggerCounter.incrementAndGet();
              return GAME_DATE
                  .atStartOfDay(ZoneId.systemDefault())
                  .plusSeconds(staggerCounter.get())
                  .toInstant();
            });
  }

  private final AtomicLong staggerCounter = new AtomicLong();

  // ── Pass A: segment_date normalization ─────────────────────────────────────

  @Test
  @DisplayName("Pass A: every segment date normalizes to its show's date at start of day")
  void migrate_segments_normalizedToShowDate() {
    Show show = new Show();
    show.setName("Legends Converge");
    show.setShowDate(SHOW_DATE);
    Segment contaminated = new Segment();
    contaminated.setSegmentDate(Instant.parse("2026-08-31T20:45:07Z")); // wall clock
    contaminated.setShow(show);
    when(segmentRepository.findAllWithShow()).thenReturn(List.of(contaminated));

    migration.migrate();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<Segment>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(segmentRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .singleElement()
        .satisfies(
            s ->
                assertThat(s.getSegmentDate())
                    .isEqualTo(SHOW_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  @DisplayName("Pass A: segments on an unscheduled show keep their date (no anchor available)")
  void migrate_segments_unscheduledShow_skipped() {
    Show show = new Show(); // no showDate
    Segment orphan = new Segment();
    orphan.setSegmentDate(Instant.now());
    orphan.setShow(show);
    when(segmentRepository.findAllWithShow()).thenReturn(List.of(orphan));

    migration.migrate();

    verify(segmentRepository).saveAll(List.of()); // nothing changed
  }

  // ── Pass B: event-row re-anchoring ─────────────────────────────────────────

  @Test
  @DisplayName("Pass B: traceable heat events anchor to their rivalry's latest segment show date")
  void migrate_heatEvents_traceable_anchoredToShowDate() {
    Show show = new Show();
    show.setShowDate(SHOW_DATE);
    Segment segment = new Segment();
    segment.setSegmentDate(SHOW_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant());
    segment.setShow(show);
    segment.setRivalryId(9L);

    Rivalry rivalry = new Rivalry();
    rivalry.setId(9L);

    HeatEvent fromSegment = new HeatEvent();
    fromSegment.setReason("From segment: Promo");
    fromSegment.setEventDate(Instant.parse("2026-10-10T00:31:44Z")); // wall clock
    fromSegment.setRivalry(rivalry);

    when(segmentRepository.findAllWithShow()).thenReturn(List.of(segment));
    when(rivalryRepository.findAll()).thenReturn(List.of(rivalry));
    when(heatEventRepository.findAll()).thenReturn(List.of(fromSegment));

    migration.migrate();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<HeatEvent>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(heatEventRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getEventDate())
                    .isEqualTo(SHOW_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  @DisplayName("Pass B: untraceable batch rows stagger forward on the game date")
  void migrate_batchRows_staggerOnGameDate() {
    Rivalry npcBatch1 = new Rivalry();
    npcBatch1.setId(1L);
    npcBatch1.setStartedDate(Instant.parse("2026-10-10T00:31:44Z"));
    Rivalry npcBatch2 = new Rivalry();
    npcBatch2.setId(2L);
    npcBatch2.setStartedDate(Instant.parse("2026-10-10T00:31:44Z"));
    when(rivalryRepository.findAll()).thenReturn(List.of(npcBatch1, npcBatch2));
    when(segmentRepository.findAllWithShow()).thenReturn(List.of());
    when(heatEventRepository.findAll()).thenReturn(List.of());
    when(segmentRepository.findByShow(any())).thenReturn(List.of());

    migration.migrate();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<Rivalry>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(rivalryRepository).saveAll(captor.capture());
    List<Rivalry> saved = (List<Rivalry>) captor.getValue();
    assertThat(saved).hasSize(2);
    // Both land on the game day; the stagger keeps them strictly ordered.
    Instant first = saved.get(0).getStartedDate();
    Instant second = saved.get(1).getStartedDate();
    assertThat(first).isAfterOrEqualTo(GAME_MIDNIGHT);
    assertThat(first).isBefore(GAME_MIDNIGHT.plus(1, ChronoUnit.DAYS));
    assertThat(second).isAfterOrEqualTo(GAME_MIDNIGHT);
    assertThat(second).isBefore(GAME_MIDNIGHT.plus(1, ChronoUnit.DAYS));
    assertThat(second).isAfter(first);
  }

  @Test
  @DisplayName("Pass B: rows already on or before the game date are left alone")
  void migrate_cleanRows_untouched() {
    Rivalry clean = new Rivalry();
    clean.setId(1L);
    clean.setStartedDate(GAME_MIDNIGHT.minus(30, ChronoUnit.DAYS));
    when(rivalryRepository.findAll()).thenReturn(List.of(clean));
    when(segmentRepository.findAllWithShow()).thenReturn(List.of());
    when(heatEventRepository.findAll()).thenReturn(List.of());
    when(segmentRepository.findByShow(any())).thenReturn(List.of());

    migration.migrate();

    verify(rivalryRepository).saveAll(List.of());
  }

  @Test
  @DisplayName("Pass B: duration-derived rivalry ends recompute from corrected start dates")
  void migrate_durationEnds_recomputed() {
    // A wall-clock rivalry started AFTER the game date (the 2026-10-10 NPC batch shape) and ended
    // 90d later; moving the start to the game date must carry the end along (start + 90d),
    // preserving the kayfabe duration.
    Rivalry expired = new Rivalry();
    expired.setId(3L);
    Instant wallStart = GAME_DATE.plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant();
    expired.setStartedDate(wallStart);
    expired.setEndedDate(wallStart.plus(90, ChronoUnit.DAYS));
    when(rivalryRepository.findAll()).thenReturn(List.of(expired));
    when(segmentRepository.findAllWithShow()).thenReturn(List.of());
    when(heatEventRepository.findAll()).thenReturn(List.of());
    when(segmentRepository.findByShow(any())).thenReturn(List.of());

    migration.migrate();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<Rivalry>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(rivalryRepository).saveAll(captor.capture());
    assertThat(captor.getValue())
        .singleElement()
        .satisfies(
            r -> {
              // Start re-anchored onto the game day (staggered, so within the day, not exactly
              // midnight); the end carries the preserved 90-day duration.
              assertThat(r.getStartedDate())
                  .isAfterOrEqualTo(GAME_MIDNIGHT)
                  .isBefore(GAME_MIDNIGHT.plus(1, ChronoUnit.DAYS));
              assertThat(r.getEndedDate()).isEqualTo(r.getStartedDate().plus(90, ChronoUnit.DAYS));
            });
  }
}
