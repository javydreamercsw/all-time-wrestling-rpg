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
package com.github.javydreamercsw.management.sync.review;

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;

/**
 * SPI for applying an administrator's seed-update decision to one seed domain's entities
 * (seed-update review feature). The apply service ({@code SeedUpdateDecisionService}) looks
 * implementations up by {@link #domainKey()} and dispatches each decided row to exactly one
 * implementation — register one Spring bean per domain.
 *
 * <p>All {@code apply*} methods run inside the apply service's transaction: implementations write
 * entity data (and, per the keep-mine stamp contract, stamp the decision row's detection-time
 * {@code new_fingerprint} on the entity when keeping the database's value) and must not commit,
 * open their own transaction boundaries, or lower the review gate — the apply service owns the
 * atomicity and the gate refresh after ALL rows are applied.
 */
public interface SeedDomainApplier {

  /**
   * @return the seed domain this applier handles (e.g. {@code "wrestler"}), matching {@link
   *     SeedUpdateDecision#getDomainKey()}.
   */
  String domainKey();

  /** Applies a {@code KEEP_MINE} decision: keep the database's current value. */
  void applyKeep(SeedUpdateDecision decision);

  /**
   * Applies a {@code USE_NEW} decision: the applier writes the new seed value AND stamps the row's
   * seed fingerprint with the decision's {@code new_fingerprint} (same stamp obligation as {@link
   * #applyKeep}).
   */
  void applyNew(SeedUpdateDecision decision);

  /** Applies a {@code KEEP_AS_CUSTOM} decision: keep the value and mark it no longer seed-owned. */
  void applyKeepAsCustom(SeedUpdateDecision decision);

  /** Applies a {@code REMOVE} decision: delete the database row (dependents already checked). */
  void applyRemove(SeedUpdateDecision decision);

  /**
   * @return true when the entity cannot be removed because other data references it — the apply
   *     service refuses a {@code REMOVE} decision with this answer and leaves the row PENDING.
   */
  boolean hasDependents(String entityKey);
}
