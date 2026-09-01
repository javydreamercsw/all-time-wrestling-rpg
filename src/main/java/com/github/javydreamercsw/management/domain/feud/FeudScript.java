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
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/** A booker-authored story arc linking a sequence of beats to a rivalry or multi-wrestler feud. */
@Entity
@Table(name = "feud_script")
@Getter
@Setter
public class FeudScript extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feud_script_id")
  private Long id;

  @NotBlank @Size(max = DESCRIPTION_MAX_LENGTH) @Column(name = "name", nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rivalry_id")
  @JsonIgnoreProperties({"heatEvents", "segments"})
  @Nullable private Rivalry rivalry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feud_id")
  @JsonIgnoreProperties({"participants", "heatEvents"})
  @Nullable private MultiWrestlerFeud feud;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private FeudScriptStatus status = FeudScriptStatus.ACTIVE;

  @Min(1) @Max(3) @Column(name = "max_ple_appearances", nullable = false)
  private int maxPleAppearances = 3;

  @Column(name = "created_date", nullable = false)
  private Instant createdDate;

  @OneToMany(mappedBy = "script", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("beatOrder ASC")
  private List<FeudScriptBeat> beats = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    if (createdDate == null) {
      createdDate = Instant.now();
    }
  }

  /** Returns true if exactly one of rivalry or feud is linked. */
  public boolean hasValidLink() {
    return (rivalry != null) != (feud != null);
  }

  /** Counts how many beats target a PLE show. */
  public long countPleBeats() {
    return beats.stream()
        .filter(b -> b.getTargetShow() != null && b.getTargetShow().isPremiumLiveEvent())
        .count();
  }
}
