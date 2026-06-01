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
package com.github.javydreamercsw.management.ui.view.holiday;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HolidayFormDialogFieldsTest extends AbstractViewTest {

  private HolidayFormDialog dialog;

  @BeforeEach
  void setup() {
    dialog = new HolidayFormDialog();
  }

  @Test
  @DisplayName("HolidayFormDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "HolidayFormDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain description and theme TextField components")
  void textFieldsExist() {
    List<TextField> textFields = _find(dialog, TextField.class);
    assertFalse(textFields.isEmpty(), "Expected at least one TextField (description, theme)");
  }

  @Test
  @DisplayName("Dialog should contain decorations TextArea")
  void textAreaExists() {
    List<TextArea> textAreas = _find(dialog, TextArea.class);
    assertFalse(textAreas.isEmpty(), "Expected at least one TextArea (decorations)");
  }

  @Test
  @DisplayName("Dialog should contain ComboBox components for type, month, dayOfWeek")
  void comboBoxesExist() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertTrue(combos.size() >= 3, "Expected at least 3 ComboBoxes (type, month, dayOfWeek)");
  }

  @Test
  @DisplayName("Dialog should contain IntegerField components for day/week of month")
  void integerFieldsExist() {
    List<IntegerField> intFields = _find(dialog, IntegerField.class);
    assertTrue(
        intFields.size() >= 2, "Expected at least 2 IntegerFields (dayOfMonth, weekOfMonth)");
  }
}
