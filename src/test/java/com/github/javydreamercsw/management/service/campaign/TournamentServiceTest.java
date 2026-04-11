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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentService segmentService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private TournamentService tournamentService;

  private Campaign campaign;
  private Wrestler player;

  @BeforeEach
  void setUp() {
    player = new Wrestler();
    player.setId(1L);
    player.setName("Player One");

    CampaignState state = new CampaignState();

    campaign = new Campaign();
    campaign.setWrestler(player);
    campaign.setState(state);
  }

  @Test
  void testInitializeTournament() {
    List<Wrestler> roster = new ArrayList<>();
    // Adding 4 wrestlers + Player = 5 total.
    // Next power of 2 is 8.
    // Rounds = log2(8) = 3.
    // Matches = 4 + 2 + 1 = 7.
    for (long i = 2; i <= 5; i++) {
      Wrestler w = new Wrestler();
      w.setId(i);
      w.setName("Wrestler " + i);
      roster.add(w);
    }
    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    TournamentDTO tournament = tournamentService.getTournamentState(campaign);
    assertThat(tournament).isNotNull();
    assertThat(tournament.getTotalRounds()).isEqualTo(3);
    assertThat(tournament.getMatches()).hasSize(7);

    verify(campaignStateRepository).save(any(CampaignState.class));
  }

  @Test
  void testInitializeTournament_AlreadyInitialized() throws Exception {
    TournamentDTO existing = new TournamentDTO();
    String json = "{\"tournamentState\":" + objectMapper.writeValueAsString(existing) + "}";
    campaign.getState().setFeatureData(json);

    tournamentService.initializeTournament(campaign);

    // Should not call save if already initialized
    verify(campaignStateRepository, org.mockito.Mockito.never()).save(any(CampaignState.class));
  }

  @Test
  void testAdvanceTournament() {
    // Setup a simple 2-man tournament (Player vs Opponent)
    List<Wrestler> roster = new ArrayList<>();
    Wrestler opponent = new Wrestler();
    opponent.setId(2L);
    opponent.setName("Opponent");
    roster.add(opponent);

    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    // There should be 1 match in round 1
    TournamentDTO tournament = tournamentService.getTournamentState(campaign);
    assertThat(tournament.getMatches()).hasSize(3); // 4 slots forced, so 2 matches R1, 1 match R2.
    // Wait, dynamic sizing: 2 participants -> bracketSize 4?
    // Code says: if (bracketSize < 4) bracketSize = 4;
    // So yes, 4 slots. Player vs Opponent, Bye vs Bye (or similar).

    // Let's verify the player match exists
    TournamentDTO.TournamentMatch playerMatch = tournamentService.getCurrentPlayerMatch(campaign);
    assertThat(playerMatch).isNotNull();
    assertThat(playerMatch.getRound()).isEqualTo(1);

    // Advance - Player Wins
    tournamentService.advanceTournament(campaign, true, null);

    tournament = tournamentService.getTournamentState(campaign);
    TournamentDTO.TournamentMatch updatedMatch =
        tournament.getMatches().stream()
            .filter(m -> m.getId().equals(playerMatch.getId()))
            .findFirst()
            .orElseThrow();

    assertThat(updatedMatch.getWinnerId()).isEqualTo(player.getId());

    // Check next round propagation
    TournamentDTO.TournamentMatch nextMatch =
        tournament.getMatches().stream()
            .filter(m -> m.getId().equals(updatedMatch.getNextMatchId()))
            .findFirst()
            .orElseThrow();

    // Should have propagated the player to the next match
    boolean playerInNext =
        (nextMatch.getWrestler1Id() != null && nextMatch.getWrestler1Id().equals(player.getId()))
            || (nextMatch.getWrestler2Id() != null
                && nextMatch.getWrestler2Id().equals(player.getId()));
    assertThat(playerInNext).isTrue();
  }

  @Test
  void testIsPlayerChampion() {
    // Manually construct a completed tournament state where player won finals
    TournamentDTO tournament = new TournamentDTO();
    tournament.setTotalRounds(1);

    TournamentDTO.TournamentMatch finalMatch = new TournamentDTO.TournamentMatch();
    finalMatch.setId("R1-M1");
    finalMatch.setRound(1);
    finalMatch.setWrestler1Id(player.getId());
    finalMatch.setWrestler2Id(2L);
    finalMatch.setWinnerId(player.getId());

    tournament.setMatches(List.of(finalMatch));

    try {
      String json = "{\"tournamentState\":" + objectMapper.writeValueAsString(tournament) + "}";
      campaign.getState().setFeatureData(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    assertThat(tournamentService.isPlayerChampion(campaign)).isTrue();
  }
}
