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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "tournament_round")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRound {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  /** 1-based round number. */
  @Column(name = "round_number", nullable = false)
  private int roundNumber;

  @Column(name = "round_name", nullable = false)
  private String roundName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  @Builder.Default
  private TournamentRoundStatus status = TournamentRoundStatus.PENDING;

  /** The show on which this round's matches are booked. Null until assigned. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "show_id")
  @Nullable private Show show;

  /** Fixed segment rule for this round. Overrides the tournament's allowedRules pool. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_rule_id")
  @Nullable private SegmentRule fixedRule;

  @OneToMany(
      mappedBy = "round",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("id ASC")
  @Builder.Default
  private List<TournamentMatch> matches = new ArrayList<>();
}
