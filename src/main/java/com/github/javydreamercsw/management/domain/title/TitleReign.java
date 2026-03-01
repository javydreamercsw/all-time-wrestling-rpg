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
package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single title reign in the ATW RPG system. Tracks when a wrestler held a
 * championship, including start and end dates.
 */
@Entity
@Table(name = "title_reign")
@Getter
@Setter
public class TitleReign extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "title_reign_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "title_id", nullable = false)
  private Title title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "won_at_segment_id")
  private Segment wonAtSegment;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "title_reign_champion",
      joinColumns = @JoinColumn(name = "title_reign_id"),
      inverseJoinColumns = @JoinColumn(name = "wrestler_id"))
  private List<Wrestler> champions = new ArrayList<>();

  @Column(name = "start_date", nullable = false)
  private Instant startDate;

  @Column(name = "end_date")
  private Instant endDate;

  @Column(name = "reign_number", nullable = false)
  private Integer reignNumber = 1;

  @Lob
  @Column(name = "notes")
  private String notes;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // ==================== ATW RPG METHODS ====================

  /** Check if this is the current/active reign. */
  public boolean isCurrentReign() {
    return endDate == null;
  }

  /** End this title reign. */
  public void endReign(Instant endDate) {
    this.endDate = endDate;
  }

  /** Get the length of this reign in days. */
  public long getReignLengthDays(Instant now) {
    Instant end = endDate != null ? endDate : now;
    return java.time.Duration.between(startDate, end).toDays();
  }

  /** Get the length of this reign in a human-readable format. */
  public String getReignLengthDisplay(Instant now) {
    long days = getReignLengthDays(now);

    if (days == 0) {
      return "Less than 1 day";
    } else if (days == 1) {
      return "1 day";
    } else if (days < 7) {
      return days + " days";
    } else if (days < 30) {
      long weeks = days / 7;
      long remainingDays = days % 7;
      if (remainingDays == 0) {
        return weeks + (weeks == 1 ? " week" : " weeks");
      } else {
        return weeks
            + (weeks == 1 ? " week" : " weeks")
            + " and "
            + remainingDays
            + (remainingDays == 1 ? " day" : " days");
      }
    } else {
      long months = days / 30;
      long remainingDays = days % 30;
      if (remainingDays == 0) {
        return months + (months == 1 ? " month" : " months");
      } else {
        return months
            + (months == 1 ? " month" : " months")
            + " and "
            + remainingDays
            + (remainingDays == 1 ? " day" : " days");
      }
    }
  }

  /** Get display string for this reign. */
  public String getDisplayString() {
    String status = isCurrentReign() ? " (Current)" : "";
    return String.format(
        "%s - Reign #%d (%s)%s",
        getChampionNames(), reignNumber, getReignLengthDisplay(Instant.now()), status);
  }

  /**
   * Gets the champion names as a formatted string.
   *
   * @return Formatted champion names.
   */
  private String getChampionNames() {
    return champions.stream().map(Wrestler::getName).collect(Collectors.joining(" & "));
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
