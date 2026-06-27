/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleVariantGuide;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentRuleServiceTest {

  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private SegmentRuleService segmentRuleService;

  private SegmentRule rule1;
  private SegmentRule rule2;

  @BeforeEach
  void setUp() {
    rule1 = new SegmentRule();
    rule1.setName("No DQ");
    rule1.setDescription("No disqualification match");
    rule1.setRequiresHighHeat(true);
    rule1.setNoDq(true);
    rule1.setBumpAddition(BumpAddition.NONE);
    rule1.setExpansionCode("BASE_GAME");

    rule2 = new SegmentRule();
    rule2.setName("Standard");
    rule2.setDescription("Standard match rules");
    rule2.setRequiresHighHeat(false);
    rule2.setNoDq(false);
    rule2.setBumpAddition(BumpAddition.NONE);
    rule2.setExpansionCode("BASE_GAME");

    // Default: no active universe, all expansions enabled
    Mockito.when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    Mockito.when(expansionService.getEnabledExpansionCodes())
        .thenReturn(List.of("BASE_GAME", "CUS"));
  }

  @Test
  void findByName_found_returnsOptional() {
    when(segmentRuleRepository.findByName("No DQ")).thenReturn(Optional.of(rule1));

    Optional<SegmentRule> result = segmentRuleService.findByName("No DQ");

    assertThat(result).isPresent().contains(rule1);
    verify(segmentRuleRepository).findByName("No DQ");
  }

  @Test
  void findByName_notFound_returnsEmpty() {
    when(segmentRuleRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<SegmentRule> result = segmentRuleService.findByName("Unknown");

    assertThat(result).isEmpty();
    verify(segmentRuleRepository).findByName("Unknown");
  }

  @Test
  void getHighHeatRules_returnsList() {
    when(segmentRuleRepository.findSuitableForHighHeat()).thenReturn(List.of(rule1));

    List<SegmentRule> result = segmentRuleService.getHighHeatRules();

    assertThat(result).hasSize(1).contains(rule1);
    verify(segmentRuleRepository).findSuitableForHighHeat();
  }

  @Test
  void getHighHeatRules_noHighHeatRules_returnsEmptyList() {
    when(segmentRuleRepository.findSuitableForHighHeat()).thenReturn(Collections.emptyList());

    List<SegmentRule> result = segmentRuleService.getHighHeatRules();

    assertThat(result).isEmpty();
    verify(segmentRuleRepository).findSuitableForHighHeat();
  }

  @Test
  void getStandardRules_returnsList() {
    when(segmentRuleRepository.findStandardRules()).thenReturn(List.of(rule2));

    List<SegmentRule> result = segmentRuleService.getStandardRules();

    assertThat(result).hasSize(1).contains(rule2);
    verify(segmentRuleRepository).findStandardRules();
  }

  @Test
  void getStandardRules_noStandardRules_returnsEmptyList() {
    when(segmentRuleRepository.findStandardRules()).thenReturn(Collections.emptyList());

    List<SegmentRule> result = segmentRuleService.getStandardRules();

    assertThat(result).isEmpty();
    verify(segmentRuleRepository).findStandardRules();
  }

  @Test
  void createRule_newName_createsAndSavesRule() {
    when(segmentRuleRepository.existsByName("Steel Cage")).thenReturn(false);
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result = segmentRuleService.createRule("Steel Cage", "Steel cage match", true);

    assertThat(result.getName()).isEqualTo("Steel Cage");
    assertThat(result.getDescription()).isEqualTo("Steel cage match");
    assertThat(result.getRequiresHighHeat()).isTrue();
    verify(segmentRuleRepository).existsByName("Steel Cage");
    verify(segmentRuleRepository).save(any(SegmentRule.class));
  }

  @Test
  void createRule_duplicateName_throwsIllegalArgumentException() {
    when(segmentRuleRepository.existsByName("No DQ")).thenReturn(true);

    assertThatThrownBy(() -> segmentRuleService.createRule("No DQ", "No DQ match", true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No DQ");
  }

  @Test
  void updateRule_existingId_updatesAndSavesRule() {
    rule1.setName("No DQ");
    when(segmentRuleRepository.findById(1L)).thenReturn(Optional.of(rule1));
    when(segmentRuleRepository.existsByName("Updated Name")).thenReturn(false);
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result =
        segmentRuleService.updateRule(1L, "Updated Name", "Updated description", false);

    assertThat(result.getName()).isEqualTo("Updated Name");
    assertThat(result.getDescription()).isEqualTo("Updated description");
    assertThat(result.getRequiresHighHeat()).isFalse();
    verify(segmentRuleRepository).findById(1L);
    verify(segmentRuleRepository).save(any(SegmentRule.class));
  }

  @Test
  void updateRule_nonExistentId_throwsIllegalArgumentException() {
    when(segmentRuleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> segmentRuleService.updateRule(99L, "Name", "Desc", false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("99");
  }

  @Test
  void existsByName_existingName_returnsTrue() {
    when(segmentRuleRepository.existsByName("No DQ")).thenReturn(true);

    boolean result = segmentRuleService.existsByName("No DQ");

    assertThat(result).isTrue();
    verify(segmentRuleRepository).existsByName("No DQ");
  }

  @Test
  void existsByName_nonExistentName_returnsFalse() {
    when(segmentRuleRepository.existsByName("Unknown")).thenReturn(false);

    boolean result = segmentRuleService.existsByName("Unknown");

    assertThat(result).isFalse();
    verify(segmentRuleRepository).existsByName("Unknown");
  }

  @Test
  void findById_existingId_returnsOptional() {
    when(segmentRuleRepository.findById(1L)).thenReturn(Optional.of(rule1));

    Optional<SegmentRule> result = segmentRuleService.findById(1L);

    assertThat(result).isPresent().contains(rule1);
    verify(segmentRuleRepository).findById(1L);
  }

  @Test
  void findById_nonExistentId_returnsEmpty() {
    when(segmentRuleRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<SegmentRule> result = segmentRuleService.findById(99L);

    assertThat(result).isEmpty();
    verify(segmentRuleRepository).findById(99L);
  }

  @Test
  void createOrUpdateRule_newRule_createsNewRule() {
    when(segmentRuleRepository.findByName("Ladder Match")).thenReturn(Optional.empty());
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result =
        segmentRuleService.createOrUpdateRule(
            "Ladder Match", "Ladder match stipulation", false, false, BumpAddition.NONE);

    assertThat(result.getName()).isEqualTo("Ladder Match");
    assertThat(result.getDescription()).isEqualTo("Ladder match stipulation");
    verify(segmentRuleRepository).save(any(SegmentRule.class));
  }

  @Test
  void createOrUpdateRule_existingRuleUnchanged_returnsExistingWithoutSave() {
    rule2.setDescription("Standard match rules");
    rule2.setRequiresHighHeat(false);
    rule2.setNoDq(false);
    rule2.setBumpAddition(BumpAddition.NONE);
    rule2.setExpansionCode("BASE_GAME");
    when(segmentRuleRepository.findByName("Standard")).thenReturn(Optional.of(rule2));

    SegmentRule result =
        segmentRuleService.createOrUpdateRule(
            "Standard", "Standard match rules", false, false, BumpAddition.NONE);

    assertThat(result).isSameAs(rule2);
  }

  @Test
  void createOrUpdateRule_existingRuleChanged_updatesRule() {
    rule2.setDescription("Old description");
    rule2.setRequiresHighHeat(false);
    rule2.setNoDq(false);
    rule2.setBumpAddition(BumpAddition.NONE);
    when(segmentRuleRepository.findByName("Standard")).thenReturn(Optional.of(rule2));
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result =
        segmentRuleService.createOrUpdateRule(
            "Standard", "New description", false, false, BumpAddition.NONE);

    assertThat(result.getDescription()).isEqualTo("New description");
    verify(segmentRuleRepository).save(rule2);
  }

  @Test
  void findAll_returnsList() {
    when(segmentRuleRepository.findAll()).thenReturn(List.of(rule1, rule2));

    List<SegmentRule> result = segmentRuleService.findAll();

    assertThat(result).hasSize(2);
    verify(segmentRuleRepository).findAll();
  }

  @Test
  void count_returnsRepositoryCount() {
    when(segmentRuleRepository.count()).thenReturn(5L);

    long result = segmentRuleService.count();

    assertThat(result).isEqualTo(5L);
    verify(segmentRuleRepository).count();
  }

  @Test
  void count_emptyRepository_returnsZero() {
    when(segmentRuleRepository.count()).thenReturn(0L);

    long result = segmentRuleService.count();

    assertThat(result).isZero();
  }

  @Test
  void createOrUpdateRule_withRules_persistsRulesOnNewRule() throws Exception {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview", "setup", null, null, "win", null, null, null, null, null, null, null);
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, null);

    when(segmentRuleRepository.findByName("Cage")).thenReturn(java.util.Optional.empty());
    when(objectMapper.writeValueAsString(guide))
        .thenReturn("{\"solo\":{\"overview\":\"overview\"}}");
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result =
        segmentRuleService.createOrUpdateRule(
            "Cage", "Cage match", true, true, BumpAddition.ALL, "BASE_GAME", guide);

    assertThat(result.getGuide()).isEqualTo(guide);
    assertThat(result.getGuideHash()).isNotBlank();
  }

  @Test
  void createOrUpdateRule_withRules_hashChangeTriggersUpdate() throws Exception {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "old overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide oldGuide = new SegmentRulePlayGuide(solo, null);
    SegmentRuleVariantGuide soloNew =
        new SegmentRuleVariantGuide(
            "new overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide newGuide = new SegmentRulePlayGuide(soloNew, null);

    SegmentRule existing = new SegmentRule();
    existing.setName("Cage");
    existing.setDescription("Cage match");
    existing.setRequiresHighHeat(true);
    existing.setNoDq(true);
    existing.setBumpAddition(BumpAddition.ALL);
    existing.setExpansionCode("BASE_GAME");
    existing.setGuide(oldGuide);
    existing.setGuideHash("oldhash");

    when(segmentRuleRepository.findByName("Cage")).thenReturn(java.util.Optional.of(existing));
    when(objectMapper.writeValueAsString(newGuide))
        .thenReturn("{\"solo\":{\"overview\":\"new overview\"}}");
    when(segmentRuleRepository.save(any(SegmentRule.class))).thenAnswer(inv -> inv.getArgument(0));

    SegmentRule result =
        segmentRuleService.createOrUpdateRule(
            "Cage", "Cage match", true, true, BumpAddition.ALL, "BASE_GAME", newGuide);

    assertThat(result.getGuide()).isEqualTo(newGuide);
    assertThat(result.getGuideHash()).isNotEqualTo("oldhash");
    verify(segmentRuleRepository).save(existing);
  }

  @Test
  void createOrUpdateRule_identicalHash_skipsWrite() throws Exception {
    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "overview", null, null, null, null, null, null, null, null, null, null, null);
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, null);
    String serialized = "{\"solo\":{\"overview\":\"overview\"}}";

    when(objectMapper.writeValueAsString(guide)).thenReturn(serialized);

    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(serialized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String expectedHash = java.util.HexFormat.of().formatHex(hashBytes);

    SegmentRule existing = new SegmentRule();
    existing.setName("Cage");
    existing.setDescription("Cage match");
    existing.setRequiresHighHeat(true);
    existing.setNoDq(true);
    existing.setBumpAddition(BumpAddition.ALL);
    existing.setExpansionCode("BASE_GAME");
    existing.setGuide(guide);
    existing.setGuideHash(expectedHash);

    when(segmentRuleRepository.findByName("Cage")).thenReturn(java.util.Optional.of(existing));

    segmentRuleService.createOrUpdateRule(
        "Cage", "Cage match", true, true, BumpAddition.ALL, "BASE_GAME", guide);

    verify(segmentRuleRepository, Mockito.never()).save(any());
  }
}
