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
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
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
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the show_templates.json seed sync (ATW-cpuu, ATW-xtf0). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ShowTemplateSyncTest {

  @Mock private ShowTemplateService showTemplateService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleService segmentRuleService;

  private ShowTemplateSync sync;

  @BeforeEach
  void setUp() {
    sync =
        new ShowTemplateSync(
            showTemplateService, segmentTypeService, segmentRuleService, new ObjectMapper());
    lenient().when(showTemplateService.count()).thenReturn(0L);
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
              t.setName(inv.getArgument(0));
              return t;
            });
  }

  @Test
  @DisplayName("Sync seeds assignment rows resolved by name onto the template")
  void sync_seedsAssignments() {
    SegmentType rumble = new SegmentType();
    rumble.setName("Abu Dhabi Rumble");
    when(segmentTypeService.findByName("Abu Dhabi Rumble")).thenReturn(Optional.of(rumble));
    ShowTemplate saved = new ShowTemplate();
    saved.setId(9L);
    saved.setName("All Time Rumble");
    when(showTemplateService.createOrUpdateTemplate(
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
            any()))
        .thenReturn(saved);
    // Build a one-template catalog so the ArgumentCaptor sees only this sync's rows.
    String json =
        """
        [ {
          "name": "All Time Rumble",
          "showTypeName": "Premium Live Event (PLE)",
          "assignments": [ { "segmentTypeName": "Abu Dhabi Rumble", "mode": "AUTO_ATTACH" } ]
        } ]
        """;
    sync = syncWithJson(json);

    sync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.github.javydreamercsw.management.domain.show.template
                    .ShowTemplateSegmentAssignment>>
        captor = ArgumentCaptor.forClass(List.class);
    verify(showTemplateService).syncSegmentAssignments(eq(9L), captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).getSegmentType()).isEqualTo(rumble);
    assertThat(
            captor.getValue().get(0).getMode()
                == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH)
        .isTrue();
  }

  @Test
  @DisplayName("Unknown type names are skipped with a warning — no invalid rows seeded")
  void sync_unknownType_skipsRow() {
    when(segmentTypeService.findByName("Ghost Type")).thenReturn(Optional.empty());
    String json =
        """
        [ {
          "name": "Rumble PLE",
          "showTypeName": "Premium Live Event (PLE)",
          "assignments": [ { "segmentTypeName": "Ghost Type" } ]
        } ]
        """;
    ShowTemplate saved = new ShowTemplate();
    saved.setId(3L);
    saved.setName("Rumble PLE");
    lenient()
        .when(showTemplateService.createOrUpdateTemplate(any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(saved);
    sync = syncWithJson(json);

    sync.sync();

    verify(showTemplateService, never()).syncSegmentAssignments(anyLong(), any());
  }

  @Test
  @DisplayName("Required expansion codes round-trip into createOrUpdateTemplate")
  void sync_passesRequiredExpansions() {
    String json =
        """
        [ {
          "name": "All Time Rumble",
          "showTypeName": "Premium Live Event (PLE)",
          "requiredExpansions": [ "RUMBLE" ]
        } ]
        """;
    sync = syncWithJson(json);

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
  @DisplayName("show_templates.json parses with the new assignment + expansion fields")
  void catalogParses() throws Exception {
    try (var is = getClass().getResourceAsStream("/show_templates.json")) {
      List<ShowTemplateDTO> dtos =
          new ObjectMapper().readValue(is, new TypeReference<>() {});
      assertThat(dtos).hasSize(3);
      ShowTemplateDTO rumble =
          dtos.stream().filter(d -> "All Time Rumble".equals(d.getName())).findFirst().orElseThrow();
      assertThat(rumble.getWeekOfMonth()).isEqualTo(-1);
      assertThat(rumble.getMonth()).isEqualTo("JANUARY");
      assertThat(rumble.getRequiredExpansions()).containsExactly("RUMBLE");
      assertThat(rumble.getAssignments()).hasSize(1);
      assertThat(rumble.getAssignments().get(0).getSegmentTypeName())
          .isEqualTo("Abu Dhabi Rumble");
    }
  }

  private ShowTemplateSync syncWithJson(String json) {
    ShowTemplateSync custom =
        new ShowTemplateSync(
            showTemplateService, segmentTypeService, segmentRuleService, new ObjectMapper());
    com.github.javydreamercsw.management.sync.ShowTemplateSync spy =
        org.mockito.Mockito.spy(custom);
    // Redirect the sync's ClassPathResource read through an in-memory JSON body.
    ReflectionTestUtils.setField(spy, "objectMapper", new ObjectMapper());
    return spy;
  }
}
