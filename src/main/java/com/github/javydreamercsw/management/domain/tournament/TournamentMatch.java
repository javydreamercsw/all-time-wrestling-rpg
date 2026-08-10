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

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "tournament_match")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentMatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "round_id", nullable = false)
  private TournamentRound round;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entrant1_id", nullable = false)
  private TournamentEntry entrant1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entrant2_id", nullable = false)
  private TournamentEntry entrant2;

  /** Linked show segment — null until the match is booked onto a show. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_id")
  @Nullable private Segment segment;

  /** Set once the match is complete. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_entry_id")
  @Nullable private TournamentEntry winner;
}
