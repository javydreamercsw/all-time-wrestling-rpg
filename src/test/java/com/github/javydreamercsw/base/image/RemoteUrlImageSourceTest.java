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

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteUrlImageSourceTest {

  private RemoteUrlImageSource source;

  @BeforeEach
  void setUp() {
    source = new RemoteUrlImageSource();
  }

  @Test
  void testResolveHttpUrl() {
    String url = "http://example.com/image.png";
    Optional<String> result = source.resolveImage(url, ImageCategory.WRESTLER);
    assertTrue(result.isPresent());
    assertEquals(url, result.get());
  }

  @Test
  void testResolveHttpsUrl() {
    String url = "https://example.com/image.png";
    Optional<String> result = source.resolveImage(url, ImageCategory.WRESTLER);
    assertTrue(result.isPresent());
    assertEquals(url, result.get());
  }

  @Test
  void testResolveNonUrl() {
    Optional<String> result = source.resolveImage("Not a URL", ImageCategory.WRESTLER);
    assertTrue(result.isEmpty());
  }

  @Test
  void testPriority() {
    assertEquals(10, source.getPriority()); // Highest priority
  }
}
