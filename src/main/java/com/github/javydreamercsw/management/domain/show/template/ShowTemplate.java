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
package com.github.javydreamercsw.management.domain.show.template;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Entity representing a show template in the ATW RPG system. Templates define the structure and
 * characteristics of different types of wrestling shows.
 */
@Entity
@Table(name = "show_template", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class ShowTemplate extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "template_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "description")
  @Size(max = DESCRIPTION_MAX_LENGTH) private String description;

  @ManyToOne(optional = false)
  @JoinColumn(name = "show_type_id", nullable = false)
  private ShowType showType;

  @Column(name = "image_url")
  @Size(max = 512) private String imageUrl;

  @Column(name = "expected_matches")
  private Integer expectedMatches;

  @Column(name = "expected_promos")
  private Integer expectedPromos;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender_constraint")
  private Gender genderConstraint;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "commentary_team_id")
  private CommentaryTeam commentaryTeam;

  @Column(name = "duration_days")
  private Integer durationDays = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "recurrence_type")
  private RecurrenceType recurrenceType = RecurrenceType.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "recurrence_day_of_week")
  private DayOfWeek dayOfWeek;

  @Column(name = "recurrence_day_of_month")
  private Integer dayOfMonth;

  @Column(name = "recurrence_week_of_month")
  private Integer weekOfMonth;

  @Enumerated(EnumType.STRING)
  @Column(name = "recurrence_month")
  private Month month;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  /**
   * Segment type/rule assignments for this template (ATW-0331): event-only types the AI may use on
   * this template's shows, encouraged rules (AI preference) and auto-attach rules (deterministic
   * merge at proposal approval). Edited in the template admin view.
   */
  @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ShowTemplateSegmentAssignment> segmentAssignments = new ArrayList<>();

  /** Assigned event-only segment types offered to the AI for this template's shows. */
  public List<ShowTemplateSegmentAssignment> getAssignedEventTypes() {
    return segmentAssignments.stream()
        .filter(a -> a.getSegmentType() != null && a.isValid())
        .toList();
  }

  /** Rule-only assignments with AUTO_ATTACH mode: attach to every approved match segment. */
  public List<ShowTemplateSegmentAssignment> getRuleAutoAttachAssignments() {
    return segmentAssignments.stream()
        .filter(
            a ->
                a.getSegmentRule() != null
                    && a.getSegmentType() == null
                    && a.getMode() == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH)
        .toList();
  }

  /** Type-paired AUTO_ATTACH assignments: attach the rule whenever its paired type is used. */
  public List<ShowTemplateSegmentAssignment> getTypePairedAutoAttachAssignments() {
    return segmentAssignments.stream()
        .filter(
            a ->
                a.getSegmentType() != null
                    && a.getSegmentRule() != null
                    && a.getMode() == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH)
        .toList();
  }

  /** Rule-only assignments with ENCOURAGED mode: AI preference only. */
  public List<ShowTemplateSegmentAssignment> getEncouragedRuleAssignments() {
    return segmentAssignments.stream()
        .filter(
            a ->
                a.getSegmentRule() != null
                    && a.getSegmentType() == null
                    && a.getMode() == ShowTemplateSegmentAssignment.AssignmentMode.ENCOURAGED)
        .toList();
  }

  // ── Tournament assignments (ATW-oahn) ─────────────────────────────────────

  /** Assignments referencing a tournament or defining one via spec (with or without a pairing). */
  public List<ShowTemplateSegmentAssignment> getTournamentAssignments() {
    return segmentAssignments.stream()
        .filter(a -> (a.getTournament() != null || a.hasTournamentSpec()) && a.isValid())
        .toList();
  }

  /**
   * The AUTO_ATTACH pairing of a segment type with a tournament (ATW-oahn): when the given type's
   * match is created on this template's show, the returned tournament's participants fill it. Spec
   * rows qualify too — {@code TournamentTemplateBookingService} resolves their instance before this
   * lookup matters (ATW-etws).
   */
  public Optional<ShowTemplateSegmentAssignment> findTournamentForSegmentType(
      final SegmentType segmentType) {
    return segmentAssignments.stream()
        .filter(
            a ->
                (a.getTournament() != null || a.hasTournamentSpec())
                    && a.getSegmentType() != null
                    && a.getSegmentType().getId().equals(segmentType.getId())
                    && a.getMode() == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH
                    && a.isValid())
        .findFirst();
  }

  /**
   * Check if this is a Premium Live Event (PLE) template.
   *
   * @return true if this template is for a PLE
   */
  public boolean isPremiumLiveEvent() {
    return showType != null && showType.getCategory() == ShowCategory.PLE;
  }

  /**
   * Check if this is a Weekly show template.
   *
   * @return true if this template is for a weekly show
   */
  public boolean isWeeklyShow() {
    return showType != null && showType.getCategory() == ShowCategory.WEEKLY;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}
