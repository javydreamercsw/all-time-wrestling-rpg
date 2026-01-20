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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campaign_encounter")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignEncounter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Column(name = "chapter_number", nullable = false)
  private int chapterNumber;

  @Column(name = "narrative_text", nullable = false, length = 4000)
  private String narrativeText;

  @Column(name = "player_choice", length = 1000)
  private String playerChoice;

  @Column(name = "alignment_shift", nullable = false)
  @Builder.Default
  private int alignmentShift = 0;

  @Column(name = "vp_reward", nullable = false)
  @Builder.Default
  private int vpReward = 0;

  @Column(name = "encounter_date", nullable = false)
  private LocalDateTime encounterDate;
}
