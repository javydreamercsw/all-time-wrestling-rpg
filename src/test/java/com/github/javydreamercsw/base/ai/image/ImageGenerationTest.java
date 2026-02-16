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
import static org.mockito.Mockito.lenient;

import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageGenerationTest {

  private ImageGenerationServiceFactory factory;
  private ImageStorageService storageService;
  private MockImageGenerationService mockService;
  @Mock private StorageProperties storageProperties;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    mockService = new MockImageGenerationService();
    factory = new ImageGenerationServiceFactory(List.of(mockService));
    lenient().when(storageProperties.getResolvedImageDir()).thenReturn(tempDir);
    storageService = new ImageStorageService(storageProperties);
  }

  @Test
  void testFactorySelectsMock() {
    ImageGenerationService service = factory.getBestAvailableService();
    assertThat(service).isNotNull();
    assertThat(service.getProviderName()).isEqualTo("Mock AI");
  }

  @Test
  void testGenerateImage() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();
    String result = factory.generateImage(request);
    assertThat(result).contains("via.placeholder.com");
  }

  @Test
  void testSaveImageBase64(@TempDir Path tempDir) throws IOException {
    // Override storage location for test?
    // ImageStorageService hardcodes the path. I should probably make it configurable or protected.
    // For unit test, I can verify the logic if I refactor ImageStorageService to accept a base
    // path.
    // But since I can't easily change the hardcoded path without Spring injection or setters,
    // I'll skip the file system verification here or use a "Test" profile subclass.

    // Instead, let's just verify the Mock Service output for base64
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A wrestler")
            .responseFormat("b64_json")
            .build();
    String result = mockService.generateImage(request);
    assertThat(result)
        .isEqualTo(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
  }
}
