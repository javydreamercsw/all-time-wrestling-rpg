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

import com.github.javydreamercsw.base.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileSystemImageSourceTest {

  @Mock private StorageProperties storageProperties;
  @TempDir Path tempDir;

  private FileSystemImageSource source;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(storageProperties.getResolvedDefaultImageDir())
        .thenReturn(tempDir);
    source = new FileSystemImageSource(storageProperties);
  }

  @Test
  void testResolveExistingImage() throws IOException {
    Path wrestlerDir = tempDir.resolve("wrestlers");
    Files.createDirectories(wrestlerDir);
    Path imageFile = wrestlerDir.resolve("Test Wrestler.png");
    Files.createFile(imageFile);

    Optional<String> result = source.resolveImage("Test Wrestler", ImageCategory.WRESTLER);
    assertTrue(result.isPresent());
    assertEquals("images/defaults/wrestlers/Test Wrestler.png", result.get());
  }

  @Test
  void testResolveNonExistingImage() {
    Optional<String> result = source.resolveImage("Non Existing", ImageCategory.WRESTLER);
    assertTrue(result.isEmpty());
  }

  @Test
  void testPriority() {
    assertEquals(50, source.getPriority());
  }
}
