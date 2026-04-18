/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.ranking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private com.github.javydreamercsw.base.image.DefaultImageService imageService;
  @InjectMocks private RankingService rankingService;

  private Title title;
  private Wrestler champion;
  private Wrestler contender1;
  private Wrestler contender2;

  @BeforeEach
  void setUp() {
    title = new Title();
    title.setId(1L);
    title.setName("World Heavyweight Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setGender(Gender.MALE);
    title.setChampionshipType(ChampionshipType.SINGLE);

    champion = new Wrestler();
    champion.setId(1L);
    champion.setName("Champion");
    champion.setGender(Gender.MALE);

    WrestlerState championState =
        WrestlerState.builder().wrestler(champion).fans(1000L).tier(WrestlerTier.CONTENDER).build();
    lenient().when(wrestlerService.getOrCreateState(eq(1L), anyLong())).thenReturn(championState);

    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    List<Wrestler> champions = new ArrayList<>();
    champions.add(champion);
    reign.setChampions(champions);
    reign.setStartDate(Instant.now());
    title.getTitleReigns().add(reign);
    champion.getReigns().add(reign);

    contender1 = new Wrestler();
    contender1.setId(2L);
    contender1.setName("Contender 1");
    contender1.setGender(Gender.MALE);

    WrestlerState state1 =
        WrestlerState.builder().wrestler(contender1).fans(500L).tier(WrestlerTier.RISER).build();
    lenient().when(wrestlerService.getOrCreateState(eq(2L), anyLong())).thenReturn(state1);

    contender2 = new Wrestler();
    contender2.setId(3L);
    contender2.setName("Contender 2");
    contender2.setGender(Gender.MALE);

    WrestlerState state2 =
        WrestlerState.builder().wrestler(contender2).fans(700L).tier(WrestlerTier.RISER).build();
    lenient().when(wrestlerService.getOrCreateState(eq(3L), anyLong())).thenReturn(state2);
  }

  @Test
  void testGetChampionships() {
    when(titleRepository.findAll()).thenReturn(List.of(title));
    when(imageService.resolveImage(any(), any()))
        .thenReturn(new com.github.javydreamercsw.base.image.ImageResolution("default.png", true));

    List<ChampionshipDTO> championships = rankingService.getChampionships();

    assertEquals(1, championships.size());
    assertEquals("World Heavyweight Championship", championships.get(0).getName());
    assertEquals("default.png", championships.get(0).getImageUrl());
  }

  @Test
  void testGetRankedContendersWithTierPrioritization() {
    // Setup for a Midcard Championship
    title.setName("Midcard Championship");
    title.setTier(WrestlerTier.MIDCARDER); // Ordinal 3
    title.setGender(Gender.MALE);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIncludeInRankings(true); // Explicitly include in rankings

    // Create diverse wrestlers
    Wrestler wrestlerA =
        createMockWrestler(
            10L,
            "Wrestler A",
            WrestlerTier.MAIN_EVENTER.getMinFans() + 5000L,
            WrestlerTier.MAIN_EVENTER);
    Wrestler wrestlerB =
        createMockWrestler(
            11L, "Wrestler B", WrestlerTier.MIDCARDER.getMinFans() + 5000L, WrestlerTier.MIDCARDER);
    Wrestler wrestlerC =
        createMockWrestler(
            12L, "Wrestler C", WrestlerTier.ICON.getMinFans() + 5000L, WrestlerTier.ICON);
    Wrestler wrestlerD =
        createMockWrestler(
            13L, "Wrestler D", WrestlerTier.MIDCARDER.getMinFans() + 2000L, WrestlerTier.MIDCARDER);
    Wrestler wrestlerE =
        createMockWrestler(
            14L, "Wrestler E", WrestlerTier.ROOKIE.getMinFans() + 500L, WrestlerTier.ROOKIE);
    Wrestler wrestlerF =
        createMockWrestler(
            15L, "Wrestler F", WrestlerTier.MIDCARDER.getMinFans() + 3000L, WrestlerTier.MIDCARDER);
    wrestlerF.setActive(false);
    Wrestler wrestlerG =
        createMockWrestler(
            16L, "Wrestler G", WrestlerTier.MIDCARDER.getMinFans() + 4000L, WrestlerTier.MIDCARDER);

    // Set Wrestler G as the champion
    TitleReign reign = new TitleReign();
    reign.setChampions(List.of(wrestlerG));
    reign.setStartDate(Instant.now());
    title.getTitleReigns().clear(); // Clear existing champion from setup
    title.getTitleReigns().add(reign);

    // Explicitly define the list of active, eligible, non-champion contenders
    List<Wrestler> activeEligibleNonChampionContenders =
        List.of(wrestlerA, wrestlerB, wrestlerC, wrestlerD);

    when(titleRepository.findById(title.getId())).thenReturn(Optional.of(title));
    when(wrestlerRepository.findAllByGenderAndActive(title.getGender(), true))
        .thenReturn(activeEligibleNonChampionContenders);

    List<RankedWrestlerDTO> contenders =
        (List<RankedWrestlerDTO>) rankingService.getRankedContenders(title.getId());

    assertEquals(4, contenders.size());
    List<String> contenderList = contenders.stream().map(RankedWrestlerDTO::getName).toList();
    assertEquals(
        "Wrestler B",
        contenders.get(0).getName(),
        "Wrestler B should be ranked #1 (MIDCARDER, highest fans at title tier)\n" + contenderList);
    assertEquals(1, contenders.get(0).getRank());
    assertEquals(
        "Wrestler D",
        contenders.get(1).getName(),
        "Wrestler D should be ranked #2 (MIDCARDER, lower fans at title tier)\n" + contenderList);
    assertEquals(2, contenders.get(1).getRank());
    assertEquals(
        "Wrestler C",
        contenders.get(2).getName(),
        "Wrestler C should be ranked #3 (ICON, highest tier overall)\n" + contenderList);
    assertEquals(3, contenders.get(2).getRank());
    assertEquals(
        "Wrestler A",
        contenders.get(3).getName(),
        "Wrestler A should be ranked #4 (MAIN_EVENTER, lower tier than ICON)\n" + contenderList);
    assertEquals(4, contenders.get(3).getRank());
  }

  @Test
  void testGetRankedTeamContenders() {
    title.setChampionshipType(ChampionshipType.TEAM);
    title.setTier(WrestlerTier.MIDCARDER);

    Wrestler team1member1 = createMockWrestler(4L, "Team 1 Member 1", 500L, WrestlerTier.ROOKIE);
    Wrestler team1member2 = createMockWrestler(5L, "Team 1 Member 2", 600L, WrestlerTier.ROOKIE);

    title.setChampion(List.of(team1member1, team1member2));
    title.getCurrentReign().get().setChampions(List.of(team1member1, team1member2));

    Wrestler team2member1 = createMockWrestler(6L, "Team 2 Member 1", 700L, WrestlerTier.RISER);
    Wrestler team2member2 = createMockWrestler(7L, "Team 2 Member 2", 800L, WrestlerTier.RISER);

    Team contenderTeam = new Team();
    contenderTeam.setId(2L);
    contenderTeam.setName("The Contenders");
    contenderTeam.setWrestler1(team2member1);
    contenderTeam.setWrestler2(team2member2);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll()).thenReturn(List.of(contenderTeam));

    List<?> contenders = rankingService.getRankedContenders(1L);

    assertEquals(1, contenders.size());
    assertTrue(contenders.get(0) instanceof RankedTeamDTO);
    assertEquals("The Contenders", ((RankedTeamDTO) contenders.get(0)).getName());
    assertEquals(1, ((RankedTeamDTO) contenders.get(0)).getRank());
    assertEquals(1500, ((RankedTeamDTO) contenders.get(0)).getFans());
  }

  @Test
  void testGetRankedContendersExcludesInactive() {
    createMockWrestler(2L, "Contender 1", 70000L, WrestlerTier.MIDCARDER);

    Wrestler inactiveWrestler =
        createMockWrestler(4L, "Inactive Wrestler", 80000L, WrestlerTier.MIDCARDER);
    inactiveWrestler.setActive(false);

    title.setTier(WrestlerTier.MIDCARDER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    when(wrestlerRepository.findAllByGenderAndActive(any(Gender.class), eq(true)))
        .thenReturn(new ArrayList<>(List.of(contender1, inactiveWrestler)));

    List<?> contenders = rankingService.getRankedContenders(1L);

    assertEquals(1, contenders.size());
    assertEquals("Contender 1", ((RankedWrestlerDTO) contenders.get(0)).getName());
  }

  @Test
  void testGetTitleReignHistory() {
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    List<TitleReignDTO> history = rankingService.getTitleReignHistory(1L);

    assertEquals(1, history.size());
    assertEquals("Champion", history.get(0).getChampionNames().get(0));
    assertTrue(history.get(0).isCurrent());
  }

  @Test
  void testGetWrestlerTitleHistory() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(champion));

    List<TitleReignDTO> history = rankingService.getWrestlerTitleHistory(1L);

    assertEquals(1, history.size());
    assertEquals("World Heavyweight Championship", history.get(0).getChampionshipName());
  }

  @Test
  void testGetCurrentChampions() {
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    List<ChampionDTO> result = rankingService.getCurrentChampions(1L);

    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    assertEquals("Champion", result.get(0).getName());
  }

  private Wrestler createMockWrestler(Long id, String name, long fans, WrestlerTier tier) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setGender(Gender.MALE);
    w.setActive(true);

    WrestlerState state = WrestlerState.builder().wrestler(w).fans(fans).tier(tier).build();
    lenient().when(wrestlerService.getOrCreateState(eq(id), anyLong())).thenReturn(state);
    return w;
  }
}
