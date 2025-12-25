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
package com.github.javydreamercsw.management.domain.drama;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jspecify.annotations.Nullable;

/**
 * Represents a drama event in the ATW RPG system. Drama events are random occurrences that can
 * affect storylines, rivalries, wrestler development, and create dynamic narrative moments.
 *
 * <p>Examples: Backstage altercations, social media feuds, contract disputes, surprise returns,
 * betrayals, injuries, fan incidents, etc.
 */
@Entity
@Table(name = "drama_event")
@Getter
@Setter
public class DramaEvent extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "drama_event_id")
  private Long id;

  @Column(name = "title", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String title;

  @Lob
  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "event_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private DramaEventType eventType;

  @Column(name = "severity", nullable = false)
  @Enumerated(EnumType.STRING)
  private DramaEventSeverity severity;

  // Primary wrestler involved (always required)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "primary_wrestler_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Wrestler primaryWrestler;

  // Secondary wrestler involved (optional - for multi-wrestler events)
  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "secondary_wrestler_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Wrestler secondaryWrestler;

  @Column(name = "event_date", nullable = false)
  private Instant eventDate;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // Impact tracking
  @Column(name = "heat_impact")
  private Integer heatImpact; // Heat added/removed from rivalry

  @Column(name = "fan_impact")
  private Long fanImpact; // Fans gained/lost

  @Column(name = "injury_caused", nullable = false)
  private Boolean injuryCaused = false;

  @Column(name = "rivalry_created", nullable = false)
  private Boolean rivalryCreated = false;

  @Column(name = "rivalry_ended", nullable = false)
  private Boolean rivalryEnded = false;

  // Processing status
  @Column(name = "is_processed", nullable = false)
  private Boolean isProcessed = false;

  @Column(name = "processed_date")
  private Instant processedDate;

  @Lob
  @Column(name = "processing_notes")
  private String processingNotes;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Check if this event involves multiple wrestlers. */
  public boolean isMultiWrestlerEvent() {
    return secondaryWrestler != null;
  }

  /** Check if this event has positive impact (gains fans, creates excitement). */
  public boolean hasPositiveImpact() {
    return (fanImpact != null && fanImpact > 0) || severity == DramaEventSeverity.POSITIVE;
  }

  /** Check if this event has negative impact (loses fans, creates problems). */
  public boolean hasNegativeImpact() {
    return (fanImpact != null && fanImpact < 0)
        || severity == DramaEventSeverity.NEGATIVE
        || injuryCaused;
  }

  /** Check if this event affects rivalries. */
  public boolean affectsRivalries() {
    return heatImpact != null || rivalryCreated || rivalryEnded;
  }

  /** Get display string for the event impact. */
  public String getImpactSummary() {
    StringBuilder summary = new StringBuilder();

    if (fanImpact != null && fanImpact != 0) {
      summary.append(fanImpact > 0 ? "+" : "").append(fanImpact).append(" fans");
    }

    if (heatImpact != null && heatImpact != 0) {
      if (!summary.isEmpty()) summary.append(", ");
      summary.append(heatImpact > 0 ? "+" : "").append(heatImpact).append(" heat");
    }

    if (injuryCaused) {
      if (!summary.isEmpty()) summary.append(", ");
      summary.append("injury caused");
    }

    if (rivalryCreated) {
      if (!summary.isEmpty()) summary.append(", ");
      summary.append("rivalry created");
    }

    if (rivalryEnded) {
      if (!summary.isEmpty()) summary.append(", ");
      summary.append("rivalry ended");
    }

    return !summary.isEmpty() ? summary.toString() : "no impact";
  }

  /** Get emoji representation of the event severity. */
  public String getSeverityEmoji() {
    return switch (severity) {
      case POSITIVE -> "✨";
      case NEUTRAL -> "📰";
      case NEGATIVE -> "⚠️";
      case MAJOR -> "🚨";
    };
  }

  /** Get emoji representation of the event type. */
  public String getEventTypeEmoji() {
    return switch (eventType) {
      case BACKSTAGE_INCIDENT -> "🎭";
      case SOCIAL_MEDIA_DRAMA -> "📱";
      case INJURY_INCIDENT -> "🏥";
      case FAN_INTERACTION -> "👥";
      case CONTRACT_DISPUTE -> "📋";
      case BETRAYAL -> "🗡️";
      case ALLIANCE_FORMED -> "🤝";
      case SURPRISE_RETURN -> "🎉";
      case RETIREMENT_TEASE -> "👋";
      case CHAMPIONSHIP_CHALLENGE -> "🏆";
      case PERSONAL_ISSUE -> "💔";
      case MEDIA_CONTROVERSY -> "📺";
    };
  }

  /** Mark the event as processed. */
  public void markAsProcessed(String notes) {
    this.isProcessed = true;
    this.processedDate = Instant.now();
    this.processingNotes = notes;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (eventDate == null) {
      eventDate = Instant.now();
    }
    if (isProcessed == null) {
      isProcessed = false;
    }
    if (injuryCaused == null) {
      injuryCaused = false;
    }
    if (rivalryCreated == null) {
      rivalryCreated = false;
    }
    if (rivalryEnded == null) {
      rivalryEnded = false;
    }
  }
}
