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
package com.github.javydreamercsw.base.image;

import java.util.Optional;

/** Strategy for resolving images from a specific source. */
public interface ImageSource {
  /**
   * Attempts to resolve an image for a given name and category.
   *
   * @param name The name of the entity (e.g. "Princess Aussie").
   * @param category The category of the image.
   * @return An Optional containing the resolved path if found.
   */
  Optional<String> resolveImage(String name, ImageCategory category);

  /**
   * Returns the priority of this source. Lower values are checked first.
   *
   * @return The priority.
   */
  int getPriority();
}
