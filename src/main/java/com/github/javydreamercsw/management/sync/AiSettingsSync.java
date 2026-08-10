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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
public class AiSettingsSync implements DataSyncContributor {

  private final GameSettingRepository gameSettingRepository;
  private final GameSettingService gameSettingService;
  private final Environment env;

  @Autowired
  public AiSettingsSync(
      final GameSettingRepository gameSettingRepository,
      final GameSettingService gameSettingService,
      final Environment env) {
    this.gameSettingRepository = gameSettingRepository;
    this.gameSettingService = gameSettingService;
    this.env = env;
  }

  @Override
  public void sync() {
    log.debug(
        "Syncing AI settings from environment variables/system properties/Spring environment...");

    Map<String, GameSetting> existingSettings =
        gameSettingRepository.findAllGlobal().stream()
            .collect(Collectors.toMap(GameSetting::getSettingKey, s -> s, (a, b) -> a));
    boolean forceOverride =
        Boolean.parseBoolean(env.getProperty("data.initializer.aiSettings.forceOverride", "false"));
    List<GameSetting> toSave = new ArrayList<>();

    syncSetting("AI_TIMEOUT", "300", existingSettings, forceOverride, toSave);
    syncSetting("AI_PROVIDER_AUTO", "true", existingSettings, forceOverride, toSave);

    // OpenAI
    syncSetting("AI_OPENAI_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_OPENAI_API_URL",
        "https://api.openai.com/v1/chat/completions",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_OPENAI_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_OPENAI_DEFAULT_MODEL", "gpt-3.5-turbo", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_PREMIUM_MODEL", "gpt-4", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_IMAGE_MODEL", "dall-e-3", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_MAX_TOKENS", "4000", existingSettings, forceOverride, toSave);
    syncSetting("AI_OPENAI_TEMPERATURE", "0.7", existingSettings, forceOverride, toSave);

    // Claude
    syncSetting("AI_CLAUDE_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_CLAUDE_API_URL",
        "https://api.anthropic.com/v1/messages/",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_CLAUDE_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_CLAUDE_MODEL_NAME", "claude-3-haiku-20240307", existingSettings, forceOverride, toSave);

    // Gemini
    syncSetting("AI_GEMINI_ENABLED", "false", existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_GEMINI_API_URL",
        "https://generativelanguage.googleapis.com/v1beta/models/",
        existingSettings,
        forceOverride,
        toSave);
    syncSetting("AI_GEMINI_API_KEY", null, existingSettings, forceOverride, toSave);
    syncSetting(
        "AI_GEMINI_MODEL_NAME",
        "gemini-3.1-flash-lite-preview",
        existingSettings,
        forceOverride,
        toSave);

    if (!toSave.isEmpty()) {
      gameSettingRepository.saveAll(toSave);
      log.debug("AI settings synchronization saved {} settings.", toSave.size());
    }
    log.debug("AI settings synchronization complete.");
  }

  private void syncSetting(
      @NonNull final String key,
      final String defaultValue,
      final Map<String, GameSetting> existingSettings,
      final boolean forceOverride,
      final List<GameSetting> toSave) {
    String envValue = env.getProperty(key);

    if (envValue != null) {
      if (forceOverride) {
        log.debug(
            "AI setting sync: overriding '{}' from environment (forceOverride=true): {}",
            key,
            maskIfSecret(key, envValue));
        gameSettingService.save(key, envValue);
      } else if (existingSettings.containsKey(key)) {
        log.debug(
            "AI setting sync: skipping '{}' from environment because DB already has a value: {}",
            key,
            maskIfSecret(key, envValue));
      } else {
        log.debug(
            "AI setting sync: saving missing '{}' from environment: {}",
            key,
            maskIfSecret(key, envValue));
        saveIfMissing(key, envValue, existingSettings, toSave);
      }
      return;
    }

    if (defaultValue != null) {
      if (existingSettings.containsKey(key)) {
        log.debug("AI setting sync: '{}' already present in DB; default not applied.", key);
      } else {
        log.debug(
            "AI setting sync: seeding missing '{}' with default: {}",
            key,
            maskIfSecret(key, defaultValue));
        saveIfMissing(key, defaultValue, existingSettings, toSave);
      }
    } else {
      log.debug("AI setting sync: '{}' has no env value and no default; leaving as-is.", key);
    }
  }

  private void saveIfMissing(
      @NonNull final String key,
      @NonNull final String value,
      final Map<String, GameSetting> existingSettings,
      final List<GameSetting> toSave) {
    if (!existingSettings.containsKey(key)) {
      GameSetting setting = new GameSetting();
      setting.setSettingKey(key);
      setting.setValue(value);
      existingSettings.put(key, setting);
      toSave.add(setting);
      log.debug("Initialized missing setting: {} = {}", key, maskIfSecret(key, value));
    }
  }

  private String maskIfSecret(final String key, final String value) {
    if (key != null && key.toUpperCase().contains("KEY")) {
      return "********";
    }
    return value;
  }
}
