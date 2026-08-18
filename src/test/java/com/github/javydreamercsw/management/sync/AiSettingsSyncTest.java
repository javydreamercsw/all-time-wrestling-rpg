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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSettingsSyncTest {

  @Mock private GameSettingRepository gameSettingRepository;
  @Mock private GameSettingService gameSettingService;
  @Mock private Environment env;

  private AiSettingsSync sync;

  @BeforeEach
  void setUp() {
    sync = new AiSettingsSync(gameSettingRepository, gameSettingService, env);
  }

  @Test
  void sync_doesNotOverwriteExistingDbValue() {
    when(env.getProperty("AI_OPENAI_ENABLED")).thenReturn("true");

    GameSetting existingSetting = new GameSetting();
    existingSetting.setSettingKey("AI_OPENAI_ENABLED");
    existingSetting.setValue("false");
    when(gameSettingRepository.findAllGlobal()).thenReturn(List.of(existingSetting));
    when(env.getProperty("data.initializer.aiSettings.forceOverride", "false")).thenReturn("false");

    sync.sync();

    verify(gameSettingService, never()).save("AI_OPENAI_ENABLED", "true");
  }

  @Test
  void sync_seedsMissingDefaultsWhenNoEnvAndNoDb() {
    when(gameSettingRepository.findAllGlobal()).thenReturn(List.of());
    when(env.getProperty("data.initializer.aiSettings.forceOverride", "false")).thenReturn("false");

    sync.sync();

    // At least one setting should be saved (e.g. AI_TIMEOUT default "300")
    verify(gameSettingRepository)
        .saveAll(org.mockito.ArgumentMatchers.argThat(list -> list.iterator().hasNext()));
  }

  @Test
  void sync_overwritesWhenForceOverrideEnabled() {
    when(env.getProperty("AI_OPENAI_ENABLED")).thenReturn("true");
    when(gameSettingRepository.findAllGlobal()).thenReturn(List.of());
    when(env.getProperty("data.initializer.aiSettings.forceOverride", "false")).thenReturn("true");

    sync.sync();

    verify(gameSettingService).save("AI_OPENAI_ENABLED", "true");
  }
}
