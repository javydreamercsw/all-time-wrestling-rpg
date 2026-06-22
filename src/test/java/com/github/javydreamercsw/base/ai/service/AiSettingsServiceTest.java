/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSettingsServiceTest {

  @Mock private GameSettingService gameSettingService;

  private AiSettingsService aiSettingsService;

  @BeforeEach
  void setUp() {
    aiSettingsService = new AiSettingsService(gameSettingService);
  }

  // ----------------------------------------------------------------
  // Group A — setting absent, returns default
  // ----------------------------------------------------------------

  @Test
  void getAiTimeout_noSetting_returns300() {
    when(gameSettingService.findById("AI_TIMEOUT")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.getAiTimeout()).isEqualTo(300);
  }

  @Test
  void isAiProviderAuto_noSetting_returnsTrue() {
    when(gameSettingService.findById("AI_PROVIDER_AUTO")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.isAiProviderAuto()).isTrue();
  }

  @Test
  void isOpenAIEnabled_noSetting_returnsFalse() {
    when(gameSettingService.findById("AI_OPENAI_ENABLED")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.isOpenAIEnabled()).isFalse();
  }

  @Test
  void getOpenAIApiKey_noSetting_returnsEmpty() {
    when(gameSettingService.findById("AI_OPENAI_API_KEY")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.getOpenAIApiKey()).isEmpty();
  }

  @Test
  void getOpenAIDefaultModel_noSetting_returnsGpt35Turbo() {
    when(gameSettingService.findById("AI_OPENAI_DEFAULT_MODEL")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.getOpenAIDefaultModel()).isEqualTo("gpt-3.5-turbo");
  }

  @Test
  void isClaudeEnabled_noSetting_returnsFalse() {
    when(gameSettingService.findById("AI_CLAUDE_ENABLED")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.isClaudeEnabled()).isFalse();
  }

  @Test
  void isGeminiEnabled_noSetting_returnsFalse() {
    when(gameSettingService.findById("AI_GEMINI_ENABLED")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.isGeminiEnabled()).isFalse();
  }

  @Test
  void getGeminiModelName_noSetting_returnsGeminiFlash() {
    when(gameSettingService.findById("AI_GEMINI_MODEL_NAME")).thenReturn(Optional.empty());

    assertThat(aiSettingsService.getGeminiModelName()).isEqualTo("gemini-3.1-flash-lite-preview");
  }

  // ----------------------------------------------------------------
  // Group B — setting present, returns parsed value
  // ----------------------------------------------------------------

  @Test
  void getAiTimeout_settingPresent_returnsValue() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setSettingKey("AI_TIMEOUT");
    gameSetting.setValue("600");
    when(gameSettingService.findById("AI_TIMEOUT")).thenReturn(Optional.of(gameSetting));

    assertThat(aiSettingsService.getAiTimeout()).isEqualTo(600);
  }

  @Test
  void isOpenAIEnabled_settingPresent_returnsTrue() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setSettingKey("AI_OPENAI_ENABLED");
    gameSetting.setValue("true");
    when(gameSettingService.findById("AI_OPENAI_ENABLED")).thenReturn(Optional.of(gameSetting));

    assertThat(aiSettingsService.isOpenAIEnabled()).isTrue();
  }

  @Test
  void getOpenAIMaxTokens_settingPresent_returnsValue() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setSettingKey("AI_OPENAI_MAX_TOKENS");
    gameSetting.setValue("2000");
    when(gameSettingService.findById("AI_OPENAI_MAX_TOKENS")).thenReturn(Optional.of(gameSetting));

    assertThat(aiSettingsService.getOpenAIMaxTokens()).isEqualTo(2000);
  }

  @Test
  void isClaudeEnabled_settingPresent_returnsTrue() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setSettingKey("AI_CLAUDE_ENABLED");
    gameSetting.setValue("true");
    when(gameSettingService.findById("AI_CLAUDE_ENABLED")).thenReturn(Optional.of(gameSetting));

    assertThat(aiSettingsService.isClaudeEnabled()).isTrue();
  }

  @Test
  void getOpenAITemperature_settingPresent_returnsValue() {
    GameSetting gameSetting = new GameSetting();
    gameSetting.setSettingKey("AI_OPENAI_TEMPERATURE");
    gameSetting.setValue("0.5");
    when(gameSettingService.findById("AI_OPENAI_TEMPERATURE")).thenReturn(Optional.of(gameSetting));

    assertThat(aiSettingsService.getOpenAITemperature()).isEqualTo(0.5f);
  }
}
