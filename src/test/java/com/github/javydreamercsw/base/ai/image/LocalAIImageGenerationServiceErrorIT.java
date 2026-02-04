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
package com.github.javydreamercsw.base.ai.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"local-ai-it-error"})
class LocalAIImageGenerationServiceErrorIT {

  @Autowired private LocalAIImageGenerationService localAIImageGenerationService;

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    AiSettingsService mockedAiSettingsService() {
      AiSettingsService mock = mock(AiSettingsService.class);
      when(mock.isLocalAIEnabled()).thenReturn(true);
      // Simulate an invalid base URL to force a connection error
      when(mock.getLocalAIBaseUrl()).thenReturn("http://localhost:99999");
      when(mock.getAiTimeout()).thenReturn(1); // Set a short timeout for faster test failures
      return mock;
    }

    @Bean
    @Primary
    LocalAIStatusService mockedLocalAIStatusService() {
      LocalAIStatusService mock = mock(LocalAIStatusService.class);
      when(mock.isReady()).thenReturn(true); // Ensure service is considered ready
      return mock;
    }

    @Bean
    @Primary
    LocalAIConfigProperties testLocalAIConfigProperties(AiSettingsService mockedAiSettingsService) {
      // LocalAIConfigProperties now takes the mocked AiSettingsService in its constructor
      return new LocalAIConfigProperties(mockedAiSettingsService);
    }
  }

  @Test
  void testGenerateImage_connectionError() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("test prompt").build();

    assertThatThrownBy(() -> localAIImageGenerationService.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .hasMessageContaining("Error during image generation");
  }
}
