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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class QrCodeUtilTest {

  @Test
  void toBase64PngProducesValidBase64() throws Exception {
    String result = QrCodeUtil.toBase64Png("http://192.168.1.100:8080/atw-rpg/match/42", 256);
    assertNotNull(result);
    assertFalse(result.isBlank());
    // verify it decodes without error
    byte[] decoded = java.util.Base64.getDecoder().decode(result);
    assertNotEquals(0, decoded.length);
  }

  @Test
  void toBase64PngWorksWithLocalhostUrl() throws Exception {
    String result = QrCodeUtil.toBase64Png("http://localhost:8080/match/1", 256);
    assertNotNull(result);
    assertFalse(result.isBlank());
  }
}
