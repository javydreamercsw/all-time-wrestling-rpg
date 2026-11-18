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
package com.github.javydreamercsw.management.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Fired once per boot after the seed-update review detection/parking phase completes with undecided
 * {@code SeedUpdateDecision} rows (seed-update review feature). The inbox listener turns this into
 * a single administrator notification — batched, never per parked row.
 */
@Getter
public class SeedUpdateReviewPendingEvent extends ApplicationEvent {

  private final int pendingCount;
  private final String appVersion;

  public SeedUpdateReviewPendingEvent(
      final Object source, final int pendingCount, final String appVersion) {
    super(source);
    this.pendingCount = pendingCount;
    this.appVersion = appVersion;
  }
}
