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
package com.github.javydreamercsw.base.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollinationsImageSourceTest {

  @Mock private ImageGenerationService imageGenerationService;

  private PollinationsImageSource source;

  @BeforeEach
  void setUp() {
    source = new PollinationsImageSource(imageGenerationService);
  }

  @Test
  void testResolveImageAvailable() {
    when(imageGenerationService.isAvailable()).thenReturn(true);
    when(imageGenerationService.generateImage(any()))
        .thenReturn("data:image/png;base64,encoded...");

    Optional<String> result = source.resolveImage("Test Wrestler", ImageCategory.WRESTLER);
    assertTrue(result.isPresent());
    assertEquals("data:image/png;base64,encoded...", result.get());
  }

  @Test
  void testResolveImageNotAvailable() {
    when(imageGenerationService.isAvailable()).thenReturn(false);

    Optional<String> result = source.resolveImage("Test Wrestler", ImageCategory.WRESTLER);
    assertTrue(result.isEmpty());
  }

  @Test
  void testPriority() {
    assertEquals(1000, source.getPriority()); // Lowest priority
  }
}
