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
package com.github.javydreamercsw.management.service.tutorial;

import com.github.javydreamercsw.management.domain.universe.Universe;
import java.util.List;

/** Provides the ordered list of {@link TutorialStep}s for a specific universe mode. */
public interface TutorialDefinition {

  Universe.UniverseType getMode();

  List<TutorialStep> getSteps();

  /** Route to navigate to when the tutorial is completed. Defaults to the tutorial home page. */
  default String getCompletionRoute() {
    return "tutorial";
  }

  /**
   * Returns true when this tutorial requires admin-level access and is not recommended for new
   * players. The mode-selection card will display an "Advanced" badge and the warning text.
   */
  default boolean isAdvanced() {
    return false;
  }

  /**
   * Warning text shown on the mode-selection card for advanced tutorials. Only displayed when
   * {@link #isAdvanced()} returns true.
   */
  default String getWarning() {
    return null;
  }
}
