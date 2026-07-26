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
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
  void fetchLatestRelease_returnsNullWhenApiReturnsNon200() throws Exception {
    HttpServer server = startJsonServer("/releases", "Not Found");
    server.createContext(
        "/bad",
        exchange -> {
          exchange.sendResponseHeaders(404, 0);
          exchange.getResponseBody().close();
        });
    server.stop(0);

    // Repurpose: start a server that returns 503
    HttpServer errorServer = HttpServer.create(new InetSocketAddress(0), 0);
    errorServer.createContext(
        "/releases",
        exchange -> {
          exchange.sendResponseHeaders(503, 0);
          exchange.getResponseBody().close();
        });
    errorServer.start();
    int port = errorServer.getAddress().getPort();
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
    try {
      assertThat(Launcher.fetchLatestRelease()).isNull();
    } finally {
      System.clearProperty("atw.launcher.releases-api");
      errorServer.stop(0);
    }
  }

  @Test
  void fetchLatestRelease_returnsNullWhenTagNameMissing() throws Exception {
    String json =
        """
        {"html_url": "https://example.com/releases/v2.6.0",
         "assets": [{"browser_download_url": "https://example.com/app.jar"}]}
        """;
    HttpServer server = startJsonServer("/releases", json);
    int port = server.getAddress().getPort();
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
    try {
      assertThat(Launcher.fetchLatestRelease()).isNull();
    } finally {
      System.clearProperty("atw.launcher.releases-api");
      server.stop(0);
    }
  }

  @Test
  void fetchLatestRelease_returnsNullWhenNoJarAsset() throws Exception {
    String json =
        """
        {"tag_name": "v2.6.0",
         "assets": [{"browser_download_url": "https://example.com/app.war"}]}
        """;
    HttpServer server = startJsonServer("/releases", json);
    int port = server.getAddress().getPort();
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
    try {
      assertThat(Launcher.fetchLatestRelease()).isNull();
    } finally {
      System.clearProperty("atw.launcher.releases-api");
      server.stop(0);
    }
  }

  @Test
  void semver_parsesVersionSegments() {
    assertThat(Launcher.semver("2.6.0")).containsExactly(2, 6, 0);
    assertThat(Launcher.semver("1.0")).containsExactly(1, 0, 0);
    assertThat(Launcher.semver("3.0.0-RC1")).containsExactly(3, 0, 0);
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

  // ── app directory resolution ──────────────────────────────────────────────

  @Test
  void resolveAppDir_returnsMacPath_whenOsNameContainsMac() {
    String saved = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Mac OS X");
      Path dir = Launcher.resolveAppDir();
      assertThat(dir.toString()).contains("Application Support");
      assertThat(dir.getFileName().toString()).isEqualTo("ATW");
    } finally {
      System.setProperty("os.name", saved);
    }
  }

  @Test
  void resolveAppDir_returnsWindowsPath_whenOsNameContainsWin() {
    String saved = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Windows 10");
      Path dir = Launcher.resolveAppDir();
      assertThat(dir.getFileName().toString()).isEqualTo("ATW");
    } finally {
      System.setProperty("os.name", saved);
    }
  }

  @Test
  void resolveAppDir_returnsLinuxPath_whenOsNameIsOther() {
    String saved = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Linux");
      Path dir = Launcher.resolveAppDir();
      assertThat(dir.getFileName().toString()).isEqualTo("atw");
    } finally {
      System.setProperty("os.name", saved);
    }
  }

  // ── extractVersion ────────────────────────────────────────────────────────

  @Test
  void extractVersion_parsesVersionFromStandardFilename() {
    assertThat(Launcher.extractVersion("all-time-wrestling-rpg-2.6.0.jar")).isEqualTo("2.6.0");
  }

  @Test
  void extractVersion_parsesVersionWithPreReleaseSuffix() {
    assertThat(Launcher.extractVersion("all-time-wrestling-rpg-2.5.2-SNAPSHOT.jar"))
        .isEqualTo("2.5.2-SNAPSHOT");
  }

  @Test
  void extractVersion_returnsDefaultWhenFilenameDoesNotMatch() {
    assertThat(Launcher.extractVersion("unknown.jar")).isEqualTo("0.0.0");
    assertThat(Launcher.extractVersion("")).isEqualTo("0.0.0");
  }

  // ── findCurrentJar ────────────────────────────────────────────────────────

  @Test
  void findCurrentJar_returnsHighestVersionWhenMultipleJarsPresent(@TempDir Path dir)
      throws Exception {
    Files.write(dir.resolve("all-time-wrestling-rpg-2.5.0.jar"), new byte[] {});
    Files.write(dir.resolve("all-time-wrestling-rpg-2.6.0.jar"), new byte[] {});
    Files.write(dir.resolve("unrelated.txt"), new byte[] {});

    Optional<Path> result = Launcher.findCurrentJar(dir);
    assertThat(result).isPresent();
    assertThat(result.get().getFileName().toString()).isEqualTo("all-time-wrestling-rpg-2.6.0.jar");
  }

  @Test
  void findCurrentJar_returnsEmptyWhenNoJarPresent(@TempDir Path dir) throws Exception {
    Files.write(dir.resolve("readme.txt"), new byte[] {});
    assertThat(Launcher.findCurrentJar(dir)).isEmpty();
  }

  @Test
  void findCurrentJar_returnsEmptyWhenDirectoryDoesNotExist(@TempDir Path parent) throws Exception {
    assertThat(Launcher.findCurrentJar(parent.resolve("missing"))).isEmpty();
  }

  // ── cleanStaleTmp ─────────────────────────────────────────────────────────

  @Test
  void cleanStaleTmp_deletesStaleTemporaryFiles(@TempDir Path dir) throws Exception {
    Path stale = dir.resolve("old.jar.tmp");
    Files.write(stale, new byte[] {1});
    Files.setLastModifiedTime(stale, FileTime.from(Instant.now().minus(Duration.ofHours(2))));

    Launcher.cleanStaleTmp(dir);

    assertThat(stale).doesNotExist();
  }

  @Test
  void cleanStaleTmp_keepsRecentTemporaryFiles(@TempDir Path dir) throws Exception {
    Path fresh = dir.resolve("recent.jar.tmp");
    Files.write(fresh, new byte[] {1});

    Launcher.cleanStaleTmp(dir);

    assertThat(fresh).exists();
  }

  @Test
  void cleanStaleTmp_doesNothingWhenDirectoryDoesNotExist(@TempDir Path parent) throws Exception {
    Launcher.cleanStaleTmp(parent.resolve("no-such-dir")); // must not throw
  }

  // ── restoreBackupIfNeeded ─────────────────────────────────────────────────

  @Test
  void restoreBackupIfNeeded_restoresBackupWhenNoCurrentJar(@TempDir Path dir) throws Exception {
    Path backup = dir.resolve("all-time-wrestling-rpg-2.5.2.jar.old");
    Files.write(backup, minimalJarBytes());

    Launcher.restoreBackupIfNeeded(dir);

    assertThat(dir.resolve("all-time-wrestling-rpg-2.5.2.jar")).exists();
    assertThat(backup).doesNotExist();
  }

  @Test
  void restoreBackupIfNeeded_deletesBackupWhenCurrentJarExists(@TempDir Path dir) throws Exception {
    Files.write(dir.resolve("all-time-wrestling-rpg-2.6.0.jar"), minimalJarBytes());
    Path backup = dir.resolve("all-time-wrestling-rpg-2.5.2.jar.old");
    Files.write(backup, minimalJarBytes());

    Launcher.restoreBackupIfNeeded(dir);

    assertThat(dir.resolve("all-time-wrestling-rpg-2.6.0.jar")).exists();
    assertThat(backup).doesNotExist();
  }

  @Test
  void restoreBackupIfNeeded_doesNothingWhenNoBackupExists(@TempDir Path dir) throws Exception {
    Path current = dir.resolve("all-time-wrestling-rpg-2.6.0.jar");
    Files.write(current, minimalJarBytes());

    Launcher.restoreBackupIfNeeded(dir);

    assertThat(current).exists();
  }

  @Test
  void restoreBackupIfNeeded_doesNothingWhenDirectoryDoesNotExist(@TempDir Path parent)
      throws Exception {
    Launcher.restoreBackupIfNeeded(parent.resolve("no-such-dir")); // must not throw
  }

  // ── download non-200 ──────────────────────────────────────────────────────

  @Test
  void download_returnsNullWhenDownloadServerReturnsNon200(@TempDir Path tempDir) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/app.jar",
        exchange -> {
          exchange.sendResponseHeaders(503, 0);
          exchange.getResponseBody().close();
        });
    server.start();
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");
      assertThat(Launcher.download(release, tempDir, null)).isNull();
    } finally {
      server.stop(0);
    }
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
