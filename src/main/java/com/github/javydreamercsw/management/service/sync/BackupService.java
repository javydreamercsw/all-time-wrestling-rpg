package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
