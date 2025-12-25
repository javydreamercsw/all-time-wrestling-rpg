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
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a multi-wrestler feud in the ATW RPG system. Unlike regular rivalries (2 wrestlers) or
 * faction rivalries (2 factions), this represents complex feuds involving 3+ individual wrestlers.
 */
@Entity
@Table(name = "multi_wrestler_feud")
@Getter
@Setter
public class MultiWrestlerFeud extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "multi_wrestler_feud_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

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

  // Feud participants
  @OneToMany(mappedBy = "feud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"feud"})
  private List<FeudParticipant> participants = new ArrayList<>();

  // Heat events for this feud
  @OneToMany(mappedBy = "feud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"feud"})
  private List<FeudHeatEvent> heatEvents = new ArrayList<>();

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

  /** Add a wrestler to the feud. */
  public void addParticipant(Wrestler wrestler, FeudRole role) {
    if (wrestler != null && !hasParticipant(wrestler)) {
      FeudParticipant participant = new FeudParticipant();
      participant.setFeud(this);
      participant.setWrestler(wrestler);
      participant.setRole(role);
      participant.setJoinedDate(Instant.now());
      participants.add(participant);
    }
  }

  /** Remove a wrestler from the feud. */
  public void removeParticipant(Wrestler wrestler, String reason) {
    participants.stream()
        .filter(p -> p.getWrestler().equals(wrestler))
        .findFirst()
        .ifPresent(
            participant -> {
              participant.setLeftDate(Instant.now());
              participant.setLeftReason(reason);
              participant.setIsActive(false);
            });
  }

  /** Check if a wrestler is participating in this feud. */
  public boolean hasParticipant(Wrestler wrestler) {
    return participants.stream().anyMatch(p -> p.getWrestler().equals(wrestler) && p.getIsActive());
  }

  /** Get all active participants. */
  public List<FeudParticipant> getActiveParticipants() {
    return participants.stream().filter(FeudParticipant::getIsActive).toList();
  }

  /** Get all wrestlers currently in the feud. */
  public List<Wrestler> getActiveWrestlers() {
    return getActiveParticipants().stream().map(FeudParticipant::getWrestler).toList();
  }

  /** Get the number of active participants. */
  public int getActiveParticipantCount() {
    return getActiveParticipants().size();
  }

  /** Add heat to the feud. */
  public void addHeat(int heatGain, String reason) {
    this.heat += heatGain;

    // Create heat event for tracking
    FeudHeatEvent event = new FeudHeatEvent();
    event.setFeud(this);
    event.setHeatChange(heatGain);
    event.setReason(reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Check if participants must have matches at next show (10+ heat). */
  public boolean mustHaveMatchesNextShow() {
    return isActive && heat >= 10;
  }

  /** Check if feud can be resolved with a roll (20+ heat). */
  public boolean canAttemptResolution() {
    return isActive && heat >= 20;
  }

  /** Check if feud requires rule segment (30+ heat). */
  public boolean requiresStipulationMatch() {
    return isActive && heat >= 30;
  }

  /** End the feud. */
  public void endFeud(String reason) {
    this.isActive = false;
    this.endedDate = Instant.now();

    // Mark all participants as inactive
    for (FeudParticipant participant : participants) {
      if (participant.getIsActive()) {
        participant.setLeftDate(Instant.now());
        participant.setLeftReason("Feud ended: " + reason);
        participant.setIsActive(false);
      }
    }

    // Add final heat event
    FeudHeatEvent event = new FeudHeatEvent();
    event.setFeud(this);
    event.setHeatChange(0);
    event.setReason("Multi-wrestler feud ended: " + reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Get feud intensity level based on heat. */
  public RivalryIntensity getIntensity() {
    if (heat < 10) return RivalryIntensity.SIMMERING;
    if (heat < 20) return RivalryIntensity.HEATED;
    if (heat < 30) return RivalryIntensity.INTENSE;
    return RivalryIntensity.EXPLOSIVE;
  }

  /** Get display name with participant count. */
  public String getDisplayName() {
    if (!isActive) {
      return name + " (Ended)";
    }
    return name + " (" + getActiveParticipantCount() + " wrestlers)";
  }

  /** Get feud summary with heat and intensity. */
  public String getFeudSummary() {
    return String.format(
        "%s (%d heat - %s)", getDisplayName(), heat, getIntensity().getDisplayName());
  }

  /** Check if this is a valid multi-wrestler feud (3+ participants). */
  public boolean isValidMultiWrestlerFeud() {
    return getActiveParticipantCount() >= 3;
  }

  /** Get participants by role. */
  public List<FeudParticipant> getParticipantsByRole(FeudRole role) {
    return getActiveParticipants().stream().filter(p -> p.getRole() == role).toList();
  }

  /** Get main antagonists in the feud. */
  public List<Wrestler> getAntagonists() {
    return getParticipantsByRole(FeudRole.ANTAGONIST).stream()
        .map(FeudParticipant::getWrestler)
        .toList();
  }

  /** Get protagonists in the feud. */
  public List<Wrestler> getProtagonists() {
    return getParticipantsByRole(FeudRole.PROTAGONIST).stream()
        .map(FeudParticipant::getWrestler)
        .toList();
  }

  /** Get neutral participants in the feud. */
  public List<Wrestler> getNeutralParticipants() {
    return getParticipantsByRole(FeudRole.NEUTRAL).stream()
        .map(FeudParticipant::getWrestler)
        .toList();
  }
}
