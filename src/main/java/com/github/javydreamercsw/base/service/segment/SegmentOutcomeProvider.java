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
package com.github.javydreamercsw.base.service.segment;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;

/**
 * Interface for providing segment outcome calculations. This interface breaks the circular
 * dependency between base and management packages.
 */
public interface SegmentOutcomeProvider {

  /**
   * Determines the segment outcome if none is provided in the context.
   *
   * @param context The segment narration context
   * @return The context with determined outcome
   */
  SegmentNarrationContext determineOutcomeIfNeeded(SegmentNarrationContext context);
}
