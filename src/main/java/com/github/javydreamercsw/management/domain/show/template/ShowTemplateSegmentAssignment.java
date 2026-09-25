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
package com.github.javydreamercsw.management.domain.show.template;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * An assignment of a segment type and/or segment rule to a {@link ShowTemplate}, each row carrying
 * its own mode (ATW-0331). Exactly one of segmentType/segmentRule must be set (validated in {@code
 * isValid()}). Rows drive AI proposal behavior:
 *
 * <ul>
 *   <li>type-only — the AI may use that event-only type on this template's shows
 *   <li>type+rule+AUTO_ATTACH — the rule attaches whenever that type is used
 *   <li>rule-only+AUTO_ATTACH — the rule attaches to every approved match segment
 *   <li>rule-only+ENCOURAGED — the AI prefers the rule
 * </ul>
 */
@Entity
@Table(name = "show_template_segment_assignment")
@Getter
@Setter
public class ShowTemplateSegmentAssignment extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "assignment_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private ShowTemplate template;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "segment_type_id")
  @Nullable private SegmentType segmentType;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "segment_rule_id")
  @Nullable private SegmentRule segmentRule;

  /**
   * Optional tournament whose participants feed the auto-attached segment (ATW-oahn). With a type+
   * rule pairing this is "the tournament resolves that match's entrants"; tournament-only rows let
   * the tournament book its rounds onto this template's shows. Spec rows start without one — the
   * booking path creates the instance on first use and stores it here (ATW-etws).
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tournament_id")
  @Nullable private Tournament tournament;

  /**
   * Full tournament spec carried by the row (ATW-etws). A row may reference either an existing
   * {@link #tournament} or define a spec — never both. The booking path resolves the spec into one
   * persistent tournament on first use, then the row behaves like a tournament-linked row.
   */
  @Column(name = "spec_name")
  @Nullable private String specName;

  /** Tournament format id from {@code TournamentFormatRegistry} (e.g. SINGLE_ELIMINATION). */
  @Column(name = "spec_format_id", length = 64)
  @Nullable private String specFormatId;

  /**
   * Requested entrant count for the spec tournament. Null = fall back to the tournament's {@code
   * defaultEntrantCount}, then the format max.
   */
  @Column(name = "spec_entrant_count")
  @Nullable private Integer specEntrantCount;

  /**
   * Optional rule forced onto the bracket final regardless of round rules or the allowed pool. Null
   * = no override.
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "spec_final_rule_id")
  @Nullable private SegmentRule specFinalRule;

  /** Optional title awarded to the spec tournament's winner. Null = no title at stake. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "spec_title_id")
  @Nullable private Title specTitle;

  /**
   * Allowed-rules pool for bracket rounds ({@link SegmentRule} is name-keyed, hence the join
   * table). Lower precedence than round fixed rules and the row's {@link #segmentRule}; the pool
   * seeds the tournament's per-round random selection.
   */
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "show_template_assignment_rule",
      joinColumns = @JoinColumn(name = "assignment_id"),
      inverseJoinColumns = @JoinColumn(name = "segment_rule_id"))
  @OrderColumn(name = "position")
  private List<SegmentRule> specAllowedRules = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 20)
  private AssignmentMode mode = AssignmentMode.ENCOURAGED;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  /**
   * An assignment must target at least one of segmentType/segmentRule/tournament/tournament-spec.
   * Both type and rule set is the type+rule AUTO_ATTACH pairing (rule attaches whenever that type
   * is used); a tournament reference feeds its participants into the auto-attached match
   * (ATW-oahn); a tournament spec defines a tournament to create on first use (ATW-etws).
   */
  public boolean isValid() {
    return segmentType != null || segmentRule != null || tournament != null || hasTournamentSpec();
  }

  /**
   * True when this row defines a tournament spec (name + format) rather than referencing an
   * existing tournament. Spec rows must not carry a {@link #tournament} — the booking path creates
   * and stores it.
   */
  public boolean hasTournamentSpec() {
    return tournament == null && specName != null && !specName.isBlank() && specFormatId != null;
  }

  @PrePersist
  @PreUpdate
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }

  /** How an assigned type/rule influences proposal behavior. */
  public enum AssignmentMode {
    /** AI preference: surface to the prompt as preferred; nothing enforced. */
    ENCOURAGED,
    /** Deterministic: merged into approved segments at approval time. */
    AUTO_ATTACH
  }
}
