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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    champion.setFans(1000L);
    champion.setGender(Gender.MALE);
    champion.setTier(WrestlerTier.fromFanCount(champion.getFans()));

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
    contender1.setFans(500L);
    contender1.setGender(Gender.MALE);
    contender1.setTier(WrestlerTier.fromFanCount(contender1.getFans()));

    contender2 = new Wrestler();
    contender2.setId(3L);
    contender2.setName("Contender 2");
    contender2.setFans(700L);
    contender2.setGender(Gender.MALE);
    contender2.setTier(WrestlerTier.fromFanCount(contender2.getFans()));
  }

  @Test
  void testGetChampionships() {
    when(titleRepository.findAll()).thenReturn(List.of(title));

    List<ChampionshipDTO> championships = rankingService.getChampionships();

    assertEquals(1, championships.size());
    assertEquals("World Heavyweight Championship", championships.get(0).getName());
    assertEquals("world-heavyweight-championship.png", championships.get(0).getImageName());
  }

  @Test
  void testGetRankedContendersWithTierPrioritization() {
    // Setup for a Midcard Championship
    title.setName("Midcard Championship");
    title.setTier(WrestlerTier.MIDCARDER); // Ordinal 2
    title.setGender(Gender.MALE);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIncludeInRankings(true); // Explicitly include in rankings

    // Create diverse wrestlers with fan counts aligned with their tiers
    Wrestler wrestlerA = new Wrestler();
    wrestlerA.setId(10L);
    wrestlerA.setName("Wrestler A");
    wrestlerA.setFans(WrestlerTier.MAIN_EVENTER.getMinFans() + 5000L); // Fans for MAIN_EVENTER
    wrestlerA.setGender(Gender.MALE);
    wrestlerA.setTier(WrestlerTier.MAIN_EVENTER); // Ordinal 4
    wrestlerA.setActive(true);

    Wrestler wrestlerB = new Wrestler();
    wrestlerB.setId(11L);
    wrestlerB.setName("Wrestler B");
    wrestlerB.setFans(WrestlerTier.MIDCARDER.getMinFans() + 5000L); // Fans for MIDCARDER
    wrestlerB.setGender(Gender.MALE);
    wrestlerB.setTier(WrestlerTier.MIDCARDER); // Ordinal 3 - Title tier, higher fans
    wrestlerB.setActive(true);

    Wrestler wrestlerC = new Wrestler();
    wrestlerC.setId(12L);
    wrestlerC.setName("Wrestler C");
    wrestlerC.setFans(WrestlerTier.ICON.getMinFans() + 5000L); // Fans for ICON
    wrestlerC.setGender(Gender.MALE);
    wrestlerC.setTier(WrestlerTier.ICON); // Ordinal 5 - Highest tier
    wrestlerC.setActive(true);

    Wrestler wrestlerD = new Wrestler();
    wrestlerD.setId(13L);
    wrestlerD.setName("Wrestler D");
    wrestlerD.setFans(WrestlerTier.MIDCARDER.getMinFans() + 2000L); // Fans for MIDCARDER
    wrestlerD.setGender(Gender.MALE);
    wrestlerD.setTier(WrestlerTier.MIDCARDER); // Ordinal 3 - Title tier, lower fans
    wrestlerD.setActive(true);

    Wrestler wrestlerE = new Wrestler();
    wrestlerE.setId(14L);
    wrestlerE.setName("Wrestler E");
    wrestlerE.setFans(WrestlerTier.ROOKIE.getMinFans() + 500L); // Fans for ROOKIE
    wrestlerE.setGender(Gender.MALE);
    wrestlerE.setTier(WrestlerTier.ROOKIE); // Ordinal 0 - Too low tier
    wrestlerE.setActive(true);

    Wrestler wrestlerF = new Wrestler();
    wrestlerF.setId(15L);
    wrestlerF.setName("Wrestler F");
    wrestlerF.setFans(WrestlerTier.MIDCARDER.getMinFans() + 3000L); // Fans for MIDCARDER
    wrestlerF.setGender(Gender.MALE);
    wrestlerF.setTier(WrestlerTier.MIDCARDER);
    wrestlerF.setActive(false); // Inactive

    Wrestler wrestlerG = new Wrestler();
    wrestlerG.setId(16L);
    wrestlerG.setName("Wrestler G");
    wrestlerG.setFans(WrestlerTier.MIDCARDER.getMinFans() + 4000L); // Fans for MIDCARDER
    wrestlerG.setGender(Gender.MALE);
    wrestlerG.setTier(WrestlerTier.MIDCARDER);
    wrestlerG.setActive(true); // Champion

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

    // Assertions
    // WrestlerE (ROOKIE) should be filtered out because tier is too low (5 > 2)
    // WrestlerF (Inactive) should be filtered out
    // WrestlerG (Champion) should be filtered out
    // Expected remaining: WrestlerB, WrestlerD, WrestlerC, WrestlerA

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

    Wrestler team1member1 = new Wrestler();
    team1member1.setId(4L);
    team1member1.setName("Team 1 Member 1");
    team1member1.setFans(500L);
    team1member1.setGender(Gender.MALE);

    Wrestler team1member2 = new Wrestler();
    team1member2.setId(5L);
    team1member2.setName("Team 1 Member 2");
    team1member2.setFans(600L);
    team1member2.setGender(Gender.MALE);

    Faction championTeam = new Faction();
    championTeam.setId(1L);
    championTeam.setName("The Champions");
    championTeam.setMembers(Set.of(team1member1, team1member2));

    title.setChampion(List.of(team1member1, team1member2));
    title.getCurrentReign().get().setChampions(List.of(team1member1, team1member2));

    Wrestler team2member1 = new Wrestler();
    team2member1.setId(6L);
    team2member1.setName("Team 2 Member 1");
    team2member1.setFans(700L);
    team2member1.setGender(Gender.MALE);
    team2member1.setTier(WrestlerTier.fromFanCount(team2member1.getFans()));

    Wrestler team2member2 = new Wrestler();
    team2member2.setId(7L);
    team2member2.setName("Team 2 Member 2");
    team2member2.setFans(800L);
    team2member2.setGender(Gender.MALE);
    team2member2.setTier(WrestlerTier.fromFanCount(team2member2.getFans()));

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
    contender1.setFans(70000L);
    contender1.setTier(WrestlerTier.MIDCARDER);
    contender1.setActive(true);

    Wrestler inactiveWrestler = new Wrestler();
    inactiveWrestler.setId(4L);
    inactiveWrestler.setName("Inactive Wrestler");
    inactiveWrestler.setFans(80000L);
    inactiveWrestler.setGender(Gender.MALE);
    inactiveWrestler.setTier(WrestlerTier.MIDCARDER);
    inactiveWrestler.setActive(false);

    title.setTier(WrestlerTier.MIDCARDER);
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    // Mock repository to return both, and let the service filter out the inactive one.
    // Even though the service calls it with active=true, we return both to test the service's
    // filter.
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
}
