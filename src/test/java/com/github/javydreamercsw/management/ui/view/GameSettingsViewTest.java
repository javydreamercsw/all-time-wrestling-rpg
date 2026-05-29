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
package com.github.javydreamercsw.management.ui.view;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class GameSettingsViewTest extends AbstractViewTest {

  @Mock private GameSettingService gameSettingService;
  @Mock private ThemeService themeService;

  private GameSettingsView view;

  @BeforeEach
  void setup() {
    when(gameSettingService.getCurrentGameDate()).thenReturn(LocalDate.now());
    when(themeService.getAvailableThemes()).thenReturn(List.of("light", "dark"));
    when(themeService.getGlobalDefaultTheme()).thenReturn(Optional.of("light"));

    view = new GameSettingsView(gameSettingService, themeService, Optional.empty());
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the default theme selection combo box")
  void shouldRenderThemeSelector() {
    @SuppressWarnings("unchecked")
    ComboBox<String> combo =
        _get(view, ComboBox.class, spec -> spec.withId("default-theme-selection"));
    assertTrue(combo.isVisible());
  }
}
