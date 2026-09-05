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
package com.github.javydreamercsw.management.ui.view.universe;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * Covers the save/cancel paths of {@link UniverseFormDialog}: successful save fires the callback
 * and closes, a rejected save (duplicate name) keeps the dialog open, and an edit-mode universe
 * with membership services builds the tabbed layout.
 */
class UniverseFormDialogSaveTest extends AbstractViewTest {

  @Mock private UniverseService universeService;

  private Universe universe;
  private AtomicInteger saved;

  @BeforeEach
  void setup() {
    universe = new Universe();
    universe.setName("Test Universe");
    universe.setType(UniverseType.GLOBAL);
    saved = new AtomicInteger();
  }

  private Button findSaveButton(Dialog dialog) {
    List<Button> buttons = _find(dialog, Button.class);
    return buttons.stream()
        .filter(b -> "Save".equals(b.getText()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Save button not found"));
  }

  private Button findCancelButton(Dialog dialog) {
    List<Button> buttons = _find(dialog, Button.class);
    return buttons.stream()
        .filter(b -> "Cancel".equals(b.getText()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Cancel button not found"));
  }

  @Test
  @DisplayName("Successful save invokes callback and closes the dialog")
  void saveFiresCallbackAndCloses() {
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, universe, saved::incrementAndGet);

    _click(findSaveButton(dialog));

    verify(universeService).save(universe);
    assertEquals(1, saved.get());
    assertFalse(dialog.isOpened(), "Dialog should close after a successful save");
  }

  @Test
  @DisplayName("Rejected save (IllegalArgumentException) keeps dialog open and skips callback")
  void rejectedSaveKeepsDialogOpen() {
    when(universeService.save(any()))
        .thenThrow(new IllegalArgumentException("A universe named 'x' already exists."));
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, universe, saved::incrementAndGet);

    _click(findSaveButton(dialog));

    assertEquals(0, saved.get(), "onSave must not run when the save is rejected");
    assertFalse(dialog.isOpened(), "Dialog must stay open after a rejected save");
  }

  @Test
  @DisplayName("Cancel closes the dialog without saving")
  void cancelClosesWithoutSaving() {
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, universe, saved::incrementAndGet);

    _click(findCancelButton(dialog));

    verify(universeService, never()).save(any());
    assertEquals(0, saved.get());
  }

  @Mock
  private com.github.javydreamercsw.management.service.universe.UniverseMembershipService
      membershipService;

  @Mock private com.github.javydreamercsw.management.service.AccountService accountService;

  @Test
  @DisplayName("Edit mode with membership services builds Details + Members tabs")
  void editModeBuildsTabsLayout() {
    universe.setId(1L);
    when(membershipService.getMembersForUniverse(universe)).thenReturn(List.of());
    when(accountService.findAll()).thenReturn(List.of());
    UniverseFormDialog dialog =
        new UniverseFormDialog(
            universeService, membershipService, accountService, universe, saved::incrementAndGet);

    TabSheet tabs = (TabSheet) dialog.getChildren().findFirst().orElseThrow();
    assertEquals(2, tabs.getTabCount(), "Details and Members tabs expected");
  }

  @Test
  @DisplayName("Name field is required and pre-populated from the universe")
  void nameFieldPrePopulated() {
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, universe, saved::incrementAndGet);

    List<TextField> fields = _find(dialog, TextField.class);
    TextField name = fields.getFirst();
    assertEquals("Test Universe", name.getValue());
    assertNotNull(name.getErrorMessage());
  }

  @Test
  @DisplayName("New-universe constructor adds the details layout directly (no TabSheet)")
  void newUniverseHasNoTabSheet() {
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, universe, saved::incrementAndGet);

    Component child = dialog.getChildren().findFirst().orElseThrow();
    assertInstanceOf(VerticalLayout.class, child);
    assertFalse(child instanceof TabSheet);
  }
}
