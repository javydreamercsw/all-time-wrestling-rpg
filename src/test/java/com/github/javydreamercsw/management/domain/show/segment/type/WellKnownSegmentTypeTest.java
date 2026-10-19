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
package com.github.javydreamercsw.management.domain.show.segment.type;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class WellKnownSegmentTypeTest {

  @Test
  void enumAndJsonCodesAreInSync() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> dtos =
        mapper.readValue(
            new ClassPathResource("segment_types.json").getInputStream(), new TypeReference<>() {});

    Set<String> jsonCodes =
        dtos.stream()
            .map(d -> (String) d.get("code"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Set<String> enumCodes =
        Arrays.stream(WellKnownSegmentType.values())
            .map(WellKnownSegmentType::getCode)
            .collect(Collectors.toSet());

    assertThat(enumCodes)
        .as(
            "WellKnownSegmentType constants not present in segment_types.json — "
                + "add the missing code(s) to the JSON or remove the obsolete enum constant(s)")
        .isSubsetOf(jsonCodes);

    assertThat(jsonCodes)
        .as(
            "segment_types.json code(s) not registered in WellKnownSegmentType enum — "
                + "add the missing constant(s) to WellKnownSegmentType")
        .isSubsetOf(enumCodes);
  }

  @Test
  void triosIsAWellKnownSeededSegmentType() throws Exception {
    // ATW-01g2 Task 4: the trios team-tournament format resolves its payoff
    // segment type from the catalog like every other well-known type.
    assertThat(WellKnownSegmentType.TRIOS.getCode()).isEqualTo("trios");
    assertThat(WellKnownSegmentType.TRIOS.getDisplayName()).isEqualTo("Trios");
    assertThat(WellKnownSegmentType.fromCode("trios")).contains(WellKnownSegmentType.TRIOS);

    // The segment_types.json catalog parse covers trios, so SegmentTypeSync
    // seeds a SegmentType row with code "trios" on startup.
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> dtos =
        mapper.readValue(
            new ClassPathResource("segment_types.json").getInputStream(), new TypeReference<>() {});
    assertThat(dtos)
        .anySatisfy(
            dto -> {
              assertThat(dto.get("code")).isEqualTo("trios");
              assertThat(dto.get("name")).isEqualTo("Trios");
            });
  }

  @Test
  void enumDisplayNamesMatchJsonNames() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, Object>> dtos =
        mapper.readValue(
            new ClassPathResource("segment_types.json").getInputStream(), new TypeReference<>() {});

    Map<String, String> jsonNamesByCode =
        dtos.stream()
            .filter(d -> d.get("code") != null && d.get("name") != null)
            .collect(Collectors.toMap(d -> (String) d.get("code"), d -> (String) d.get("name")));

    for (WellKnownSegmentType type : WellKnownSegmentType.values()) {
      assertThat(type.getDisplayName())
          .as(
              "Display name drift for %s — segment_types.json says '%s'",
              type, jsonNamesByCode.get(type.getCode()))
          .isEqualTo(jsonNamesByCode.get(type.getCode()));
    }
  }
}
