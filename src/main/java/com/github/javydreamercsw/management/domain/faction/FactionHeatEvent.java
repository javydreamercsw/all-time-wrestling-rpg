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
package com.github.javydreamercsw.management.domain.faction;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single heat-generating event in a faction rivalry. Tracks what happened, when, and
 * how much heat was gained/lost.
 */
@Entity
@Table(name = "faction_heat_event")
@Getter
@Setter
public class FactionHeatEvent extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "faction_heat_event_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_rivalry_id", nullable = false)
  private FactionRivalry factionRivalry;

  @Column(name = "heat_change", nullable = false)
  private Integer heatChange;

  @Column(name = "heat_after_event", nullable = false)
  private Integer heatAfterEvent;

  @Column(name = "reason", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String reason;

  @Column(name = "event_date", nullable = false)
  private Instant eventDate;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (eventDate == null) {
      eventDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Get display string for this heat event. */
  public String getDisplayString() {
    String changeStr;
    if (heatChange > 0) {
      changeStr = "+" + heatChange;
    } else if (heatChange < 0) {
      changeStr = String.valueOf(heatChange);
    } else {
      changeStr = "±0";
    }

    return "%s (%s heat → %d total)".formatted(reason, changeStr, heatAfterEvent);
  }
}
