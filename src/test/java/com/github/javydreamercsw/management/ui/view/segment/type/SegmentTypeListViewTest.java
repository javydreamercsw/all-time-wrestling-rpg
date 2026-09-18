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
package com.github.javydreamercsw.management.ui.view.segment.type;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
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
import org.mockito.Mockito;

class SegmentTypeListViewTest extends AbstractViewTest {

  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SecurityUtils securityUtils;

  private SegmentTypeListView view;

  @BeforeEach
  void setup() {
    when(segmentTypeService.findAll()).thenReturn(Collections.emptyList());
    when(segmentTypeService.findAllForAdmin()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    view = new SegmentTypeListView(segmentTypeService, securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the segment type grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class);
    assertTrue(grid.isVisible());
  }

  @Test
  @DisplayName("Should render the create segment type button")
  void shouldRenderCreateButton() {
    Button createButton = _get(view, Button.class, spec -> spec.withText("Create Segment Type"));
    assertTrue(createButton.isVisible());
  }

  @Test
  @DisplayName("Saving a new segment type stamps it as CUSTOM content")
  void saveNewSegmentType_stampsCustom() {
    view.openEditDialogForTest(new SegmentType());

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("My Custom Match");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("My Custom Match"), any(), eq("CUSTOM"), isNull(), isNull(), eq(false));
    assertFalse(dialog.isOpened(), "Dialog should close after save");
  }

  @Test
  @DisplayName("Editing an existing segment type preserves its expansion code")
  void saveExistingSegmentType_preservesExpansionCode() {
    SegmentType existing = new SegmentType();
    existing.setId(7L);
    existing.setName("Ladder Match");
    existing.setDescription("Climb and grab.");
    existing.setExpansionCode("BASE_GAME");

    view.openEditDialogForTest(existing);

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("Ladder Match"), any(), eq("BASE_GAME"), isNull(), isNull(), eq(false));
    assertFalse(dialog.isOpened(), "Dialog should close after update");
  }

  @Test
  @DisplayName("Event-only checkbox round-trips through the dialog (ATW-0331)")
  void saveExistingSegmentType_eventOnlyCheckboxRoundTrips() {
    SegmentType existing = new SegmentType();
    existing.setId(9L);
    existing.setName("Abu Dhabi Rumble");
    existing.setDescription("Large-scale elimination match.");
    existing.setExpansionCode("RUMBLE");
    existing.setEventOnly(true);

    view.openEditDialogForTest(existing);

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened());

    // The checkbox reflects the bean state on open (readBean).
    com.vaadin.flow.component.checkbox.Checkbox eventOnly =
        _get(
            UI.getCurrent(),
            com.vaadin.flow.component.checkbox.Checkbox.class,
            spec ->
                spec.withLabel(
                    "Event-only (special PLE event format, excluded from AI proposals)"));
    assertTrue(eventOnly.getValue(), "Checkbox must reflect eventOnly=true on dialog open");
    eventOnly.setValue(false);

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    // The eventOnly flag rides through the binder onto the bean and into the 6-arg service call.
    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("Abu Dhabi Rumble"), any(), eq("RUMBLE"), isNull(), isNull(), eq(false));
    assertFalse(dialog.isOpened(), "Dialog should close after update");
  }

  @Test
  @DisplayName("Failed save keeps the dialog open")
  void saveFailure_keepsDialogOpen() {
    when(segmentTypeService.createOrUpdateSegmentType(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("DB down"));

    view.openEditDialogForTest(new SegmentType());

    TextField name = _get(UI.getCurrent(), TextField.class, spec -> spec.withLabel("Name"));
    name.setValue("Doomed Type");

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Dialog should stay open after a failed save");
  }

  @Test
  @DisplayName("Blank name blocks save and keeps the dialog open")
  void saveBlankName_blocked() {
    view.openEditDialogForTest(new SegmentType());

    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Save")).click();

    verify(segmentTypeService, Mockito.never())
        .createOrUpdateSegmentType(any(), any(), any(), any(), any(), any());
    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertTrue(dialog.isOpened(), "Dialog should stay open on validation failure");
  }
}
