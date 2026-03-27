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

import com.github.javydreamercsw.base.config.StorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Image source that resolves images from a local filesystem directory. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemImageSource implements ImageSource {

  private final StorageProperties storageProperties;

  @Override
  public Optional<String> resolveImage(String name, ImageCategory category) {
    Path baseDir = storageProperties.getResolvedDefaultImageDir();
    String subDir = category.getDirectoryName();
    String filename = category.formatName(name) + ".png";
    Path imagePath = baseDir.resolve(subDir).resolve(filename);

    if (Files.exists(imagePath)) {
      log.debug("Found image in filesystem: {}", imagePath);
      return Optional.of("images/defaults/" + subDir + "/" + filename);
    }

    return Optional.empty();
  }

  @Override
  public int getPriority() {
    return 50; // Higher priority than classpath
  }
}
