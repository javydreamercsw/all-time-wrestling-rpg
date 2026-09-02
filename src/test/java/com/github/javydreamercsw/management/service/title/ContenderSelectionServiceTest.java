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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderDesignatedEvent;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
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

  private ContenderSelectionService service;

  private Title title;
  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    service =
        new ContenderSelectionService(
            titleService, rankingService, gameSettingService, wrestlerRepository, eventPublisher);

    title = new Title();
    title.setId(1L);
    title.setName("World Title");
    title.setChampionshipType(ChampionshipType.SINGLE);

    wrestler = new Wrestler();
    wrestler.setId(10L);
    wrestler.setName("Top Contender");

    when(gameSettingService.isContenderAutoSelectEnabled()).thenReturn(true);
    when(gameSettingService.getContenderTieThresholdPercent()).thenReturn(10);
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
