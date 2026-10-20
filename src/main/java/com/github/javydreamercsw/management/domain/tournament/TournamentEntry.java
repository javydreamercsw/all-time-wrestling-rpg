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

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * One tournament entrant: either an individual {@link Wrestler} (singles tournament) or a {@link
 * Team} (team tournament — {@code Tournament.teamSize} gates which). Exactly one of the two
 * references is set; the DB check constraint {@code chk_entry_entrant_type} is the backstop, and
 * {@link TournamentService#validateEntrantShape} enforces it at the service layer. Bracket formats
 * pair entries without caring which shape they hold — display code resolves names through {@link
 * #entrantName()} / {@link #entrantId()} rather than dereferencing {@link #getWrestler()} directly.
 */
@Entity
@Table(name = "tournament_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  /**
   * The individual entrant of a singles-tournament entry. Null for a team entry ({@code team} set
   * instead) — exactly one of the two is non-null.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler_id")
  @Nullable private Wrestler wrestler;

  /** The team entrant of a team-tournament entry. Null for an individual entry. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  @Nullable private Team team;

  /** 1-based seed position (1 = top seed). */
  @Column(name = "seed", nullable = false)
  private int seed;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  @Builder.Default
  private TournamentEntryStatus status = TournamentEntryStatus.ACTIVE;

  /**
   * The entrant's display name — the team name for a team entry, the wrestler name for an
   * individual one. Null only for a shapeless (persisted-corrupt) entry; display code guards on
   * that. Every {@code getWrestler().getName()} call site on entries routes here.
   */
  @Nullable public String entrantName() {
    return team != null ? team.getName() : wrestler != null ? wrestler.getName() : null;
  }

  /**
   * The entrant's id — the team id for a team entry, the wrestler id for an individual one (two
   * disjoint id spaces; callers use it only for winner/loser styling and same-entity checks within
   * one entry shape). Every {@code getWrestler().getId()} call site on entries routes here. Never
   * compare it against a Wrestler id from outside the entry — segment winners are wrestlers, so a
   * team entry matches those by its members' ids (see {@code
   * TournamentTemplateBookingService#isEntryWinner}), not by this value.
   */
  @Nullable public Long entrantId() {
    return team != null ? team.getId() : wrestler != null ? wrestler.getId() : null;
  }
}
