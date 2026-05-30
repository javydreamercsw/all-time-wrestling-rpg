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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
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

    // Rivalry lifecycle defaults
    when(gameSettingService.getRivalryResolutionThresholdPle()).thenReturn(30);
    when(gameSettingService.getRivalryResolutionThresholdRegular()).thenReturn(35);
    when(gameSettingService.isRivalryResolutionOnRegularShowsEnabled()).thenReturn(false);
    when(gameSettingService.getRivalryMaxDurationDays()).thenReturn(0);
    when(gameSettingService.isRivalryHeatDecayEnabled()).thenReturn(false);
    when(gameSettingService.getRivalryHeatDecayPerInterval()).thenReturn(1);
    when(gameSettingService.getRivalryHeatDecayIntervalDays()).thenReturn(7);

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

  // ── Rivalry Configuration — render with defaults ───────────────────────────

  @Test
  @DisplayName("PLE threshold field renders with default value 30")
  void rivalryPleThreshold_rendersWithDefault() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("PLE Resolution Threshold (2d20 must exceed)"));
    assertEquals(30, field.getValue());
  }

  @Test
  @DisplayName("Regular show threshold field renders with default value 35")
  void rivalryRegularThreshold_rendersWithDefault() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("Regular Show Resolution Threshold (2d20 must exceed)"));
    assertEquals(35, field.getValue());
  }

  @Test
  @DisplayName("Regular show resolution checkbox renders unchecked by default")
  void rivalryResolutionOnRegular_rendersUnchecked() {
    Checkbox cb =
        _get(
            view,
            Checkbox.class,
            spec -> spec.withLabel("Allow rivalry resolution on regular (non-PLE) shows"));
    assertFalse(cb.getValue());
  }

  @Test
  @DisplayName("Max duration field renders with default value 0")
  void rivalryMaxDuration_rendersWithDefault() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("Max Rivalry Duration (days, 0 = unlimited)"));
    assertEquals(0, field.getValue());
  }

  @Test
  @DisplayName("Heat decay enabled checkbox renders unchecked by default")
  void rivalryHeatDecayEnabled_rendersUnchecked() {
    Checkbox cb = _get(view, Checkbox.class, spec -> spec.withLabel("Enable automatic heat decay"));
    assertFalse(cb.getValue());
  }

  @Test
  @DisplayName("Heat decay per interval field renders with default value 1")
  void rivalryDecayAmount_rendersWithDefault() {
    IntegerField field =
        _get(view, IntegerField.class, spec -> spec.withLabel("Heat decay per interval"));
    assertEquals(1, field.getValue());
  }

  @Test
  @DisplayName("Heat decay interval field renders with default value 7")
  void rivalryDecayInterval_rendersWithDefault() {
    IntegerField field =
        _get(view, IntegerField.class, spec -> spec.withLabel("Heat decay interval (days)"));
    assertEquals(7, field.getValue());
  }

  // ── Rivalry Configuration — saves on change ───────────────────────────────

  @Test
  @DisplayName("Changing PLE threshold persists via service")
  void rivalryPleThreshold_savesOnChange() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("PLE Resolution Threshold (2d20 must exceed)"));
    field.setValue(25);
    verify(gameSettingService).setRivalryResolutionThresholdPle(25);
  }

  @Test
  @DisplayName("Changing regular show threshold persists via service")
  void rivalryRegularThreshold_savesOnChange() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("Regular Show Resolution Threshold (2d20 must exceed)"));
    field.setValue(32);
    verify(gameSettingService).setRivalryResolutionThresholdRegular(32);
  }

  @Test
  @DisplayName("Toggling regular show resolution persists via service")
  void rivalryResolutionOnRegular_savesOnChange() {
    Checkbox cb =
        _get(
            view,
            Checkbox.class,
            spec -> spec.withLabel("Allow rivalry resolution on regular (non-PLE) shows"));
    cb.setValue(true);
    verify(gameSettingService).setRivalryResolutionOnRegularShowsEnabled(true);
  }

  @Test
  @DisplayName("Changing max duration persists via service")
  void rivalryMaxDuration_savesOnChange() {
    IntegerField field =
        _get(
            view,
            IntegerField.class,
            spec -> spec.withLabel("Max Rivalry Duration (days, 0 = unlimited)"));
    field.setValue(90);
    verify(gameSettingService).setRivalryMaxDurationDays(90);
  }

  @Test
  @DisplayName("Toggling heat decay enabled persists via service")
  void rivalryHeatDecayEnabled_savesOnChange() {
    Checkbox cb = _get(view, Checkbox.class, spec -> spec.withLabel("Enable automatic heat decay"));
    cb.setValue(true);
    verify(gameSettingService).setRivalryHeatDecayEnabled(true);
  }

  @Test
  @DisplayName("Changing heat decay per interval persists via service")
  void rivalryDecayAmount_savesOnChange() {
    IntegerField field =
        _get(view, IntegerField.class, spec -> spec.withLabel("Heat decay per interval"));
    field.setValue(3);
    verify(gameSettingService).setRivalryHeatDecayPerInterval(3);
  }

  @Test
  @DisplayName("Changing heat decay interval persists via service")
  void rivalryDecayInterval_savesOnChange() {
    IntegerField field =
        _get(view, IntegerField.class, spec -> spec.withLabel("Heat decay interval (days)"));
    field.setValue(14);
    verify(gameSettingService).setRivalryHeatDecayIntervalDays(14);
  }
}
