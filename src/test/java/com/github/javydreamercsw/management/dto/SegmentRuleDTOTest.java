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
package com.github.javydreamercsw.management.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import org.junit.jupiter.api.Test;

class SegmentRuleDTOTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void deserialize_fullRulesBlock_mapsCorrectly() throws Exception {
    String json =
        """
        {
          "name": "Cage",
          "description": "Steel Cage Match",
          "requiresHighHeat": true,
          "noDq": true,
          "bumpAddition": "ALL",
          "expansion_code": "BASE_GAME",
          "guide": {
            "solo": {
              "overview": "Solo overview",
              "setup": "Solo setup",
              "winCondition": "Pin or escape"
            },
            "multiplayer": {
              "concepts": "Multiplayer concepts",
              "gameEndConditions": "Last team standing"
            }
          }
        }
        """;

    SegmentRuleDTO dto = mapper.readValue(json, SegmentRuleDTO.class);

    assertThat(dto.getName()).isEqualTo("Cage");
    assertThat(dto.isRequiresHighHeat()).isTrue();
    assertThat(dto.getBumpAddition()).isEqualTo(BumpAddition.ALL);
    assertThat(dto.getGuide()).isNotNull();
    assertThat(dto.getGuide().solo()).isNotNull();
    assertThat(dto.getGuide().solo().overview()).isEqualTo("Solo overview");
    assertThat(dto.getGuide().solo().winCondition()).isEqualTo("Pin or escape");
    assertThat(dto.getGuide().multiplayer()).isNotNull();
    assertThat(dto.getGuide().multiplayer().concepts()).isEqualTo("Multiplayer concepts");
    assertThat(dto.getGuide().multiplayer().gameEndConditions()).isEqualTo("Last team standing");
  }

  @Test
  void deserialize_missingRulesField_rulesIsNull() throws Exception {
    String json =
        """
        {
          "name": "Normal",
          "description": "Standard match",
          "requiresHighHeat": false,
          "noDq": false,
          "bumpAddition": "NONE"
        }
        """;

    SegmentRuleDTO dto = mapper.readValue(json, SegmentRuleDTO.class);

    assertThat(dto.getName()).isEqualTo("Normal");
    assertThat(dto.getGuide()).isNull();
  }

  @Test
  void deserialize_unknownFieldsInRules_ignoredGracefully() throws Exception {
    String json =
        """
        {
          "name": "Extreme",
          "description": "Extreme rules",
          "requiresHighHeat": true,
          "noDq": true,
          "bumpAddition": "ALL",
          "guide": {
            "solo": {
              "overview": "Extreme overview",
              "unknownField": "should be ignored"
            }
          }
        }
        """;

    SegmentRuleDTO dto = mapper.readValue(json, SegmentRuleDTO.class);

    assertThat(dto.getGuide()).isNotNull();
    assertThat(dto.getGuide().solo().overview()).isEqualTo("Extreme overview");
    assertThat(dto.getGuide().multiplayer()).isNull();
  }
}
