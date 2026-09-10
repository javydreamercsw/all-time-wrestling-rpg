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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * A participant row on a single {@link FeudScriptBeat}: an external wrestler (surprise opponent or
 * run-in extra) or — under a custom per-beat team layout — one of the arc's own wrestlers ({@link
 * FeudBeatParticipantRole#FEUD_MEMBER}) with an explicit team number.
 */
@Entity
@Table(
    name = "feud_script_beat_participant",
    uniqueConstraints = @UniqueConstraint(columnNames = {"beat_id", "wrestler_id"}))
@Getter
@Setter
public class FeudScriptBeatParticipant extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feud_script_beat_participant_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "beat_id", nullable = false)
  @JsonIgnoreProperties({"externalParticipants"})
  private FeudScriptBeat beat;

  /** EAGER: the External column and beat-edit dialog read wrestler names on detached grids. */
  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "wrestler_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns", "faction"})
  private Wrestler wrestler;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 16)
  private FeudBeatParticipantRole role;

  /**
   * 1-based team assignment when the beat uses a custom team layout; {@code null} on legacy
   * quick-path rows (feud wrestlers = team 1, externals = team 2, derived at read time).
   */
  @Column(name = "team_number")
  @Nullable private Integer teamNumber;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}
