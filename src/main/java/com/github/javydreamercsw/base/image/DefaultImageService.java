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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service that orchestrates multiple {@link ImageSource}s to resolve images for entities. */
@Service
@Slf4j
public class DefaultImageService {

  private final List<ImageSource> sources;

  @Autowired
  public DefaultImageService(List<ImageSource> sources) {
    this.sources =
        sources.stream().sorted(Comparator.comparingInt(ImageSource::getPriority)).toList();
    log.info("Initialized DefaultImageService with {} sources", sources.size());
  }

  /**
   * Resolves an image for a given name and category.
   *
   * @param name The name of the entity.
   * @param category The category of the image.
   * @return The resolution result (specific image or fallback).
   */
  public ImageResolution resolveImage(String name, ImageCategory category) {
    for (ImageSource source : sources) {
      Optional<String> path = source.resolveImage(name, category);
      if (path.isPresent()) {
        return new ImageResolution(path.get(), false);
      }
    }

    log.debug("No specific image found for {} in category {}, using fallback", name, category);
    return new ImageResolution("images/" + category.getDefaultFilename(), true);
  }
}
