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

import com.github.javydreamercsw.management.domain.tournament.TrophyReign;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the trophy lineage recorded at tournament completion: per-wrestler honor lines for AI
 * narration — "2026 Dusty Cup winner" — and the detail view's honors list. Team wins expand to
 * every member, so each member of the 2026 Dusty Cup-winning team is a "2026 Dusty Cup winner (with
 * A & B)" to the narrator, the same channel the briefcase context rides.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrophyReignService {

  private final TrophyReignRepository trophyReignRepository;

  /**
   * The named-tournament trophies the wrestler has won, newest first: "2026 Dusty Cup winner" for a
   * singles win, "2026 Dusty Cup winner (with A & B)" for a team win (teammates named, the queried
   * wrestler excluded). Empty when the wrestler holds no trophy honors.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<String> trophyHonorsOf(Long wrestlerId) {
    List<TrophyReign> reigns = new ArrayList<>();
    reigns.addAll(trophyReignRepository.findByWinnerWrestlerId(wrestlerId));
    reigns.addAll(trophyReignRepository.findByTeamMemberWrestlerId(wrestlerId));
    return reigns.stream()
        .sorted((a, b) -> Integer.compare(b.getYearWon(), a.getYearWon()))
        .map(reign -> honorLineOf(reign, wrestlerId))
        .toList();
  }

  private String honorLineOf(TrophyReign reign, Long wrestlerId) {
    StringBuilder line =
        new StringBuilder(reign.getYearWon() + " " + reign.getTrophyName() + " winner");
    if (reign.getWinnerTeam() != null) {
      List<String> teammates =
          reign.getWinnerTeam().getMembers().stream()
              .map(member -> member.getWrestler())
              .filter(w -> !w.getId().equals(wrestlerId))
              .map(Wrestler::getName)
              .toList();
      if (!teammates.isEmpty()) {
        line.append(" (with ").append(String.join(" & ", teammates)).append(")");
      }
    }
    return line.toString();
  }

  /** Detail-view lineage for a trophy: newest edition first. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<TrophyReign> lineageOf(String trophyName) {
    return trophyReignRepository.findByTrophyNameOrderByYearWonDesc(trophyName);
  }

  /** The reign stamped for one tournament edition, if any. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public @Nullable TrophyReign reignOfTournament(Long tournamentId) {
    return trophyReignRepository.findByTournamentId(tournamentId).orElse(null);
  }
}
