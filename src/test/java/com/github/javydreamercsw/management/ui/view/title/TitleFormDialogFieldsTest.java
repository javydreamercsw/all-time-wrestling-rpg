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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hibernate.collection.spi.PersistentCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class TitleFormDialogFieldsTest extends AbstractViewTest {

  @Mock private TitleService titleService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private SecurityUtils securityUtils;

  private TitleFormDialog dialog;

  @BeforeEach
  void setup() {
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
            Collections.emptyList(),
            imageStorageService,
            title,
            () -> {},
            securityUtils);
  }

  @Test
  @DisplayName(
      "Opening edit dialog with wrestlers having uninitialized Hibernate states does not throw")
  void openEditDialogWithUninitializedWrestlerStatesDoesNotThrow() {
    @SuppressWarnings("unchecked")
    Set<WrestlerState> lazyStates =
        (Set<WrestlerState>)
            mock(Set.class, withSettings().extraInterfaces(PersistentCollection.class));
    when(((PersistentCollection<?>) lazyStates).wasInitialized()).thenReturn(false);

    Wrestler wrestler = new Wrestler();
    ReflectionTestUtils.setField(wrestler, "wrestlerStates", lazyStates);

    Title title = new Title();
    title.setId(2L);
    title.setName("World Title");
    title.setTier(WrestlerTier.ICON);

    assertDoesNotThrow(
        () ->
            new TitleFormDialog(
                titleService,
                List.of(wrestler),
                imageStorageService,
                title,
                () -> {},
                securityUtils));
  }

  @Test
  @DisplayName(
      "defenseFrequencyType ComboBox and effectScript TextArea should exist in TitleFormDialog")
  void defenseFrequencyTypeAndEffectScriptFieldsExist() {
    com.vaadin.flow.component.combobox.ComboBox<
            com.github.javydreamercsw.management.domain.title.DefenseFrequencyType>
        defenseFrequencyType =
            (com.vaadin.flow.component.combobox.ComboBox<
                    com.github.javydreamercsw.management.domain.title.DefenseFrequencyType>)
                ReflectionTestUtils.getField(dialog, "defenseFrequencyType");
    assertNotNull(defenseFrequencyType, "defenseFrequencyType field should not be null");

    com.vaadin.flow.component.textfield.TextArea effectScript =
        (com.vaadin.flow.component.textfield.TextArea)
            ReflectionTestUtils.getField(dialog, "effectScript");
    assertNotNull(effectScript, "effectScript field should not be null");
  }
}
