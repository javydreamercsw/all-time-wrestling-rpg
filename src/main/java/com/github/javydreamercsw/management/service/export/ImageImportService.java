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
package com.github.javydreamercsw.management.service.export;

import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Extracts images from a ZIP produced by {@link ImageExportService} and writes them to the
 * configured storage directories. No database changes are made — existing {@code imageUrl}
 * references remain valid because filenames are preserved exactly.
 *
 * <p>Accepted ZIP entry paths:
 *
 * <ul>
 *   <li>{@code images/generated/{filename}} — flat, no subdirectories
 *   <li>{@code images/defaults/{subdir}/{filename}} — one subdirectory level (e.g. {@code
 *       images/defaults/wrestlers/John.png})
 * </ul>
 *
 * All other paths are skipped. {@code ..} components are always rejected.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageImportService {

  static final String GENERATED_PREFIX = "images/generated/";
  static final String DEFAULTS_PREFIX = "images/defaults/";

  private final StorageProperties storageProperties;

  /**
   * Extracts the ZIP entries from {@code inputStream} into the appropriate storage directories.
   *
   * @param inputStream the ZIP bytes (not closed by this method)
   * @return a summary of what happened
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ImportSummary importImages(final InputStream inputStream) throws IOException {
    int written = 0;
    int skipped = 0;
    int errors = 0;

    try (ZipInputStream zip = new ZipInputStream(inputStream)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String entryName = entry.getName();

        Path target = resolveTarget(entryName);
        if (target == null) {
          log.debug("Skipping ZIP entry outside allowed paths: {}", entryName);
          skipped++;
          zip.closeEntry();
          continue;
        }

        try {
          Files.createDirectories(target.getParent());
          Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
          log.debug("Restored image: {}", target);
          written++;
        } catch (IOException e) {
          log.warn("Failed to write image {}: {}", target, e.getMessage());
          errors++;
        }
        zip.closeEntry();
      }
    }

    log.info("Image import complete: {} written, {} skipped, {} errors", written, skipped, errors);
    return new ImportSummary(written, skipped, errors);
  }

  /**
   * Maps a ZIP entry name to an absolute filesystem path, or returns {@code null} if the entry is
   * outside the two allowed prefixes or contains a path-traversal sequence.
   */
  Path resolveTarget(final String entryName) {
    if (entryName == null || entryName.contains("..")) {
      return null;
    }

    if (entryName.startsWith(GENERATED_PREFIX)) {
      String filename = entryName.substring(GENERATED_PREFIX.length());
      // Flat only: no sub-directories allowed under images/generated/
      if (filename.isEmpty() || filename.contains("/")) {
        return null;
      }
      return storageProperties.getResolvedImageDir().resolve(filename);
    }

    if (entryName.startsWith(DEFAULTS_PREFIX)) {
      String relativePath = entryName.substring(DEFAULTS_PREFIX.length());
      // Allow exactly one subdirectory level: "{subdir}/{filename}"
      if (relativePath.isEmpty()) {
        return null;
      }
      int slashCount = relativePath.length() - relativePath.replace("/", "").length();
      if (slashCount > 1) {
        return null;
      }
      return storageProperties.getResolvedDefaultImageDir().resolve(relativePath);
    }

    return null;
  }

  public record ImportSummary(int written, int skipped, int errors) {
    public String toMessage() {
      return "Image restore complete: %d restored, %d skipped, %d errors"
          .formatted(written, skipped, errors);
    }
  }
}
