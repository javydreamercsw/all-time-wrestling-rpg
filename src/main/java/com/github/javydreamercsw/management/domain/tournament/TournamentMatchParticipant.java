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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * One ordered slot of a tournament match's entrant list (ATW-oloa). Two-slot matches keep their
 * classic {@code entrant1}/{@code entrant2} columns; matches with three or more entrants
 * (Free-for-All qualifiers, multi-man finals) carry every entrant here — slot 0..n-1 — and the
 * classic columns mirror the first two slots so existing queries stay valid.
 */
@Entity
@Table(
    name = "tournament_match_participant",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"match_id", "slot"}),
      @UniqueConstraint(columnNames = {"match_id", "entry_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentMatchParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private TournamentMatch match;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entry_id", nullable = false)
  private TournamentEntry entry;

  /** Zero-based position in the match's entrant order. */
  @Column(name = "slot", nullable = false)
  private int slot;
}
