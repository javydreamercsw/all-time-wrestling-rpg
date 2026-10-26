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
package com.github.javydreamercsw.management.domain.team;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * One wrestler's membership in a {@link Team}, at an explicit slot. Slots are 0-based and ordered
 * ({@code @OrderBy("slot ASC")} on the owning side), so the legacy wrestler1/wrestler2 semantics
 * map to slots 0 and 1. Mirrors the plain-entity style of {@code TournamentEntry} and the slot
 * pattern of {@code TournamentMatchParticipant}.
 */
@Entity
@Table(
    name = "team_member",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"team_id", "slot"}),
      @UniqueConstraint(columnNames = {"team_id", "wrestler_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  /** The team this membership belongs to (owning side of the ordered member list). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  /** The wrestler occupying this slot on the team. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  /** 0-based position within the team; slot 0 is the legacy "wrestler1". */
  @Column(name = "slot", nullable = false)
  private int slot;
}
