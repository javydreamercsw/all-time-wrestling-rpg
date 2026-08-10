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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "tournament")
@Getter
@Setter
public class Tournament extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "tournament_id")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  /** Format identifier matching {@code TournamentFormat#getFormatId()}. */
  @Column(name = "format_id", nullable = false, length = 64)
  private String formatId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private TournamentStatus status = TournamentStatus.SCHEDULED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "universe_id")
  @Nullable private Universe universe;

  /** Optional title awarded to the tournament winner. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "title_id")
  @Nullable private Title linkedTitle;

  @Column(name = "start_date")
  @Nullable private LocalDate startDate;

  @Column(name = "end_date")
  @Nullable private LocalDate endDate;

  @OneToMany(
      mappedBy = "tournament",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("seed ASC")
  private List<TournamentEntry> entries = new ArrayList<>();

  @OneToMany(
      mappedBy = "tournament",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("roundNumber ASC")
  private List<TournamentRound> rounds = new ArrayList<>();
}
