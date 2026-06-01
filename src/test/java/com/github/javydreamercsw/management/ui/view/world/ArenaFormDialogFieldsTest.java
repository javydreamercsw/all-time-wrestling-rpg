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
package com.github.javydreamercsw.management.ui.view.world;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class ArenaFormDialogFieldsTest extends AbstractViewTest {

  @Mock private ArenaService arenaService;
  @Mock private LocationService locationService;
  @Mock private ImageStorageService storageService;

  private ArenaFormDialog dialog;

  @BeforeEach
  void setup() {
    when(locationService.findAll()).thenReturn(Collections.emptyList());
    when(arenaService.findByName(anyString())).thenReturn(Optional.empty());
    when(arenaService.resolveArenaImage(null)).thenReturn("images/generic-venue.png");

    dialog = new ArenaFormDialog(arenaService, locationService, storageService, null, () -> {});
  }

  @Test
  @DisplayName("ArenaFormDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "ArenaFormDialog should not be null");
  }

  @Test
  @DisplayName("name field should exist in ArenaFormDialog")
  void nameFieldExists() {
    TextField name = (TextField) ReflectionTestUtils.getField(dialog, "name");
    assertNotNull(name, "name field should not be null");
  }

  @Test
  @DisplayName("capacity IntegerField should exist in ArenaFormDialog")
  void capacityFieldExists() {
    IntegerField capacity = (IntegerField) ReflectionTestUtils.getField(dialog, "capacity");
    assertNotNull(capacity, "capacity field should not be null");
  }

  @Test
  @DisplayName("locationCombo ComboBox should exist in ArenaFormDialog")
  void locationComboExists() {
    ComboBox<?> locationCombo = (ComboBox<?>) ReflectionTestUtils.getField(dialog, "locationCombo");
    assertNotNull(locationCombo, "locationCombo should not be null");
  }

  @Test
  @DisplayName("alignmentBias ComboBox should exist in ArenaFormDialog")
  void alignmentBiasExists() {
    ComboBox<?> alignmentBias = (ComboBox<?>) ReflectionTestUtils.getField(dialog, "alignmentBias");
    assertNotNull(alignmentBias, "alignmentBias should not be null");
  }
}
