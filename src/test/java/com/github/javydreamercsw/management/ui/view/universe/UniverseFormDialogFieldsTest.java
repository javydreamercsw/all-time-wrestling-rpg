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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class UniverseFormDialogFieldsTest extends AbstractViewTest {

  @Mock private UniverseService universeService;

  private UniverseFormDialog dialog;

  @BeforeEach
  void setup() {
    Universe universe = new Universe();
    dialog = new UniverseFormDialog(universeService, universe, () -> {});
  }

  @Test
  @DisplayName("UniverseFormDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "UniverseFormDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain name TextField")
  void nameFieldExists() {
    List<TextField> fields = _find(dialog, TextField.class);
    assertFalse(fields.isEmpty(), "Expected at least one TextField (name)");
  }

  @Test
  @DisplayName("Dialog should contain type ComboBox")
  void typeComboBoxExists() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertFalse(combos.isEmpty(), "Expected at least one ComboBox (type)");
  }
}
