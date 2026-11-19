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
package com.github.javydreamercsw.management.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Per-segment progress during a show adjudication (or narration-only retry) run. Published by
 * {@code ShowService} from inside the run's async thread; {@code AdjudicationProgressBroadcaster}
 * forwards these to attached UIs.
 *
 * <p>Carries only primitives and a precomputed label — never JPA entities — because the synchronous
 * event multicast happens inside the open adjudication transaction and must not touch lazy state.
 */
@Getter
public class ShowAdjudicationProgressEvent extends ApplicationEvent {
  public enum Phase {
    NARRATION,
    ADJUDICATION,
    FINALIZING,
    RETRY
  }

  private final Long showId;
  private final Phase phase;

  /** Segments finished in this phase so far. */
  private final int completed;

  /** Segments this phase will process (post player-exclusion filter). */
  private final int total;

  /**
   * Human-readable label of the segment just processed (or about to be), e.g. "Segment 4: A vs B".
   */
  private final String currentLabel;

  public ShowAdjudicationProgressEvent(
      final Object source,
      final Long showId,
      final Phase phase,
      final int completed,
      final int total,
      final String currentLabel) {
    super(source);
    this.showId = showId;
    this.phase = phase;
    this.completed = completed;
    this.total = total;
    this.currentLabel = currentLabel;
  }
}
