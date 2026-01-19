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
package com.github.javydreamercsw.management.domain.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campaign_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Column(name = "current_chapter", nullable = false)
  private int currentChapter;

  @Column(name = "victory_points", nullable = false)
  private int victoryPoints;

  @Column(name = "skill_tokens", nullable = false)
  private int skillTokens;

  @Column(name = "bumps", nullable = false)
  private int bumps;

  @Column(name = "health_penalty", nullable = false)
  private int healthPenalty;

  @Column(name = "hand_size_penalty", nullable = false)
  private int handSizePenalty;

  @Column(name = "stamina_penalty", nullable = false)
  private int staminaPenalty;

  @Column(name = "last_sync")
  private LocalDateTime lastSync;
}
