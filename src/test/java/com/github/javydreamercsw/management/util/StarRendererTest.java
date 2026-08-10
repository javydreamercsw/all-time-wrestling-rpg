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
package com.github.javydreamercsw.management.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StarRendererTest {

  @Test
  void oneFullStar() {
    assertEquals("★☆☆☆☆", StarRenderer.renderStars(1.0));
  }

  @Test
  void oneAndHalfStars() {
    assertEquals("★½☆☆☆", StarRenderer.renderStars(1.5));
  }

  @Test
  void twoFullStars() {
    assertEquals("★★☆☆☆", StarRenderer.renderStars(2.0));
  }

  @Test
  void twoAndHalfStars() {
    assertEquals("★★½☆☆", StarRenderer.renderStars(2.5));
  }

  @Test
  void threeFullStars() {
    assertEquals("★★★☆☆", StarRenderer.renderStars(3.0));
  }

  @Test
  void threeAndHalfStars() {
    assertEquals("★★★½☆", StarRenderer.renderStars(3.5));
  }

  @Test
  void fourFullStars() {
    assertEquals("★★★★☆", StarRenderer.renderStars(4.0));
  }

  @Test
  void fourAndHalfStars() {
    assertEquals("★★★★½", StarRenderer.renderStars(4.5));
  }

  @Test
  void fiveFullStars() {
    assertEquals("★★★★★", StarRenderer.renderStars(5.0));
  }

  @Test
  void halfIsOnlyWhenFractionalPartAtLeast0point5() {
    // 3.4 rounds down — no half star
    assertEquals("★★★☆☆", StarRenderer.renderStars(3.4));
  }

  @Test
  void halfTriggersAtExactly0point5() {
    assertEquals("★★★½☆", StarRenderer.renderStars(3.5));
  }
}
