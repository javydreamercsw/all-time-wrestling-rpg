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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultImageServiceTest {

  @Mock private ImageSource source1;
  @Mock private ImageSource source2;

  private DefaultImageService service;

  @BeforeEach
  void setUp() {
    when(source1.getPriority()).thenReturn(10);
    when(source2.getPriority()).thenReturn(20);
    service = new DefaultImageService(List.of(source2, source1));
  }

  @Test
  void testResolveSpecificImageFromHighPrioritySource() {
    when(source1.resolveImage("Entity", ImageCategory.WRESTLER))
        .thenReturn(Optional.of("path/to/image.png"));

    ImageResolution result = service.resolveImage("Entity", ImageCategory.WRESTLER);
    assertEquals("path/to/image.png", result.url());
    assertFalse(result.isFallback());
  }

  @Test
  void testResolveSpecificImageFromLowPrioritySource() {
    when(source1.resolveImage("Entity", ImageCategory.WRESTLER)).thenReturn(Optional.empty());
    when(source2.resolveImage("Entity", ImageCategory.WRESTLER))
        .thenReturn(Optional.of("other/path.png"));

    ImageResolution result = service.resolveImage("Entity", ImageCategory.WRESTLER);
    assertEquals("other/path.png", result.url());
    assertFalse(result.isFallback());
  }

  @Test
  void testFallback() {
    when(source1.resolveImage("Entity", ImageCategory.WRESTLER)).thenReturn(Optional.empty());
    when(source2.resolveImage("Entity", ImageCategory.WRESTLER)).thenReturn(Optional.empty());

    ImageResolution result = service.resolveImage("Entity", ImageCategory.WRESTLER);
    assertEquals("images/generic-wrestler.png", result.url());
    assertTrue(result.isFallback());
  }
}
