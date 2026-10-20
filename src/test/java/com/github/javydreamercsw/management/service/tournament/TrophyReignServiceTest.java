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
package com.github.javydreamercsw.management.service.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TrophyReign;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the trophy honor lines the narrator reads ({@code trophyHonorsOf}): singles wins
 * render "2026 Dusty Cup winner", team wins name the teammates — the same honor-line shape the
 * briefcase context feeds {@code WrestlerContext}.
 */
@ExtendWith(MockitoExtension.class)
class TrophyReignServiceTest {

  @Mock private TrophyReignRepository trophyReignRepository;

  @InjectMocks private TrophyReignService service;

  @Test
  @DisplayName("A singles win renders '<year> <trophy> winner'")
  void trophyHonorsOf_singlesWin() {
    Wrestler ace = wrestler("Ace", 1L);
    TrophyReign reign = reign("Dusty Cup", 2026);
    reign.setWinnerWrestler(ace);
    when(trophyReignRepository.findByWinnerWrestlerId(1L)).thenReturn(List.of(reign));
    when(trophyReignRepository.findByTeamMemberWrestlerId(1L)).thenReturn(List.of());

    assertThat(service.trophyHonorsOf(1L)).containsExactly("2026 Dusty Cup winner");
  }

  @Test
  @DisplayName("A team win names the teammates — the queried wrestler is excluded")
  void trophyHonorsOf_teamWinNamesTeammates() {
    Wrestler ace = wrestler("Ace", 1L);
    Wrestler bea = wrestler("Bea", 2L);
    Wrestler cy = wrestler("Cy", 3L);
    Team duo = team("High Flyers", ace, bea, cy);
    TrophyReign reign = reign("Dusty Cup", 2026);
    reign.setWinnerTeam(duo);
    // Team wins surface through the team-membership query.
    when(trophyReignRepository.findByWinnerWrestlerId(2L)).thenReturn(List.of());
    when(trophyReignRepository.findByTeamMemberWrestlerId(2L)).thenReturn(List.of(reign));

    assertThat(service.trophyHonorsOf(2L)).containsExactly("2026 Dusty Cup winner (with Ace & Cy)");
  }

  @Test
  @DisplayName("Multiple honors sort newest year first")
  void trophyHonorsOf_newestFirst() {
    Wrestler ace = wrestler("Ace", 1L);
    TrophyReign older = reign("Iron Cup", 2024);
    older.setWinnerWrestler(ace);
    TrophyReign newer = reign("Dusty Cup", 2026);
    newer.setWinnerWrestler(ace);
    when(trophyReignRepository.findByWinnerWrestlerId(1L)).thenReturn(List.of(older, newer));
    when(trophyReignRepository.findByTeamMemberWrestlerId(1L)).thenReturn(List.of());

    assertThat(service.trophyHonorsOf(1L))
        .containsExactly("2026 Dusty Cup winner", "2024 Iron Cup winner");
  }

  @Test
  @DisplayName("A wrestler with no trophy wins gets no honor lines")
  void trophyHonorsOf_empty() {
    when(trophyReignRepository.findByWinnerWrestlerId(1L)).thenReturn(List.of());
    when(trophyReignRepository.findByTeamMemberWrestlerId(1L)).thenReturn(List.of());

    assertThat(service.trophyHonorsOf(1L)).isEmpty();
  }

  @Test
  @DisplayName("lineageOf returns the trophy's editions newest first; reignOfTournament one row")
  void lineageAndReignQueries() {
    TrophyReign reign = reign("Dusty Cup", 2026);
    reign.setTournament(new Tournament());
    when(trophyReignRepository.findByTrophyNameOrderByYearWonDesc("Dusty Cup"))
        .thenReturn(List.of(reign));
    when(trophyReignRepository.findByTournamentId(7L)).thenReturn(java.util.Optional.of(reign));

    assertThat(service.lineageOf("Dusty Cup")).containsExactly(reign);
    assertThat(service.reignOfTournament(7L)).isSameAs(reign);
    assertThat(service.reignOfTournament(99L)).isNull();
  }

  // ── Fixtures ─────────────────────────────────────────────────────────────

  private Wrestler wrestler(String name, long id) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private Team team(String name, Wrestler... members) {
    Team team = new Team();
    team.setName(name);
    team.setMembers(new java.util.ArrayList<>(List.of(members)));
    return team;
  }

  private TrophyReign reign(String trophyName, int yearWon) {
    TrophyReign reign = new TrophyReign();
    reign.setTrophyName(trophyName);
    reign.setYearWon(yearWon);
    return reign;
  }
}
