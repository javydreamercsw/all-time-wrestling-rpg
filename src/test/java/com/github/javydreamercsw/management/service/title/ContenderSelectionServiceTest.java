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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldown;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderDesignatedEvent;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContenderSelectionServiceTest {

  @Mock private TitleService titleService;
  @Mock private RankingService rankingService;
  @Mock private GameSettingService gameSettingService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private WrestlerTitleCooldownRepository cooldownRepository;
  @Mock private WrestlerService wrestlerService;

  private ContenderSelectionService service;

  private Title title;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    service =
        new ContenderSelectionService(
            titleService,
            rankingService,
            gameSettingService,
            wrestlerRepository,
            eventPublisher,
            cooldownRepository,
            wrestlerService);

    title = new Title();
    title.setId(1L);
    title.setName("World Title");
    title.setChampionshipType(ChampionshipType.SINGLE);
    Universe universe = new Universe();
    universe.setId(1L);
    title.setUniverse(universe);

    wrestler = new Wrestler();
    wrestler.setId(10L);
    wrestler.setName("Top Contender");

    when(gameSettingService.isContenderAutoSelectEnabled()).thenReturn(true);
    when(gameSettingService.getContenderTieThresholdPercent()).thenReturn(10);
    when(gameSettingService.getContenderFailedChallengeCooldownDefenses()).thenReturn(2);
    when(wrestlerService.getOrCreateState(10L, 1L))
        .thenReturn(WrestlerState.builder().id(55L).tier(WrestlerTier.MIDCARDER).build());
  }

  private RankedWrestlerDTO ranked(final long id, final String name, final long fans) {
    return RankedWrestlerDTO.builder().id(id).name(name).fans(fans).onCooldown(false).build();
  }

  private RankedWrestlerDTO rankedOnCooldown(final long id, final String name, final long fans) {
    return RankedWrestlerDTO.builder().id(id).name(name).fans(fans).onCooldown(true).build();
  }

  @Test
  void designateAsContender_publishesEventOnSuccess() {
    when(titleService.setSoleChallenger(1L, 10L))
        .thenReturn(new TitleService.ChallengeResult(true, "ok"));

    boolean result = service.designateAsContender(title, wrestler);

    assertThat(result).isTrue();
    ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(ContenderDesignatedEvent.class);
    ContenderDesignatedEvent event = (ContenderDesignatedEvent) captor.getValue();
    assertThat(event.getContender()).isEqualTo(wrestler);
    assertThat(event.getTitle()).isEqualTo(title);
  }

  // =====================================================================
  // designateEarnedContender (ATW-9ub4) — earned shots bypass the tier gate
  // =====================================================================

  @Test
  void designateEarnedContender_delegatesToEarnedSinkAndPublishesEvent() {
    when(titleService.grantEarnedChallengerStatus(1L, 10L, true))
        .thenReturn(new TitleService.ChallengeResult(true, "ok"));

    boolean result = service.designateEarnedContender(title, wrestler);

    assertThat(result).isTrue();
    ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(ContenderDesignatedEvent.class);
  }

  @Test
  void designateEarnedContender_sinkRejects_noEventAndReturnsFalse() {
    when(titleService.grantEarnedChallengerStatus(1L, 10L, true))
        .thenReturn(new TitleService.ChallengeResult(false, "not active"));

    boolean result = service.designateEarnedContender(title, wrestler);

    assertThat(result).isFalse();
    verify(eventPublisher, never()).publishEvent(any(ContenderDesignatedEvent.class));
  }

  @Test
  void designateEarnedContender_onCooldown_skipsDesignation() {
    // The failed-challenge cooldown blocks even earned shots (product decision).
    // Stubs: state id 55, cooldown stamped at defense 0, title defenseCount 0 (< threshold 2).
    title.setDefenseCount(0);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(55L, 1L))
        .thenReturn(
            Optional.of(WrestlerTitleCooldown.builder().defenseCountAtChallenge(0L).build()));

    boolean result = service.designateEarnedContender(title, wrestler);

    assertThat(result).isFalse();
    verify(titleService, never()).grantEarnedChallengerStatus(any(), any(), anyBoolean());
  }

  @Test
  void designateEarnedContender_cooldownExpired_designates() {
    // 4 defenses since the failed challenge vs. threshold 2 — cooldown expired.
    // Stubs from setUp: state id 55; cooldown row stamped at defense 0.
    title.setDefenseCount(4);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(55L, 1L))
        .thenReturn(
            Optional.of(WrestlerTitleCooldown.builder().defenseCountAtChallenge(0L).build()));
    when(titleService.grantEarnedChallengerStatus(1L, 10L, true))
        .thenReturn(new TitleService.ChallengeResult(true, "ok"));

    boolean result = service.designateEarnedContender(title, wrestler);

    assertThat(result).isTrue();
    verify(titleService).grantEarnedChallengerStatus(1L, 10L, true);
  }

  // =====================================================================
  // grantChampionSlayerAvailability (ATW-9ub4) — append, not sole
  // =====================================================================

  @Test
  void grantChampionSlayerAvailability_appendsViaEarnedSink() {
    when(titleService.grantEarnedChallengerStatus(1L, 10L, false))
        .thenReturn(new TitleService.ChallengeResult(true, "added"));

    boolean result = service.grantChampionSlayerAvailability(title, wrestler);

    assertThat(result).isTrue();
    verify(titleService).grantEarnedChallengerStatus(1L, 10L, false);
    verify(eventPublisher, never()).publishEvent(any(ContenderDesignatedEvent.class));
  }

  @Test
  void grantChampionSlayerAvailability_onCooldown_skips() {
    // Stubs from setUp: state id 55, threshold 2; cooldown stamped at 0 with defenseCount 0.
    title.setDefenseCount(0);
    when(cooldownRepository.findByWrestlerState_IdAndTitle_Id(55L, 1L))
        .thenReturn(
            Optional.of(WrestlerTitleCooldown.builder().defenseCountAtChallenge(0L).build()));

    boolean result = service.grantChampionSlayerAvailability(title, wrestler);

    assertThat(result).isFalse();
    verify(titleService, never()).grantEarnedChallengerStatus(any(), any(), anyBoolean());
  }

  @Test
  void grantChampionSlayerAvailability_alreadyChallenger_isNoOp() {
    // M3: the champion lost a contender-deciding segment they entered — the winner was already
    // designated #1 contender this segment. Re-granting would fire a redundant second event.
    title.addChallenger(wrestler);

    boolean result = service.grantChampionSlayerAvailability(title, wrestler);

    assertThat(result).isFalse();
    verify(titleService, never()).grantEarnedChallengerStatus(any(), any(), anyBoolean());
  }

  // =====================================================================
  // contendersMissingRankingCriteria (ATW-9ub4) — booking gate input
  // =====================================================================

  @Test
  void contendersMissingRankingCriteria_rankedWrestler_passes() {
    doReturn(List.of(ranked(10L, "Top", 100_000L))).when(rankingService).getRankedContenders(1L);

    List<Wrestler> missing = service.contendersMissingRankingCriteria(title, List.of(wrestler));

    assertThat(missing).isEmpty();
  }

  @Test
  void contendersMissingRankingCriteria_unrankedWrestler_fails() {
    doReturn(List.of(ranked(99L, "Other", 100_000L))).when(rankingService).getRankedContenders(1L);

    List<Wrestler> missing = service.contendersMissingRankingCriteria(title, List.of(wrestler));

    assertThat(missing).containsExactly(wrestler);
  }

  @Test
  void contendersMissingRankingCriteria_currentChallenger_passesEvenIfUnranked() {
    // Earned challengers stay eligible to contend even when the percentile rankings exclude them.
    doReturn(List.of()).when(rankingService).getRankedContenders(1L);
    title.addChallenger(wrestler);

    List<Wrestler> missing = service.contendersMissingRankingCriteria(title, List.of(wrestler));

    assertThat(missing).isEmpty();
  }

  @Test
  void designateAsContender_noEventOnFailure() {
    when(titleService.setSoleChallenger(1L, 10L))
        .thenReturn(new TitleService.ChallengeResult(false, "not eligible"));

    boolean result = service.designateAsContender(title, wrestler);

    assertThat(result).isFalse();
    verify(eventPublisher, never()).publishEvent(any(ContenderDesignatedEvent.class));
  }

  @Test
  void autoSelectNextContender_skipsWhenDisabled() {
    when(gameSettingService.isContenderAutoSelectEnabled()).thenReturn(false);

    service.autoSelectNextContender(title);

    verify(rankingService, never()).getRankedContenders(any());
  }

  @Test
  void autoSelectNextContender_skipsTeamTitles() {
    title.setChampionshipType(ChampionshipType.TEAM);

    service.autoSelectNextContender(title);

    verify(rankingService, never()).getRankedContenders(any());
  }

  @Test
  void autoSelectNextContender_designatesClearLeader() {
    doReturn(List.of(ranked(10L, "Top Contender", 1000L), ranked(11L, "Runner Up", 500L)))
        .when(rankingService)
        .getRankedContenders(1L);
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(wrestler));
    when(titleService.setSoleChallenger(1L, 10L))
        .thenReturn(new TitleService.ChallengeResult(true, "ok"));

    service.autoSelectNextContender(title);

    verify(titleService).setSoleChallenger(1L, 10L);
    verify(eventPublisher, never()).publishEvent(any(ContenderTieDetectedEvent.class));
  }

  @Test
  void autoSelectNextContender_publishesTieEventWhenTopTwoAreClose() {
    // 950 is within 10% of 1000 → tie
    doReturn(
            List.of(
                ranked(10L, "Top Contender", 1000L),
                ranked(11L, "Close Second", 950L),
                ranked(12L, "Far Third", 100L)))
        .when(rankingService)
        .getRankedContenders(1L);

    service.autoSelectNextContender(title);

    ArgumentCaptor<ContenderTieDetectedEvent> captor =
        ArgumentCaptor.forClass(ContenderTieDetectedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().getTiedWrestlers()).hasSize(2);
    verify(titleService, never()).setSoleChallenger(any(), any());
  }

  @Test
  void autoSelectNextContender_doesNothingWhenRankingsEmpty() {
    doReturn(List.of()).when(rankingService).getRankedContenders(1L);

    service.autoSelectNextContender(title);

    verify(titleService, never()).setSoleChallenger(any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void autoSelectNextContender_skipsOnCooldownTopContender() {
    // Top contender is on cooldown; runner-up should be selected instead
    doReturn(
            List.of(
                rankedOnCooldown(10L, "Top Contender (Cooldown)", 1000L),
                ranked(11L, "Runner Up", 500L)))
        .when(rankingService)
        .getRankedContenders(1L);
    Wrestler runnerUp = new Wrestler();
    runnerUp.setId(11L);
    runnerUp.setName("Runner Up");
    when(wrestlerRepository.findById(11L)).thenReturn(Optional.of(runnerUp));
    when(titleService.setSoleChallenger(1L, 11L))
        .thenReturn(new TitleService.ChallengeResult(true, "ok"));

    service.autoSelectNextContender(title);

    verify(titleService).setSoleChallenger(1L, 11L);
    verify(titleService, never()).setSoleChallenger(1L, 10L);
  }

  @Test
  void autoSelectNextContender_doesNothingWhenAllOnCooldown() {
    doReturn(
            List.of(
                rankedOnCooldown(10L, "Top Contender (Cooldown)", 1000L),
                rankedOnCooldown(11L, "Runner Up (Cooldown)", 500L)))
        .when(rankingService)
        .getRankedContenders(1L);

    service.autoSelectNextContender(title);

    verify(titleService, never()).setSoleChallenger(any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void isTie_trueWhenWithinThreshold() {
    assertThat(service.isTie(List.of(ranked(1L, "A", 1000L), ranked(2L, "B", 950L)))).isTrue();
  }

  @Test
  void isTie_falseWhenClearGap() {
    assertThat(service.isTie(List.of(ranked(1L, "A", 1000L), ranked(2L, "B", 500L)))).isFalse();
  }

  @Test
  void isTie_allTiedWhenTopHasZeroFans() {
    assertThat(service.isTie(List.of(ranked(1L, "A", 0L), ranked(2L, "B", 0L)))).isTrue();
  }
}
