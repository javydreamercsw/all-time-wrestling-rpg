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
package com.github.javydreamercsw.management.domain.show.segment.rule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SegmentRulePlayGuideConverterTest {

  private final SegmentRulePlayGuideConverter converter = new SegmentRulePlayGuideConverter();

  @Test
  void roundTrip_fullGuide_preservesAllFields() {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview",
            "setup",
            "attacking",
            "defending",
            "winCondition",
            "npcRecovery",
            "topOfCageStruggle",
            "npcWinConditions",
            null,
            null,
            null,
            null);
    SegmentRuleVariantGuide multiplayer =
        new SegmentRuleVariantGuide(
            null,
            "mp-setup",
            null,
            null,
            null,
            null,
            null,
            null,
            "concepts",
            "gameplayChanges",
            "modeSpecificAbilities",
            "gameEndConditions");
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, multiplayer);

    String json = converter.convertToDatabaseColumn(guide);
    assertThat(json).isNotBlank();

    SegmentRulePlayGuide restored = converter.convertToEntityAttribute(json);
    assertThat(restored).isNotNull();
    assertThat(restored.solo()).isNotNull();
    assertThat(restored.solo().overview()).isEqualTo("overview");
    assertThat(restored.solo().topOfCageStruggle()).isEqualTo("topOfCageStruggle");
    assertThat(restored.multiplayer()).isNotNull();
    assertThat(restored.multiplayer().concepts()).isEqualTo("concepts");
    assertThat(restored.multiplayer().gameEndConditions()).isEqualTo("gameEndConditions");
  }

  @Test
  void convertToDatabaseColumn_null_returnsNull() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
  }

  @Test
  void convertToEntityAttribute_null_returnsNull() {
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }

  @Test
  void convertToEntityAttribute_blankString_returnsNull() {
    assertThat(converter.convertToEntityAttribute("")).isNull();
    assertThat(converter.convertToEntityAttribute("   ")).isNull();
  }

  @Test
  void convertToEntityAttribute_nullString_returnsNull() {
    assertThat(converter.convertToEntityAttribute("null")).isNull();
  }

  @Test
  void roundTrip_soloOnlyGuide_multiplayer_isNull() {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview", null, null, null, "winCondition", null, null, null, null, null, null, null);
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, null);

    String json = converter.convertToDatabaseColumn(guide);
    SegmentRulePlayGuide restored = converter.convertToEntityAttribute(json);

    assertThat(restored).isNotNull();
    assertThat(restored.solo().overview()).isEqualTo("overview");
    assertThat(restored.solo().winCondition()).isEqualTo("winCondition");
    assertThat(restored.multiplayer()).isNull();
  }
}
