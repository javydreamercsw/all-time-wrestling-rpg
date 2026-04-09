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
package com.github.javydreamercsw.management.domain.relationship;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/** Represents a social relationship between two wrestlers. */
@Entity
@Table(
    name = "wrestler_relationship",
    indexes = {@Index(name = "idx_rel_wrestlers", columnList = "wrestler1_id, wrestler2_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class WrestlerRelationship extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "relationship_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler1_id", nullable = false)
  @JsonIgnoreProperties({"relationships", "rivalries", "injuries", "decks", "reigns"})
  private Wrestler wrestler1;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler2_id", nullable = false)
  @JsonIgnoreProperties({"relationships", "rivalries", "injuries", "decks", "reigns"})
  private Wrestler wrestler2;

  @Enumerated(EnumType.STRING)
  @Column(name = "relationship_type", nullable = false)
  private RelationshipType type;

  @Column(name = "level", nullable = false)
  @Min(0) @Max(100) @Builder.Default
  private Integer level = 50;

  @Column(name = "is_storyline", nullable = false)
  @Builder.Default
  private Boolean isStoryline = false;

  @Column(name = "started_date", nullable = false)
  private Instant startedDate;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "notes", length = 1000)
  private String notes;

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
    if (level == null) {
      level = 50;
    }
    if (isStoryline == null) {
      isStoryline = false;
    }
  }

  /** Get the other wrestler in the relationship. */
  public Wrestler getPartner(Wrestler wrestler) {
    if (wrestler.equals(wrestler1)) {
      return wrestler2;
    } else if (wrestler.equals(wrestler2)) {
      return wrestler1;
    }
    throw new IllegalArgumentException("Wrestler is not part of this relationship");
  }

  /** Check if a wrestler is involved in this relationship. */
  public boolean involvesWrestler(Wrestler wrestler) {
    return wrestler.equals(wrestler1) || wrestler.equals(wrestler2);
  }
}
