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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    when(aiSettingsService.getOllamaBaseUrl()).thenReturn("");
    when(aiSettingsService.getOllamaModel()).thenReturn("llama3.2:1b");

    view = new AiSettingsView(aiSettingsService, gameSettingService, notificationService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the auto provider checkbox")
  void shouldRenderAutoProviderCheckbox() {
    Checkbox checkbox = _get(view, Checkbox.class, spec -> spec.withLabel("Auto Select Provider"));
    assertTrue(checkbox.isVisible());
  }

  // ── Collapsible sections ────────────────────────────────────────────────────

  @Test
  @DisplayName("AI settings are grouped into six collapsible sections")
  void settingsAreGroupedIntoCollapsibleSections() {
    List<Details> sections = _find(view, Details.class);
    assertEquals(6, sections.size());
    assertEquals(
        List.of(
            "settings-section-common",
            "settings-section-pollinations",
            "settings-section-ollama",
            "settings-section-openai",
            "settings-section-claude",
            "settings-section-gemini"),
        sections.stream()
            .map(Details::getId)
            .flatMap(Optional::stream)
            .collect(Collectors.toList()));
  }

  @Test
  @DisplayName("Common section is opened by default and holds the provider controls")
  void commonSection_openedWithProviderControls() {
    Details common = _get(view, Details.class, spec -> spec.withId("settings-section-common"));
    assertTrue(common.isOpened());
    assertTrue(common.getSummaryText().contains("Common AI Settings"));
    List<Component> content = common.getContent().collect(Collectors.toList());
    assertEquals(1, content.size());
    VerticalLayout layout = (VerticalLayout) content.get(0);
    List<Class<?>> fieldTypes =
        layout.getChildren().map(Component::getClass).collect(Collectors.toList());
    assertTrue(fieldTypes.contains(FormLayout.class));
    Checkbox auto = _get(common, Checkbox.class, spec -> spec.withLabel("Auto Select Provider"));
    assertTrue(auto.isVisible());
  }

  @Test
  @DisplayName("Provider sections render collapsed by default")
  void providerSections_renderCollapsedByDefault() {
    for (String id :
        List.of(
            "settings-section-openai",
            "settings-section-claude",
            "settings-section-gemini",
            "settings-section-pollinations",
            "settings-section-ollama")) {
      Details section = _get(view, Details.class, spec -> spec.withId(id));
      assertFalse(section.isOpened(), id + " should be collapsed by default");
    }
  }

  @Test
  @DisplayName("Toggling a section changes its open state")
  void sections_toggleOpenState() {
    Details openai = _get(view, Details.class, spec -> spec.withId("settings-section-openai"));
    assertFalse(openai.isOpened());
    openai.setOpened(true);
    assertTrue(openai.isOpened());
    openai.setOpened(false);
    assertFalse(openai.isOpened());
  }

  @Test
  @DisplayName("OpenAI section contains the expected fields")
  void openAiSection_containsExpectedFields() {
    Details openai = _get(view, Details.class, spec -> spec.withId("settings-section-openai"));
    _get(openai, Checkbox.class, spec -> spec.withLabel("Enabled"));
    _get(openai, TextField.class, spec -> spec.withLabel("Default Model"));
    _get(openai, NumberField.class, spec -> spec.withLabel("Max Tokens"));
  }

  @Test
  @DisplayName("Claude section contains the expected fields")
  void claudeSection_containsExpectedFields() {
    Details claude = _get(view, Details.class, spec -> spec.withId("settings-section-claude"));
    _get(claude, Checkbox.class, spec -> spec.withLabel("Enabled"));
    _get(claude, TextField.class, spec -> spec.withLabel("Model Name"));
  }

  @Test
  @DisplayName("Gemini section contains the expected fields")
  void geminiSection_containsExpectedFields() {
    Details gemini = _get(view, Details.class, spec -> spec.withId("settings-section-gemini"));
    _get(gemini, Checkbox.class, spec -> spec.withLabel("Enabled"));
    _get(gemini, TextField.class, spec -> spec.withLabel("Model Name"));
  }
}
