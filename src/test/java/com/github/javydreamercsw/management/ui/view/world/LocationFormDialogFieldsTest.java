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
import com.github.javydreamercsw.management.service.world.LocationService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class LocationFormDialogFieldsTest extends AbstractViewTest {

  @Mock private LocationService locationService;
  @Mock private ImageStorageService storageService;

  private LocationFormDialog dialog;

  @BeforeEach
  void setup() {
    when(locationService.findByName(anyString())).thenReturn(Optional.empty());
    when(locationService.resolveLocationImage(null)).thenReturn("images/generic-venue.png");

    dialog = new LocationFormDialog(locationService, storageService, null, () -> {});
  }

  @Test
  @DisplayName("LocationFormDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "LocationFormDialog should not be null");
  }

  @Test
  @DisplayName("name TextField should exist in LocationFormDialog")
  void nameFieldExists() {
    TextField name = (TextField) ReflectionTestUtils.getField(dialog, "name");
    assertNotNull(name, "name field should not be null");
  }

  @Test
  @DisplayName("description TextArea should exist in LocationFormDialog")
  void descriptionFieldExists() {
    TextArea description = (TextArea) ReflectionTestUtils.getField(dialog, "description");
    assertNotNull(description, "description field should not be null");
  }

  @Test
  @DisplayName("culturalTags TextField should exist in LocationFormDialog")
  void culturalTagsFieldExists() {
    TextField culturalTags = (TextField) ReflectionTestUtils.getField(dialog, "culturalTags");
    assertNotNull(culturalTags, "culturalTags field should not be null");
  }
}
