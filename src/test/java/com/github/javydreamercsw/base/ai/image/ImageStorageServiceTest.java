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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ImageStorageServiceTest {

  @Test
  void decodeDataUriToBytes_validBase64_decodes() throws IOException {
    byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);
    String payload = Base64.getEncoder().encodeToString(expected);

    byte[] actual = ImageStorageService.decodeDataUriToBytes("data:image/png;base64," + payload);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void decodeDataUriToBytes_missingComma_throws() {
    assertThatThrownBy(() -> ImageStorageService.decodeDataUriToBytes("data:image/png;base64"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("missing");
  }

  @Test
  void decodeDataUriToBytes_notBase64_throws() {
    assertThatThrownBy(() -> ImageStorageService.decodeDataUriToBytes("data:image/png,abc"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("base64");
  }

  @Test
  void decodeDataUriToBytes_invalidBase64_throws() {
    assertThatThrownBy(() -> ImageStorageService.decodeDataUriToBytes("data:image/png;base64,%%%"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Invalid base64");
  }
}
