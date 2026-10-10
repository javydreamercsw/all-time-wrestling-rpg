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
package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
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
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * A held, cashable title shot — the Money in the Bank-style briefcase (ATW-8p72). Awarded to the
 * winner of a briefcase-deciding tournament (e.g. "Time Vault"); the holder may cash it in exactly
 * once, at any time, for a title match against the reigning champion(s) of any active championship.
 * Deliberately NOT a {@link Title}: no reigns, defenses, contenders or rankings.
 */
@Entity
@Table(name = "title_opportunity")
@Getter
@Setter
public class TitleOpportunity extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "id")
  private Long id;

  /** Display name, e.g. "Time Vault briefcase". */
  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private TitleOpportunityStatus status = TitleOpportunityStatus.HELD;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "universe_id")
  @Nullable private Universe universe;

  /** Kayfabe date the opportunity was earned (the tournament's endDate). */
  @Column(name = "earned_at", nullable = false)
  private LocalDate earnedAt;

  /** Division the case belongs to, copied from the granting tournament. Null = all genders. */
  @Column(name = "gender")
  @Enumerated(EnumType.STRING)
  @Nullable private Gender gender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "earned_from_tournament_id")
  @Nullable private Tournament earnedFromTournament;

  /** Session-safe tournament id for detached reads (the lazy proxy would throw). */
  @Nullable public Long getEarnedFromTournamentId() {
    return earnedFromTournament != null ? earnedFromTournament.getId() : null;
  }

  /** Kayfabe date after which the opportunity can no longer be cashed in; null = no expiry. */
  @Column(name = "expiry_date")
  @Nullable private LocalDate expiryDate;

  /** Kayfabe date the cash-in match was booked. */
  @Column(name = "cashed_at")
  @Nullable private LocalDate cashedAt;

  /** The cash-in match segment. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cashed_at_segment_id")
  @Nullable private Segment cashedAtSegment;

  /** The championship the holder chose to challenge. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cashed_against_title_id")
  @Nullable private Title cashedAgainstTitle;

  /** Optional artwork for the briefcase badge (ATW-jpki); mirrors Title.imageUrl. */
  @Column(name = "image_url", length = 512)
  @Nullable private String imageUrl;

  public boolean isHeld() {
    return status == TitleOpportunityStatus.HELD;
  }

  public boolean isExpired(@NonNull LocalDate gameDate) {
    return expiryDate != null && gameDate.isAfter(expiryDate);
  }

  /**
   * Spends the case (WWE rule: consumed at booking — a failed cash-in still spends it). Only {@link
   * TitleOpportunityStatus#HELD} opportunities can be cashed in.
   */
  public void markCashedIn(
      @NonNull Title title, @NonNull Segment segment, @NonNull LocalDate gameDate) {
    if (!isHeld()) {
      throw new IllegalStateException(
          "Only a HELD opportunity can be cashed in — this one is " + status);
    }
    this.status = TitleOpportunityStatus.CASHED_IN;
    this.cashedAgainstTitle = title;
    this.cashedAtSegment = segment;
    this.cashedAt = gameDate;
  }

  public void markExpired() {
    this.status = TitleOpportunityStatus.EXPIRED;
  }
}
