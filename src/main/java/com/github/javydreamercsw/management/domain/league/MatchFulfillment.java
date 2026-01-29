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
package com.github.javydreamercsw.management.domain.league;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MatchFulfillment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull @ManyToOne
  @JoinColumn(name = "segment_id")
  private Segment segment;

  @NotNull @ManyToOne
  @JoinColumn(name = "league_id")
  private League league;

  @Enumerated(EnumType.STRING)
  private FulfillmentStatus status = FulfillmentStatus.PENDING_RESULTS;

  @ManyToOne
  @JoinColumn(name = "winner_id")
  private Wrestler reportedWinner;

  @ManyToOne
  @JoinColumn(name = "submitted_by_id")
  private Account submittedBy;

  public enum FulfillmentStatus {
    PENDING_RESULTS,
    SUBMITTED,
    FINALIZED
  }
}
