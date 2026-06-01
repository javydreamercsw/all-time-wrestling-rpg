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
package com.github.javydreamercsw.management.domain.outcome;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * One row in an OutcomeMatrix: a dice roll mapped to a narrative template and mechanical effects.
 */
@Entity
@Table(
    name = "outcome_matrix_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"outcome_matrix_id", "dice_roll"}))
@Getter
@Setter
public class OutcomeMatrixEntry extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "outcome_matrix_entry_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "outcome_matrix_id", nullable = false)
  private OutcomeMatrix matrix;

  /** d66 dice value (11–66, using pairs of d6 rolls). */
  @Column(name = "dice_roll", nullable = false)
  private int diceRoll;

  /** Raw outcome text with placeholder variables intact (e.g. FAVORED, UNDERDOG, ALLY). */
  @Lob
  @Column(name = "template_text", nullable = false)
  private String templateText;

  // Structured mechanical effect columns — all nullable; null means no effect

  @Column(name = "heat_delta")
  private Integer heatDelta;

  @Column(name = "fan_delta")
  private Long fanDelta;

  /** Letter-grade steps for TV Grade (positive = up, negative = down). */
  @Column(name = "tv_grade_delta")
  private Integer tvGradeDelta;

  @Column(name = "grudge_grade_delta")
  private Integer grudgeGradeDelta;

  @Column(name = "injury_caused", nullable = false)
  private boolean injuryCaused = false;

  /** When set, resolving this entry redirects to the referenced matrix instead. */
  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "redirect_matrix_id")
  private OutcomeMatrix redirectToMatrix;

  public boolean hasEffects() {
    return heatDelta != null
        || fanDelta != null
        || tvGradeDelta != null
        || grudgeGradeDelta != null
        || injuryCaused;
  }

  public boolean isRedirect() {
    return redirectToMatrix != null;
  }
}
