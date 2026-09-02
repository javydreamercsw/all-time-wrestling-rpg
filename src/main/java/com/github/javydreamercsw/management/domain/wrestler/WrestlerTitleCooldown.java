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
package com.github.javydreamercsw.management.domain.wrestler;

import com.github.javydreamercsw.management.domain.title.Title;
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
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks a per-title challenge cooldown for a wrestler state. A record here means the wrestler
 * failed to win the title on {@link #failedChallengeDate} and cannot re-qualify as #1 contender for
 * the same title until the configured cooldown period has elapsed.
 */
@Entity
@Table(
    name = "wrestler_title_cooldown",
    uniqueConstraints = @UniqueConstraint(columnNames = {"wrestler_state_id", "title_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrestlerTitleCooldown {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler_state_id", nullable = false)
  private WrestlerState wrestlerState;

  @NotNull @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "title_id", nullable = false)
  private Title title;

  @NotNull @Column(name = "failed_challenge_date", nullable = false)
  private LocalDate failedChallengeDate;
}
