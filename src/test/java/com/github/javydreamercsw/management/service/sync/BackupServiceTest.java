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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupServiceTest {

  @Mock private NotionSyncProperties syncProperties;
  @Mock private StorageProperties storageProperties;
  @Mock private ResourceLoader resourceLoader;
  @Mock private Resource mockResource;
  @Mock private NotionSyncProperties.Backup mockBackup;

  @TempDir Path tempDir;

  private BackupService backupService;

  @BeforeEach
  void setUp() {
    backupService = new BackupService(syncProperties, storageProperties, resourceLoader);
    // Default: backup dir points to our temp dir
    when(storageProperties.getResolvedBackupDir()).thenReturn(tempDir);
    // Default: backup config with 10 max files
    when(syncProperties.getBackup()).thenReturn(mockBackup);
    when(mockBackup.getMaxFiles()).thenReturn(10);
  }

  @Test
  void createBackup_sourceFileNotFound_doesNothing() throws IOException {
    // Classpath resource does not exist, and dev path won't exist for "nonexistent.json"
    when(resourceLoader.getResource("classpath:nonexistent.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(false);

    assertThatNoException().isThrownBy(() -> backupService.createBackup("nonexistent.json"));

    // No backup files created
    try (Stream<Path> files = Files.list(tempDir)) {
      assertThat(files.count()).isZero();
    }
  }

  @Test
  void createBackup_sourceFileExists_createsTimestampedBackup() throws IOException {
    // Create a real temp source file
    Path sourceFile = tempDir.resolve("source_data.json");
    Files.writeString(sourceFile, "{\"test\": true}");

    // Mock resourceLoader to return a resource backed by our temp file
    when(resourceLoader.getResource("classpath:data.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    backupService.createBackup("data.json");

    // Verify a backup file was created in the temp backup dir
    try (Stream<Path> files = Files.list(tempDir)) {
      List<Path> backupFiles =
          files
              .filter(p -> p.getFileName().toString().startsWith("data_") && !p.equals(sourceFile))
              .toList();
      assertThat(backupFiles).hasSize(1);
    }
  }

  @Test
  void createBackup_backupFileHasTimestampPattern() throws IOException {
    Path sourceFile = tempDir.resolve("test-wrestlers-unit.json");
    Files.writeString(sourceFile, "{}");

    when(resourceLoader.getResource("classpath:test-wrestlers-unit.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    backupService.createBackup("test-wrestlers-unit.json");

    try (Stream<Path> files = Files.list(tempDir)) {
      List<Path> backupFiles =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("test-wrestlers-unit_")
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .toList();
      assertThat(backupFiles).hasSize(1);
      // File name should contain a timestamp portion, e.g.
      // test-wrestlers-unit_20250101_120000.json
      String name = backupFiles.get(0).getFileName().toString();
      assertThat(name).matches("test-wrestlers-unit_\\d{8}_\\d{6}\\.json");
    }
  }

  @Test
  void createBackup_cleansUpOldBackupsExceedingMaxFiles() throws IOException {
    int maxFiles = 3;
    when(mockBackup.getMaxFiles()).thenReturn(maxFiles);

    // Create source file (use unique name to avoid matching real classpath resources)
    Path sourceFile = tempDir.resolve("test-shows-unit.json");
    Files.writeString(sourceFile, "{}");

    when(resourceLoader.getResource("classpath:test-shows-unit.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    // Pre-create maxFiles existing backup files (older timestamps come first alphabetically)
    for (int i = 1; i <= maxFiles; i++) {
      Path oldBackup = tempDir.resolve(String.format("test-shows-unit_2020010%d_120000.json", i));
      Files.writeString(oldBackup, "{}");
    }

    // createBackup will add one more, then cleanup should remove the oldest
    backupService.createBackup("test-shows-unit.json");

    // After cleanup, only maxFiles should remain
    try (Stream<Path> files = Files.list(tempDir)) {
      long backupCount =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("test-shows-unit_")
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .count();
      assertThat(backupCount).isEqualTo(maxFiles);
    }
  }

  @Test
  void createBackup_exactlyMaxFilesExist_noCleanup() throws IOException {
    int maxFiles = 2;
    when(mockBackup.getMaxFiles()).thenReturn(maxFiles);

    // Pre-create exactly maxFiles - 1 backups so after createBackup we reach exactly maxFiles
    Path sourceFile = tempDir.resolve("titles.json");
    Files.writeString(sourceFile, "{}");

    when(resourceLoader.getResource("classpath:titles.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    // Create maxFiles - 1 = 1 pre-existing backup
    Path existingBackup = tempDir.resolve("titles_20200101_120000.json");
    Files.writeString(existingBackup, "{}");

    backupService.createBackup("titles.json");

    // Should now have exactly maxFiles backups and none deleted
    try (Stream<Path> files = Files.list(tempDir)) {
      long backupCount =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("titles_")
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .count();
      assertThat(backupCount).isEqualTo(maxFiles);
    }
    // The pre-existing backup should still be there (no deletion happened)
    assertThat(existingBackup).exists();
  }

  @Test
  void createBackup_resourceExistsAsClasspath_copiesContent() throws IOException {
    // Use a filename that does not exist in src/main/resources so resolveSourceFile falls through
    // to the classpath resource loader path.
    String uniqueFileName = "test-backup-classpathonly-99999.json";
    Path sourceFile = tempDir.resolve("source_" + uniqueFileName);
    String content = "{\"items\": [\"a\", \"b\"]}";
    Files.writeString(sourceFile, content);

    when(resourceLoader.getResource("classpath:" + uniqueFileName)).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    backupService.createBackup(uniqueFileName);

    String expectedPrefix = uniqueFileName.replace(".json", "_");
    try (Stream<Path> files = Files.list(tempDir)) {
      List<Path> backupFiles =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith(expectedPrefix)
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .toList();
      assertThat(backupFiles).hasSize(1);
      assertThat(Files.readString(backupFiles.get(0))).isEqualTo(content);
    }
  }

  @Test
  void createBackup_resourceExistsButGetFileThrows_doesNothing() throws IOException {
    when(resourceLoader.getResource("classpath:broken.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenThrow(new IOException("Cannot access file"));

    // Should not propagate the exception — it just logs and returns null from resolveSourceFile
    assertThatNoException().isThrownBy(() -> backupService.createBackup("broken.json"));

    try (Stream<Path> files = Files.list(tempDir)) {
      assertThat(files.count()).isZero();
    }
  }

  @Test
  void createBackup_multipleCallsCreateMultipleBackups() throws IOException {
    when(mockBackup.getMaxFiles()).thenReturn(10);

    Path sourceFile = tempDir.resolve("test-arenas-unit.json");
    Files.writeString(sourceFile, "{}");

    when(resourceLoader.getResource("classpath:test-arenas-unit.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    // Call createBackup twice with a small delay to get different timestamps
    backupService.createBackup("test-arenas-unit.json");
    // Ensure different timestamp by sleeping 1 second
    try {
      Thread.sleep(1100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    backupService.createBackup("test-arenas-unit.json");

    try (Stream<Path> files = Files.list(tempDir)) {
      long backupCount =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("test-arenas-unit_")
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .count();
      assertThat(backupCount).isEqualTo(2);
    }
  }

  @Test
  void createBackup_zeroMaxFiles_deletesAllOldBackups() throws IOException {
    when(mockBackup.getMaxFiles()).thenReturn(0);

    Path sourceFile = tempDir.resolve("test-seasons-unit.json");
    Files.writeString(sourceFile, "{}");

    when(resourceLoader.getResource("classpath:test-seasons-unit.json")).thenReturn(mockResource);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.getFile()).thenReturn(sourceFile.toFile());

    // Pre-create some existing backups
    Files.writeString(tempDir.resolve("test-seasons-unit_20200101_120000.json"), "{}");
    Files.writeString(tempDir.resolve("test-seasons-unit_20200102_120000.json"), "{}");

    backupService.createBackup("test-seasons-unit.json");

    // All backups (including the just-created one) should be deleted since maxFiles = 0
    try (Stream<Path> files = Files.list(tempDir)) {
      long backupCount =
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("test-seasons-unit_")
                          && !p.equals(sourceFile)
                          && p.getFileName().toString().endsWith(".json"))
              .count();
      assertThat(backupCount).isZero();
    }
  }

  @Test
  void createBackup_nullFileName_throwsNullPointerException() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> backupService.createBackup(null))
        .isInstanceOf(NullPointerException.class);
  }

  /** Helper to create a temp file backed by the given path, visible to the filesystem. */
  private File createTempFileAt(final Path path, final String content) throws IOException {
    Files.writeString(path, content);
    return path.toFile();
  }
}
