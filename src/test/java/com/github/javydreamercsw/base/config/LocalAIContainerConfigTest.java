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
package com.github.javydreamercsw.base.config;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.management.service.GameSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class LocalAIContainerConfigTest {

  private AiSettingsService aiSettingsService;
  private LocalAIStatusService statusService;
  private GameSettingService gameSettingService;
  private LocalAIContainerConfig containerConfig;

  @BeforeEach
  void setUp() {
    aiSettingsService = mock(AiSettingsService.class);
    statusService = mock(LocalAIStatusService.class);
    gameSettingService = mock(GameSettingService.class);
    containerConfig =
        spy(new LocalAIContainerConfig(aiSettingsService, statusService, gameSettingService));
  }

  @Test
  void testUpdateConfigurationFromContainer() {
    // Given
    GenericContainer<?> container = mock(GenericContainer.class);
    when(container.getHost()).thenReturn("localhost");
    when(container.getMappedPort(8080)).thenReturn(12345);

    // Using reflection to set the private field or relying on the spy behavior
    // Since I can't easily set the private field without reflection or a setter,
    // I'll add a package-private setter for testing.
    containerConfig.setLocalAiContainer(container);

    // When
    containerConfig.updateConfigurationFromContainer("gpt-4", "stablediffusion");

    // Then
    verify(gameSettingService).save(eq("AI_LOCALAI_BASE_URL"), contains("http://localhost:12345"));
  }
}
