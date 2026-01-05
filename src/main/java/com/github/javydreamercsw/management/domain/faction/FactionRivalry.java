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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a rivalry between two factions in the ATW RPG system. Faction rivalries can involve
 * multiple wrestlers and create complex storylines.
 */
@Entity
@Table(
    name = "faction_rivalry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"faction1_id", "faction2_id"}))
@Getter
@Setter
public class FactionRivalry extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "faction_rivalry_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "faction1_id", nullable = false)
  @JsonIgnoreProperties({"members", "rivalriesAsFaction1", "rivalriesAsFaction2"})
  private Faction faction1;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "faction2_id", nullable = false)
  @JsonIgnoreProperties({"members", "rivalriesAsFaction1", "rivalriesAsFaction2"})
  private Faction faction2;

  @Column(name = "heat", nullable = false)
  @Min(0) private Integer heat = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "started_date", nullable = false)
  private Instant startedDate;

  @Column(name = "ended_date")
  private Instant endedDate;

  @Lob
  @Column(name = "storyline_notes")
  private String storylineNotes;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // Heat events for this faction rivalry
  @OneToMany(mappedBy = "factionRivalry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"factionRivalry"})
  private List<FactionHeatEvent> heatEvents = new ArrayList<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (startedDate == null) {
      startedDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Add heat to the faction rivalry. */
  public void addHeat(int heatGain, String reason) {
    this.heat += heatGain;

    // Create heat event for tracking
    FactionHeatEvent event = new FactionHeatEvent();
    event.setFactionRivalry(this);
    event.setHeatChange(heatGain);
    event.setReason(reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Check if factions must have segments at next show (10+ heat). */
  public boolean mustHaveSegmentsNextShow() {
    return isActive && heat >= 10;
  }

  /** Check if rivalry can be resolved with a roll (20+ heat). */
  public boolean canAttemptResolution() {
    return isActive && heat >= 20;
  }

  /** Check if rivalry requires rule segment (30+ heat). */
  public boolean requiresStipulationSegment() {
    return isActive && heat >= 30;
  }

  /**
   * Attempt to resolve the faction rivalry with dice roll. ATW Rule: Both factions roll d20 → total
   * >30 = rivalry ends
   */
  public boolean attemptResolution(int faction1Roll, int faction2Roll) {
    if (!canAttemptResolution()) {
      return false;
    }

    int totalRoll = faction1Roll + faction2Roll;
    boolean resolved = totalRoll > 30;

    if (resolved) {
      endRivalry(
          "Resolved by dice roll: " + faction1Roll + " + " + faction2Roll + " = " + totalRoll);
    }

    // Create heat event for the resolution attempt
    String reason =
        resolved
            ? "Faction rivalry resolved by dice roll (" + totalRoll + ")"
            : "Failed faction resolution attempt (" + totalRoll + ")";

    FactionHeatEvent event = new FactionHeatEvent();
    event.setFactionRivalry(this);
    event.setHeatChange(0);
    event.setReason(reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);

    return resolved;
  }

  /** End the faction rivalry. */
  public void endRivalry(String reason) {
    this.isActive = false;
    this.endedDate = Instant.now();

    // Add final heat event
    FactionHeatEvent event = new FactionHeatEvent();
    event.setFactionRivalry(this);
    event.setHeatChange(0);
    event.setReason("Faction rivalry ended: " + reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Get the opposing faction in the rivalry. */
  public Faction getOpponent(Faction faction) {
    if (faction.equals(faction1)) {
      return faction2;
    } else if (faction.equals(faction2)) {
      return faction1;
    }
    throw new IllegalArgumentException("Faction is not part of this rivalry");
  }

  /** Check if a faction is involved in this rivalry. */
  public boolean involvesFaction(Faction faction) {
    return faction.equals(faction1) || faction.equals(faction2);
  }

  /** Get rivalry intensity level based on heat. */
  public RivalryIntensity getIntensity() {
    if (heat < 10) return RivalryIntensity.SIMMERING;
    if (heat < 20) return RivalryIntensity.HEATED;
    if (heat < 30) return RivalryIntensity.INTENSE;
    return RivalryIntensity.EXPLOSIVE;
  }

  /** Get heat multiplier based on faction rivalry intensity. */
  public double getIntensityHeatMultiplier() {
    // Use rivalry intensity instead of alignment for heat calculation
    return getIntensity().getHeatMultiplier();
  }

  /** Get display name for this rivalry. */
  public String getDisplayName() {
    return faction1.getName() + " vs " + faction2.getName();
  }

  /** Get rivalry summary with heat and intensity. */
  public String getRivalrySummary() {
    return String.format(
        "%s (%d heat - %s)", getDisplayName(), heat, getIntensity().getDisplayName());
  }

  /** Check if both factions are still active. */
  public boolean areBothFactionsActive() {
    return faction1.isActive() && faction2.isActive();
  }

  /** Get total number of wrestlers involved in this rivalry. */
  public int getTotalWrestlersInvolved() {
    return faction1.getMemberCount() + faction2.getMemberCount();
  }
}
