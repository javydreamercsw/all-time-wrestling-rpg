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
package com.github.javydreamercsw.management.domain.tournament;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * A named trophy won by a tournament edition's winner: "2026 Dusty Cup — X". One row per completed
 * trophy tournament (stamped once by {@code TournamentService#markWinner}, unique on the
 * tournament), so a recurring trophy's lineage is its rows across editions ordered by year. Exactly
 * one of {@code winnerWrestler}/{@code winnerTeam} is set (DB CHECK {@code chk_tr_winner}); team
 * wins expand to member-level honors at read time so every member can be referenced as a "2026
 * Dusty Cup winner" in AI narration.
 */
@Entity
@Table(name = "trophy_reign")
@Getter
@Setter
public class TrophyReign extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "id")
  private Long id;

  /** The tournament edition whose winner lifted the trophy. */
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  /** The trophy name, denormalized from the tournament for queryability ("Dusty Cup"). */
  @Column(name = "trophy_name", nullable = false)
  private String trophyName;

  /** Game-calendar year of the win — the "2026" in "2026 Dusty Cup winner". */
  @Column(name = "year_won", nullable = false)
  private int yearWon;

  /** Game date the tournament completed. */
  @Column(name = "won_on", nullable = false)
  private LocalDate wonOn;

  /** Singles winner. Null exactly when {@code winnerTeam} is set. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "winner_wrestler_id")
  @Nullable private Wrestler winnerWrestler;

  /** Team winner (members expand at read time). Null exactly when {@code winnerWrestler} is set. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "winner_team_id")
  @Nullable private Team winnerTeam;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "universe_id")
  @Nullable private Universe universe;
}
