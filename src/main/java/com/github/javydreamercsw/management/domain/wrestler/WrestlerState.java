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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.Universe;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores the dynamic state of a wrestler within a specific universe playthrough. This isolates
 * fans, health, and other progression metrics per playthrough instance.
 */
@Entity
@Table(name = "wrestler_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrestlerState implements com.github.javydreamercsw.base.domain.WrestlerData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler_id", nullable = false)
  @JsonIgnore
  private Wrestler wrestler;

  @NotNull @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "universe_id", nullable = false)
  @JsonIgnore
  private Universe universe;

  @NotNull @Column(nullable = false)
  @Min(0) @Builder.Default
  private Long fans = 0L;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private WrestlerTier tier = WrestlerTier.ROOKIE;

  @NotNull @Column(name = "current_health", nullable = false)
  @Builder.Default
  private Integer currentHealth = 15;

  @NotNull @Column(nullable = false)
  @Builder.Default
  private Integer bumps = 0;

  @NotNull @Column(nullable = false)
  @Min(0) @Builder.Default
  private Integer morale = 100;

  @Column(name = "management_stamina")
  @Min(0) @Builder.Default
  private Integer managementStamina = 100;

  @Column(name = "physical_condition")
  @Min(0) @Builder.Default
  private Integer physicalCondition = 100;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_id")
  @JsonIgnore
  private Faction faction;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  @JsonIgnore
  private Npc manager;

  // ==================== WRESTLER DATA IMPLEMENTATION ====================

  @Override
  @JsonIgnore
  public String getName() {
    return wrestler != null ? wrestler.getName() : "Unknown";
  }

  @Override
  @JsonIgnore
  public Gender getGender() {
    return wrestler != null ? wrestler.getGender() : Gender.MALE;
  }

  // ==================== LOGIC METHODS ====================

  public boolean canAfford(long cost) {
    return fans != null && fans >= cost;
  }

  public boolean addBump() {
    bumps++;
    // Basic threshold: 3 bumps = automatic injury
    int threshold = 3;

    // Increase risk if management stamina is low (below 40)
    if (managementStamina != null && managementStamina < 40) {
      // Injuries occur faster when exhausted
      threshold = 2;
    }

    if (bumps >= threshold) {
      bumps = 0; // Reset bumps after injury
      return true;
    }
    return false;
  }

  @JsonIgnore
  public List<com.github.javydreamercsw.management.domain.injury.Injury> getActiveInjuries() {
    // This is now handled by InjuryService, but we'll keep a placeholder if needed
    return new ArrayList<>();
  }

  @JsonIgnore
  public Integer getTotalInjuryPenalty() {
    // This is now handled by InjuryService
    return 0;
  }

  @JsonIgnore
  public List<com.github.javydreamercsw.management.domain.injury.Injury> getInjuries() {
    // This is now handled by InjuryService
    return new ArrayList<>();
  }
}
