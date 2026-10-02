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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  /**
   * Full ordered entrant list for multi-entrant matches (ATW-oloa): three or more entrants carry
   * every participant here, slot 0..n-1. Two-entrant matches leave this empty and use {@link
   * #entrant1}/{@link #entrant2} — {@link #entrants()} normalizes both shapes.
   *
   * <p>EAGER: the bracket renders on detached rows outside a transaction (list/detail views) — a
   * lazy collection here would throw LazyInitializationException in the UI (same reason payoffShow
   * is EAGER on Tournament, ATW-xbn4).
   */
  @OneToMany(
      mappedBy = "match",
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @OrderBy("slot ASC")
  @Builder.Default
  private List<TournamentMatchParticipant> participants = new ArrayList<>();

  /**
   * All entrants in match order: the participant rows when the match is multi-entrant, otherwise
   * the classic two columns. The bracket UI, booking, and result recording all read this.
   */
  @Transient
  public List<TournamentEntry> entrants() {
    if (participants != null && !participants.isEmpty()) {
      return participants.stream()
          .sorted(Comparator.comparingInt(TournamentMatchParticipant::getSlot))
          .map(TournamentMatchParticipant::getEntry)
          .toList();
    }
    return List.of(entrant1, entrant2);
  }

  /**
   * True when this match has more than two entrants (Free-for-All qualifier, multi-man final).
   * Booking picks the multi-team segment-resolution path for these.
   */
  @Transient
  public boolean isMultiEntrant() {
    return participants != null && participants.size() > 2;
  }

  /** Linked show segment — null until the match is booked onto a show. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_id")
  @Nullable private Segment segment;

  /** Set once the match is complete. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_entry_id")
  @Nullable private TournamentEntry winner;
}
