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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockImageGenerationServiceTest {

  private MockImageGenerationService service;

  @BeforeEach
  void setUp() {
    service = new MockImageGenerationService();
  }

  @Test
  void generateImage_urlFormat_returnsPlaceholderUrl() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestling champion").build();

    String result = service.generateImage(request);

    assertThat(result).contains("via.placeholder.com");
  }

  @Test
  void generateImage_b64Format_returnsBase64Pixel() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A wrestling belt")
            .responseFormat("b64_json")
            .build();

    String result = service.generateImage(request);

    assertThat(result)
        .isEqualTo(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
  }

  @Test
  void generateImage_b64FormatCaseInsensitive_returnsBase64Pixel() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A match")
            .responseFormat("B64_JSON")
            .build();

    String result = service.generateImage(request);

    assertThat(result).startsWith("iVBORw0KGgo");
  }

  @Test
  void generateImage_nullPrompt_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> service.generateImage(null));
  }

  @Test
  void getProviderName_returnsMockAi() {
    assertThat(service.getProviderName()).isEqualTo("Mock AI");
  }

  @Test
  void isAvailable_alwaysTrue() {
    assertThat(service.isAvailable()).isTrue();
  }
}
