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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherTest {

  /**
   * Forces the no-GUI seams for the whole class: several tests exercise {@code showError}/{@code
   * promptUser} paths, and without the property a display-equipped developer machine would pop a
   * real modal Swing dialog that blocks the suite forever. Restored afterwards so other test
   * classes are unaffected.
   */
  @BeforeAll
  static void forceHeadlessLauncherSeams() {
    System.setProperty("atw.launcher.headless", "true");
  }

  @AfterAll
  static void clearHeadlessLauncherSeams() {
    System.clearProperty("atw.launcher.headless");
  }

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

  // ── release candidate ordering + channel (ATW-rgxy) ───────────────────────

  @Test
  void isPreRelease_detectsPrereleaseSuffixes() {
    assertThat(Launcher.isPreRelease("2.10.0-RC1")).isTrue();
    assertThat(Launcher.isPreRelease("2.10.0-rc2")).isTrue();
    assertThat(Launcher.isPreRelease("2.10.0-BETA1")).isTrue();
    assertThat(Launcher.isPreRelease("2.10.0-SNAPSHOT")).isTrue();
    assertThat(Launcher.isPreRelease("2.10.0")).isFalse();
    assertThat(Launcher.isPreRelease(null)).isFalse();
  }

  @Test
  void isNewer_ordersRcVersionsWithinTheSameNumeric() {
    // RC install updating RC1 -> RC2 was impossible before suffix-aware ordering.
    assertThat(Launcher.isNewer("2.10.0-RC2", "2.10.0-RC1")).isTrue();
    assertThat(Launcher.isNewer("2.10.0-RC1", "2.10.0-RC2")).isFalse();
    // Final beats any RC of the same numeric; RC never shadows the final.
    assertThat(Launcher.isNewer("2.10.0", "2.10.0-RC1")).isTrue();
    assertThat(Launcher.isNewer("2.10.0-RC1", "2.10.0")).isFalse();
    // Higher numeric always wins regardless of suffix.
    assertThat(Launcher.isNewer("2.10.1-RC1", "2.10.0")).isTrue();
    assertThat(Launcher.isNewer("2.11.0-RC1", "2.10.0-RC9")).isTrue();
  }

  @Test
  void allowPreRelease_isImplicitForRcInstallsAndOptInForStable() {
    System.clearProperty("atw.launcher.allow-prerelease");
    try {
      // An RC install is on the prerelease channel by definition — it must be
      // able to discover RC2 and the final without any property.
      assertThat(Launcher.allowPreRelease("2.10.0-RC1")).isTrue();
      // Stable installs never see prereleases unless the property forces them.
      assertThat(Launcher.allowPreRelease("2.10.0")).isFalse();
      assertThat(Launcher.allowPreRelease(null)).isFalse();
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }

    System.setProperty("atw.launcher.allow-prerelease", "true");
    try {
      assertThat(Launcher.allowPreRelease("2.10.0")).isTrue();
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }
  }

  @Test
  void newestReleaseFromList_picksNewestStableWhenChannelClosed() {
    String json =
        """
        [
          {"tag_name": "v2.10.0-RC1", "assets": [{"browser_download_url": "https://x/rc1.jar"}]},
          {"tag_name": "v2.9.0", "assets": [{"browser_download_url": "https://x/2.9.0.jar"}]}
        ]
        """;
    // Stable install (no property): RC must be skipped, 2.9.0 wins.
    System.clearProperty("atw.launcher.allow-prerelease");
    try {
      Launcher.ReleaseInfo info = Launcher.newestReleaseFromList(json, "2.9.0");
      assertThat(info.version()).isEqualTo("2.9.0");
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }
  }

  @Test
  void newestReleaseFromList_letsRcInstallsSeeRcsAndFinals() {
    String json =
        """
        [
          {"tag_name": "v2.10.0-RC2", "assets": [{"browser_download_url": "https://x/rc2.jar"}]},
          {"tag_name": "v2.10.0-RC1", "assets": [{"browser_download_url": "https://x/rc1.jar"}]}
        ]
        """;
    System.clearProperty("atw.launcher.allow-prerelease");
    try {
      // RC1 install: RC2 is eligible and newest — this is the reporter's update path.
      Launcher.ReleaseInfo info = Launcher.newestReleaseFromList(json, "2.10.0-RC1");
      assertThat(info.version()).isEqualTo("2.10.0-RC2");
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }
  }

  @Test
  void newestReleaseFromList_prefersFinalOverRcOfSameNumeric() {
    String json =
        """
        [
          {"tag_name": "v2.10.0", "assets": [{"browser_download_url": "https://x/final.jar"}]},
          {"tag_name": "v2.10.0-RC2", "assets": [{"browser_download_url": "https://x/rc2.jar"}]}
        ]
        """;
    System.clearProperty("atw.launcher.allow-prerelease");
    try {
      Launcher.ReleaseInfo info = Launcher.newestReleaseFromList(json, "2.10.0-RC1");
      assertThat(info.version()).isEqualTo("2.10.0");
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }
  }

  @Test
  void newestReleaseFromList_skipsReleasesWithoutJarAssets() {
    String json =
        """
        [
          {"tag_name": "v2.10.0", "assets": [{"browser_download_url": "https://x/app.war"}]},
          {"tag_name": "v2.9.0", "assets": [{"browser_download_url": "https://x/2.9.0.jar"}]}
        ]
        """;
    System.clearProperty("atw.launcher.allow-prerelease");
    try {
      Launcher.ReleaseInfo info = Launcher.newestReleaseFromList(json, "2.9.0");
      assertThat(info.version()).isEqualTo("2.9.0");
    } finally {
      System.clearProperty("atw.launcher.allow-prerelease");
    }
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

  /**
   * GitHub release-asset URLs respond with HTTP 302 to release-assets.githubusercontent.com
   * (ATW-mcwe). With the default NEVER redirect policy every fresh-install download failed with
   * "Download failed with HTTP 302". The launcher must follow redirects.
   */
  @Test
  void download_followsRedirects(@TempDir Path tempDir) throws Exception {
    HttpServer target = startJarServer(minimalJarBytes());
    int targetPort = target.getAddress().getPort();

    HttpServer redirector = HttpServer.create(new InetSocketAddress(0), 0);
    redirector.createContext(
        "/app.jar",
        exchange -> {
          exchange
              .getResponseHeaders()
              .add("Location", "http://127.0.0.1:" + targetPort + "/app.jar");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    redirector.start();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo(
              "2.6.0", "http://127.0.0.1:" + redirector.getAddress().getPort() + "/app.jar");

      Path result = Launcher.download(release, tempDir, null);

      assertThat(result).isNotNull();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar")).exists();
    } finally {
      redirector.stop(0);
      target.stop(0);
    }
  }

  /**
   * The launcher JAR must carry every Launcher*.class — nested classes are separate class files
   * (ATW-mcwe). The pre-fix include pattern shipped only Launcher.class, so fetchLatestRelease
   * crashed with NoClassDefFoundError: Launcher$ReleaseInfo the moment the update check ran. Skips
   * when the packaged launcher JAR is absent (plain mvn test without the package phase).
   */
  @Test
  void packagedLauncherJar_containsAllLauncherClasses() throws Exception {
    Path launcherJar = Path.of("target", "all-time-wrestling-rpg-2.10.0-SNAPSHOT-launcher.jar");
    if (!Files.isRegularFile(launcherJar)) {
      // Glob across versions so the test survives version bumps without edits.
      try (var stream = Files.list(Path.of("target"))) {
        launcherJar =
            stream
                .filter(
                    p ->
                        p.getFileName()
                            .toString()
                            .matches("all-time-wrestling-rpg-.*-launcher\\.jar"))
                .findFirst()
                .orElse(null);
      }
    }
    if (launcherJar == null || !Files.isRegularFile(launcherJar)) {
      System.out.println(
          "[LauncherTest] No packaged launcher JAR found — skipping packaged-jar check.");
      return;
    }

    // Every Launcher*.class compiled from the main sources must be in the packaged JAR.
    try (var compiled =
        Files.list(Path.of("target", "classes", "com", "github", "javydreamercsw"))) {
      List<String> expected =
          compiled
              .map(p -> p.getFileName().toString())
              .filter(n -> n.startsWith("Launcher") && n.endsWith(".class"))
              .toList();
      assertThat(expected).isNotEmpty();

      try (ZipFile zip = new ZipFile(launcherJar.toFile())) {
        for (String classFile : expected) {
          assertThat(zip.getEntry("com/github/javydreamercsw/" + classFile))
              .as("launcher JAR must contain %s", classFile)
              .isNotNull();
        }
      }
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

  // ── end-to-end run() flow (real child processes) ──────────────────────────

  @Test
  void run_returnsErrorWhenNoCurrentJarAndReleaseFetchFails(@TempDir Path tempDir)
      throws Exception {
    withIsolatedHome(
        tempDir,
        () -> {
          // A reserved-then-closed port makes the release fetch fail without touching
          // the real GitHub API (which would trigger a real download + app launch).
          setReleasesApiToDeadPort();

          assertThat(Launcher.run(new String[0])).isEqualTo(1);
        });
  }

  @Test
  void run_launchesExistingVersionWhenUpdateCheckFails(@TempDir Path tempDir) throws Exception {
    withIsolatedHome(
        tempDir,
        () -> {
          Files.write(appJar(tempDir, "1.0.0"), minimalJarBytes());
          setReleasesApiToDeadPort();

          // The bare manifest JAR has no Main-Class: the child exits non-zero and the
          // launcher reports the crash instead of hanging.
          assertThat(Launcher.run(new String[0])).isNotZero();
        });
  }

  @Test
  void run_returnsErrorWhenDownloadFailsAndNoLocalJarExists(@TempDir Path tempDir)
      throws Exception {
    withIsolatedHome(
        tempDir,
        () -> {
          HttpServer deadAsset = HttpServer.create(new InetSocketAddress(0), 0);
          int deadPort = deadAsset.getAddress().getPort();
          deadAsset.stop(0); // reserved then closed: download must fail
          String json =
              """
              {"tag_name": "v2.6.0",
               "assets": [{"browser_download_url": "http://127.0.0.1:%d/app.jar"}]}
              """
                  .formatted(deadPort);
          HttpServer api = startJsonServer("/releases", json);
          int port = api.getAddress().getPort();
          System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
          try {
            assertThat(Launcher.run(new String[0])).isEqualTo(1);
          } finally {
            System.clearProperty("atw.launcher.releases-api");
            api.stop(0);
          }
        });
  }

  @Test
  void run_downloadsNewerReleaseAndLaunchesIt(@TempDir Path tempDir) throws Exception {
    withIsolatedHome(
        tempDir,
        () -> {
          Path smokeJar = buildExecutableJar(tempDir, "SmokeApp", "SmokeApp");
          byte[] payload = Files.readAllBytes(smokeJar);
          HttpServer asset = startJarServer(payload);
          int assetPort = asset.getAddress().getPort();
          String json =
              """
              {"tag_name": "v99.0.0-smoke",
               "assets": [{"browser_download_url": "http://127.0.0.1:%d/app.jar"}]}
              """
                  .formatted(assetPort);
          HttpServer api = startJsonServer("/releases", json);
          int port = api.getAddress().getPort();
          System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
          try {
            assertThat(Launcher.run(new String[0])).isZero();
            // The downloaded JAR was swapped into the app directory.
            assertThat(appJar(tempDir, "99.0.0-smoke")).exists();
          } finally {
            System.clearProperty("atw.launcher.releases-api");
            api.stop(0);
            asset.stop(0);
          }
        });
  }

  @Test
  void launchApp_propagatesChildExitCodeWhenAppCrashesImmediately(@TempDir Path tempDir)
      throws Exception {
    Path crashJar = buildExecutableJar(tempDir, "CrashApp", "CrashApp");

    assertThat(Launcher.launchApp(crashJar, new String[0])).isEqualTo(3);
  }

  @Test
  void launchApp_returnsZeroWhenChildRunsCleanly(@TempDir Path tempDir) throws Exception {
    Path smokeJar = buildExecutableJar(tempDir, "SmokeApp", "SmokeApp");

    assertThat(Launcher.launchApp(smokeJar, new String[0])).isZero();
  }

  @Test
  void download_rejectsValidButEmptyZip(@TempDir Path tempDir) throws Exception {
    // A ZIP with zero entries is structurally valid but carries no application —
    // the launcher must treat it as corrupt rather than swap it in.
    ByteArrayOutputStream emptyZip = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(emptyZip)) {
      // no entries
    }
    HttpServer server = startJarServer(emptyZip.toByteArray());
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");
      assertThat(Launcher.download(release, tempDir, null)).isNull();
      assertThat(tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar.tmp")).doesNotExist();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void download_failsCleanlyWhenTmpPathIsOccupiedByDirectory(@TempDir Path tempDir)
      throws Exception {
    // A non-empty directory where the .tmp file would land makes both the copy and the
    // cleanup's deleteIfExists fail — the launcher must still return null, not crash.
    Path tmpDir = tempDir.resolve("all-time-wrestling-rpg-2.6.0.jar.tmp");
    Files.createDirectories(tmpDir);
    Files.writeString(tmpDir.resolve("junk"), "x");
    HttpServer server = startJarServer(minimalJarBytes());
    int port = server.getAddress().getPort();
    try {
      Launcher.ReleaseInfo release =
          new Launcher.ReleaseInfo("2.6.0", "http://127.0.0.1:" + port + "/app.jar");
      assertThat(Launcher.download(release, tempDir, null)).isNull();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void main_exitsWithErrorWhenReleaseFetchFails(@TempDir Path tempDir) throws Exception {
    // Runs the real main() in a fresh JVM — System.exit would kill this one. Covers the
    // entry point plus the fatal no-jar/no-release path end to end.
    Path classes =
        Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    setReleasesApiToDeadPort();
    try {
      Process process =
          new ProcessBuilder(
                  Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                  "-Duser.home=" + tempDir,
                  "-Datw.launcher.headless=true",
                  "-Datw.launcher.releases-api=" + System.getProperty("atw.launcher.releases-api"),
                  "-cp",
                  classes.toString(),
                  "com.github.javydreamercsw.Launcher")
              .redirectErrorStream(true)
              .start();
      String output;
      try (InputStream in = process.getInputStream()) {
        output = new String(in.readAllBytes());
      }
      assertThat(process.waitFor()).isEqualTo(1);
      assertThat(output).contains("Could not connect to GitHub");
    } finally {
      System.clearProperty("atw.launcher.releases-api");
    }
  }

  // ── GUI availability / user interaction seams ─────────────────────────────
  // The class-level @BeforeAll forces the headless seam, so every test here sees
  // the no-GUI behavior without touching the property itself.

  @Test
  void isGuiAvailable_isFalseWhenHeadlessPropertyIsSet() {
    assertThat(Launcher.isGuiAvailable()).isFalse();
  }

  @Test
  void promptUser_refusesUpdateWhenNoGuiIsAvailable() {
    assertThat(Launcher.promptUser("9.9.9")).isFalse();
  }

  @Test
  void showError_writesToStderrWithoutBlockingWhenNoGuiIsAvailable() {
    Launcher.showError("boom"); // must not throw or block
  }

  // ── java executable resolution ────────────────────────────────────────────

  @Test
  void javaBinaryName_appendsExeSuffixOnlyOnWindows() {
    assertThat(Launcher.javaBinaryName("Windows 11")).isEqualTo("java.exe");
    assertThat(Launcher.javaBinaryName("Mac OS X")).isEqualTo("java");
    assertThat(Launcher.javaBinaryName("Linux")).isEqualTo("java");
    assertThat(Launcher.javaBinaryName(null)).isEqualTo("java");
  }

  @Test
  void resolveJavaExecutable_fallsBackWhenPackagedRuntimeIsMissing(@TempDir Path tempDir)
      throws Exception {
    String realJavaHome = System.getProperty("java.home");
    // java.home without bin/java forces the process-command fallback branch.
    System.setProperty("java.home", tempDir.toString());
    try {
      String resolved = Launcher.resolveJavaExecutable();
      assertThat(resolved).isNotBlank();
      assertThat(resolved.endsWith("java") || resolved.endsWith("java.exe")).isTrue();
    } finally {
      System.setProperty("java.home", realJavaHome);
    }
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  /**
   * Points the launcher at a localhost port that was bound and immediately released: every request
   * fails fast and locally. Far safer than leaving the releases-api property unset, where the
   * launcher would consult the real GitHub API and actually download + launch the production app.
   */
  private static void setReleasesApiToDeadPort() throws IOException {
    HttpServer dead = HttpServer.create(new InetSocketAddress(0), 0);
    int port = dead.getAddress().getPort();
    dead.stop(0);
    System.setProperty("atw.launcher.releases-api", "http://127.0.0.1:" + port + "/releases");
  }

  /**
   * Points {@code user.home} at an isolated directory so the launcher's app dir ({@code
   * resolveAppDir()}) starts empty, and forces the headless no-GUI seams. Restores everything
   * afterwards. Used by the {@code run()} end-to-end tests.
   */
  private static void withIsolatedHome(Path home, Thrower body) throws Exception {
    String realHome = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
    try {
      body.run();
    } finally {
      System.setProperty("user.home", realHome);
      System.clearProperty("atw.launcher.releases-api");
    }
  }

  @FunctionalInterface
  private interface Thrower {
    void run() throws Exception;
  }

  private static Path appJar(Path home, String version) throws IOException {
    // Mirrors Launcher.resolveAppDir() for the current OS.
    String os = System.getProperty("os.name", "").toLowerCase();
    Path appDir;
    if (os.contains("mac")) {
      appDir = home.resolve("Library/Application Support/ATW");
    } else if (os.contains("win")) {
      appDir = home.resolve("ATW");
    } else {
      appDir = home.resolve(".local/share/atw");
    }
    Files.createDirectories(appDir);
    return appDir.resolve("all-time-wrestling-rpg-" + version + ".jar");
  }

  /** Compiles a single-class program and packages it as a runnable JAR via the JDK tools. */
  private static Path buildExecutableJar(Path dir, String className, String mainClass)
      throws Exception {
    String source =
        """
        public class %s {
          public static void main(String[] args) {
            %s
          }
        }
        """
            .formatted(
                className,
                "CrashApp".equals(className)
                    ? "System.exit(3);"
                    : "System.out.println(\"started\");");
    Path src = dir.resolve(className + ".java");
    Files.writeString(src, source);
    Path classes = dir.resolve("classes");
    Files.createDirectories(classes);
    var javac = ToolProvider.getSystemJavaCompiler();
    if (javac == null) {
      throw new IllegalStateException("No system Java compiler — a full JDK is required");
    }
    if (javac.run(null, null, null, "-d", classes.toString(), src.toString()) != 0) {
      throw new IllegalStateException("Failed to compile " + className);
    }
    Path jar = dir.resolve(className + ".jar");
    var jarTool =
        java.util.spi.ToolProvider.findFirst("jar")
            .orElseThrow(() -> new IllegalStateException("No jar tool available"));
    if (jarTool.run(
            System.out,
            System.err,
            "--create",
            "--file",
            jar.toString(),
            "--main-class",
            mainClass,
            "-C",
            classes.toString(),
            className + ".class")
        != 0) {
      throw new IllegalStateException("Failed to package " + className);
    }
    return jar;
  }

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

  @Test
  void resolveJavaExecutable_prefersPackagedRuntime() {
    String resolved = Launcher.resolveJavaExecutable();
    // java.home is always set in a JVM, and a JDK/JRE image always carries bin/java.
    assertThat(resolved).contains("bin");
    assertThat(Path.of(resolved).getFileName().toString()).startsWith("java");
  }
}
