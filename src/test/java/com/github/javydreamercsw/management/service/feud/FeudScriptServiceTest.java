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
package com.github.javydreamercsw.management.service.feud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeudScriptServiceTest {

  @Mock private FeudScriptRepository feudScriptRepository;
  @Mock private FeudScriptBeatRepository feudScriptBeatRepository;
  @Mock private RivalryService rivalryService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private GameSettingService gameSettingService;
  @Mock private UniverseContextService universeContextService;
  @Mock private ContenderSelectionService contenderSelectionService;

  @InjectMocks private FeudScriptService service;

  // ── findBeatForSegment ────────────────────────────────────────────────────

  @Test
  void findBeatForSegment_nullId_returnsEmpty() {
    Segment segment = new Segment(); // no ID set

    Optional<FeudScriptBeat> result = service.findBeatForSegment(segment);

    assertThat(result).isEmpty();
    verifyNoInteractions(feudScriptBeatRepository);
  }

  @Test
  void findBeatForSegment_withId_delegatesToRepository() {
    Segment segment = new Segment();
    segment.setId(42L);

    FeudScriptBeat beat = new FeudScriptBeat();
    FeudScript script = new FeudScript();
    script.setName("The Bloodline Saga");
    beat.setScript(script);
    beat.setBeatOrder(2);

    when(feudScriptBeatRepository.findByActualSegment(segment)).thenReturn(Optional.of(beat));

    Optional<FeudScriptBeat> result = service.findBeatForSegment(segment);

    assertThat(result).isPresent();
    assertThat(result.get().getScript().getName()).isEqualTo("The Bloodline Saga");
    assertThat(result.get().getBeatOrder()).isEqualTo(2);
    verify(feudScriptBeatRepository).findByActualSegment(segment);
  }

  @Test
  void findBeatForSegment_notLinked_returnsEmpty() {
    Segment segment = new Segment();
    segment.setId(99L);

    when(feudScriptBeatRepository.findByActualSegment(segment)).thenReturn(Optional.empty());

    assertThat(service.findBeatForSegment(segment)).isEmpty();
  }

  // ── createFromWizard validation ───────────────────────────────────────────

  @Test
  void createFromWizard_fewerThanTwoWrestlers_throws() {
    Wrestler solo = new Wrestler();
    solo.setId(1L);

    assertThatThrownBy(() -> service.createFromWizard("Solo Arc", List.of(solo), 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 2");
  }

  // ── autoCompleteBeatForSegment ────────────────────────────────────────────

  @Test
  void autoCompleteBeatForSegment_fewerThanTwoParticipants_returnsEmpty() {
    Segment segment = new Segment(); // no participants

    assertThat(service.autoCompleteBeatForSegment(segment)).isEmpty();
    verifyNoInteractions(feudScriptBeatRepository);
  }

  @Test
  void autoCompleteBeatForSegment_noMatchingBeats_returnsEmpty() {
    Segment segment = new Segment();
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(List.of(1L, 2L)))
        .thenReturn(List.of());
    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(List.of(2L, 1L)))
        .thenReturn(List.of());

    assertThat(service.autoCompleteBeatForSegment(segment)).isEmpty();
  }

  @Test
  void autoCompleteBeatForSegment_matchFound_marksBeatCompletedAndReturnsIt() {
    Segment segment = new Segment();
    segment.setId(55L);
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);

    FeudScript script = new FeudScript();
    script.setName("Rivalry Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);

    Optional<FeudScriptBeat> result = service.autoCompleteBeatForSegment(segment);

    assertThat(result).isPresent();
    assertThat(result.get().getBeatStatus()).isEqualTo(FeudScriptBeatStatus.COMPLETED);
    assertThat(result.get().getActualSegment()).isEqualTo(segment);
    verify(feudScriptBeatRepository).save(beat);
  }

  @Test
  void autoCompleteBeatForSegment_allBeatsDone_completesScript() {
    Segment segment = new Segment();
    segment.setId(66L);
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);

    FeudScript script = new FeudScript();
    script.setName("Final Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    service.autoCompleteBeatForSegment(segment);

    assertThat(script.getStatus()).isEqualTo(FeudScriptStatus.COMPLETED);
    verify(feudScriptRepository).save(script);
  }

  // ── contender designation (CONTENDER_DESIGNATION beat outcome) ───────────

  @Test
  void autoCompleteBeatForSegment_beatWithContenderTitle_designatesWinner() {
    Segment segment = new Segment();
    segment.setId(77L);
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    w1.setName("Winner");
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment.setWinners(List.of(w1));

    Title title = new Title();
    title.setName("World Title");

    FeudScript script = new FeudScript();
    script.setName("Contender Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    beat.setContenderTitle(title);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    service.autoCompleteBeatForSegment(segment);

    verify(contenderSelectionService).designateAsContender(title, w1);
  }

  @Test
  void autoCompleteBeatForSegment_noContenderTitle_doesNotDesignate() {
    Segment segment = new Segment();
    segment.setId(78L);
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment.setWinners(List.of(w1));

    FeudScript script = new FeudScript();
    script.setName("Plain Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    service.autoCompleteBeatForSegment(segment);

    verifyNoInteractions(contenderSelectionService);
  }

  @Test
  void completeBeat_contenderTitleButNoWinner_doesNotDesignate() {
    Segment segment = new Segment();
    segment.setId(79L);

    Title title = new Title();
    title.setName("World Title");

    FeudScript script = new FeudScript();
    script.setName("Contender Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    beat.setContenderTitle(title);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    service.completeBeat(beat, segment);

    verifyNoInteractions(contenderSelectionService);
  }

  // ── getDefaultMaxPleAppearances ───────────────────────────────────────────

  @Test
  void getDefaultMaxPleAppearances_delegatesToGameSettings() {
    when(gameSettingService.getMaxPleFeudAppearances()).thenReturn(2);

    assertThat(service.getDefaultMaxPleAppearances()).isEqualTo(2);
    verify(gameSettingService).getMaxPleFeudAppearances();
  }
}
