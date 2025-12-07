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
package com.github.javydreamercsw.management.domain.season;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.Show;
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
 * Represents a wrestling season in the ATW RPG system. A season is composed of shows and
 * pay-per-views (PPVs) and tracks the overall progression of storylines, rivalries, and
 * championship reigns.
 *
 * <p>Game Flow: Every 4–6 Shows → 1 PPV
 */
@Entity
@Table(name = "season", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class Season extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "season_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "start_date", nullable = false)
  private Instant startDate;

  @Column(name = "end_date")
  private Instant endDate;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = false;

  @Column(name = "shows_per_ppv", nullable = false)
  @Min(4) private Integer showsPerPpv = 5; // Default: Every 5 shows → 1 PPV

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // Relationships
  @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Show> shows = new ArrayList<>();

  // ==================== ATW RPG METHODS ====================

  /** Check if this season is currently active. Only one season should be active at a time. */
  public boolean isCurrentSeason() {
    return isActive && endDate == null;
  }

  /** Get the total number of shows in this season. */
  public int getTotalShows() {
    return shows.size();
  }

  /** Get the number of PPVs that should have occurred based on show count. */
  public int getExpectedPpvCount() {
    return getTotalShows() / showsPerPpv;
  }

  /** Check if it's time for the next PPV based on show count. */
  public boolean isTimeForPpv() {
    int regularShows =
        (int)
            shows.stream()
                .filter(show -> !show.getType().getName().toLowerCase().contains("ppv"))
                .count();
    int ppvShows =
        (int)
            shows.stream()
                .filter(show -> show.getType().getName().toLowerCase().contains("ppv"))
                .count();

    return regularShows >= (ppvShows + 1) * showsPerPpv;
  }

  /** End the current season. */
  public void endSeason() {
    this.isActive = false;
    this.endDate = Instant.now();
  }

  /** Add a show to this season. */
  public void addShow(Show show) {
    shows.add(show);
    show.setSeason(this);
  }

  /** Remove a show from this season. */
  public void removeShow(Show show) {
    shows.remove(show);
    show.setSeason(null);
  }

  /** Get display name. */
  public String getDisplayName() {
    return name;
  }

  /** Get the duration of this season in days. */
  public long getDurationDays() {
    Instant end = endDate != null ? endDate : Instant.now();
    return java.time.Duration.between(startDate, end).toDays();
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (startDate == null) {
      startDate = Instant.now();
    }
  }
}
