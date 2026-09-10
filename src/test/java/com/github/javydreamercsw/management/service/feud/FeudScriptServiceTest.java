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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservation;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private WrestlerService wrestlerService;
  @Mock private com.github.javydreamercsw.management.service.segment.SegmentService segmentService;
  @Mock private com.github.javydreamercsw.management.service.show.ShowService showService;
  @Mock private com.github.javydreamercsw.management.service.title.TitleService titleService;
  @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

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
  void autoCompleteBeatForSegment_beatWithContenderTitle_copiesFlagsToPendingSegment() {
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

    // Segment is still PENDING: the contender designation rides on the segment's own
    // adjudication (isContenderMatch + title), not a direct designation here.
    assertThat(segment.isContenderMatch()).isTrue();
    assertThat(segment.getTitles()).containsExactly(title);
    verify(contenderSelectionService, never()).designateAsContender(any(), any());
    verify(segmentService).saveSegment(segment);
  }

  @Test
  void autoCompleteBeatForSegment_adjudicatedSegment_fallsBackToDirectDesignation() {
    Segment segment = new Segment();
    segment.setId(87L);
    segment.setAdjudicationStatus(
        com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED);
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
    script.setName("Late Arc");
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

    // Already adjudicated: adjudication will not re-run, so designate directly.
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

  // ── getUpcomingBeatsForShow fallback (ATW-kpyt) ──────────────────────────

  private Show show(Long id) {
    Show show = new Show();
    show.setId(id);
    return show;
  }

  private FeudScriptBeat pendingBeat(long id, FeudScript script) {
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setId(id);
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    return beat;
  }

  @Test
  void getUpcomingBeatsForShow_noTargetShow_fallsBackToNextPendingBeat() {
    FeudScript script = new FeudScript();
    script.setName("Lashley Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    Rivalry rivalry = new Rivalry();
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    script.setRivalry(rivalry);

    FeudScriptBeat beat = pendingBeat(10L, script);

    when(feudScriptBeatRepository.findPendingBeatsForShow(5L)).thenReturn(List.of());
    when(feudScriptBeatRepository.findNextPendingBeatPerActiveScript()).thenReturn(List.of(beat));

    List<FeudScriptBeat> result = service.getUpcomingBeatsForShow(show(5L), Set.of(1L, 2L));

    assertThat(result).containsExactly(beat);
  }

  @Test
  void getUpcomingBeatsForShow_participantsNotOnRoster_beatExcluded() {
    FeudScript script = new FeudScript();
    script.setName("Inactive Roster Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    Rivalry rivalry = new Rivalry();
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    script.setRivalry(rivalry);

    when(feudScriptBeatRepository.findPendingBeatsForShow(5L)).thenReturn(List.of());
    when(feudScriptBeatRepository.findNextPendingBeatPerActiveScript())
        .thenReturn(List.of(pendingBeat(11L, script)));

    List<FeudScriptBeat> result = service.getUpcomingBeatsForShow(show(5L), Set.of(3L, 4L));

    assertThat(result).isEmpty();
  }

  @Test
  void getUpcomingBeatsForShow_showTargetedBeat_takesPrecedenceAndDeduplicates() {
    FeudScript script = new FeudScript();
    script.setName("Targeted Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    Rivalry rivalry = new Rivalry();
    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    script.setRivalry(rivalry);

    FeudScriptBeat targeted = pendingBeat(20L, script);
    Show targetShow = show(5L);
    targeted.setTargetShow(targetShow);

    when(feudScriptBeatRepository.findPendingBeatsForShow(5L)).thenReturn(List.of(targeted));
    when(feudScriptBeatRepository.findNextPendingBeatPerActiveScript())
        .thenReturn(List.of(pendingBeat(20L, script)));

    List<FeudScriptBeat> result = service.getUpcomingBeatsForShow(show(5L), Set.of(1L, 2L));

    assertThat(result).containsExactly(targeted);
  }

  @Test
  void getUpcomingBeatsForShow_nullShowId_returnsEmpty() {
    assertThat(service.getUpcomingBeatsForShow(show(null), Set.of())).isEmpty();
    verifyNoInteractions(feudScriptBeatRepository);
  }

  // ── external participants (ATW-iukb) ─────────────────────────────────────

  private Rivalry rivalry(Wrestler w1, Wrestler w2) {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    return rivalry;
  }

  private FeudScript rivalryScript(Rivalry rivalry) {
    FeudScript script = new FeudScript();
    script.setName("External Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry);
    return script;
  }

  @Test
  void addBeat_withExternalOpponent_beatCarriesRole() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler external = wrestlerWith(30L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(external), List.of());
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    FeudScriptBeat saved = service.addBeat(script, beat);

    assertThat(saved.getExternalParticipants()).hasSize(1);
    assertThat(saved.getExternalOpponents()).containsExactly(external);
    assertThat(saved.getExternalExtras()).isEmpty();
  }

  @Test
  void addBeat_externalAlreadyInFeud_throws() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(w2), List.of());

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    assertThatThrownBy(() -> service.addBeat(script, beat))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("part of this arc");
  }

  @Test
  void addBeat_sameWrestlerAsOpponentAndExtra_throws() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler external = wrestlerWith(30L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Singles Match");
    beat.addExternalParticipant(external, FeudBeatParticipantRole.OPPONENT);
    beat.addExternalParticipant(external, FeudBeatParticipantRole.EXTRA);
    beat.setScript(script);

    // Upsert by wrestler: the second add flips the role instead of duplicating, so the
    // duplicate-role case can only arise through direct list manipulation.
    beat.getExternalParticipants().add(externalRow(external, FeudBeatParticipantRole.EXTRA));

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    assertThatThrownBy(() -> service.addBeat(script, beat))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cannot be added more than once");
  }

  @Test
  void addBeat_intergenderDisabled_mixedGenderOpponentNonPromo_throws() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler femaleOpponent = wrestlerWith(30L, Gender.FEMALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(femaleOpponent), List.of());
    beat.setSegmentType("Singles Match");

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(false);
    when(segmentTypeService.findByName("Singles Match")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.addBeat(script, beat))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Intergender");
  }

  @Test
  void addBeat_intergenderDisabled_promoSegment_allowsMixedGender() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler femaleGuest = wrestlerWith(30L, Gender.FEMALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(femaleGuest), List.of());
    beat.setSegmentType("Promo");

    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(false);
    when(segmentTypeService.findByName("Promo")).thenReturn(Optional.empty());
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);

    FeudScriptBeat saved = service.addBeat(script, beat);

    assertThat(saved.getExternalParticipants()).hasSize(1);
  }

  @Test
  void addBeat_noExternals_unchanged() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Singles Match");
    beat.setScript(script);
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);

    FeudScriptBeat saved = service.addBeat(script, beat);

    assertThat(saved.getExternalParticipants()).isEmpty();
  }

  @Test
  void toDTO_noExternals_teamsFallBackToOnePerWrestler() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("Shelton Benjamin");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Bobby Lashley");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = pendingBeat(10L, script);
    beat.setSegmentType("Singles Match");

    FeudScriptBeatDTO dto = service.toDTOForTest(beat);

    assertThat(dto.getParticipantIds()).containsExactly(1L, 2L);
    assertThat(dto.getTeamIds()).isNull();
    assertThat(dto.getTeamIdLists()).containsExactly(List.of(1L), List.of(2L));
    assertThat(dto.getExternalSummary()).isNull();
  }

  @Test
  void toDTO_externalOpponent_landsOnSecondTeam() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("Shelton Benjamin");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Bobby Lashley");
    Wrestler external = wrestlerWith(30L, Gender.MALE);
    external.setName("Randy Orton");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(external), List.of());

    FeudScriptBeatDTO dto = service.toDTOForTest(beat);

    assertThat(dto.getParticipantIds()).containsExactly(1L, 2L);
    assertThat(dto.getTeamIdLists()).containsExactly(List.of(1L, 2L), List.of(30L));
    // ATW-978m: team 1 must list each feud wrestler separately — building it by splitting the
    // "A vs B" display string on commas produced one bogus "A vs B" team entry, which the
    // planning grid rendered as garbage and the edit dialog silently dropped.
    assertThat(dto.getTeamNameLists().get(0)).containsExactly("Shelton Benjamin", "Bobby Lashley");
    assertThat(dto.getTeamNameLists().get(1)).containsExactly("Randy Orton");
    assertThat(dto.getExternalSummary()).isEqualTo("Randy Orton (Opponent)");
  }

  @Test
  void toDTO_externalExtras_appendedToSecondTeam() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("A");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("B");
    Wrestler opponent = wrestlerWith(30L, Gender.MALE);
    opponent.setName("Randy Orton");
    Wrestler extra1 = wrestlerWith(31L, Gender.MALE);
    extra1.setName("Extra One");
    Wrestler extra2 = wrestlerWith(32L, Gender.MALE);
    extra2.setName("Extra Two");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(opponent), List.of(extra1, extra2));

    FeudScriptBeatDTO dto = service.toDTOForTest(beat);

    assertThat(dto.getTeamIdLists()).containsExactly(List.of(1L, 2L), List.of(30L, 31L, 32L));
    assertThat(dto.getExternalSummary())
        .isEqualTo("Randy Orton (Opponent), Extra One (Extra), Extra Two (Extra)");
  }

  @Test
  void getUpcomingBeatsForShow_externalNotOnRoster_beatStillIncluded() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler external = wrestlerWith(30L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = beatWithExternals(script, List.of(external), List.of());
    beat.setId(21L);

    when(feudScriptBeatRepository.findPendingBeatsForShow(5L)).thenReturn(List.of());
    when(feudScriptBeatRepository.findNextPendingBeatPerActiveScript()).thenReturn(List.of(beat));

    // Roster contains the feud pair but NOT the external — the beat must still be injected.
    List<FeudScriptBeat> result = service.getUpcomingBeatsForShow(show(5L), Set.of(1L, 2L));

    assertThat(result).containsExactly(beat);
  }

  @Test
  void autoCompleteBeatForSegment_externalSubstitutedAtShowTime_stillCompletes() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler substitute = wrestlerWith(40L, Gender.MALE);
    Segment segment = new Segment();
    segment.setId(80L);
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment.addParticipant(substitute); // show-time substitution for the planned external

    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat =
        beatWithExternals(script, List.of(wrestlerWith(30L, Gender.MALE)), List.of());
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    Optional<FeudScriptBeat> result = service.autoCompleteBeatForSegment(segment);

    assertThat(result).isPresent();
    assertThat(result.get().getBeatStatus()).isEqualTo(FeudScriptBeatStatus.COMPLETED);
  }

  private Wrestler wrestlerWith(Long id, Gender gender) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setGender(gender);
    return w;
  }

  // ── updateBeat (ATW-yux4) ─────────────────────────────────────────────────

  @Test
  void updateBeat_pendingBeat_copiesEditedFields() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("Shelton Benjamin");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Bobby Lashley");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    script.getBeats().add(pendingBeat(10L, script));
    FeudScriptBeat existing = script.getBeats().get(0);
    existing.setSegmentType("Singles Match");

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Ladder Match");
    edited.setSegmentRule("Ladder");
    edited.setWinnerControl(FeudScriptWinnerControl.BOOKER_PICKS);
    edited.setPlannedWinner(w2);
    edited.setCulmination(true);
    edited.setNotes("Blowoff angle");
    edited.setScript(script);

    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    assertThat(saved.getSegmentType()).isEqualTo("Ladder Match");
    assertThat(saved.getSegmentRule()).isEqualTo("Ladder");
    assertThat(saved.getWinnerControl()).isEqualTo(FeudScriptWinnerControl.BOOKER_PICKS);
    assertThat(saved.getPlannedWinner()).isEqualTo(w2);
    assertThat(saved.isCulmination()).isTrue();
    assertThat(saved.getNotes()).isEqualTo("Blowoff angle");
    assertThat(saved.getBeatOrder()).isEqualTo(1);
  }

  @Test
  void updateBeat_nonPendingBeat_throws() {
    FeudScript script =
        rivalryScript(rivalry(wrestlerWith(1L, Gender.MALE), wrestlerWith(2L, Gender.MALE)));
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setBeatStatus(FeudScriptBeatStatus.COMPLETED);
    script.getBeats().add(existing);

    assertThatThrownBy(() -> service.updateBeat(script, existing, new FeudScriptBeat()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Only pending beats can be edited");
  }

  @Test
  void updateBeat_externalInFeud_throws() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    script.getBeats().add(existing);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Singles Match");
    edited.addExternalParticipant(w1, FeudBeatParticipantRole.OPPONENT);

    assertThatThrownBy(() -> service.updateBeat(script, existing, edited))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("part of this arc");
  }

  @Test
  void updateBeat_replacesExternalParticipants() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler oldExternal = wrestlerWith(30L, Gender.MALE);
    Wrestler newExternal = wrestlerWith(31L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = beatWithExternals(script, List.of(oldExternal), List.of());
    existing.setId(10L);
    script.getBeats().add(existing);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Singles Match");
    edited.addExternalParticipant(newExternal, FeudBeatParticipantRole.EXTRA);
    edited.setScript(script);

    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    assertThat(saved.getExternalParticipants()).hasSize(1);
    assertThat(saved.getExternalExtras()).containsExactly(newExternal);
    assertThat(saved.getExternalOpponents()).isEmpty();
  }

  @Test
  void updateBeat_removesAllExternals_whenEditedHasNone() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler external = wrestlerWith(30L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = beatWithExternals(script, List.of(external), List.of());
    existing.setId(10L);
    script.getBeats().add(existing);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Singles Match");
    edited.setScript(script);

    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    assertThat(saved.getExternalParticipants()).isEmpty();
  }

  @Test
  void updateBeat_pleCapReachedByOtherBeats_throws() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    script.setMaxPleAppearances(1);
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    script.getBeats().add(existing);

    FeudScriptBeat other = pendingBeat(11L, script);
    other.setTargetShow(pleShow(91L));
    script.getBeats().add(other);

    // Editing the beat onto a PLE while another beat already claims the only PLE slot.
    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Singles Match");
    edited.setTargetShow(pleShow(90L));

    assertThatThrownBy(() -> service.updateBeat(script, existing, edited))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PLE appearance cap");
  }

  @Test
  void updateBeat_nonPleEdit_skipsPleCapAndReservations() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    script.setMaxPleAppearances(1);
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    script.getBeats().add(existing);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Promo");
    edited.setScript(script);
    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    assertThat(saved.getSegmentType()).isEqualTo("Promo");
    verifyNoInteractions(reservationService);
  }

  @Test
  void updateBeat_pleTargetedBeat_preservesShowAndReservation() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    Show ple = pleShow(90L);
    existing.setTargetShow(ple);
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    existing.setReservation(reservation);
    script.getBeats().add(existing);

    // The editor's target-show combo now carries the beat's persisted show through the edit.
    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Ladder Match");
    edited.setTargetShow(ple);
    edited.setScript(script);
    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    // Same show: reservation is kept, nothing cancelled or re-created.
    assertThat(saved.getTargetShow()).isSameAs(ple);
    assertThat(saved.getReservation()).isSameAs(reservation);
    verify(reservationService, never()).cancelReservation(any());
    verify(reservationService, never()).reserveSlot(any(), any(), any(), any());
  }

  @Test
  void updateBeat_showRemoved_clearsShowAndCancelsReservation() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    existing.setTargetShow(pleShow(90L));
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    existing.setReservation(reservation);
    script.getBeats().add(existing);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Ladder Match");
    // No target show on the edit → beat becomes show-agnostic.
    edited.setScript(script);
    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    FeudScriptBeat saved = service.updateBeat(script, existing, edited);

    assertThat(saved.getTargetShow()).isNull();
    verify(reservationService).cancelReservation(reservation);
    assertThat(saved.getReservation()).isNull();
  }

  @Test
  void updateBeat_detachedScript_reloadsById() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript detached = rivalryScript(rivalry(w1, w2));
    detached.setId(5L);
    FeudScript managed = rivalryScript(rivalry(w1, w2));
    managed.setId(5L);
    when(feudScriptRepository.findById(5L)).thenReturn(Optional.of(managed));
    FeudScriptBeat existing = pendingBeat(10L, managed);
    existing.setSegmentType("Singles Match");
    managed.getBeats().add(existing);

    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Promo");
    edited.setScript(detached);
    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    service.updateBeat(detached, existing, edited);

    verify(feudScriptRepository).findById(5L);
  }

  @Test
  void updateBeat_externalWrestler_resolvedById() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat existing = pendingBeat(10L, script);
    existing.setSegmentType("Singles Match");
    script.getBeats().add(existing);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);

    Wrestler detachedExternal = wrestlerWith(30L, Gender.MALE);
    detachedExternal.setName("Randy Orton");
    FeudScriptBeat edited = new FeudScriptBeat();
    edited.setSegmentType("Singles Match");
    edited.addExternalParticipant(detachedExternal, FeudBeatParticipantRole.OPPONENT);
    edited.setScript(script);
    when(wrestlerService.findById(30L)).thenReturn(Optional.of(detachedExternal));
    when(feudScriptBeatRepository.save(existing)).thenReturn(existing);

    service.updateBeat(script, existing, edited);

    verify(wrestlerService).findById(30L);
    assertThat(existing.getExternalOpponents()).containsExactly(detachedExternal);
  }

  // ── full-segment beat setup (ATW-ushl) ─────────────────────────────────────

  @Test
  void createScriptWithBeats_singleSave_nothingPersistedOnBeatFailure() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);
    when(rivalryService.getRivalryBetweenWrestlers(any(), any()))
        .thenReturn(Optional.of(rivalry(w1, w2)));
    FeudScript persisted = rivalryScript(rivalry(w1, w2));
    when(feudScriptRepository.save(any())).thenReturn(persisted);
    when(feudScriptBeatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeudScriptBeat goodBeat = new FeudScriptBeat();
    goodBeat.setSegmentType("Singles Match");
    FeudScriptBeat badBeat = new FeudScriptBeat();
    badBeat.setSegmentType("Ladder Match");
    // External who is part of the arc → addBeat's validateExternals throws.
    badBeat.addExternalParticipant(w1, FeudBeatParticipantRole.OPPONENT);

    assertThatThrownBy(
            () ->
                service.createScriptWithBeats(
                    "Doomed Arc", List.of(w1, w2), 2, List.of(goodBeat, badBeat)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("part of this arc");
    // Only the good beat reached save; the @Transactional boundary rolls it back in production.
    verify(feudScriptBeatRepository, times(1)).save(any());
  }

  @Test
  void createScriptWithBeats_feudNameExists_reusesExistingFeud() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    Wrestler w3 = wrestlerWith(3L, Gender.MALE);
    when(gameSettingService.isIntergenderMatchesEnabled()).thenReturn(true);
    when(multiWrestlerFeudService.createFeud(any(), any(), any(), anyList()))
        .thenReturn(Optional.empty());
    when(multiWrestlerFeudService.getFeudByName("Stable War"))
        .thenReturn(Optional.of(new MultiWrestlerFeud()));
    FeudScript persisted = new FeudScript();
    persisted.setName("Stable War");
    persisted.setStatus(FeudScriptStatus.ACTIVE);
    persisted.setFeud(new MultiWrestlerFeud());
    when(feudScriptRepository.save(any())).thenReturn(persisted);

    FeudScript created =
        service.createScriptWithBeats("Stable War", List.of(w1, w2, w3), 2, List.of());

    assertThat(created.getFeud()).isNotNull();
    verify(multiWrestlerFeudService).getFeudByName("Stable War");
  }

  @Test
  void autoCompleteBeatForSegment_feudArcMembersCovered_completes() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("One");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Two");
    Wrestler w3 = wrestlerWith(3L, Gender.MALE);
    w3.setName("Three");

    FeudParticipant p1 = new FeudParticipant();
    p1.setWrestler(w1);
    p1.setIsActive(true);
    FeudParticipant p2 = new FeudParticipant();
    p2.setWrestler(w2);
    p2.setIsActive(true);
    FeudParticipant p3 = new FeudParticipant();
    p3.setWrestler(w3);
    p3.setIsActive(true);
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("Stable War");
    feud.getParticipants().addAll(List.of(p1, p2, p3));

    FeudScript script = new FeudScript();
    script.setName("Stable Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setFeud(feud);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setScript(script);
    script.getBeats().add(beat);

    // Segment contains only 2 of the 3 feud members → no full coverage, no completion.
    Segment partial = new Segment();
    partial.setId(91L);
    partial.addParticipant(w1);
    partial.addParticipant(w2);
    when(feudScriptBeatRepository.findPendingBeatsForWrestlers(anyList()))
        .thenReturn(List.of(beat));

    assertThat(service.autoCompleteBeatForSegment(partial)).isEmpty();

    // Full coverage: the beat completes.
    Segment full = new Segment();
    full.setId(92L);
    full.addParticipant(w1);
    full.addParticipant(w2);
    full.addParticipant(w3);
    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);
    when(feudScriptRepository.save(script)).thenReturn(script);

    assertThat(service.autoCompleteBeatForSegment(full)).isPresent();
  }

  @Test
  void skipBeat_cancelsReservationAndMarksSkipped() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = pendingBeat(10L, script);
    beat.setSegmentType("Cage Match");
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    beat.setReservation(reservation);
    script.getBeats().add(beat);

    when(feudScriptBeatRepository.save(beat)).thenReturn(beat);

    FeudScriptBeat saved = service.skipBeat(script, beat);

    assertThat(saved.getBeatStatus()).isEqualTo(FeudScriptBeatStatus.SKIPPED);
    verify(reservationService).cancelReservation(reservation);
    assertThat(saved.getReservation()).isNull();
  }

  @Test
  void markBeatsBookedForShow_booksTargetedPendingBeats() {
    FeudScript script =
        rivalryScript(rivalry(wrestlerWith(1L, Gender.MALE), wrestlerWith(2L, Gender.MALE)));
    FeudScriptBeat targeted = pendingBeat(20L, script);
    targeted.setTargetShow(show(5L));
    when(feudScriptBeatRepository.findPendingBeatsForShow(5L)).thenReturn(List.of(targeted));
    when(feudScriptBeatRepository.save(targeted)).thenReturn(targeted);

    service.markBeatsBookedForShow(show(5L));

    assertThat(targeted.getBeatStatus()).isEqualTo(FeudScriptBeatStatus.BOOKED);
    verify(feudScriptBeatRepository).save(targeted);
  }

  @Test
  void toDTO_customTeams_usesExplicitLayout() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("Alpha");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Beta");
    Wrestler w3 = wrestlerWith(3L, Gender.MALE);
    w3.setName("Gamma");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Tag Team Match");
    beat.addExternalParticipant(w1, FeudBeatParticipantRole.FEUD_MEMBER, 1);
    beat.addExternalParticipant(w3, FeudBeatParticipantRole.FEUD_MEMBER, 1);
    beat.addExternalParticipant(w2, FeudBeatParticipantRole.FEUD_MEMBER, 2);
    beat.setScript(script);

    FeudScriptBeatDTO dto = service.toDTOForTest(beat);

    assertThat(dto.isCustomTeams()).isTrue();
    assertThat(dto.getTeamIdLists()).containsExactly(List.of(1L, 3L), List.of(2L));
    assertThat(dto.getExternalSummary()).contains("Team 1", "Team 2");
  }

  @Test
  void toDTO_feudOnlyBeat_splitsParticipantNamesCorrectly() {
    Wrestler w1 = wrestlerWith(1L, Gender.MALE);
    w1.setName("Shelton Benjamin");
    Wrestler w2 = wrestlerWith(2L, Gender.MALE);
    w2.setName("Bobby Lashley");
    FeudScript script = rivalryScript(rivalry(w1, w2));
    FeudScriptBeat beat = pendingBeat(10L, script);
    beat.setSegmentType("Singles Match");

    FeudScriptBeatDTO dto = service.toDTOForTest(beat);

    // Regression: " vs "-joined names must split back into individual participants.
    assertThat(dto.getTeamNameLists())
        .containsExactly(List.of("Shelton Benjamin"), List.of("Bobby Lashley"));
  }

  private Show pleShow(long id) {
    ShowType showType = new ShowType();
    showType.setCategory(ShowCategory.PLE);
    ShowTemplate template = new ShowTemplate();
    template.setShowType(showType);
    Show show = new Show();
    show.setId(id);
    show.setTemplate(template);
    return show;
  }

  private FeudScriptBeat beatWithExternals(
      FeudScript script, List<Wrestler> opponents, List<Wrestler> extras) {
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setBeatOrder(1);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setSegmentType("Singles Match");
    opponents.forEach(w -> beat.addExternalParticipant(w, FeudBeatParticipantRole.OPPONENT));
    extras.forEach(w -> beat.addExternalParticipant(w, FeudBeatParticipantRole.EXTRA));
    beat.setScript(script);
    return beat;
  }

  private FeudScriptBeatParticipant externalRow(Wrestler w, FeudBeatParticipantRole role) {
    FeudScriptBeatParticipant participant = new FeudScriptBeatParticipant();
    participant.setWrestler(w);
    participant.setRole(role);
    return participant;
  }
}
