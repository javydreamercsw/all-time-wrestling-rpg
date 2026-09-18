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
package com.github.javydreamercsw.management.ui.view.segment.rule;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class SegmentRuleListViewTest extends AbstractViewTest {

  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SecurityUtils securityUtils;

  private SegmentRuleListView view;

  @BeforeEach
  void setup() {
    when(segmentRuleService.findAll()).thenReturn(Collections.emptyList());
    when(segmentRuleService.findAllForAdmin()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    view = new SegmentRuleListView();
    ReflectionTestUtils.setField(view, "segmentRuleService", segmentRuleService);
    ReflectionTestUtils.setField(view, "securityUtils", securityUtils);
    ReflectionTestUtils.invokeMethod(view, "initializeUI");
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the segment rule grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the create segment rule button")
  void shouldRenderCreateButton() {
    Button createButton = _get(view, Button.class, spec -> spec.withText("Create Segment Rule"));
    assertTrue(createButton.isVisible());
  }

  @Test
  @DisplayName("Saving a new segment rule stamps it as CUSTOM content")
  void saveNewSegmentRule_stampsCustom() {
    view.openEditDialogForTest(new SegmentRule());

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("My Custom Rule");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentRuleService)
        .createOrUpdateRule(
            eq("My Custom Rule"), any(), anyBoolean(), anyBoolean(), any(), eq("CUSTOM"));
    assertFalse(dialog.isOpened(), "Dialog should close after save");
  }

  @Test
  @DisplayName("Editing an existing segment rule preserves its expansion code")
  void saveExistingSegmentRule_preservesExpansionCode() {
    SegmentRule existing = new SegmentRule();
    existing.setId(3L);
    existing.setName("Ladder Match");
    existing.setDescription("Climb and grab.");
    existing.setExpansionCode("BASE_GAME");

    view.openEditDialogForTest(existing);

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentRuleService)
        .createOrUpdateRule(
            eq("Ladder Match"), any(), anyBoolean(), anyBoolean(), any(), eq("BASE_GAME"));
    assertFalse(dialog.isOpened(), "Dialog should close after update");
  }

  @Test
  @DisplayName("Failed save keeps the dialog open")
  void saveFailure_keepsDialogOpen() {
    when(segmentRuleService.createOrUpdateRule(
            any(), any(), anyBoolean(), anyBoolean(), any(), any()))
        .thenThrow(new RuntimeException("DB down"));

    view.openEditDialogForTest(new SegmentRule());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("Doomed Rule");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Dialog should stay open after a failed save");
  }

  @Test
  @DisplayName("Blank name blocks save and keeps the dialog open")
  void saveBlankName_blocked() {
    view.openEditDialogForTest(new SegmentRule());

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentRuleService, org.mockito.Mockito.never())
        .createOrUpdateRule(any(), any(), anyBoolean(), anyBoolean(), any(), any());
    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Dialog should stay open on validation failure");
  }

  @Test
  @DisplayName("Bump addition edits round-trip through the dialog")
  void saveRule_withBumpAddition() {
    view.openEditDialogForTest(new SegmentRule());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("Bump Rule");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentRuleService)
        .createOrUpdateRule(
            eq("Bump Rule"),
            any(),
            anyBoolean(),
            anyBoolean(),
            eq(BumpAddition.NONE),
            eq("CUSTOM"));
  }
}
