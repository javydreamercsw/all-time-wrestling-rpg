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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class EditShowNameDialogFieldsTest extends AbstractViewTest {

  @Mock private ShowService showService;

  private EditShowNameDialog dialog;

  @BeforeEach
  void setup() {
    ShowType showType = new ShowType();
    showType.setName("Weekly");

    Show show = new Show();
    show.setName("Monday Night Show");
    show.setType(showType);
    show.setShowDate(LocalDate.now());

    dialog = new EditShowNameDialog(showService, show);
  }

  @Test
  @DisplayName("nameField should exist in EditShowNameDialog")
  void nameFieldExists() {
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "nameField");
    assertNotNull(nameField, "nameField should not be null");
  }

  @Test
  @DisplayName("nameField should be pre-populated with the show name")
  void nameFieldIsPopulated() {
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "nameField");
    assertNotNull(nameField, "nameField should not be null");
    assertNotNull(nameField.getValue(), "nameField value should not be null");
  }
}
