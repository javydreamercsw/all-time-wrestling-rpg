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
package com.github.javydreamercsw.management.util.docs;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentationManifest {
  private static final List<DocEntry> ENTRIES = new CopyOnWriteArrayList<>();
  private static final ObjectMapper MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  /** Writer that produces Spotless-compatible JSON (no space before colon). */
  private static final ObjectWriter WRITER =
      MAPPER.writer(
          new DefaultPrettyPrinter()
              .withSeparators(
                  Separators.createDefaultInstance()
                      .withObjectFieldValueSpacing(Separators.Spacing.AFTER)));

  /**
   * Add a new entry to the manifest.
   *
   * @param entry The entry to add
   */
  public static void addEntry(final DocEntry entry) {
    // Remove existing if same ID to allow updates/retries
    ENTRIES.removeIf(e -> e.getId().equals(entry.getId()));
    ENTRIES.add(entry);
  }

  /**
   * Get all entries.
   *
   * @return List of entries
   */
  public static List<DocEntry> getEntries() {
    return new ArrayList<>(ENTRIES);
  }

  private static void loadExisting(final Path path) {
    if (Files.exists(path)) {
      try {
        ManifestWrapper wrapper = MAPPER.readValue(path.toFile(), ManifestWrapper.class);
        if (wrapper != null && wrapper.getFeatures() != null) {
          for (DocEntry entry : wrapper.getFeatures()) {
            addEntry(entry);
          }
        }
      } catch (IOException e) {
        // Ignore load errors, start fresh
      }
    }
  }

  /**
   * Write the manifest to a JSON file.
   *
   * @param path The path to write to
   * @throws IOException If writing fails
   */
  public static void write(final Path path) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }

    // Load existing first to be cumulative
    loadExisting(path);

    ManifestWrapper wrapper = new ManifestWrapper();
    wrapper.setFeatures(getEntries());

    WRITER.writeValue(path.toFile(), wrapper);
  }

  /** Inner wrapper class for JSON root structure. */
  private static class ManifestWrapper {
    @Getter @Setter private List<DocEntry> features;
  }
}
