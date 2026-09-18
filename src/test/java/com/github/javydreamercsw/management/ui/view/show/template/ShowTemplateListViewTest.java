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
package com.github.javydreamercsw.management.ui.view.show.template;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ShowTemplateListViewTest extends AbstractViewTest {

  @Mock private ShowTemplateService showTemplateService;
  @Mock private ShowTypeService showTypeService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageGenerationServiceFactory imageGenerationServiceFactory;
  @Mock private ImageStorageService imageStorageService;
  @Mock private AiSettingsService aiSettingsService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleService segmentRuleService;

  private ShowTemplateListView view;

  @BeforeEach
  void setup() {
    lenient().when(showTypeService.findAll()).thenReturn(List.of(showType("PLE")));
    lenient().when(showTemplateService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(commentaryTeamRepository.findAll()).thenReturn(Collections.emptyList());
    lenient().when(segmentTypeService.findAllForAdmin()).thenReturn(Collections.emptyList());
    lenient().when(segmentRuleService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(securityUtils.canCreate()).thenReturn(true);
    lenient().when(securityUtils.canEdit()).thenReturn(true);

    view =
        new ShowTemplateListView(
            showTemplateService,
            showTypeService,
            commentaryTeamRepository,
            securityUtils,
            imageGenerationServiceFactory,
            imageStorageService,
            aiSettingsService,
            segmentTypeService,
            segmentRuleService);
    UI.getCurrent().add(view);
  }

  private static com.github.javydreamercsw.management.domain.show.type.ShowType showType(
      String name) {
    com.github.javydreamercsw.management.domain.show.type.ShowType t =
        new com.github.javydreamercsw.management.domain.show.type.ShowType();
    t.setName(name);
    return t;
  }

  @Test
  @DisplayName("Should render the Show Templates toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName(
      "Grid should have Commentary Team, Recurrence, Duration, Matches, and Promos columns")
  void shouldHaveNewGridColumns() {
    Grid<?> grid = _get(view, Grid.class);
    List<String> headers =
        grid.getColumns().stream()
            .map(Grid.Column::getHeaderText)
            .filter(h -> h != null && !h.isEmpty())
            .collect(Collectors.toList());
    assertTrue(headers.contains("Commentary Team"));
    assertTrue(headers.contains("Recurrence"));
    assertTrue(headers.contains("Duration"));
    assertTrue(headers.contains("Matches"));
    assertTrue(headers.contains("Promos"));
  }

  @Test
  @DisplayName("Edit dialog loads template assignments into the grid (ATW-0331)")
  void editDialog_loadsAssignments() {
    ShowTemplate template = new ShowTemplate();
    template.setId(1L);
    template.setName("Big PLE");
    template.setShowType(showType("PLE"));
    SegmentType rumble = new SegmentType();
    rumble.setId(1L);
    rumble.setName("Abu Dhabi Rumble");
    rumble.setEventOnly(true);
    SegmentRule rumbleRules = new SegmentRule();
    rumbleRules.setId(2L);
    rumbleRules.setName("Rumble Rules");
    ShowTemplateSegmentAssignment assignment = new ShowTemplateSegmentAssignment();
    assignment.setTemplate(template);
    assignment.setSegmentType(rumble);
    assignment.setSegmentRule(rumbleRules);
    assignment.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    template.getSegmentAssignments().add(assignment);

    view.openEditDialogForTest(template);

    Grid<?> assignments = view.getAssignmentGridForTest();
    assertEquals(1, assignments.getListDataView().getItemCount(), "Assignment row must load");
  }

  @Test
  @DisplayName("Saving syncs dialog assignment rows onto the template (ATW-0331)")
  void saveTemplate_syncsAssignments() {
    ShowTemplate template = new ShowTemplate();
    template.setId(3L);
    template.setName("Big PLE");
    template.setShowType(showType("PLE"));
    when(showTemplateService.getTemplateById(3L)).thenReturn(Optional.of(template));
    when(showTemplateService.save(template)).thenReturn(template);

    view.openEditDialogForTest(template);
    view.addAssignmentForTest(
        null, rule("Rumble Rules"), ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    view.saveTemplateForTest();

    assertEquals(
        1,
        template.getSegmentAssignments().size(),
        "Dialog assignment rows must sync onto the template");
  }

  private static SegmentRule rule(String name) {
    SegmentRule r = new SegmentRule();
    r.setName(name);
    return r;
  }

  private static void assertEquals(int expected, int actual, String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
  }
}
