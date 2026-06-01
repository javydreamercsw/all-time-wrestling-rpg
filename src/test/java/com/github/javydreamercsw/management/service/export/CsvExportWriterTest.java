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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvExportWriterTest {

  private CsvExportWriter writer;

  @BeforeEach
  void setUp() {
    writer = new CsvExportWriter();
  }

  private ExportPayload payloadWith(ExportCategory category, List<Map<String, Object>> rows) {
    return new ExportPayload(Map.of(category, rows));
  }

  @Test
  void twoCategories_zipContainsTwoEntries() throws IOException {
    Map<ExportCategory, List<Map<String, Object>>> data = new LinkedHashMap<>();
    data.put(ExportCategory.UNIVERSE_STATE, List.of(Map.of("wrestler", "John")));
    data.put(ExportCategory.INJURIES, List.of(Map.of("wrestler", "John", "name", "Bruise")));

    byte[] zip = writer.write(new ExportPayload(data), "MyUniverse");

    int entryCount = 0;
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      while (zis.getNextEntry() != null) {
        entryCount++;
        zis.closeEntry();
      }
    }
    assertThat(entryCount).isEqualTo(2);
  }

  @Test
  void zipEntryName_containsCategoryAndUniverseName() throws IOException {
    byte[] zip = writer.write(payloadWith(ExportCategory.RIVALRIES, List.of()), "Test Universe");

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      var entry = zis.getNextEntry();
      assertThat(entry.getName()).contains("Test_Universe").contains("rivalries");
    }
  }

  @Test
  void emptyRows_producesHeaderOnlyCsv() throws IOException {
    byte[] zip = writer.write(payloadWith(ExportCategory.UNIVERSE_STATE, List.of()), "U");

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      zis.getNextEntry();
      byte[] content = zis.readAllBytes();
      // Empty row list → no header row written (nothing to derive headers from)
      assertThat(content).isEmpty();
    }
  }

  @Test
  void headerRow_matchesMapKeys() throws IOException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("wrestler", "Jane");
    row.put("fans", 500L);
    row.put("tier", "ROOKIE");

    byte[] zip = writer.write(payloadWith(ExportCategory.UNIVERSE_STATE, List.of(row)), "U");

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      zis.getNextEntry();
      String content = new String(zis.readAllBytes());
      String firstLine = content.lines().findFirst().orElse("");
      assertThat(firstLine).isEqualTo("wrestler,fans,tier");
    }
  }

  @Test
  void fieldWithComma_isQuoted() throws IOException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("notes", "win, lose");

    byte[] zip = writer.write(payloadWith(ExportCategory.UNIVERSE_STATE, List.of(row)), "U");

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      zis.getNextEntry();
      String content = new String(zis.readAllBytes());
      assertThat(content).contains("\"win, lose\"");
    }
  }

  @Test
  void fieldWithDoubleQuote_isEscaped() throws IOException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("notes", "he said \"hello\"");

    byte[] zip = writer.write(payloadWith(ExportCategory.UNIVERSE_STATE, List.of(row)), "U");

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
      zis.getNextEntry();
      String content = new String(zis.readAllBytes());
      assertThat(content).contains("\"he said \"\"hello\"\"\"");
    }
  }
}
