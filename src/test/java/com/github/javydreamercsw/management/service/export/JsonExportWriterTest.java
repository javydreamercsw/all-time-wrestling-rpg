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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonExportWriterTest {

  private JsonExportWriter writer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    writer = new JsonExportWriter(objectMapper);
  }

  @Test
  void output_hasTopLevelUniverseAndExportedAtAndCategories() throws Exception {
    ExportPayload payload = new ExportPayload(Map.of(ExportCategory.UNIVERSE_STATE, List.of()));

    byte[] json = writer.write(payload, "TestUniverse");
    JsonNode root = objectMapper.readTree(json);

    assertThat(root.has("universe")).isTrue();
    assertThat(root.has("exportedAt")).isTrue();
    assertThat(root.has("categories")).isTrue();
    assertThat(root.get("universe").asText()).isEqualTo("TestUniverse");
  }

  @Test
  void exportedAt_isIso8601() throws Exception {
    byte[] json = writer.write(new ExportPayload(Map.of(ExportCategory.INJURIES, List.of())), "U");
    JsonNode root = objectMapper.readTree(json);

    String exportedAt = root.get("exportedAt").asText();
    assertThat(exportedAt).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
  }

  @Test
  void onlySelectedCategories_appearInOutput() throws Exception {
    Map<ExportCategory, List<Map<String, Object>>> data = new LinkedHashMap<>();
    data.put(ExportCategory.UNIVERSE_STATE, List.of());
    data.put(ExportCategory.INJURIES, List.of());

    byte[] json = writer.write(new ExportPayload(data), "U");
    JsonNode categories = objectMapper.readTree(json).get("categories");

    assertThat(categories.has("UNIVERSE_STATE")).isTrue();
    assertThat(categories.has("INJURIES")).isTrue();
    assertThat(categories.has("RIVALRIES")).isFalse();
  }

  @Test
  void emptyCategory_appearsAsEmptyArray() throws Exception {
    byte[] json = writer.write(new ExportPayload(Map.of(ExportCategory.RIVALRIES, List.of())), "U");
    JsonNode categories = objectMapper.readTree(json).get("categories");

    assertThat(categories.get("RIVALRIES").isArray()).isTrue();
    assertThat(categories.get("RIVALRIES").size()).isZero();
  }

  @Test
  void categoryWithRows_containsCorrectData() throws Exception {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("wrestler", "John");
    row.put("fans", 1000L);

    byte[] json =
        writer.write(new ExportPayload(Map.of(ExportCategory.UNIVERSE_STATE, List.of(row))), "U");
    JsonNode categories = objectMapper.readTree(json).get("categories");
    JsonNode firstRow = categories.get("UNIVERSE_STATE").get(0);

    assertThat(firstRow.get("wrestler").asText()).isEqualTo("John");
    assertThat(firstRow.get("fans").asLong()).isEqualTo(1000L);
  }

  @Test
  void output_isValidJson() throws Exception {
    Map<ExportCategory, List<Map<String, Object>>> data = new LinkedHashMap<>();
    for (ExportCategory cat : ExportCategory.values()) {
      data.put(cat, List.of());
    }

    byte[] json = writer.write(new ExportPayload(data), "Full Export");
    assertThat(objectMapper.readTree(json)).isNotNull();
  }
}
