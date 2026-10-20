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
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageResolution;
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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
  @Mock private DefaultImageService imageService;
  @Mock private GameSettingService gameSettingService;
  @Mock private WrestlerTitleCooldownRepository cooldownRepository;
  @InjectMocks private RankingService rankingService;

  private Title title;
  private Wrestler champion;
  private Wrestler contender1;
  private Wrestler contender2;

  @BeforeEach
  public void setUp() {
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
    lenient()
        .when(gameSettingService.getCurrentGameDate())
        .thenReturn(LocalDate.now(ZoneOffset.UTC));

    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    List<Wrestler> champions = new ArrayList<>();
    champions.add(champion);
    reign.setChampions(new LinkedHashSet<>(champions));
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
        .thenReturn(new ImageResolution("default.png", true));

    List<ChampionshipDTO> championships = rankingService.getChampionships();

    assertEquals(1, championships.size());
    assertEquals("World Heavyweight Championship", championships.getFirst().getName());
    assertEquals("default.png", championships.getFirst().getImageUrl());
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
    reign.setChampions(new LinkedHashSet<>(List.of(wrestlerG)));
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

    title.setChampion(new ArrayList<>(List.of(team1member1, team1member2)));
    title
        .getCurrentReign()
        .get()
        .setChampions(new LinkedHashSet<>(List.of(team1member1, team1member2)));

    Wrestler team2member1 = createMockWrestler(6L, "Team 2 Member 1", 700L, WrestlerTier.RISER);
    Wrestler team2member2 = createMockWrestler(7L, "Team 2 Member 2", 800L, WrestlerTier.RISER);

    Team contenderTeam = new Team();
    contenderTeam.setId(2L);
    contenderTeam.setName("The Contenders");
    contenderTeam.setMembers(List.of(team2member1, team2member2));
    // Team fans are now the team-level counter; the migration backfilled it from the member sum,
    // so a pre-existing team reports the same value under both metrics.
    contenderTeam.setFans(1500L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll()).thenReturn(List.of(contenderTeam));

    List<?> contenders = rankingService.getRankedContenders(1L);

    assertEquals(1, contenders.size());
    assertTrue(contenders.get(0) instanceof RankedTeamDTO);
    RankedTeamDTO teamDto = (RankedTeamDTO) contenders.getFirst();
    assertEquals("The Contenders", teamDto.getName());
    assertEquals(1, teamDto.getRank());
    assertEquals(1500, teamDto.getFans());
    assertEquals(
        List.of("Team 2 Member 1", "Team 2 Member 2"),
        teamDto.getMemberNames(),
        "Both team members must be present, not just the first");
  }

  @Test
  void teamRankingsSortByTeamFansNotMemberSum() {
    title.setChampionshipType(ChampionshipType.TEAM);
    title.setTier(WrestlerTier.MIDCARDER);
    title.getTitleReigns().clear(); // No champion — all teams are contenders

    // Team X: low team fans, high member fans
    Wrestler x1 = createMockWrestler(4L, "X Member 1", 100L, WrestlerTier.ROOKIE);
    Wrestler x2 = createMockWrestler(5L, "X Member 2", 100L, WrestlerTier.ROOKIE);
    Team teamX = new Team();
    teamX.setId(10L);
    teamX.setName("Team X");
    teamX.setMembers(List.of(x1, x2));
    teamX.setFans(500L);

    // Team Y: high team fans, low member fans
    Wrestler y1 = createMockWrestler(6L, "Y Member 1", 1000L, WrestlerTier.RISER);
    Wrestler y2 = createMockWrestler(7L, "Y Member 2", 1000L, WrestlerTier.RISER);
    Team teamY = new Team();
    teamY.setId(11L);
    teamY.setName("Team Y");
    teamY.setMembers(List.of(y1, y2));
    teamY.setFans(300L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll()).thenReturn(new ArrayList<>(List.of(teamX, teamY)));

    List<RankedTeamDTO> contenders = (List<RankedTeamDTO>) rankingService.getRankedContenders(1L);

    assertEquals(2, contenders.size());
    assertEquals(
        "Team X",
        contenders.get(0).getName(),
        "Team X (team fans 500) must rank above Team Y (team fans 300) — "
            + "ordering is by the team-level fan counter, not the member sum"
            + contenders.stream().map(RankedTeamDTO::getName).toList());
    assertEquals(500L, contenders.get(0).getFans());
    assertEquals(300L, contenders.get(1).getFans());
  }

  @Test
  void teamContendersFilteredByAllMemberGender() {
    title.setChampionshipType(ChampionshipType.TEAM);
    title.setTier(WrestlerTier.MIDCARDER);
    title.getTitleReigns().clear();

    // A DUO where only one member has the wrong gender → the whole team is excluded
    Wrestler maleMember = createMockWrestler(4L, "Male Member", 500L, WrestlerTier.ROOKIE);
    Wrestler femaleMember = createMockWrestler(5L, "Female Member", 600L, WrestlerTier.ROOKIE);
    femaleMember.setGender(Gender.FEMALE);
    Team mixedTeam = new Team();
    mixedTeam.setId(10L);
    mixedTeam.setName("Mixed Team");
    mixedTeam.setMembers(List.of(maleMember, femaleMember));
    mixedTeam.setFans(500L);

    // A TRIO where one member has the wrong gender → also excluded
    Wrestler trioA = createMockWrestler(6L, "Trio A", 700L, WrestlerTier.RISER);
    Wrestler trioB = createMockWrestler(7L, "Trio B", 800L, WrestlerTier.RISER);
    Wrestler trioC = createMockWrestler(8L, "Trio C", 900L, WrestlerTier.RISER);
    trioC.setGender(Gender.FEMALE);
    Team trioTeam = new Team();
    trioTeam.setId(11L);
    trioTeam.setName("Trio Team");
    trioTeam.setMembers(List.of(trioA, trioB, trioC));
    trioTeam.setFans(900L);

    // All-male team → included
    Wrestler ok1 = createMockWrestler(9L, "Ok Member 1", 100L, WrestlerTier.ROOKIE);
    Wrestler ok2 = createMockWrestler(10L, "Ok Member 2", 200L, WrestlerTier.ROOKIE);
    Team okTeam = new Team();
    okTeam.setId(12L);
    okTeam.setName("Ok Team");
    okTeam.setMembers(List.of(ok1, ok2));
    okTeam.setFans(100L);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll())
        .thenReturn(new ArrayList<>(List.of(mixedTeam, trioTeam, okTeam)));

    List<RankedTeamDTO> contenders = (List<RankedTeamDTO>) rankingService.getRankedContenders(1L);

    assertEquals(
        1,
        contenders.size(),
        "Only the all-male team passes the all-member gender filter"
            + contenders.stream().map(RankedTeamDTO::getName).toList());
    assertEquals("Ok Team", contenders.getFirst().getName());
  }

  @Test
  void championTeamRemovedByIdViaReignReference() {
    title.setChampionshipType(ChampionshipType.TEAM);
    title.setTier(WrestlerTier.MIDCARDER);

    // The champion team (members A & B, team fans 1000)
    Wrestler memberA = createMockWrestler(4L, "Member A", 500L, WrestlerTier.ROOKIE);
    Wrestler memberB = createMockWrestler(5L, "Member B", 500L, WrestlerTier.ROOKIE);
    Team championTeam = new Team();
    championTeam.setId(10L);
    championTeam.setName("Champion Team");
    championTeam.setMembers(List.of(memberA, memberB));
    championTeam.setFans(1000L);

    // A DIFFERENT team with the exact same members — must stay in contention
    Team rivalTeam = new Team();
    rivalTeam.setId(11L);
    rivalTeam.setName("Rival Team");
    rivalTeam.setMembers(List.of(memberA, memberB));
    rivalTeam.setFans(800L);

    // Reign references the champion team explicitly
    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setChampions(new LinkedHashSet<>(List.of(memberA, memberB)));
    reign.setTeam(championTeam);
    reign.setStartDate(Instant.now());
    title.getTitleReigns().clear();
    title.getTitleReigns().add(reign);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll()).thenReturn(new ArrayList<>(List.of(championTeam, rivalTeam)));

    List<RankedTeamDTO> contenders = (List<RankedTeamDTO>) rankingService.getRankedContenders(1L);

    assertEquals(
        1,
        contenders.size(),
        "Only the rival team remains — the champion team is matched by ID"
            + contenders.stream().map(RankedTeamDTO::getName).toList());
    assertEquals("Rival Team", contenders.getFirst().getName());
  }

  @Test
  void legacyReignWithoutTeamFallsBackToMemberSetMatch() {
    title.setChampionshipType(ChampionshipType.TEAM);
    title.setTier(WrestlerTier.MIDCARDER);

    Wrestler memberA = createMockWrestler(4L, "Member A", 500L, WrestlerTier.ROOKIE);
    Wrestler memberB = createMockWrestler(5L, "Member B", 500L, WrestlerTier.ROOKIE);
    Team championTeam = new Team();
    championTeam.setId(10L);
    championTeam.setName("Champion Team");
    championTeam.setMembers(List.of(memberA, memberB));
    championTeam.setFans(1000L);

    // A trio contender team whose champions held the title before the team reference existed
    Wrestler trioA = createMockWrestler(6L, "Trio A", 400L, WrestlerTier.ROOKIE);
    Wrestler trioB = createMockWrestler(7L, "Trio B", 400L, WrestlerTier.ROOKIE);
    Wrestler trioC = createMockWrestler(8L, "Trio C", 400L, WrestlerTier.ROOKIE);
    Team trioTeam = new Team();
    trioTeam.setId(11L);
    trioTeam.setName("Trio Team");
    trioTeam.setMembers(List.of(trioA, trioB, trioC));
    trioTeam.setFans(900L);

    // Legacy reign: team == null, champions == the trio's member set
    TitleReign trioReign = new TitleReign();
    trioReign.setTitle(title);
    trioReign.setChampions(new LinkedHashSet<>(List.of(trioA, trioB, trioC)));
    trioReign.setStartDate(Instant.now());
    title.getTitleReigns().clear();
    title.getTitleReigns().add(trioReign);

    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));
    when(teamRepository.findAll()).thenReturn(new ArrayList<>(List.of(championTeam, trioTeam)));

    List<RankedTeamDTO> contenders = (List<RankedTeamDTO>) rankingService.getRankedContenders(1L);

    assertEquals(
        1,
        contenders.size(),
        "The legacy trio reign removes the trio team (member-set fallback); the duo team "
            + "was never champion under this reign"
            + contenders.stream().map(RankedTeamDTO::getName).toList());
    assertEquals("Champion Team", contenders.getFirst().getName());
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
    assertEquals("Contender 1", ((RankedWrestlerDTO) contenders.getFirst()).getName());
  }

  @Test
  void testGetTitleReignHistory() {
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    List<TitleReignDTO> history = rankingService.getTitleReignHistory(1L);

    assertEquals(1, history.size());
    assertEquals("Champion", history.getFirst().getChampionNames().getFirst());
    assertTrue(history.getFirst().isCurrent());
  }

  @Test
  void testGetWrestlerTitleHistory() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(champion));

    List<TitleReignDTO> history = rankingService.getWrestlerTitleHistory(1L);

    assertEquals(1, history.size());
    assertEquals("World Heavyweight Championship", history.getFirst().getChampionshipName());
  }

  @Test
  void testGetCurrentChampions() {
    when(titleRepository.findById(1L)).thenReturn(Optional.of(title));

    List<ChampionDTO> result = rankingService.getCurrentChampions(1L);

    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    assertEquals("Champion", result.getFirst().getName());
  }

  private Wrestler createMockWrestler(
      final Long id, final String name, final long fans, final WrestlerTier tier) {
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
