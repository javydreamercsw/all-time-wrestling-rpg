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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ringside_action_type", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class RingsideActionType extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ringside_action_type_id")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "increases_awareness", nullable = false)
  private boolean increasesAwareness = true;

  @Column(name = "can_cause_dq", nullable = false)
  private boolean canCauseDq = true;

  @Column(name = "base_risk_multiplier", nullable = false)
  private double baseRiskMultiplier = 1.0;

  @Override
  public @Nullable Long getId() {
    return id;
  }
}
