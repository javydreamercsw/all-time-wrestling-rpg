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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationManifestTest {

  @Test
  void addEntry_addsToManifest() {
    String uniqueId = "test-" + UUID.randomUUID();
    DocEntry entry = new DocEntry(uniqueId, "rules", "Test Entry", "A test entry", null, 1);

    DocumentationManifest.addEntry(entry);

    assertThat(DocumentationManifest.getEntries())
        .anyMatch(e -> e.getId().equals(uniqueId) && e.getTitle().equals("Test Entry"));
  }

  @Test
  void addEntry_updatesExistingEntry() {
    String uniqueId = "update-" + UUID.randomUUID();
    DocEntry original = new DocEntry(uniqueId, "rules", "Original", "Original desc", null, 1);
    DocEntry updated = new DocEntry(uniqueId, "rules", "Updated", "Updated desc", null, 2);

    DocumentationManifest.addEntry(original);
    DocumentationManifest.addEntry(updated);

    List<DocEntry> entries = DocumentationManifest.getEntries();
    long count = entries.stream().filter(e -> e.getId().equals(uniqueId)).count();
    assertThat(count).isEqualTo(1);
    assertThat(
            entries.stream()
                .filter(e -> e.getId().equals(uniqueId))
                .findFirst()
                .map(DocEntry::getTitle)
                .orElse(""))
        .isEqualTo("Updated");
  }

  @Test
  void getEntries_returnsDefensiveCopy() {
    List<DocEntry> entries1 = DocumentationManifest.getEntries();
    List<DocEntry> entries2 = DocumentationManifest.getEntries();

    assertThat(entries1).isNotSameAs(entries2);
  }

  @Test
  void write_createsJsonFile(@TempDir Path tempDir) throws IOException {
    String uniqueId = "write-" + UUID.randomUUID();
    DocumentationManifest.addEntry(
        new DocEntry(uniqueId, "features", "Write Test", "Test write", null, 1));

    Path manifestPath = tempDir.resolve("manifest.json");
    DocumentationManifest.write(manifestPath);

    assertThat(Files.exists(manifestPath)).isTrue();
    String content = Files.readString(manifestPath);
    assertThat(content).contains("features");
  }

  @Test
  void docEntry_constructorAndGetters() {
    DocEntry entry =
        new DocEntry(
            "doc-001", "campaign", "Campaign Basics", "How to play", "/img/campaign.png", 5);

    assertThat(entry.getId()).isEqualTo("doc-001");
    assertThat(entry.getCategory()).isEqualTo("campaign");
    assertThat(entry.getTitle()).isEqualTo("Campaign Basics");
    assertThat(entry.getDescription()).isEqualTo("How to play");
    assertThat(entry.getImagePath()).isEqualTo("/img/campaign.png");
    assertThat(entry.getOrder()).isEqualTo(5);
  }

  @Test
  void docEntry_defaultConstructorAndSetters() {
    DocEntry entry = new DocEntry();
    entry.setId("doc-002");
    entry.setCategory("matches");
    entry.setTitle("Match Types");
    entry.setDescription("All match types");
    entry.setImagePath("/img/matches.png");
    entry.setOrder(10);

    assertThat(entry.getId()).isEqualTo("doc-002");
    assertThat(entry.getCategory()).isEqualTo("matches");
    assertThat(entry.getTitle()).isEqualTo("Match Types");
    assertThat(entry.getOrder()).isEqualTo(10);
  }
}
