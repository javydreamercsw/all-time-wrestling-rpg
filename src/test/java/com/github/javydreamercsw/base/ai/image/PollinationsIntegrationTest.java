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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PollinationsIntegrationTest {

  @Test
  void testDownloadRealImage() throws Exception {
    // 1. Setup Service
    AiSettingsService settings = mock(AiSettingsService.class);
    when(settings.isPollinationsEnabled()).thenReturn(true);
    when(settings.getPollinationsApiKey()).thenReturn("sk_2l9axRlebpFO4PscYDm37QgGM1s3jCYy");
    when(settings.getAiTimeout()).thenReturn(30);

    PollinationsImageGenerationService service = new PollinationsImageGenerationService(settings);

    // 2. Generate URL
    // Using a simple prompt and small size for speed
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A pixel art wrestling belt")
            .size("256x256")
            .model("flux")
            .build();

    String imageUrl = service.generateImage(request);
    log.info("Generated Data URI length: {}", imageUrl.length());
    assertTrue(imageUrl.startsWith("data:image/jpeg;base64,"));

    // 3. Decode and save
    String base64Data = imageUrl.split(",")[1];
    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);

    Path targetDir = Paths.get("target/generated-images");
    if (!Files.exists(targetDir)) {
      Files.createDirectories(targetDir);
    }

    Path targetFile = targetDir.resolve("pollinations-test.jpg");
    Files.write(targetFile, imageBytes);

    // 4. Verify file
    assertTrue(Files.exists(targetFile), "File should exist");
    assertTrue(Files.size(targetFile) > 0, "File should not be empty");
    log.info("Successfully downloaded image to: {}", targetFile.toAbsolutePath());
  }
}
