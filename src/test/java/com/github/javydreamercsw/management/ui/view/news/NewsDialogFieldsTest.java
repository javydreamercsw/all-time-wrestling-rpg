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
package com.github.javydreamercsw.management.ui.view.news;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NewsDialogFieldsTest extends AbstractViewTest {

  @Mock private NewsService newsService;

  private NewsDialog dialog;

  @BeforeEach
  void setup() {
    dialog = new NewsDialog(newsService, () -> {});
  }

  @Test
  @DisplayName("NewsDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "NewsDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain headline TextField")
  void headlineFieldExists() {
    List<TextField> fields = _find(dialog, TextField.class);
    assertFalse(fields.isEmpty(), "Expected at least one TextField (headline)");
  }

  @Test
  @DisplayName("Dialog should contain content TextArea")
  void contentTextAreaExists() {
    List<TextArea> areas = _find(dialog, TextArea.class);
    assertFalse(areas.isEmpty(), "Expected at least one TextArea (content)");
  }

  @Test
  @DisplayName("Dialog should contain category ComboBox")
  void categoryComboBoxExists() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertFalse(combos.isEmpty(), "Expected at least one ComboBox (category)");
  }

  @Test
  @DisplayName("Dialog should contain importance IntegerField")
  void importanceFieldExists() {
    List<IntegerField> intFields = _find(dialog, IntegerField.class);
    assertFalse(intFields.isEmpty(), "Expected at least one IntegerField (importance)");
  }

  @Test
  @DisplayName("Dialog should contain isRumor Checkbox")
  void isRumorCheckboxExists() {
    List<Checkbox> checkboxes = _find(dialog, Checkbox.class);
    assertFalse(checkboxes.isEmpty(), "Expected at least one Checkbox (isRumor)");
  }
}
