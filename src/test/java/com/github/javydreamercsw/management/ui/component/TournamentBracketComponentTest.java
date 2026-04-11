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
package com.github.javydreamercsw.management.ui.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class TournamentBracketComponentTest {

  @Test
  void testComponentInitialization() {
    TournamentDTO tournament = new TournamentDTO();
    tournament.setTotalRounds(1);

    TournamentMatch match = new TournamentMatch();
    match.setRound(1);
    match.setWrestler1Id(1L);
    match.setWrestler1Name("W1");
    match.setWrestler2Id(2L);
    match.setWrestler2Name("W2");
    match.setWinnerId(1L);

    tournament.setMatches(List.of(match));

    TournamentBracketComponent component = new TournamentBracketComponent(tournament);
    assertThat(component.getChildren()).isNotEmpty();
  }

  @Test
  void testMultiRoundTitles() {
    TournamentDTO tournament = new TournamentDTO();
    tournament.setTotalRounds(4);
    tournament.setMatches(List.of());

    TournamentBracketComponent component = new TournamentBracketComponent(tournament);
    assertThat(component.getElement().getChildCount()).isEqualTo(4);
  }

  @Test
  void testFallbackRounds() {
    TournamentDTO tournament = new TournamentDTO();
    tournament.setTotalRounds(0); // Should fallback to 4
    tournament.setMatches(List.of());

    TournamentBracketComponent component = new TournamentBracketComponent(tournament);
    assertThat(component.getElement().getChildCount()).isEqualTo(4);
  }
}
