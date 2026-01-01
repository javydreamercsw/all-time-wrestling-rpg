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

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSettingsService {

  private final GameSettingService gameSettingService;

  // Common AI settings
  public int getAiTimeout() {
    return gameSettingService
        .findById("AI_TIMEOUT")
        .map(gs -> Integer.parseInt(gs.getValue()))
        .orElse(300);
  }

  public boolean isAiProviderAuto() {
    return gameSettingService
        .findById("AI_PROVIDER_AUTO")
        .map(gs -> Boolean.parseBoolean(gs.getValue()))
        .orElse(true);
  }

  // OpenAI settings
  public boolean isOpenAIEnabled() {
    return gameSettingService
        .findById("AI_OPENAI_ENABLED")
        .map(gs -> Boolean.parseBoolean(gs.getValue()))
        .orElse(false);
  }

  public String getOpenAIApiUrl() {
    return gameSettingService
        .findById("AI_OPENAI_API_URL")
        .map(GameSetting::getValue)
        .orElse("https://api.openai.com/v1/chat/completions");
  }

  public String getOpenAIApiKey() {
    return gameSettingService.findById("AI_OPENAI_API_KEY").map(GameSetting::getValue).orElse("");
  }

  public String getOpenAIDefaultModel() {
    return gameSettingService
        .findById("AI_OPENAI_DEFAULT_MODEL")
        .map(GameSetting::getValue)
        .orElse("gpt-3.5-turbo");
  }

  public String getOpenAIPremiumModel() {
    return gameSettingService
        .findById("AI_OPENAI_PREMIUM_MODEL")
        .map(GameSetting::getValue)
        .orElse("gpt-4");
  }

  public int getOpenAIMaxTokens() {
    return gameSettingService
        .findById("AI_OPENAI_MAX_TOKENS")
        .map(gs -> Integer.parseInt(gs.getValue()))
        .orElse(1000);
  }

  public float getOpenAITemperature() {
    return gameSettingService
        .findById("AI_OPENAI_TEMPERATURE")
        .map(gs -> Float.parseFloat(gs.getValue()))
        .orElse(0.7f);
  }

  // Claude settings
  public boolean isClaudeEnabled() {
    return gameSettingService
        .findById("AI_CLAUDE_ENABLED")
        .map(gs -> Boolean.parseBoolean(gs.getValue()))
        .orElse(false);
  }

  public String getClaudeApiUrl() {
    return gameSettingService
        .findById("AI_CLAUDE_API_URL")
        .map(GameSetting::getValue)
        .orElse("https://api.anthropic.com/v1/messages/");
  }

  public String getClaudeApiKey() {
    return gameSettingService.findById("AI_CLAUDE_API_KEY").map(GameSetting::getValue).orElse("");
  }

  public String getClaudeModelName() {
    return gameSettingService
        .findById("AI_CLAUDE_MODEL_NAME")
        .map(GameSetting::getValue)
        .orElse("claude-3-haiku-20240307");
  }

  // Gemini settings
  public boolean isGeminiEnabled() {
    return gameSettingService
        .findById("AI_GEMINI_ENABLED")
        .map(gs -> Boolean.parseBoolean(gs.getValue()))
        .orElse(false);
  }

  public String getGeminiApiUrl() {
    return gameSettingService
        .findById("AI_GEMINI_API_URL")
        .map(GameSetting::getValue)
        .orElse("https://generativelanguage.googleapis.com/v1beta/models/");
  }

  public String getGeminiApiKey() {
    return gameSettingService.findById("AI_GEMINI_API_KEY").map(GameSetting::getValue).orElse("");
  }

  public String getGeminiModelName() {
    return gameSettingService
        .findById("AI_GEMINI_MODEL_NAME")
        .map(GameSetting::getValue)
        .orElse("gemini-2.5-flash");
  }

  // LocalAI settings
  public boolean isLocalAIEnabled() {
    return gameSettingService
        .findById("AI_LOCALAI_ENABLED")
        .map(gs -> Boolean.parseBoolean(gs.getValue()))
        .orElse(false);
  }

  public String getLocalAIBaseUrl() {
    return gameSettingService
        .findById("AI_LOCALAI_BASE_URL")
        .map(GameSetting::getValue)
        .orElse("http://localhost:8088");
  }

  public String getLocalAIModel() {
    return gameSettingService
        .findById("AI_LOCALAI_MODEL")
        .map(GameSetting::getValue)
        .orElse("llama-3.2-1b-instruct:q4_k_m");
  }

  public String getLocalAIModelUrl() {
    return gameSettingService
        .findById("AI_LOCALAI_MODEL_URL")
        .map(GameSetting::getValue)
        .orElse("");
  }
}
