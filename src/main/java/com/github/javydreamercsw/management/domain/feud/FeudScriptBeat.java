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
package com.github.javydreamercsw.management.domain.feud;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservation;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/** A single scheduled event within a {@link FeudScript}: a match type, winner intent, and show. */
@Entity
@Table(name = "feud_script_beat")
@Getter
@Setter
public class FeudScriptBeat extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feud_script_beat_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "script_id", nullable = false)
  @JsonIgnoreProperties({"beats"})
  private FeudScript script;

  @Min(1) @Column(name = "beat_order", nullable = false)
  private int beatOrder;

  @Size(max = 128) @Column(name = "segment_type", nullable = false, length = 128)
  private String segmentType;

  @Size(max = 128) @Column(name = "segment_rule", length = 128)
  @Nullable private String segmentRule;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_show_id")
  @JsonIgnoreProperties({"segments", "reservations"})
  @Nullable private Show targetShow;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id")
  @Nullable private ShowSegmentReservation reservation;

  @Enumerated(EnumType.STRING)
  @Column(name = "winner_control", nullable = false, length = 16)
  private FeudScriptWinnerControl winnerControl = FeudScriptWinnerControl.AI_PICKS;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "planned_winner_id")
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns", "faction"})
  @Nullable private Wrestler plannedWinner;

  @Column(name = "is_culmination", nullable = false)
  private boolean culmination = false;

  @Size(max = DESCRIPTION_MAX_LENGTH) @Column(name = "notes")
  @Nullable private String notes;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actual_segment_id")
  @Nullable private Segment actualSegment;

  @Enumerated(EnumType.STRING)
  @Column(name = "beat_status", nullable = false, length = 16)
  private FeudScriptBeatStatus beatStatus = FeudScriptBeatStatus.PENDING;

  /**
   * CONTENDER_DESIGNATION outcome: when set, the winner of this beat's segment is designated as the
   * #1 contender for this title once the beat completes.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contender_title_id")
  @JsonIgnoreProperties({"titleReigns", "challengers"})
  @Nullable private Title contenderTitle;

  /**
   * External (non-feud) wrestlers involved in this beat only. EAGER because beats render in
   * detached grids and are loaded via a native query that cannot carry an {@code @EntityGraph};
   * cardinality is tiny (a few externals per beat).
   */
  @OneToMany(
      mappedBy = "beat",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private List<FeudScriptBeatParticipant> externalParticipants = new ArrayList<>();

  /** Adds (or updates the role of) an external participant; no-op when already present. */
  public void addExternalParticipant(Wrestler wrestler, FeudBeatParticipantRole role) {
    for (FeudScriptBeatParticipant existing : externalParticipants) {
      if (Objects.equals(existing.getWrestler().getId(), wrestler.getId())) {
        existing.setRole(role);
        return;
      }
    }
    FeudScriptBeatParticipant participant = new FeudScriptBeatParticipant();
    participant.setBeat(this);
    participant.setWrestler(wrestler);
    participant.setRole(role);
    externalParticipants.add(participant);
  }

  public List<Wrestler> getExternalOpponents() {
    return externalParticipants.stream()
        .filter(p -> p.getRole() == FeudBeatParticipantRole.OPPONENT)
        .map(FeudScriptBeatParticipant::getWrestler)
        .collect(Collectors.toList());
  }

  public List<Wrestler> getExternalExtras() {
    return externalParticipants.stream()
        .filter(p -> p.getRole() == FeudBeatParticipantRole.EXTRA)
        .map(FeudScriptBeatParticipant::getWrestler)
        .collect(Collectors.toList());
  }

  public Set<Long> getExternalParticipantIds() {
    return externalParticipants.stream()
        .map(p -> p.getWrestler().getId())
        .collect(Collectors.toSet());
  }

  public boolean hasExternals() {
    return !externalParticipants.isEmpty();
  }

  /** Formats this beat as a one-line AI instruction. */
  public String toAiInstruction() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(segmentType);
    if (segmentRule != null) {
      sb.append(" - ").append(segmentRule);
    }
    sb.append("]");
    if (culmination) {
      sb.append(" (Culmination/Blowoff)");
    }
    sb.append(" — Winner: ");
    switch (winnerControl) {
      case BOOKER_PICKS ->
          sb.append(plannedWinner != null ? plannedWinner.getName() : "Booker's choice");
      case AI_PICKS -> sb.append("AI choice");
      case SYSTEM_ROLL -> sb.append("System roll");
    }
    if (notes != null && !notes.isBlank()) {
      sb.append(" — \"").append(notes).append("\"");
    }
    return sb.toString();
  }
}
