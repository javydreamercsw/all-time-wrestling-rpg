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
package com.github.javydreamercsw.management.domain.team;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a tag team (2 wrestlers) in the ATW RPG system. Teams are specifically for tag team
 * matches and can be linked to a faction.
 */
@Entity
@Table(name = "team", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class Team extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "team_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  // First wrestler (required)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wrestler1_id", nullable = false)
  private Wrestler wrestler1;

  // Second wrestler (required)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wrestler2_id", nullable = false)
  private Wrestler wrestler2;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private Npc manager;

  // Optional faction association
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_id")
  private Faction faction;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TeamStatus status = TeamStatus.ACTIVE;

  @Column(name = "formed_date", nullable = false)
  private Instant formedDate;

  @Column(name = "disbanded_date")
  private Instant disbandedDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (formedDate == null) {
      formedDate = Instant.now();
    }
    if (status == null) {
      status = TeamStatus.ACTIVE;
    }
  }

  // ==================== BUSINESS METHODS ====================

  /** Check if the team is currently active. */
  public boolean isActive() {
    return status == TeamStatus.ACTIVE;
  }

  /** Check if the team is disbanded. */
  public boolean isDisbanded() {
    return status == TeamStatus.DISBANDED;
  }

  /** Disband the team. */
  public void disband() {
    this.status = TeamStatus.DISBANDED;
    this.disbandedDate = Instant.now();
  }

  /** Reactivate a disbanded team. */
  public void reactivate() {
    this.status = TeamStatus.ACTIVE;
    this.disbandedDate = null;
  }

  /** Check if a wrestler is a member of this team. */
  public boolean hasMember(Wrestler wrestler) {
    return wrestler.equals(wrestler1) || wrestler.equals(wrestler2);
  }

  /** Get the team partner of a given wrestler. */
  public Wrestler getPartner(Wrestler wrestler) {
    if (wrestler.equals(wrestler1)) {
      return wrestler2;
    } else if (wrestler.equals(wrestler2)) {
      return wrestler1;
    }
    throw new IllegalArgumentException("Wrestler is not a member of this team");
  }

  /** Get both wrestlers as a formatted string. */
  public String getMemberNames() {
    String name1 = wrestler1 != null ? wrestler1.getName() : "null";
    String name2 = wrestler2 != null ? wrestler2.getName() : "null";
    return name1 + " & " + name2;
  }

  /** Get display name with status. */
  public String getDisplayName() {
    String baseName = name != null ? name : getMemberNames();
    if (status == TeamStatus.DISBANDED) {
      return baseName + " (Disbanded)";
    }
    return baseName;
  }

  /** Check if both wrestlers belong to the same faction. */
  public boolean areFromSameFaction() {
    return wrestler1.getFaction() != null
        && wrestler2.getFaction() != null
        && wrestler1.getFaction().equals(wrestler2.getFaction());
  }

  /** Get the common faction if both wrestlers belong to the same one. */
  public Faction getCommonFaction() {
    if (areFromSameFaction()) {
      return wrestler1.getFaction();
    }
    return null;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}
