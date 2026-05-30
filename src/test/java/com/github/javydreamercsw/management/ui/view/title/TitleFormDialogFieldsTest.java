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
package com.github.javydreamercsw.management.ui.view.title;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class TitleFormDialogFieldsTest extends AbstractViewTest {

  @Mock private TitleService titleService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private SecurityUtils securityUtils;

  private TitleFormDialog dialog;

  @BeforeEach
  void setup() {
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(true);
    when(securityUtils.canCreate()).thenReturn(true);

    Title title = new Title();
    title.setId(1L);
    title.setName("Test");

    dialog =
        new TitleFormDialog(
            titleService,
            wrestlerRepository,
            tierRecalculationService,
            imageStorageService,
            title,
            () -> {},
            securityUtils);
  }

  @Test
  @DisplayName("defenseFrequency and effectScript fields should exist in TitleFormDialog")
  void defenseFrequencyAndEffectScriptFieldsExist() {
    IntegerField defenseFrequency =
        (IntegerField) ReflectionTestUtils.getField(dialog, "defenseFrequency");
    assertNotNull(defenseFrequency, "defenseFrequency field should not be null");

    TextArea effectScript = (TextArea) ReflectionTestUtils.getField(dialog, "effectScript");
    assertNotNull(effectScript, "effectScript field should not be null");
  }
}
