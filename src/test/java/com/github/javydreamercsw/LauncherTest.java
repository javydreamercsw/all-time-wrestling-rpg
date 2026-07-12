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
package com.github.javydreamercsw;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherTest {

  // ── version comparison ────────────────────────────────────────────────────

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMinor() {
    assertThat(Launcher.isNewer("2.6.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherPatch() {
    assertThat(Launcher.isNewer("2.5.3", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMajor() {
    assertThat(Launcher.isNewer("3.0.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsFalseForSameVersion() {
    assertThat(Launcher.isNewer("2.5.2", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseWhenCandidateIsOlder() {
    assertThat(Launcher.isNewer("2.4.0", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_stripsPreReleaseSuffixBeforeComparing() {
    assertThat(Launcher.isNewer("2.6.0-SNAPSHOT", "2.5.2")).isTrue();
    assertThat(Launcher.isNewer("2.5.2-RC1", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseForUnparseableInput() {
    assertThat(Launcher.isNewer("not-a-version", "2.5.2")).isFalse();
    assertThat(Launcher.isNewer("2.6.0", "not-a-version")).isFalse();
  }

  // ── GitHub API parsing ────────────────────────────────────────────────────

  @Test
  void fetchLatestRelease_parsesTagNameAndChoosesJarOverWar() throws Exception {
    String json =
        """
        {
          "tag_name": "v2.6.0",
          "html_url": "https://example.com/releases/v2.6.0",
          "assets": [
            {"browser_download_url": "https://example.com/downloads/app.war"},
            {"browser_download_url": "https://example.com/downloads/app.jar"}
          ]
        }
        """;

    HttpServer server = startJsonServer("/releases", json);
    int port = server.getAddress().getPort();
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
    try {
      Launcher.ReleaseInfo info = Launcher.fetchLatestRelease();
      assertThat(info).isNotNull();
      assertThat(info.version()).isEqualTo("2.6.0");
      assertThat(info.jarUrl()).endsWith("app.jar");
    } finally {
      System.clearProperty("atw.launcher.releases-api");
      server.stop(0);
    }
  }

  @Test
  void fetchLatestRelease_returnsNullWhenApiIsUnreachable() throws Exception {
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:1/unreachable");
    try {
      assertThat(Launcher.fetchLatestRelease()).isNull();
    } finally {
      System.clearProperty("atw.launcher.releases-api");
    }
  }

  // ── download + safe swap ──────────────────────────────────────────────────

  @Test
  void download_fetchesNewJarAndSwapsOldOne(@TempDir Path tempDir) throws Exception {
    Path oldJar = tempDir.resolve("all-time-wrestling-rpg-2.5.2.jar");
    Files.write(oldJar, minimalJarBytes());

    HttpServer server = startJarServer(minimalJarBytes());
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");

      Path result = Launcher.download(release, tempDir, oldJar);

      assertThat(result).isNotNull();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar")).exists();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.5.2.jar.old")).exists();
      assertThat(oldJar).doesNotExist();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void download_handlesFirstRunWithNoExistingJar(@TempDir Path tempDir) throws Exception {
    HttpServer server = startJarServer(minimalJarBytes());
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");

      Path result = Launcher.download(release, tempDir, null);

      assertThat(result).isNotNull();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar")).exists();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void download_abortsAndPreservesOldJarWhenDownloadIsCorrupt(@TempDir Path tempDir)
      throws Exception {
    Path oldJar = tempDir.resolve("all-time-wrestling-rpg-2.5.2.jar");
    Files.write(oldJar, minimalJarBytes());

    HttpServer server = startJarServer("THIS IS NOT A ZIP".getBytes());
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");

      Path result = Launcher.download(release, tempDir, oldJar);

      assertThat(result).isNull();
      assertThat(oldJar).exists();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar")).doesNotExist();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void download_returnsNullWhenServerIsUnreachable(@TempDir Path tempDir) throws Exception {
    Path oldJar = tempDir.resolve("all-time-wrestling-rpg-2.5.2.jar");
    Files.write(oldJar, minimalJarBytes());

    Launcher.ReleaseInfo release =
        new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:1/unreachable.jar");

    Path result = Launcher.download(release, tempDir, oldJar);

    assertThat(result).isNull();
    assertThat(oldJar).exists();
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private static byte[] minimalJarBytes() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      zos.write("Manifest-Version: 1.0\n".getBytes());
      zos.closeEntry();
    }
    return baos.toByteArray();
  }

  private static HttpServer startJsonServer(String path, String json) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    byte[] body = json.getBytes();
    server.createContext(
        path,
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
          }
        });
    server.start();
    return server;
  }

  private static HttpServer startJarServer(byte[] payload) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/app.jar",
        exchange -> {
          exchange.sendResponseHeaders(200, payload.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
          }
        });
    server.start();
    return server;
  }
}
