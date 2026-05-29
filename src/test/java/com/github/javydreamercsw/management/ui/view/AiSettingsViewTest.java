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

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AiSettingsViewTest extends AbstractViewTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private GameSettingService gameSettingService;
  @Mock private NotificationService notificationService;

  private AiSettingsView view;

  @BeforeEach
  void setup() {
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(aiSettingsService.isAiProviderAuto()).thenReturn(false);
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(false);
    when(aiSettingsService.getOpenAIApiUrl()).thenReturn("");
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("");
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("");
    when(aiSettingsService.getOpenAIImageModel()).thenReturn("");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1024);
    when(aiSettingsService.getOpenAITemperature()).thenReturn(0.7f);
    when(aiSettingsService.isClaudeEnabled()).thenReturn(false);
    when(aiSettingsService.getClaudeApiUrl()).thenReturn("");
    when(aiSettingsService.getClaudeApiKey()).thenReturn("");
    when(aiSettingsService.getClaudeModelName()).thenReturn("");
    when(aiSettingsService.isGeminiEnabled()).thenReturn(false);
    when(aiSettingsService.getGeminiApiUrl()).thenReturn("");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("");
    when(aiSettingsService.getGeminiModelName()).thenReturn("");
    when(aiSettingsService.isPollinationsEnabled()).thenReturn(false);
    when(aiSettingsService.getPollinationsApiKey()).thenReturn("");

    view = new AiSettingsView(aiSettingsService, gameSettingService, notificationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the auto provider checkbox")
  void shouldRenderAutoProviderCheckbox() {
    Checkbox checkbox =
        _get(view, Checkbox.class, spec -> spec.withCaption("Auto Select Provider"));
    assertTrue(checkbox.isVisible());
  }
}
