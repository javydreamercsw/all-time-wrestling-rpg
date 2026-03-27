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

  private static final String RESOURCE_PREFIX = "META-INF/resources/";
  private static final String IMAGES_BASE = "images/";

  @Override
  public Optional<String> resolveImage(String name, ImageCategory category) {
    String subDir = category.getDirectoryName() + "/";
    String filename = (category.isUseKebabCase() ? toKebabCase(name) : name) + ".png";
    String webPath = IMAGES_BASE + subDir + filename;
    String classpathPath = RESOURCE_PREFIX + webPath;

    URL resource = getClass().getClassLoader().getResource(classpathPath);
    if (resource != null) {
      log.debug("Found image in classpath: {}", classpathPath);
      return Optional.of(webPath);
    }

    return Optional.empty();
  }

  private String toKebabCase(String name) {
    if (name == null) {
      return "";
    }
    return name.toLowerCase().replaceAll("\\s+", "-");
  }

  @Override
  public int getPriority() {
    return 100;
  }
}
