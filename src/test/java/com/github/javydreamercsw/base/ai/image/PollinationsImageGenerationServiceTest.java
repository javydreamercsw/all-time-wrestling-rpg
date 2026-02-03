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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollinationsImageGenerationServiceTest {

  @Mock private AiSettingsService aiSettingsService;

  private PollinationsImageGenerationService service;

  @BeforeEach
  void setUp() {
    service = new PollinationsImageGenerationService(aiSettingsService);
  }

  @Test
  void testGenerateImage() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestling champion").build();

    String result = service.generateImage(request);

    assertEquals(
        "https://gen.pollinations.ai/image/A+wrestling+champion?nologo=true&model=flux&width=1024&height=1024",
        result);
  }

  @Test
  void testGenerateImageWithCustomSize() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A belt").size("512x512").build();

    String result = service.generateImage(request);

    assertEquals(
        "https://gen.pollinations.ai/image/A+belt?nologo=true&model=flux&width=512&height=512",
        result);
  }

  @Test
  void testIsAvailable() {
    when(aiSettingsService.isPollinationsEnabled()).thenReturn(true);
    assertTrue(service.isAvailable());
  }

  @Test
  void testGetProviderName() {
    assertEquals("Pollinations", service.getProviderName());
  }
}
