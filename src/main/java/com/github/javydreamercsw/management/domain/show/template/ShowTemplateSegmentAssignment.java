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
import java.time.Instant;
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

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 20)
  private AssignmentMode mode = AssignmentMode.ENCOURAGED;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  /** An assignment must target exactly one of segmentType/segmentRule. */
  public boolean isValid() {
    return (segmentType == null) != (segmentRule == null);
  }

  @jakarta.persistence.PrePersist
  @jakarta.persistence.PreUpdate
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
