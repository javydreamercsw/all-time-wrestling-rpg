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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the show_templates.json seed sync (ATW-cpuu, ATW-xtf0). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowTemplateSyncTest {

  @Mock private ShowTemplateService showTemplateService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private TournamentRepository tournamentRepository;

  private ShowTemplateSync sync;

  @BeforeEach
  void setUp() {
    lenient()
        .when(
            showTemplateService.createOrUpdateTemplate(
                anyString(),
                any(),
                anyString(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
        .thenAnswer(
            inv -> {
              ShowTemplate t = new ShowTemplate();
              t.setId(9L);
              t.setName(inv.getArgument(0));
              return t;
            });
    lenient().when(segmentRuleService.findAll()).thenReturn(List.of());
  }

  /** Sync over a caller-supplied catalog instead of the classpath file. */
  private ShowTemplateSync syncOver(List<ShowTemplateDTO> dtos) {
    return new ShowTemplateSync(
        showTemplateService,
        segmentTypeService,
        segmentRuleService,
        tournamentRepository,
        new ObjectMapper()) {
      @Override
      protected List<ShowTemplateDTO> loadCatalog() {
        return dtos;
      }
    };
  }

  @Test
  @DisplayName("Sync seeds assignment rows resolved by name onto the template")
  void sync_seedsAssignments() {
    SegmentType rumble = new SegmentType();
    rumble.setName("Abu Dhabi Rumble");
    when(segmentTypeService.findByName("Abu Dhabi Rumble")).thenReturn(Optional.of(rumble));
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("All Time Rumble");
    dto.setShowTypeName("Premium Live Event (PLE)");
    ShowTemplateDTO.AssignmentDTO assignment = new ShowTemplateDTO.AssignmentDTO();
    assignment.setSegmentTypeName("Abu Dhabi Rumble");
    assignment.setMode("AUTO_ATTACH");
    dto.setAssignments(List.of(assignment));
    sync = syncOver(List.of(dto));

    sync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ShowTemplateSegmentAssignment>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(showTemplateService).syncSegmentAssignments(eq(9L), captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).getSegmentType()).isEqualTo(rumble);
    assertThat(captor.getValue().get(0).getMode())
        .isEqualTo(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
  }

  @Test
  @DisplayName("Unknown type names are skipped with a warning — no invalid rows seeded")
  void sync_unknownType_skipsRow() {
    when(segmentTypeService.findByName("Ghost Type")).thenReturn(Optional.empty());
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("Rumble PLE");
    dto.setShowTypeName("Premium Live Event (PLE)");
    ShowTemplateDTO.AssignmentDTO assignment = new ShowTemplateDTO.AssignmentDTO();
    assignment.setSegmentTypeName("Ghost Type");
    dto.setAssignments(List.of(assignment));
    sync = syncOver(List.of(dto));

    sync.sync();

    verify(showTemplateService, never()).syncSegmentAssignments(anyLong(), anyList());
  }

  @Test
  @DisplayName("Required expansion codes round-trip into createOrUpdateTemplate")
  void sync_passesRequiredExpansions() {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("All Time Rumble");
    dto.setShowTypeName("Premium Live Event (PLE)");
    dto.setRequiredExpansions(List.of("RUMBLE"));
    sync = syncOver(List.of(dto));

    sync.sync();

    verify(showTemplateService)
        .createOrUpdateTemplate(
            eq("All Time Rumble"),
            any(),
            anyString(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(List.of("RUMBLE")));
  }

  @Test
  @DisplayName("Rule-only rows resolve the rule by name")
  void sync_ruleOnlyRow() {
    SegmentRule noDq = new SegmentRule();
    noDq.setName("No DQ");
    when(segmentRuleService.findByName("No DQ")).thenReturn(Optional.of(noDq));
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("Weekly");
    dto.setShowTypeName("Weekly");
    ShowTemplateDTO.AssignmentDTO assignment = new ShowTemplateDTO.AssignmentDTO();
    assignment.setSegmentRuleName("No DQ");
    dto.setAssignments(List.of(assignment));
    sync = syncOver(List.of(dto));

    sync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ShowTemplateSegmentAssignment>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(showTemplateService).syncSegmentAssignments(eq(9L), captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).getSegmentRule()).isEqualTo(noDq);
  }

  @Test
  @DisplayName("Tournament-code rows resolve through TournamentSync's catalog code")
  void sync_tournamentCodeRow() {
    Tournament deadly = new Tournament();
    deadly.setId(77L);
    deadly.setName("Deadly Combat");
    deadly.setCode("deadly_combat");
    when(tournamentRepository.findByCode("deadly_combat")).thenReturn(Optional.of(deadly));
    SegmentType singles = new SegmentType();
    singles.setName("One on One");
    when(segmentTypeService.findByName("One on One")).thenReturn(Optional.of(singles));
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("Valentine's Day Massacre");
    dto.setShowTypeName("Premium Live Event (PLE)");
    ShowTemplateDTO.AssignmentDTO assignment = new ShowTemplateDTO.AssignmentDTO();
    assignment.setSegmentTypeName("One on One");
    assignment.setTournamentCode("deadly_combat");
    assignment.setMode("AUTO_ATTACH");
    dto.setAssignments(List.of(assignment));
    sync = syncOver(List.of(dto));

    sync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ShowTemplateSegmentAssignment>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(showTemplateService).syncSegmentAssignments(eq(9L), captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).getSegmentType()).isEqualTo(singles);
    assertThat(captor.getValue().get(0).getTournament()).isEqualTo(deadly);
    assertThat(captor.getValue().get(0).getMode())
        .isEqualTo(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
  }

  @Test
  @DisplayName("Spec rows resolve the final rule and allowed pool by name")
  void sync_specRow_resolvesRules() {
    SegmentRule barbwire = new SegmentRule();
    barbwire.setName("Barbwire Exploding Deathmatch");
    SegmentRule lms = new SegmentRule();
    lms.setName("Last Man Standing");
    SegmentRule cage = new SegmentRule();
    cage.setName("Cage");
    SegmentRule noDq = new SegmentRule();
    noDq.setName("No DQ");
    when(segmentRuleService.findByName("Barbwire Exploding Deathmatch"))
        .thenReturn(Optional.of(barbwire));
    when(segmentRuleService.findByName("Last Man Standing")).thenReturn(Optional.of(lms));
    when(segmentRuleService.findByName("Cage")).thenReturn(Optional.of(cage));
    when(segmentRuleService.findByName("No DQ")).thenReturn(Optional.of(noDq));
    SegmentType singles = new SegmentType();
    singles.setName("One on One");
    when(segmentTypeService.findByName("One on One")).thenReturn(Optional.of(singles));
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName("Valentine's Day Massacre");
    dto.setShowTypeName("Premium Live Event (PLE)");
    ShowTemplateDTO.AssignmentDTO assignment = new ShowTemplateDTO.AssignmentDTO();
    assignment.setSegmentTypeName("One on One");
    assignment.setSpecName("Deadly Combat");
    assignment.setSpecFormatId("SINGLE_ELIMINATION");
    assignment.setSpecEntrantCount(8);
    assignment.setSpecFinalRuleName("Barbwire Exploding Deathmatch");
    assignment.setAllowedRuleNames(List.of("Last Man Standing", "Cage", "No DQ"));
    assignment.setMode("AUTO_ATTACH");
    dto.setAssignments(List.of(assignment));
    sync = syncOver(List.of(dto));

    sync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ShowTemplateSegmentAssignment>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(showTemplateService).syncSegmentAssignments(eq(9L), captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    ShowTemplateSegmentAssignment row = captor.getValue().get(0);
    assertThat(row.getSpecName()).isEqualTo("Deadly Combat");
    assertThat(row.getSpecFormatId()).isEqualTo("SINGLE_ELIMINATION");
    assertThat(row.getSpecEntrantCount()).isEqualTo(8);
    assertThat(row.getSpecFinalRule()).isEqualTo(barbwire);
    assertThat(row.getSpecAllowedRules()).containsExactly(lms, cage, noDq);
    assertThat(row.getTournament()).isNull();
  }

  @Test
  @DisplayName("show_templates.json parses with the new assignment + expansion fields")
  void catalogParses() throws Exception {
    try (var is = getClass().getResourceAsStream("/show_templates.json")) {
      List<ShowTemplateDTO> dtos = new ObjectMapper().readValue(is, new TypeReference<>() {});
      assertThat(dtos).hasSize(4);
      ShowTemplateDTO rumble =
          dtos.stream()
              .filter(d -> "All Time Rumble".equals(d.getName()))
              .findFirst()
              .orElseThrow();
      assertThat(rumble.getWeekOfMonth()).isEqualTo(-1);
      assertThat(rumble.getMonth()).isEqualTo("JANUARY");
      assertThat(rumble.getRequiredExpansions()).containsExactly("RUMBLE");
      assertThat(rumble.getAssignments()).hasSize(1);
      assertThat(rumble.getAssignments().get(0).getSegmentTypeName()).isEqualTo("Abu Dhabi Rumble");
      assertThat(rumble.getAssignments().get(0).getMode()).isEqualTo("AUTO_ATTACH");
      ShowTemplateDTO massacre =
          dtos.stream()
              .filter(d -> "Valentine's Day Massacre".equals(d.getName()))
              .findFirst()
              .orElseThrow();
      assertThat(massacre.getWeekOfMonth()).isEqualTo(2);
      assertThat(massacre.getMonth()).isEqualTo("FEBRUARY");
      assertThat(massacre.getAssignments()).hasSize(1);
      assertThat(massacre.getAssignments().get(0).getSpecName()).isEqualTo("Deadly Combat");
      assertThat(massacre.getAssignments().get(0).getSpecFormatId())
          .isEqualTo("SINGLE_ELIMINATION");
      assertThat(massacre.getAssignments().get(0).getSpecEntrantCount()).isEqualTo(8);
      assertThat(massacre.getAssignments().get(0).getSpecFinalRuleName())
          .isEqualTo("Barbwire Exploding Deathmatch");
      assertThat(massacre.getAssignments().get(0).getAllowedRuleNames())
          .containsExactly("Last Man Standing", "Cage", "No DQ");
      assertThat(massacre.getAssignments().get(0).getSegmentTypeName()).isEqualTo("One on One");
    }
  }
}
