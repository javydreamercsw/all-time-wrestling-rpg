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
package com.github.javydreamercsw.management.service.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentService segmentService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;

  private ObjectMapper objectMapper = new ObjectMapper();

  private TournamentService tournamentService;
  private Campaign campaign;
  private CampaignState state;
  private Wrestler player;

  @BeforeEach
  void setUp() {
    tournamentService =
        new TournamentService(
            campaignStateRepository,
            wrestlerRepository,
            objectMapper,
            segmentService,
            segmentTypeRepository,
            segmentRuleRepository);

    player = new Wrestler();
    player.setId(1L);
    player.setName("Player");

    state = new CampaignState();
    campaign = new Campaign();
    campaign.setWrestler(player);
    campaign.setState(state);
  }

  @Test
  void testInitializeTournament_DynamicSize_Small() throws JsonProcessingException {
    // 5 Opponents + Player = 6. Bracket Size should be 8.
    List<Wrestler> roster = new ArrayList<>();
    for (int i = 2; i <= 6; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Opponent " + i);
      roster.add(w);
    }
    // Add player to roster list (service expects findAll to return all)
    roster.add(player);

    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    verify(campaignStateRepository).save(state);
    assertThat(state.getTournamentState()).isNotNull();

    TournamentDTO dto = objectMapper.readValue(state.getTournamentState(), TournamentDTO.class);
    // Bracket size 8 -> 3 Rounds
    assertThat(dto.getTotalRounds()).isEqualTo(3);

    // Round 1 matches: 4 matches
    long r1Matches = dto.getMatches().stream().filter(m -> m.getRound() == 1).count();
    assertThat(r1Matches).isEqualTo(4);

    // Check for Byes
    // 6 participants. 2 Byes.
    long byeMatches =
        dto.getMatches().stream()
            .filter(m -> m.getRound() == 1 && "BYE".equals(m.getWrestler2Name()))
            .count();
    // Logic puts Byes in Wrestler 2 slot if possible.
    // Slots: P1..P6, Bye, Bye.
    // M1: P1, P2. M2: P3, P4. M3: P5, P6. M4: Bye, Bye (Double Bye)
    // Wait, my logic:
    // for (int i=0; i<4; i++)
    // w1 = slots[i*2], w2 = slots[i*2+1]
    // 0,1 -> P1, P2
    // 2,3 -> P3, P4
    // 4,5 -> P5, P6
    // 6,7 -> Bye, Bye

    // So there should be 1 Double Bye match?
    // And NO single Bye matches?
    // "BYE" name is set if w1 or w2 is null.
    long doubleByes =
        dto.getMatches().stream()
            .filter(
                m ->
                    m.getRound() == 1
                        && m.getWrestler1Name() == null
                        && m.getWrestler2Name() == null) // wait, names set to "BYE"
            .count();

    // Logic sets name to "BYE"
    long byeCount =
        dto.getMatches().stream()
            .filter(
                m ->
                    m.getRound() == 1
                        && "BYE".equals(m.getWrestler1Name())
                        && "BYE".equals(m.getWrestler2Name()))
            .count();

    assertThat(byeCount).isEqualTo(1);
  }

  @Test
  void testInitializeTournament_CapAt16() throws JsonProcessingException {
    // 20 Opponents + Player = 21. Should cap at 16. Bracket Size 16.
    List<Wrestler> roster = new ArrayList<>();
    for (int i = 2; i <= 21; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Opponent " + i);
      roster.add(w);
    }
    roster.add(player);

    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    TournamentDTO dto = objectMapper.readValue(state.getTournamentState(), TournamentDTO.class);
    // 16 participants -> Bracket Size 16. Rounds: 4.
    assertThat(dto.getTotalRounds()).isEqualTo(4);

    long r1Matches = dto.getMatches().stream().filter(m -> m.getRound() == 1).count();
    assertThat(r1Matches).isEqualTo(8);
  }

  @Test
  void testAdvanceTournament_PlayerWin() throws JsonProcessingException {
    // Setup a simple bracket (Size 4 -> 2 Rounds)
    // P1 vs P2. P3 vs P4.
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    TournamentMatch m1 = new TournamentMatch();
    m1.setId("R1-M1");
    m1.setRound(1);
    m1.setWrestler1Id(player.getId());
    m1.setWrestler1Name(player.getName());
    m1.setWrestler2Id(2L);
    m1.setWrestler2Name("Opponent");
    m1.setNextMatchId("R2-M1");
    m1.setPlayerMatch(true);

    TournamentMatch m2 = new TournamentMatch();
    m2.setId("R1-M2");
    m2.setRound(1);
    m2.setWrestler1Id(3L);
    m2.setWrestler2Id(4L);
    m2.setNextMatchId("R2-M1");

    TournamentMatch m3 = new TournamentMatch();
    m3.setId("R2-M1");
    m3.setRound(2);

    List<TournamentMatch> matches = new ArrayList<>();
    matches.add(m1);
    matches.add(m2);
    matches.add(m3);
    dto.setMatches(matches);

    state.setTournamentState(objectMapper.writeValueAsString(dto));

    tournamentService.advanceTournament(campaign, true, null); // Win

    TournamentDTO result = objectMapper.readValue(state.getTournamentState(), TournamentDTO.class);

    // Check Round advanced
    assertThat(result.getCurrentRound()).isEqualTo(2);

    // Check M1 winner is player
    TournamentMatch resM1 =
        result.getMatches().stream().filter(m -> m.getId().equals("R1-M1")).findFirst().get();
    assertThat(resM1.getWinnerId()).isEqualTo(player.getId());

    // Check M2 winner is set (simulated)
    TournamentMatch resM2 =
        result.getMatches().stream().filter(m -> m.getId().equals("R1-M2")).findFirst().get();
    assertThat(resM2.getWinnerId()).isNotNull();

    // Check M3 (R2) has player propagated
    TournamentMatch resM3 =
        result.getMatches().stream().filter(m -> m.getId().equals("R2-M1")).findFirst().get();
    assertThat(resM3.isPlayerMatch()).isTrue();
    // One slot should be player
    boolean playerInM3 =
        (resM3.getWrestler1Id() != null && resM3.getWrestler1Id().equals(player.getId()))
            || (resM3.getWrestler2Id() != null && resM3.getWrestler2Id().equals(player.getId()));
    assertThat(playerInM3).isTrue();
  }

  @Test
  void testAdvanceTournament_ByeLogic() throws JsonProcessingException {
    // Setup a bracket where R1 has Bye vs Bye
    // M1: Bye vs Bye -> Next M2.
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    // Dummy player match to satisfy "player must play" context usually required to trigger advance?
    // No, advanceTournament simulates all. But assumes player played one.
    // If we call advanceTournament(..., true), it assumes player match resolved.
    // Let's ensure there IS a player match.
    TournamentMatch mPlayer = new TournamentMatch();
    mPlayer.setId("R1-M1");
    mPlayer.setRound(1);
    mPlayer.setWrestler1Id(player.getId());
    mPlayer.setWrestler2Id(2L);
    mPlayer.setPlayerMatch(true);

    TournamentMatch mBye = new TournamentMatch();
    mBye.setId("R1-M2");
    mBye.setRound(1);
    mBye.setWrestler1Name("BYE");
    mBye.setWrestler2Name("BYE");
    // IDs null

    List<TournamentMatch> matches = new ArrayList<>();
    matches.add(mPlayer);
    matches.add(mBye);
    dto.setMatches(matches);

    state.setTournamentState(objectMapper.writeValueAsString(dto));

    tournamentService.advanceTournament(campaign, true, null);

    TournamentDTO result = objectMapper.readValue(state.getTournamentState(), TournamentDTO.class);

    // Bye match should be resolved with -1
    TournamentMatch resMBye =
        result.getMatches().stream().filter(m -> m.getId().equals("R1-M2")).findFirst().get();
    assertThat(resMBye.getWinnerId()).isEqualTo(-1L);

    // Round should advance (all resolved)
    // Note: totalRounds=2, matches defined only for R1? No, logic checks 'currentRoundMatches'.
    // If roundComplete, currentRound++
    assertThat(result.getCurrentRound()).isEqualTo(2);
  }

  @Test
  void testIsPlayerChampion() throws JsonProcessingException {
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);

    TournamentMatch finals = new TournamentMatch();
    finals.setId("R2-M1");
    finals.setRound(2);
    finals.setWinnerId(player.getId());

    List<TournamentMatch> matches = new ArrayList<>();
    matches.add(finals);
    dto.setMatches(matches);
    state.setTournamentState(objectMapper.writeValueAsString(dto));

    assertThat(tournamentService.isPlayerChampion(campaign)).isTrue();

    finals.setWinnerId(2L); // Other winner
    state.setTournamentState(objectMapper.writeValueAsString(dto));
    assertThat(tournamentService.isPlayerChampion(campaign)).isFalse();
  }
}
