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
package com.github.javydreamercsw.management.domain.world;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "arena")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Arena extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "arena_id")
  private Long id;

  @Column(name = "name", nullable = false, unique = true)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "location_id", nullable = false)
  private Location location;

  @Column(name = "capacity", nullable = false)
  @Min(0) private Integer capacity;

  @Column(name = "alignment_bias", nullable = false)
  @Enumerated(EnumType.STRING)
  private AlignmentBias alignmentBias;

  @Column(name = "image_url")
  private String imageUrl;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "arena_environmental_trait",
      joinColumns = @JoinColumn(name = "arena_id"))
  @Column(name = "environmental_trait")
  @Builder.Default
  private Set<String> environmentalTraits = new HashSet<>();

  @Override
  public @Nullable Long getId() {
    return id;
  }

  public enum AlignmentBias {
    FACE_FAVORABLE("Face Favorable"),
    HEEL_FAVORABLE("Heel Favorable"),
    ANARCHIC("Anarchic"),
    NEUTRAL("Neutral");

    private final String displayName;

    AlignmentBias(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
