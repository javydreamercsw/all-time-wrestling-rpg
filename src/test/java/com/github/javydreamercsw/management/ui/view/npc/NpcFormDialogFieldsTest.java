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
package com.github.javydreamercsw.management.ui.view.npc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class NpcFormDialogFieldsTest extends AbstractViewTest {

  @Mock private NpcService npcService;
  @Mock private ImageStorageService imageStorageService;

  private NpcFormDialog dialog;

  @BeforeEach
  void setup() {
    Npc npc = new Npc();
    npc.setName("Test NPC");
    dialog = new NpcFormDialog(npcService, imageStorageService, npc, () -> {});
  }

  @Test
  @DisplayName("nameField should exist in NpcFormDialog")
  void nameFieldExists() {
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "nameField");
    assertNotNull(nameField, "nameField should not be null");
  }

  @Test
  @DisplayName("npcTypeField ComboBox should exist in NpcFormDialog")
  void npcTypeFieldExists() {
    com.vaadin.flow.component.combobox.ComboBox<?> typeField =
        (com.vaadin.flow.component.combobox.ComboBox<?>)
            ReflectionTestUtils.getField(dialog, "npcTypeField");
    assertNotNull(typeField, "npcTypeField should not be null");
  }

  @Test
  @DisplayName("descriptionField TextArea should exist in NpcFormDialog")
  void descriptionFieldExists() {
    com.vaadin.flow.component.textfield.TextArea descField =
        (com.vaadin.flow.component.textfield.TextArea)
            ReflectionTestUtils.getField(dialog, "descriptionField");
    assertNotNull(descField, "descriptionField should not be null");
  }
}
