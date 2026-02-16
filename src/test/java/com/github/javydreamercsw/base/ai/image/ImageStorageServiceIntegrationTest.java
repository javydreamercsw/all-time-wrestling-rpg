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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceIntegrationTest {

  private ImageStorageService imageStorageService;

  @Mock private StorageProperties storageProperties;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    imageStorageService = new ImageStorageService(storageProperties);
  }

  @Test
  void saveImage_base64_savesToFile() throws IOException {
    when(storageProperties.getResolvedImageDir()).thenReturn(tempDir);

    String testData =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="; // 1x1 transparent PNG

    String publicPath = imageStorageService.saveImage(testData, true);

    assertThat(publicPath).startsWith("images/generated/");
    String filename = publicPath.substring("images/generated/".length());

    Path savedFile = tempDir.resolve(filename);
    assertThat(Files.exists(savedFile)).isTrue();

    byte[] savedBytes = Files.readAllBytes(savedFile);
    assertThat(savedBytes).isEqualTo(Base64.getDecoder().decode(testData));
  }
}
