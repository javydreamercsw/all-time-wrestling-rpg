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
package com.github.javydreamercsw.management.domain.feud;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a wrestler's participation in a multi-wrestler feud. Tracks their role, when they
 * joined/left, and their status in the feud.
 */
@Entity
@Table(name = "feud_participant")
@Getter
@Setter
public class FeudParticipant extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feud_participant_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "feud_id", nullable = false)
  @JsonIgnoreProperties({"participants", "heatEvents"})
  private MultiWrestlerFeud feud;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns", "faction"})
  private Wrestler wrestler;

  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private FeudRole role = FeudRole.NEUTRAL;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "joined_date", nullable = false)
  private Instant joinedDate;

  @Column(name = "left_date")
  private Instant leftDate;

  @Column(name = "left_reason")
  @Size(max = DESCRIPTION_MAX_LENGTH) private String leftReason;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (joinedDate == null) {
      joinedDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Leave the feud with a reason. */
  public void leaveFeud(String reason) {
    this.isActive = false;
    this.leftDate = Instant.now();
    this.leftReason = reason;
  }

  /** Change the participant's role in the feud. */
  public void changeRole(FeudRole newRole, String reason) {
    this.role = newRole;
    // Could add a role change event here if needed
  }

  /** Get how long the wrestler has been in the feud (in days). */
  public long getDaysInFeud() {
    Instant endDate = leftDate != null ? leftDate : Instant.now();
    return java.time.Duration.between(joinedDate, endDate).toDays();
  }

  /** Check if this participant is currently active in the feud. */
  public boolean isCurrentlyActive() {
    return isActive && leftDate == null;
  }

  /** Get display string for this participation. */
  public String getDisplayString() {
    String status = isActive ? "Active" : "Left";
    return String.format("%s (%s - %s)", wrestler.getName(), role.getDisplayName(), status);
  }

  /** Get participation summary. */
  public String getParticipationSummary() {
    if (isActive) {
      return String.format(
          "%s has been %s in %s for %d days",
          wrestler.getName(), role.getDisplayName().toLowerCase(), feud.getName(), getDaysInFeud());
    } else {
      return String.format(
          "%s was %s in %s for %d days (Left: %s)",
          wrestler.getName(),
          role.getDisplayName().toLowerCase(),
          feud.getName(),
          getDaysInFeud(),
          leftReason != null ? leftReason : "Unknown reason");
    }
  }
}
