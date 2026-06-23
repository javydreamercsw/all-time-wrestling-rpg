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
package com.github.javydreamercsw.management.service.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleVariantGuide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class SegmentRuleServiceIT extends ManagementIntegrationTest {

  @Autowired private SegmentRuleService segmentRuleService;

  @MockitoSpyBean private SegmentRuleRepository segmentRuleRepository;

  @Test
  @DisplayName("Test that createSegmentRule evicts cache")
  void testCreateSegmentRuleEvictsCache() {
    segmentRuleService.findAll();
    verify(segmentRuleRepository, times(1)).findAll();

    segmentRuleService.createOrUpdateRule(
        "Test Rule IT", "Test Description", false, false, BumpAddition.NONE);

    segmentRuleService.findAll();
    verify(segmentRuleRepository, times(2)).findAll();
  }

  @Test
  @DisplayName("createOrUpdateRule persists rules_json and rules_hash on first sync")
  void createOrUpdateRule_withRules_persistsRulesJson() {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview", "setup", null, null, "win", null, null, null, null, null, null, null);
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, null);

    SegmentRule saved =
        segmentRuleService.createOrUpdateRule(
            "IT Cage", "IT Cage match", true, true, BumpAddition.ALL, "BASE_GAME", guide);

    assertThat(saved.getRules()).isNotNull();
    assertThat(saved.getRules().solo().overview()).isEqualTo("overview");
    assertThat(saved.getRulesHash()).isNotBlank();
  }

  @Test
  @DisplayName("createOrUpdateRule skips DB write when rules content is unchanged")
  void createOrUpdateRule_identicalContent_skipsWrite() {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, null);

    SegmentRule first =
        segmentRuleService.createOrUpdateRule(
            "IT No DQ", "No DQ", false, true, BumpAddition.ALL, "BASE_GAME", guide);
    String hashAfterFirst = first.getRulesHash();

    // Same content — should return without save
    SegmentRule second =
        segmentRuleService.createOrUpdateRule(
            "IT No DQ", "No DQ", false, true, BumpAddition.ALL, "BASE_GAME", guide);

    assertThat(second.getRulesHash()).isEqualTo(hashAfterFirst);
    verify(segmentRuleRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("createOrUpdateRule triggers update when rules content changes")
  void createOrUpdateRule_changedContent_updatesRulesHash() {
    SegmentRuleVariantGuide soloV1 =
        new SegmentRuleVariantGuide(
            "original overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide guideV1 = new SegmentRulePlayGuide(soloV1, null);

    SegmentRule v1 =
        segmentRuleService.createOrUpdateRule(
            "IT Extreme", "Extreme", true, true, BumpAddition.ALL, "BASE_GAME", guideV1);

    SegmentRuleVariantGuide soloV2 =
        new SegmentRuleVariantGuide(
            "updated overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide guideV2 = new SegmentRulePlayGuide(soloV2, null);

    SegmentRule v2 =
        segmentRuleService.createOrUpdateRule(
            "IT Extreme", "Extreme", true, true, BumpAddition.ALL, "BASE_GAME", guideV2);

    assertThat(v2.getRulesHash()).isNotEqualTo(v1.getRulesHash());
    assertThat(v2.getRules().solo().overview()).isEqualTo("updated overview");
  }
}
