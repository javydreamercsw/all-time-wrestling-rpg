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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservation;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the ATW-lxnn one-time repair: unlink mismatched beat completions. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MismatchedBeatCompletionMigrationTest {

  @Mock private FeudScriptBeatRepository beatRepository;
  @Mock private FeudScriptRepository scriptRepository;
  @Mock private SegmentRepository segmentRepository;

  @InjectMocks private MismatchedBeatCompletionMigration migration;

  @Test
  void migrate_beatCompletedByWrongTypeSegment_unlinksAndReverts() {
    // Production shape (ATW-lxnn): a promo consumed a One on One ladder-match title beat.
    FeudScript script = activeScript();
    Segment promo = segment("Promo", 355L, weeklyShow());
    FeudScriptBeat beat = completedBeat(script, promo, "One on One", null, false);

    when(beatRepository.findAll()).thenReturn(List.of(beat));
    when(scriptRepository.findAll()).thenReturn(List.of(script));

    migration.migrate();

    assertThat(beat.getBeatStatus()).isEqualTo(FeudScriptBeatStatus.PENDING);
    assertThat(beat.getActualSegment()).isNull();
    assertThat(promo.getIsTitleSegment()).isFalse();
    assertThat(promo.isContenderMatch()).isFalse();
    assertThat(promo.getTitles()).isEmpty();
    verify(segmentRepository).save(promo);
    verify(beatRepository).save(beat);
  }

  @Test
  void migrate_beatCompletedOnWrongShow_unlinks() {
    FeudScript script = activeScript();
    Segment weeklyPromo = segment("Promo", 355L, weeklyShow());
    Show targetPle = showWithCategory(452L, ShowCategory.PLE);
    FeudScriptBeat beat = completedBeat(script, weeklyPromo, "Promo", targetPle, false);

    when(beatRepository.findAll()).thenReturn(List.of(beat));
    when(scriptRepository.findAll()).thenReturn(List.of(script));

    migration.migrate();

    assertThat(beat.getBeatStatus()).isEqualTo(FeudScriptBeatStatus.PENDING);
  }

  @Test
  void migrate_legitimateCompletion_untouched() {
    FeudScript script = activeScript();
    Segment match = segment("One on One", 452L, showWithCategory(452L, ShowCategory.PLE));
    FeudScriptBeat beat = completedBeat(script, match, "One on One", null, true);

    when(beatRepository.findAll()).thenReturn(List.of(beat));

    migration.migrate();

    assertThat(beat.getBeatStatus()).isEqualTo(FeudScriptBeatStatus.COMPLETED);
    assertThat(beat.getActualSegment()).isSameAs(match);
    verify(beatRepository, never()).save(any());
  }

  @Test
  void migrate_completedBeatWithoutSegment_untouched() {
    FeudScript script = activeScript();
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    beat.setScript(script);

    when(beatRepository.findAll()).thenReturn(List.of(beat));

    migration.migrate();

    verify(beatRepository, never()).save(any());
  }

  @Test
  void migrate_unlinkedBeat_reopensCompletedScript() {
    // The bug completed the final beat, flipping the whole arc to COMPLETED — the repair
    // reverts the beat to PENDING and must reopen the arc so it can be re-booked.
    FeudScript script = activeScript();
    script.setStatus(FeudScriptStatus.COMPLETED);
    Segment promo = segment("Promo", 355L, weeklyShow());
    FeudScriptBeat beat = completedBeat(script, promo, "One on One", null, false);

    when(beatRepository.findAll()).thenReturn(List.of(beat));
    when(scriptRepository.findAll()).thenReturn(List.of(script));

    migration.migrate();

    assertThat(script.getStatus()).isEqualTo(FeudScriptStatus.ACTIVE);
    verify(scriptRepository).save(script);
  }

  @Test
  void migrate_beatWithReservation_resetsReservationToPending() {
    FeudScript script = activeScript();
    Segment promo = segment("Promo", 355L, weeklyShow());
    FeudScriptBeat beat = completedBeat(script, promo, "One on One", null, false);
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    reservation.setSegment(promo);
    reservation.setStatus(ShowSegmentReservationStatus.FILLED);
    beat.setReservation(reservation);

    when(beatRepository.findAll()).thenReturn(List.of(beat));
    when(scriptRepository.findAll()).thenReturn(List.of(script));

    migration.migrate();

    assertThat(reservation.getStatus()).isEqualTo(ShowSegmentReservationStatus.PENDING);
    assertThat(reservation.getSegment()).isNull();
  }

  @Test
  void migrate_nothingMismatched_savesNothing() {
    FeudScript script = activeScript();
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    beat.setScript(script);
    when(beatRepository.findAll()).thenReturn(List.of(beat));

    migration.migrate();

    verify(beatRepository, never()).save(any());
    verify(segmentRepository, never()).save(any());
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private FeudScript activeScript() {
    FeudScript script = new FeudScript();
    script.setId(1L);
    script.setName("Bobby Lashley vs Shelton Benjamin Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    return script;
  }

  private Segment segment(String typeName, Long showId, Show show) {
    SegmentType type = new SegmentType();
    type.setName(typeName);
    Segment segment = new Segment();
    segment.setId(1217L);
    segment.setSegmentType(type);
    segment.setShow(show);
    return segment;
  }

  private Show weeklyShow() {
    return showWithCategory(355L, ShowCategory.WEEKLY);
  }

  private Show showWithCategory(Long id, ShowCategory category) {
    ShowType type = new ShowType();
    type.setCategory(category);
    ShowTemplate template = new ShowTemplate();
    template.setShowType(type);
    Show show = new Show();
    show.setId(id);
    show.setTemplate(template);
    return show;
  }

  private FeudScriptBeat completedBeat(
      FeudScript script, Segment segment, String beatType, Show targetShow, boolean culmination) {
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setId(5L);
    beat.setBeatOrder(5);
    beat.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    beat.setSegmentType(beatType);
    beat.setTargetShow(targetShow);
    beat.setCulmination(culmination);
    beat.setActualSegment(segment);
    beat.setScript(script);
    script.getBeats().add(beat);
    // The completion copied title stakes onto the segment (inert on promos but present).
    Title title = new Title();
    title.setId(1L);
    title.setName("ATW World");
    segment.setIsTitleSegment(true);
    segment.getTitles().add(title);
    return beat;
  }
}
