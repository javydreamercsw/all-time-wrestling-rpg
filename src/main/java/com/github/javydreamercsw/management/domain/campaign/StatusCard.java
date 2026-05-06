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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines a Status Card that represents a wrestler's mental or social state. Status cards are
 * double-sided (Level I and Level II) and provide modifiers to matches and backstage actions.
 */
@Entity
@Table(name = "status_card")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "status_key", nullable = false, unique = true)
  private String key;

  @Column(name = "level_1_name", nullable = false)
  private String level1Name;

  @Column(name = "level_2_name")
  private String level2Name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  @Builder.Default
  private boolean positive = true;

  @Column(name = "level_1_effect")
  private String level1Effect;

  @Column(name = "level_2_effect")
  private String level2Effect;

  @Column(name = "flip_up_condition")
  private String flipUpCondition;

  @Column(name = "flip_down_condition")
  private String flipDownCondition;

  @Column(name = "discard_condition")
  private String discardCondition;
}
