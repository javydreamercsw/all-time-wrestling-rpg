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

  @Test
  void testInitializeTournament_FullRoster() {
    Campaign campaign = createCampaign();
    List<Wrestler> roster = createRoster(15); // 15 + player = 16 (Full Bracket)

    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    verify(campaignStateRepository).save(any(CampaignState.class));
    TournamentDTO tournament = tournamentService.getTournamentState(campaign);
    assertThat(tournament).isNotNull();
    assertThat(tournament.getTotalRounds()).isEqualTo(4); // log2(16) = 4
    assertThat(tournament.getMatches()).hasSize(15); // 8 (R1) + 4 (R2) + 2 (R3) + 1 (R4) = 15
  }

  @Test
  void testInitializeTournament_SmallRoster_WithByes() {
    Campaign campaign = createCampaign();
    List<Wrestler> roster = createRoster(5); // 5 + player = 6 (Needs size 8)

    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    verify(campaignStateRepository).save(any(CampaignState.class));
    TournamentDTO tournament = tournamentService.getTournamentState(campaign);
    assertThat(tournament).isNotNull();
    assertThat(tournament.getTotalRounds()).isEqualTo(3); // log2(8) = 3
    // Check if byes were handled (winners propagated)
    long resolvedMatches =
        tournament.getMatches().stream().filter(m -> m.getWinnerId() != null).count();
    assertThat(resolvedMatches).isGreaterThan(0);
  }

  @Test
  void testGetCurrentPlayerMatch() {
    Campaign campaign = createCampaign();
    // Manually inject a tournament state where player is in R1-M1
    TournamentDTO dto = new TournamentDTO();
    TournamentDTO.TournamentMatch match = new TournamentDTO.TournamentMatch();
    match.setId("R1-M1");
    match.setPlayerMatch(true);
    dto.setMatches(List.of(match));

    saveState(campaign, dto);

    TournamentDTO.TournamentMatch found = tournamentService.getCurrentPlayerMatch(campaign);
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo("R1-M1");
  }

  @Test
  void testAdvanceTournament_PlayerWin() {
    Campaign campaign = createCampaign();
    List<Wrestler> roster = createRoster(15);
    when(wrestlerRepository.findAll()).thenReturn(roster);

    tournamentService.initializeTournament(campaign);

    TournamentDTO.TournamentMatch playerMatch = tournamentService.getCurrentPlayerMatch(campaign);
    assertThat(playerMatch).isNotNull();

    tournamentService.advanceTournament(campaign, true, null);

    TournamentDTO updatedState = tournamentService.getTournamentState(campaign);
    TournamentDTO.TournamentMatch executedMatch =
        updatedState.getMatches().stream()
            .filter(m -> m.getId().equals(playerMatch.getId()))
            .findFirst()
            .orElseThrow();

    assertThat(executedMatch.getWinnerId()).isEqualTo(campaign.getWrestler().getId());
  }

  private Campaign createCampaign() {
    Wrestler player = new Wrestler();
    player.setId(999L);
    player.setName("Player One");

    CampaignState state = new CampaignState();
    state.setFeatureData("{}");

    Campaign campaign = new Campaign();
    campaign.setWrestler(player);
    campaign.setState(state);
    return campaign;
  }

  private List<Wrestler> createRoster(int count) {
    List<Wrestler> roster = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      roster.add(w);
    }
    return roster;
  }

  private void saveState(Campaign campaign, TournamentDTO dto) {
    try {
      String json = objectMapper.writeValueAsString(java.util.Map.of("tournamentState", dto));
      campaign.getState().setFeatureData(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
