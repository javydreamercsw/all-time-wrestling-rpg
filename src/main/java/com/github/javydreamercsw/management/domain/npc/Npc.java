/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.npc;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "npc")
@Data
@EqualsAndHashCode(callSuper = true)
public class Npc extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  @Column(length = 4000)
  private String description;

  @Column private String imageUrl;

  @Column(nullable = false)
  private String
      npcType; // Used for the NPC's role (e.g., Referee, Commentator, Commissioner, Other)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender = Gender.MALE;

  @Enumerated(EnumType.STRING)
  @Column(name = "alignment", nullable = false)
  private AlignmentType alignment = AlignmentType.NEUTRAL;

  @jakarta.persistence.Convert(converter = NpcAttributesConverter.class)
  @Column(columnDefinition = "TEXT")
  private java.util.Map<String, Object> attributes =
      new java.util.HashMap<>(); // Stores JSON data for additional stats like "Awareness"

  @Override
  public Long getId() {
    return id;
  }
}
