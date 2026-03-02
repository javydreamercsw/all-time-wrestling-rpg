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
package com.github.javydreamercsw.management.domain.show.segment;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ringside_action", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class RingsideAction extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ringside_action_id")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "impact", nullable = false)
  private int impact; // Momentum/Weight boost

  @Column(name = "risk", nullable = false)
  private int risk; // Detection increase

  @Enumerated(EnumType.STRING)
  @Column(name = "alignment", nullable = false)
  private AlignmentType alignment = AlignmentType.NEUTRAL;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "ringside_action_type_id", nullable = false)
  private RingsideActionType type;

  @Override
  public @Nullable Long getId() {
    return id;
  }
}
