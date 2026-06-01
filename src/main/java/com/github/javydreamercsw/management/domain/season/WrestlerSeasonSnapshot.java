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
package com.github.javydreamercsw.management.domain.season;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Records each wrestler's fan count at the moment a new season starts. */
@Entity
@Table(
    name = "wrestler_season_snapshot",
    uniqueConstraints = @UniqueConstraint(columnNames = {"wrestler_id", "season_id"}))
@Getter
@Setter
public class WrestlerSeasonSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "snapshot_id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @ManyToOne(optional = false)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @Column(name = "starting_fans", nullable = false)
  private Long startingFans = 0L;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
