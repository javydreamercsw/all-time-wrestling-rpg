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
package com.github.javydreamercsw.management.domain.show.segment.rule;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a segment rule in the ATW RPG system. Segment rules define special conditions,
 * stipulations, or modifications that can be applied to wrestling matches.
 *
 * <p>Examples: No Disqualification, Steel Cage, Ladder Match, Hell in a Cell, etc. A segment can
 * have multiple rules applied to it.
 */
@Entity
@Table(name = "segment_rule", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class SegmentRule extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "segment_rule_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "requires_high_heat", nullable = false)
  private Boolean requiresHighHeat = false;

  @Column(name = "no_dq", nullable = false)
  private Boolean noDq = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "bump_addition", nullable = false)
  private BumpAddition bumpAddition = BumpAddition.NONE;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }

    if (requiresHighHeat == null) {
      requiresHighHeat = false;
    }
    if (noDq == null) {
      noDq = false;
    }
    if (bumpAddition == null) {
      bumpAddition = BumpAddition.NONE;
    }
  }

  /** Check if this rule is suitable for high-heat rivalries. */
  public boolean isSuitableForHighHeat() {
    return requiresHighHeat != null && requiresHighHeat;
  }

  @NonNull @Override
  public String toString() {
    return name;
  }
}
