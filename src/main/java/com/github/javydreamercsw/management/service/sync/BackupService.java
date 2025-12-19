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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BackupService {

  private final NotionSyncProperties syncProperties;

  public BackupService(NotionSyncProperties syncProperties) {
    this.syncProperties = syncProperties;
  }

  /**
   * Creates a backup of the specified JSON file before sync operation.
   *
   * @param fileName The name of the JSON file to backup
   * @throws IOException if backup creation fails
   */
  @PreAuthorize("hasAuthority('ADMIN')")
  public void createBackup(@NonNull String fileName) throws IOException {
    Path originalFile = Paths.get("src/main/resources/" + fileName);

    if (!Files.exists(originalFile)) {
      log.debug("Original file {} does not exist, skipping backup", fileName);
      return;
    }

    // Create backup directory
    Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
    Files.createDirectories(backupDir);

    // Create backup file with timestamp
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String backupFileName = fileName.replace(".json", "_" + timestamp + ".json");
    Path backupFile = backupDir.resolve(backupFileName);

    // Copy original file to backup location
    Files.copy(originalFile, backupFile);
    log.info("Created backup: {}", backupFile);

    // Clean up old backups
    cleanupOldBackups(fileName);
  }

  /**
   * Removes old backup files, keeping only the configured maximum number.
   *
   * @param fileName The base file name to clean up backups for
   */
  private void cleanupOldBackups(@NonNull String fileName) {
    try {
      Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
      if (!Files.exists(backupDir)) {
        return;
      }

      String baseFileName = fileName.replace(".json", "");
      List<Path> backupFiles =
          Files.list(backupDir)
              .filter(path -> path.getFileName().toString().startsWith(baseFileName + "_"))
              .sorted(
                  (p1, p2) ->
                      p2.getFileName()
                          .toString()
                          .compareTo(p1.getFileName().toString())) // Newest first
              .collect(Collectors.toList());

      int maxFiles = syncProperties.getBackup().getMaxFiles();
      if (backupFiles.size() > maxFiles) {
        List<Path> filesToDelete = backupFiles.subList(maxFiles, backupFiles.size());
        for (Path fileToDelete : filesToDelete) {
          Files.delete(fileToDelete);
          log.debug("Deleted old backup: {}", fileToDelete);
        }
        log.info("Cleaned up {} old backup files for {}", filesToDelete.size(), fileName);
      }

    } catch (IOException e) {
      log.warn("Failed to cleanup old backups for {}: {}", fileName, e.getMessage());
    }
  }
}
