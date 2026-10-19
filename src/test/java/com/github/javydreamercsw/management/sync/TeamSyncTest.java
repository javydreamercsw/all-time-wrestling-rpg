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
package com.github.javydreamercsw.management.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@code teams.json} catalog parsing (ATW-01g2). */
@DisplayName("TeamSync Catalog Tests")
class TeamSyncTest {

  @Test
  @DisplayName("Catalog entry without teamSize parses as DUO")
  void catalogWithoutTeamSizeSeedsDuo() throws Exception {
    // Given - a legacy catalog entry without a teamSize field
    String json =
        """
        [
          {
            "name": "Legacy Duo",
            "description": "An old-shape entry",
            "members": ["Wrestler A", "Wrestler B"]
          }
        ]
        """;

    // When
    List<TeamImportDTO> dtos = new ObjectMapper().readValue(json, new TypeReference<>() {});

    // Then
    assertThat(dtos).hasSize(1);
    TeamImportDTO dto = dtos.getFirst();
    assertThat(dto.getTeamSize()).isEqualTo(TeamSize.DUO);
    assertThat(dto.getMembers()).containsExactly("Wrestler A", "Wrestler B");
  }
}
