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
package com.github.javydreamercsw.management.service.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonAward;
import com.github.javydreamercsw.management.domain.season.SeasonAwardRepository;
import com.github.javydreamercsw.management.domain.season.SeasonAwardType;
import com.github.javydreamercsw.management.domain.season.WrestlerSeasonSnapshot;
import com.github.javydreamercsw.management.domain.season.WrestlerSeasonSnapshotRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.event.dto.SeasonEndedEvent;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonAwardsServiceTest {

  @Mock private SeasonAwardRepository awardRepository;
  @Mock private WrestlerSeasonSnapshotRepository snapshotRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private TitleReignRepository titleReignRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private NewsService newsService;

  @InjectMocks private SeasonAwardsService service;

  private Season season;
  private Wrestler wrestlerA;
  private Wrestler wrestlerB;

  @BeforeEach
  void setUp() {
    season = new Season();
    season.setId(1L);
    season.setName("Season 1");
    season.setStartDate(Instant.now().minus(30, ChronoUnit.DAYS));
    season.setEndDate(Instant.now());

    wrestlerA = new Wrestler();
    wrestlerA.setId(1L);
    wrestlerA.setName("Alpha");

    wrestlerB = new Wrestler();
    wrestlerB.setId(2L);
    wrestlerB.setName("Beta");
  }

  @Test
  @DisplayName("computeAwards — wrestler with most wins wins WRESTLER_OF_THE_YEAR")
  void wrestlerOfTheYear_picksHighestWinCount() {
    Show show = new Show();
    show.setSeason(season);
    season.addShow(show);

    Segment seg1 = mock(Segment.class);
    when(seg1.getWinners()).thenReturn(List.of(wrestlerA));

    Segment seg2 = mock(Segment.class);
    when(seg2.getWinners()).thenReturn(List.of(wrestlerA));

    Segment seg3 = mock(Segment.class);
    when(seg3.getWinners()).thenReturn(List.of(wrestlerB));

    when(segmentRepository.findByShow(show)).thenReturn(List.of(seg1, seg2, seg3));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestlerA));
    when(snapshotRepository.findAll()).thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());

    List<SeasonAward> awards = service.computeAwards(season);

    assertThat(awards)
        .filteredOn(a -> a.getAwardType() == SeasonAwardType.WRESTLER_OF_THE_YEAR)
        .hasSize(1)
        .first()
        .satisfies(
            a -> {
              assertThat(a.getWrestler()).isEqualTo(wrestlerA);
              assertThat(a.getAwardValue()).contains("2 wins");
            });
  }

  @Test
  @DisplayName("computeAwards — no shows yields no WRESTLER_OF_THE_YEAR award")
  void wrestlerOfTheYear_noShows_noAward() {
    when(snapshotRepository.findAll()).thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());

    List<SeasonAward> awards = service.computeAwards(season);

    assertThat(awards)
        .filteredOn(a -> a.getAwardType() == SeasonAwardType.WRESTLER_OF_THE_YEAR)
        .isEmpty();
  }

  @Test
  @DisplayName("computeAwards — title reigns within season date range yield MOST_DECORATED")
  void mostDecorated_picksWrestlerWithMostReigns() {
    TitleReign r1 = new TitleReign();
    r1.setChampions(Set.of(wrestlerA));
    TitleReign r2 = new TitleReign();
    r2.setChampions(Set.of(wrestlerA));
    TitleReign r3 = new TitleReign();
    r3.setChampions(Set.of(wrestlerB));

    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of(r1, r2, r3));
    when(snapshotRepository.findAll()).thenReturn(List.of());

    List<SeasonAward> awards = service.computeAwards(season);

    assertThat(awards)
        .filteredOn(a -> a.getAwardType() == SeasonAwardType.MOST_DECORATED)
        .hasSize(1)
        .first()
        .satisfies(
            a -> {
              assertThat(a.getWrestler()).isEqualTo(wrestlerA);
              assertThat(a.getAwardValue()).contains("2 title reign(s)");
            });
  }

  @Test
  @DisplayName("onSeasonEnded — skips computation if awards already exist")
  void onSeasonEnded_idempotent_whenAwardsExist() {
    season.setId(99L);
    SeasonAward existing =
        SeasonAward.builder()
            .season(season)
            .wrestler(wrestlerA)
            .awardType(SeasonAwardType.WRESTLER_OF_THE_YEAR)
            .awardValue("3 wins")
            .build();
    when(awardRepository.findBySeasonId(99L)).thenReturn(List.of(existing));

    service.onSeasonEnded(
        new com.github.javydreamercsw.management.event.dto.SeasonEndedEvent(this, season));

    verify(segmentRepository, never()).findByShow(any());
    verify(newsService, never()).createNewsItem(any(), any(), any(), anyBoolean(), anyInt());
  }

  @Test
  @DisplayName("getAwards — delegates to repository")
  void getAwards_delegatesToRepository() {
    season.setId(5L);
    SeasonAward award =
        SeasonAward.builder()
            .season(season)
            .wrestler(wrestlerA)
            .awardType(SeasonAwardType.MOST_IMPROVED)
            .awardValue("10,000 new fans")
            .build();
    when(awardRepository.findBySeasonId(5L)).thenReturn(List.of(award));

    List<SeasonAward> result = service.getAwards(5L);

    assertThat(result).containsExactly(award);
  }

  @Test
  @DisplayName("computeAwards — wrestler with highest fan growth wins MOST_IMPROVED")
  void mostImproved_picksHighestFanGrowth() {
    WrestlerSeasonSnapshot snapshotA = new WrestlerSeasonSnapshot();
    snapshotA.setSeason(season);
    snapshotA.setWrestler(wrestlerA);
    snapshotA.setStartingFans(1_000L);

    WrestlerSeasonSnapshot snapshotB = new WrestlerSeasonSnapshot();
    snapshotB.setSeason(season);
    snapshotB.setWrestler(wrestlerB);
    snapshotB.setStartingFans(5_000L);

    Wrestler wrestlerAWithState = mock(Wrestler.class);
    WrestlerState stateA = WrestlerState.builder().fans(11_000L).build();
    when(wrestlerAWithState.getDefaultState()).thenReturn(Optional.of(stateA));
    when(wrestlerRepository.findByIdWithStates(1L)).thenReturn(Optional.of(wrestlerAWithState));

    Wrestler wrestlerBWithState = mock(Wrestler.class);
    WrestlerState stateB = WrestlerState.builder().fans(6_000L).build();
    when(wrestlerBWithState.getDefaultState()).thenReturn(Optional.of(stateB));
    when(wrestlerRepository.findByIdWithStates(2L)).thenReturn(Optional.of(wrestlerBWithState));

    when(snapshotRepository.findAll()).thenReturn(List.of(snapshotA, snapshotB));
    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());

    List<SeasonAward> awards = service.computeAwards(season);

    assertThat(awards)
        .filteredOn(a -> a.getAwardType() == SeasonAwardType.MOST_IMPROVED)
        .hasSize(1)
        .first()
        .satisfies(
            a -> {
              assertThat(a.getWrestler()).isEqualTo(wrestlerA);
              assertThat(a.getAwardValue()).contains("10,000");
            });
  }

  @Test
  @DisplayName("computeAwards — no positive fan growth yields no MOST_IMPROVED award")
  void mostImproved_noGrowth_noAward() {
    WrestlerSeasonSnapshot snapshot = new WrestlerSeasonSnapshot();
    snapshot.setSeason(season);
    snapshot.setWrestler(wrestlerA);
    snapshot.setStartingFans(5_000L);

    Wrestler wrestlerAWithState = mock(Wrestler.class);
    WrestlerState state = WrestlerState.builder().fans(5_000L).build();
    when(wrestlerAWithState.getDefaultState()).thenReturn(Optional.of(state));
    when(wrestlerRepository.findByIdWithStates(1L)).thenReturn(Optional.of(wrestlerAWithState));

    when(snapshotRepository.findAll()).thenReturn(List.of(snapshot));
    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());

    List<SeasonAward> awards = service.computeAwards(season);

    assertThat(awards).filteredOn(a -> a.getAwardType() == SeasonAwardType.MOST_IMPROVED).isEmpty();
  }

  @Test
  @DisplayName("onSeasonEnded — applies fan bonuses and generates news when awards computed")
  void onSeasonEnded_appliesFanBonusesAndGeneratesNews() {
    Universe universe = new Universe();
    universe.setId(7L);

    Show show = new Show();
    show.setSeason(season);
    show.setUniverse(universe);
    season.addShow(show);

    Segment seg = mock(Segment.class);
    when(seg.getWinners()).thenReturn(List.of(wrestlerA));

    when(awardRepository.findBySeasonId(1L)).thenReturn(List.of());
    when(segmentRepository.findByShow(show)).thenReturn(List.of(seg));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestlerA));
    when(snapshotRepository.findAll()).thenReturn(List.of());
    when(titleReignRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());

    service.onSeasonEnded(new SeasonEndedEvent(this, season));

    verify(wrestlerService)
        .awardFans(eq(1L), eq(7L), eq(SeasonAwardType.WRESTLER_OF_THE_YEAR.getFanBonus()));
    verify(newsService).createNewsItem(any(), any(), eq(NewsCategory.ANALYSIS), eq(false), eq(4));
  }
}
