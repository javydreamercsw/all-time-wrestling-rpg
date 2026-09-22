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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowContextFacade;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

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
  @Mock private ShowContextFacade showContextFacade;
  @Mock private TournamentService tournamentService;
  @Mock private WrestlerFacade wrestlerFacade;

  private ShowTemplateListView view;

  @BeforeEach
  void setup() {
    lenient().when(showTypeService.findAll()).thenReturn(List.of(showType("PLE")));
    lenient().when(showTemplateService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(commentaryTeamRepository.findAll()).thenReturn(Collections.emptyList());
    lenient().when(segmentTypeService.findAllForAdmin()).thenReturn(Collections.emptyList());
    lenient().when(segmentRuleService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(tournamentService.findAll()).thenReturn(Collections.emptyList());
    lenient().when(tournamentService.getAvailableFormats()).thenReturn(Collections.emptyList());
    lenient()
        .when(wrestlerFacade.getTitleService())
        .thenReturn(
            Mockito.mock(TitleService.class));
    lenient().when(wrestlerFacade.getTitleService().findAll()).thenReturn(Collections.emptyList());
    lenient().when(showContextFacade.getTournamentService()).thenReturn(tournamentService);
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
            segmentRuleService,
            showContextFacade,
            wrestlerFacade);
    UI.getCurrent().add(view);
  }

  private static ShowType showType(String name) {
    ShowType t = new ShowType();
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
    when(showTemplateService.getTemplateWithAssignments(1L)).thenReturn(Optional.of(template));

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
    when(showTemplateService.getTemplateWithAssignments(3L)).thenReturn(Optional.of(template));

    view.openEditDialogForTest(template);
    view.addAssignmentForTest(
        null, rule("Rumble Rules"), ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    view.saveTemplateForTest();

    Mockito.verify(showTemplateService)
        .syncSegmentAssignments(
            ArgumentMatchers.eq(3L),
            ArgumentMatchers.argThat(
                rows ->
                    rows.size() == 1
                        && "Rumble Rules".equals(rows.get(0).getSegmentRule().getName())
                        && rows.get(0).getMode()
                            == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH));
  }

  private static SegmentRule rule(String name) {
    SegmentRule r = new SegmentRule();
    r.setName(name);
    return r;
  }

  @Test
  @DisplayName("Saving a type+tournament assignment row round-trips the tournament (ATW-oahn)")
  void saveTemplate_syncsTournamentAssignment() {
    ShowTemplate template = new ShowTemplate();
    template.setId(4L);
    template.setName("Tournament PLE");
    template.setShowType(showType("PLE"));
    when(showTemplateService.getTemplateWithAssignments(4L)).thenReturn(Optional.of(template));

    SegmentType rumble = new SegmentType();
    rumble.setId(1L);
    rumble.setName("Abu Dhabi Rumble");
    Tournament crownCup = new Tournament();
    crownCup.setId(9L);
    crownCup.setName("Crown Cup");

    view.openEditDialogForTest(template);
    view.addAssignmentForTest(
        rumble, null, crownCup, ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    view.saveTemplateForTest();

    Mockito.verify(showTemplateService)
        .syncSegmentAssignments(
            ArgumentMatchers.eq(4L),
            ArgumentMatchers.argThat(
                rows ->
                    rows.size() == 1
                        && rows.get(0).getTournament() == crownCup
                        && "Abu Dhabi Rumble".equals(rows.get(0).getSegmentType().getName())
                        && rows.get(0).getMode()
                            == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH));
  }

  private static void assertEquals(int expected, int actual, String message) {
    Assertions.assertEquals(expected, actual, message);
  }

  @Test
  @DisplayName("Saving a spec assignment row round-trips the full spec (ATW-etws)")
  void saveTemplate_syncsSpecAssignment() {
    ShowTemplate template = new ShowTemplate();
    template.setId(5L);
    template.setName("Spec PLE");
    template.setShowType(showType("PLE"));
    when(showTemplateService.getTemplateWithAssignments(5L)).thenReturn(Optional.of(template));

    view.openEditDialogForTest(template);
    view.addSpecAssignmentForTest(
        "Deadly Combat",
        "SINGLE_ELIMINATION",
        8,
        ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    view.saveTemplateForTest();

    Mockito.verify(showTemplateService)
        .syncSegmentAssignments(
            ArgumentMatchers.eq(5L),
            ArgumentMatchers.argThat(
                rows ->
                    rows.size() == 1
                        && "Deadly Combat".equals(rows.get(0).getSpecName())
                        && "SINGLE_ELIMINATION".equals(rows.get(0).getSpecFormatId())
                        && Integer.valueOf(8).equals(rows.get(0).getSpecEntrantCount())
                        && rows.get(0).getMode()
                            == ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH));
  }

  @Test
  @DisplayName("Edit dialog preserves the tournament reference and spec fields (ATW-etws bug)")
  void editDialog_preservesTournamentAndSpecFields() {
    // Regression for the openEditDialog copy loop dropping tournament/spec on re-save.
    ShowTemplate template = new ShowTemplate();
    template.setId(6L);
    template.setName("Copy PLE");
    template.setShowType(showType("PLE"));
    Tournament existing = new Tournament();
    existing.setId(9L);
    existing.setName("Crown Cup");
    SegmentRule finalRule = rule("Barbwire Exploding Deathmatch");
    ShowTemplateSegmentAssignment specRow = new ShowTemplateSegmentAssignment();
    specRow.setTemplate(template);
    specRow.setTournament(existing);
    specRow.setSpecName("Deadly Combat");
    specRow.setSpecFormatId("SINGLE_ELIMINATION");
    specRow.setSpecEntrantCount(8);
    specRow.setSpecFinalRule(finalRule);
    specRow.getSpecAllowedRules().add(rule("No DQ"));
    specRow.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    template.getSegmentAssignments().add(specRow);
    when(showTemplateService.getTemplateWithAssignments(6L)).thenReturn(Optional.of(template));

    view.openEditDialogForTest(template);
    view.saveTemplateForTest();

    Mockito.verify(showTemplateService)
        .syncSegmentAssignments(
            ArgumentMatchers.eq(6L),
            ArgumentMatchers.argThat(
                rows ->
                    rows.size() == 1
                        && rows.get(0).getTournament() == existing
                        && "Deadly Combat".equals(rows.get(0).getSpecName())
                        && "SINGLE_ELIMINATION".equals(rows.get(0).getSpecFormatId())
                        && Integer.valueOf(8).equals(rows.get(0).getSpecEntrantCount())
                        && "Barbwire Exploding Deathmatch"
                            .equals(rows.get(0).getSpecFinalRule().getName())
                        && rows.get(0).getSpecAllowedRules().stream()
                            .anyMatch(r -> "No DQ".equals(r.getName()))));
  }
}
