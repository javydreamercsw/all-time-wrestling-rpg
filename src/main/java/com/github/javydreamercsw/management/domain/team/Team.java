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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A team in the ATW RPG system — an ordered list of wrestlers (DUO or TRIO) that can be linked to a
 * faction. Teams are specifically for tag team / trios matches and can be linked to a faction.
 */
@Entity
@Table(name = "team", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "universe_id")
  private Universe universe;

  /** The ordered member list; slot 0 is the legacy "wrestler1", slot 1 the legacy "wrestler2". */
  @OneToMany(
      mappedBy = "team",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("slot ASC")
  @Builder.Default
  private List<TeamMember> members = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private Npc manager;

  // Optional faction association
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_id")
  private Faction faction;

  @Column(name = "theme_song")
  private String themeSong;

  @Column(name = "artist")
  private String artist;

  @Column(name = "team_finisher")
  private String teamFinisher;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TeamStatus status = TeamStatus.ACTIVE;

  /**
   * Declared team size; inferred from the member list when not set explicitly (null in memory until
   * inference or persist).
   */
  @Column(name = "team_size", nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  private TeamSize teamSize;

  /** Where this team came from (registry seed vs one-off assembly). */
  @Column(name = "origin", nullable = false, length = 16)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TeamOrigin origin = TeamOrigin.REGISTRY;

  /** The team's own fan counter; backfilled from member fans once, then accrues at team level. */
  @Column(name = "fans", nullable = false)
  @Builder.Default
  private long fans = 0L;

  @Column(name = "formed_date", nullable = false)
  private Instant formedDate;

  @Column(name = "disbanded_date")
  private Instant disbandedDate;

  /** Ensure default values before persisting. */
  @PrePersist
  @PreUpdate
  private void ensureDefaults() {
    if (formedDate == null) {
      formedDate = Instant.now();
    }
    if (status == null) {
      status = TeamStatus.ACTIVE;
    }
    if (origin == null) {
      origin = TeamOrigin.REGISTRY;
    }
    if (teamSize == null) {
      inferTeamSizeFromMembers();
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

  /**
   * Replaces the member list with the given wrestlers in order (slot 0..n-1), rebuilding the member
   * rows. The member list is the source of truth for size, so {@link TeamSize} is re-inferred from
   * the list length on every call; a caller wanting a different declared size sets it afterwards
   * via {@code setTeamSize}.
   *
   * @param wrestlers the ordered team members
   */
  public void setMembers(final List<Wrestler> wrestlers) {
    this.members.clear();
    if (wrestlers != null) {
      for (int slot = 0; slot < wrestlers.size(); slot++) {
        Wrestler wrestler = wrestlers.get(slot);
        TeamMember member = TeamMember.builder().team(this).wrestler(wrestler).slot(slot).build();
        this.members.add(member);
      }
      inferTeamSizeFromMembers();
    }
  }

  private void inferTeamSizeFromMembers() {
    if (members != null && !members.isEmpty()) {
      for (TeamSize size : TeamSize.values()) {
        if (size.getMemberCount() == members.size()) {
          teamSize = size;
          return;
        }
      }
      // No declared size for this count — leave teamSize unchanged (defaults at persist).
    }
  }

  /** Check if a wrestler is a member of this team. */
  public boolean hasMember(final Wrestler wrestler) {
    return getOrderedMembers().stream().anyMatch(m -> m.getWrestler().equals(wrestler));
  }

  /**
   * Get the team partner of a given wrestler (duo semantics).
   *
   * @throws IllegalArgumentException when the wrestler is not a member
   */
  public Wrestler getPartner(final Wrestler wrestler) {
    if (wrestler.equals(getWrestler1())) {
      return getWrestler2();
    } else if (wrestler.equals(getWrestler2())) {
      return getWrestler1();
    }
    throw new IllegalArgumentException("Wrestler is not a member of this team");
  }

  /**
   * Legacy derived accessors — the wrestler at slot 0. Kept for the many duo call sites; new code
   * should read the member list.
   *
   * @return the member at slot 0, or null when the team has no members
   */
  @Deprecated
  public Wrestler getWrestler1() {
    return memberAt(0);
  }

  /**
   * Legacy derived accessors — the wrestler at slot 1. Kept for the many duo call sites; new code
   * should read the member list.
   *
   * @return the member at slot 1, or null when the team has fewer than two members
   */
  @Deprecated
  public Wrestler getWrestler2() {
    return memberAt(1);
  }

  private Wrestler memberAt(final int slot) {
    List<TeamMember> ordered = getOrderedMembers();
    return slot < ordered.size() ? ordered.get(slot).getWrestler() : null;
  }

  private List<TeamMember> getOrderedMembers() {
    if (members == null) {
      return List.of();
    }
    return members.stream().sorted(Comparator.comparingInt(TeamMember::getSlot)).toList();
  }

  /** Get both wrestlers as a formatted string. */
  public String getMemberNames() {
    String name1 = getWrestler1() != null ? getWrestler1().getName() : "null";
    String name2 = getWrestler2() != null ? getWrestler2().getName() : "null";
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

  /** Check if both wrestlers belong to the same faction in this team's universe. */
  public boolean areFromSameFaction() {
    if (universe == null) {
      return false;
    }
    Long universeId = universe.getId();

    Faction f1 = getWrestler1().getState(universeId).map(WrestlerState::getFaction).orElse(null);
    Faction f2 = getWrestler2().getState(universeId).map(WrestlerState::getFaction).orElse(null);

    return f1 != null && f2 != null && f1.equals(f2);
  }

  /** Get the common faction if both wrestlers belong to the same one in this team's universe. */
  public Faction getCommonFaction() {
    if (universe == null) {
      return null;
    }
    Long universeId = universe.getId();

    Faction f1 = getWrestler1().getState(universeId).map(WrestlerState::getFaction).orElse(null);
    Faction f2 = getWrestler2().getState(universeId).map(WrestlerState::getFaction).orElse(null);

    if (f1 != null && f1.equals(f2)) {
      return f1;
    }
    return null;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}
