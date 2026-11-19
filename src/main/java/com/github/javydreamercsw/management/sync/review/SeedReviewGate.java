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
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import org.springframework.stereotype.Component;

/**
 * Read-only state bean for the seed-update review gate: gameplay is frozen while pending seed
 * decisions exist. Holds only the pending count — it intercepts nothing itself; the Vaadin
 * navigation observer and the REST guard are separate consumers (later PR).
 *
 * <p>{@link #isUp()} and {@link #pendingCount()} read the last {@link #refresh()} snapshot so a
 * navigation pass sees a consistent view; {@link #refresh()} re-reads the count from the decision
 * repository and is called after parking (raising the gate) and after applying decisions (lowering
 * it).
 */
@Component
public class SeedReviewGate {

  private final SeedUpdateDecisionRepository decisionRepository;

  /** Cached pending-decision count; call {@link #refresh()} to re-read from the repository. */
  private volatile long pendingCount;

  public SeedReviewGate(SeedUpdateDecisionRepository decisionRepository) {
    this.decisionRepository = decisionRepository;
    // Boot initialization: the snapshot must be correct from the first request after ANY boot —
    // including a restart with undecided decisions left over from a previous boot. Nothing else
    // is guaranteed to call refresh() before the interceptors read the gate (park* only fires
    // when detection actually parks something), so the bean seeds its own snapshot on creation.
    refresh();
  }

  /**
   * @return true while undecided seed decisions exist — gameplay must stay frozen.
   */
  public boolean isUp() {
    return pendingCount > 0;
  }

  /**
   * @return the number of undecided seed decisions at the last {@link #refresh()}
   */
  public long pendingCount() {
    return pendingCount;
  }

  /** Re-reads the pending-decision count from the decision repository. */
  public void refresh() {
    this.pendingCount = decisionRepository.countByStatus(SeedUpdateDecision.STATUS_PENDING);
  }
}
