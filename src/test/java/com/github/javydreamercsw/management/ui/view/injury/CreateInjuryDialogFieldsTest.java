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
package com.github.javydreamercsw.management.ui.view.injury;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CreateInjuryDialogFieldsTest extends AbstractViewTest {

  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private SecurityUtils securityUtils;

  private CreateInjuryDialog dialog;

  @BeforeEach
  void setup() {
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canCreate()).thenReturn(true);

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    dialog =
        new CreateInjuryDialog(
            wrestler, 1L, injuryService, injuryTypeService, () -> {}, securityUtils);
  }

  @Test
  @DisplayName("CreateInjuryDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "CreateInjuryDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain name TextField")
  void nameFieldExists() {
    List<TextField> fields = _find(dialog, TextField.class);
    assertFalse(fields.isEmpty(), "Expected at least one TextField (name)");
  }

  @Test
  @DisplayName("Dialog should contain description and notes TextArea")
  void textAreasExist() {
    List<TextArea> areas = _find(dialog, TextArea.class);
    assertFalse(areas.isEmpty(), "Expected at least one TextArea (description, notes)");
  }

  @Test
  @DisplayName("Dialog should contain severity ComboBox")
  void severityComboBoxExists() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertFalse(combos.isEmpty(), "Expected at least one ComboBox (severity)");
  }
}
