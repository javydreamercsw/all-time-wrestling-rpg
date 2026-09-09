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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class QrCodeUtilTest {

  @Test
  void toBase64PngProducesValidBase64() throws Exception {
    String result = QrCodeUtil.toBase64Png("http://192.168.1.100:8080/atw-rpg/match/42", 256);
    assertNotNull(result);
    assertFalse(result.isBlank());
    // verify it decodes without error
    byte[] decoded = Base64.getDecoder().decode(result);
    assertNotEquals(0, decoded.length);
  }

  @Test
  void toBase64PngWorksWithLocalhostUrl() throws Exception {
    String result = QrCodeUtil.toBase64Png("http://localhost:8080/match/1", 256);
    assertNotNull(result);
    assertFalse(result.isBlank());
  }

  /**
   * Regression guard for ATW-sevh: {@link javax.imageio.ImageIO#write(java.awt.image.RenderedImage,
   * String, java.io.OutputStream)} wraps the stream in a {@code FileCacheImageOutputStream} that
   * creates a transient {@code imageio*.tmp} file in {@code java.io.tmpdir}. On the production
   * Tomcat install that directory sits under a launchd WatchPath, so every QR generation restarted
   * the whole server. The file is deleted when the stream closes, so it cannot be detected by a
   * before/after directory listing — this test watches tmpdir from a concurrent thread while
   * generating repeatedly.
   */
  @Test
  void toBase64PngWritesNoTempFilesToTmpDir() throws Exception {
    Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
    AtomicBoolean watching = new AtomicBoolean(true);
    AtomicReference<String> caught = new AtomicReference<>();
    Thread watcher =
        new Thread(
            () -> {
              while (watching.get()) {
                try (var files = Files.list(tmpDir)) {
                  files
                      .map(p -> p.getFileName().toString())
                      .filter(
                          n ->
                              (n.startsWith("imageio") || n.startsWith("+~")) && n.endsWith(".tmp"))
                      .findFirst()
                      .ifPresent(n -> caught.compareAndSet(null, n));
                } catch (IOException ignored) {
                  // tmpdir entry vanished mid-listing; keep watching
                }
              }
            });
    watcher.start();
    try {
      for (int i = 0; i < 200; i++) {
        QrCodeUtil.toBase64Png("http://localhost:8080/match/" + i, 256);
      }
    } finally {
      watching.set(false);
      watcher.join();
    }
    assertNull(
        caught.get(), () -> "ImageIO created a temp file in java.io.tmpdir: " + caught.get());
  }
}
