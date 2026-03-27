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

import java.net.URL;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Image source that resolves images from the application's classpath. */
@Component
@Slf4j
public class ClasspathImageSource implements ImageSource {

  private static final String IMAGES_BASE = "images/";

  @Override
  public Optional<String> resolveImage(String name, ImageCategory category) {
    String subDir = category.name().toLowerCase() + "s/";
    String filename = name + ".png";
    String path = IMAGES_BASE + subDir + filename;

    URL resource = getClass().getClassLoader().getResource(path);
    if (resource != null) {
      log.debug("Found image in classpath: {}", path);
      return Optional.of(path);
    }

    return Optional.empty();
  }

  @Override
  public int getPriority() {
    return 100;
  }
}
