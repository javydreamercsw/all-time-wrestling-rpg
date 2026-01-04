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

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

/**
 * Represents a faction (stable) of wrestlers in the ATW RPG system. Factions can have rivalries
 * with other factions and participate in multi-wrestler feuds.
 */
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "faction")
public class Faction extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "faction_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leader_id")
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns", "faction"})
  private Wrestler leader;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private Npc manager;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "formed_date", nullable = false)
  private Instant formedDate;

  @Column(name = "disbanded_date")
  private Instant disbandedDate;

  @Column(name = "creation_date", nullable = false)
  @Builder.Default
  private Instant creationDate = Instant.now();

  @Column(name = "external_id")
  private String externalId; // External system ID (e.g., Notion page ID)

  @Column(name = "last_sync")
  private Instant lastSync;

  // Faction members
  @OneToMany(
      mappedBy = "faction",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH},
      fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction", "rivalries", "injuries", "deck", "titleReigns"})
  @Builder.Default
  @ToString.Exclude
  private Set<Wrestler> members = new HashSet<>();

  // Teams associated with this faction
  @OneToMany(mappedBy = "faction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction", "wrestler1", "wrestler2"})
  @Builder.Default
  @ToString.Exclude
  private Set<Team> teams = new HashSet<>();

  // Faction rivalries where this faction is faction1
  @OneToMany(mappedBy = "faction1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction1", "faction2"})
  @Builder.Default
  @ToString.Exclude
  private Set<FactionRivalry> rivalriesAsFaction1 = new HashSet<>();

  // Faction rivalries where this faction is faction2
  @OneToMany(mappedBy = "faction2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"faction1", "faction2"})
  @Builder.Default
  @ToString.Exclude
  private Set<FactionRivalry> rivalriesAsFaction2 = new HashSet<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (formedDate == null) {
      formedDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Add a member to the faction. */
  public void addMember(Wrestler wrestler) {
    if (wrestler != null && !members.contains(wrestler)) {
      members.add(wrestler);
      wrestler.setFaction(this);
    }
  }

  /** Remove a member from the faction. */
  public void removeMember(Wrestler wrestler) {
    if (wrestler != null && members.contains(wrestler)) {
      members.remove(wrestler);
      wrestler.setFaction(null);
    }
  }

  /** Check if a wrestler is a member of this faction. */
  public boolean hasMember(Wrestler wrestler) {
    return members.contains(wrestler);
  }

  /** Get the number of active members. */
  public int getMemberCount() {
    return members.size();
  }

  /** Check if this faction is a singles faction (1 member). */
  public boolean isSinglesFaction() {
    return members.size() == 1;
  }

  /** Check if this faction is a tag team (2 members). */
  public boolean isTagTeam() {
    return members.size() == 2;
  }

  /** Check if this faction is a stable (3+ members). */
  public boolean isStable() {
    return members.size() >= 3;
  }

  /** Disband the faction. */
  public void disband(String reason) {
    this.isActive = false;
    this.disbandedDate = Instant.now();

    // Remove all members from faction
    for (Wrestler member : new ArrayList<>(members)) {
      removeMember(member);
    }
  }

  /** Get all active faction rivalries involving this faction. */
  public List<FactionRivalry> getActiveRivalries() {
    return Stream.concat(rivalriesAsFaction1.stream(), rivalriesAsFaction2.stream())
        .filter(FactionRivalry::getIsActive)
        .collect(Collectors.toList());
  }

  /** Get the opposing faction in a rivalry. */
  public Faction getOpposingFaction(FactionRivalry rivalry) {
    if (rivalry.getFaction1().equals(this)) {
      return rivalry.getFaction2();
    } else if (rivalry.getFaction2().equals(this)) {
      return rivalry.getFaction1();
    }
    throw new IllegalArgumentException("Faction is not part of this rivalry");
  }

  /** Check if this faction has a rivalry with another faction. */
  public boolean hasRivalryWith(Faction otherFaction) {
    return getActiveRivalries().stream()
        .anyMatch(
            rivalry ->
                rivalry.getFaction1().equals(otherFaction)
                    || rivalry.getFaction2().equals(otherFaction));
  }

  /** Get display name with member count. */
  public String getDisplayName() {
    if (!isActive) {
      return name + " (Disbanded)";
    }
    return name + " (" + members.size() + " members)";
  }

  /** Get faction type based on member count. */
  public String getFactionType() {
    if (members.size() == 1) return "Singles";
    if (members.size() == 2) return "Tag Team";
    return "Stable";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Faction faction = (Faction) o;
    return id != null && Objects.equals(id, faction.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
