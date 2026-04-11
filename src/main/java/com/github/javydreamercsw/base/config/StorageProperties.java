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
package com.github.javydreamercsw.base.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for data storage. Handles resolution of writable paths for images,
 * backups, and other data.
 */
@Component
@ConfigurationProperties(prefix = "atw.storage")
@Data
@Slf4j
public class StorageProperties {

  /** Base directory for all application data. Defaults to .atwrpg in user's home directory. */
  private String baseDir = System.getProperty("user.home") + "/.atwrpg";

  /** Directory for generated images. If not absolute, it will be resolved relative to baseDir. */
  private String imageDir = "images/generated";

  /** Directory for backups. If not absolute, it will be resolved relative to baseDir. */
  private String backupDir = "backups";

  /**
   * Resolves and returns the absolute path for image storage. Ensures the directory exists.
   *
   * @return The absolute path to the image directory.
   */
  public Path getResolvedImageDir() {
    return resolveAndCreate(imageDir);
  }

  /**
   * Resolves and returns the absolute path for backup storage. Ensures the directory exists.
   *
   * @return The absolute path to the backup directory.
   */
  public Path getResolvedBackupDir() {
    return resolveAndCreate(backupDir);
  }

  private Path resolveAndCreate(String pathStr) {
    Path path = Paths.get(pathStr);
    if (!path.isAbsolute()) {
      path = Paths.get(baseDir).resolve(pathStr);
    }

    try {
      if (!Files.exists(path)) {
        Files.createDirectories(path);
        log.info("Created storage directory: {}", path.toAbsolutePath());
      }
    } catch (IOException e) {
      log.error("Failed to create storage directory: {}", path, e);
    }

    return path.toAbsolutePath();
  }
}
