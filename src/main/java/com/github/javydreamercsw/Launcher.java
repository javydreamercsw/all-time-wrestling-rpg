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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;

/**
 * Zero-dependency bootstrap launcher. Downloads or updates the full application JAR from GitHub
 * Releases and execs it. Bundled inside the desktop installers (DMG/MSI/DEB) via jpackage so the
 * installers themselves never need to be reinstalled when a new app version ships.
 */
public final class Launcher {

  private static final String REPO = "javydreamercsw/all-time-wrestling-rpg";
  private static final String RELEASES_API =
      "https://api.github.com/repos/" + REPO + "/releases/latest";
  private static final Pattern JAR_PATTERN =
      Pattern.compile("all-time-wrestling-rpg-([0-9]+(?:\\.[0-9]+){0,2}(?:-[^.]+)?)\\.jar");
  private static final Duration STALE_TMP_THRESHOLD = Duration.ofHours(1);

  public static void main(final String[] args) throws Exception {
    Path appDir = resolveAppDir();
    Files.createDirectories(appDir);

    cleanStaleTmp(appDir);
    restoreBackupIfNeeded(appDir);

    Path currentJar = findCurrentJar(appDir).orElse(null);
    String currentVersion =
        currentJar == null ? null : extractVersion(currentJar.getFileName().toString());

    ReleaseInfo release = fetchLatestRelease();

    if (release == null) {
      if (currentJar == null) {
        showError(
            "Could not connect to GitHub to download the application.\n"
                + "Check your network and try again.");
        System.exit(1);
      }
      System.out.println(
          "[Launcher] Update check failed — launching existing version " + currentVersion);
      launchApp(currentJar, args);
      return;
    }

    boolean needsDownload = currentJar == null || isNewer(release.version(), currentVersion);

    if (needsDownload) {
      boolean doUpdate = currentJar == null || promptUser(release.version());
      if (doUpdate) {
        Path newJar = download(release, appDir, currentJar);
        if (newJar != null) {
          currentJar = newJar;
        }
      }
    }

    if (currentJar == null) {
      showError("No application JAR found. Download failed.\nCheck your network and try again.");
      System.exit(1);
    }

    launchApp(currentJar, args);
  }

  // -------------------------------------------------------------------------
  // App directory resolution
  // -------------------------------------------------------------------------

  static Path resolveAppDir() {
    String os = System.getProperty("os.name", "").toLowerCase();
    String home = System.getProperty("user.home");
    if (os.contains("mac")) {
      return Path.of(home, "Library", "Application Support", "ATW");
    } else if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      return Path.of(appData != null ? appData : home, "ATW");
    } else {
      String xdg = System.getenv("XDG_DATA_HOME");
      return Path.of(xdg != null ? xdg : home + "/.local/share", "atw");
    }
  }

  // -------------------------------------------------------------------------
  // JAR discovery
  // -------------------------------------------------------------------------

  static Optional<Path> findCurrentJar(final Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return Optional.empty();
    }
    return Files.list(dir)
        .filter(p -> JAR_PATTERN.matcher(p.getFileName().toString()).matches())
        .max(Comparator.comparing(p -> extractVersion(p.getFileName().toString())));
  }

  static String extractVersion(final String filename) {
    Matcher m = JAR_PATTERN.matcher(filename);
    return m.matches() ? m.group(1) : "0.0.0";
  }

  // -------------------------------------------------------------------------
  // GitHub release fetch
  // -------------------------------------------------------------------------

  record ReleaseInfo(String version, String jarUrl) {}

  private static String releasesApiUrl() {
    String override = System.getProperty("atw.launcher.releases-api");
    return override != null ? override : RELEASES_API;
  }

  static ReleaseInfo fetchLatestRelease() {
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(releasesApiUrl()))
              .header("Accept", "application/vnd.github+json")
              .header("User-Agent", "all-time-wrestling-rpg-launcher")
              .timeout(Duration.ofSeconds(15))
              .GET()
              .build();

      HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() != 200) {
        return null;
      }

      String body = resp.body();
      String tag = extractJsonString(body, "tag_name");
      String version = tag != null && tag.startsWith("v") ? tag.substring(1) : tag;
      String jarUrl = findJarAssetUrl(body);

      return version != null && jarUrl != null ? new ReleaseInfo(version, jarUrl) : null;
    } catch (Exception e) {
      System.err.println("[Launcher] Could not fetch release info: " + e.getMessage());
      return null;
    }
  }

  private static String extractJsonString(final String json, final String key) {
    Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
    Matcher m = p.matcher(json);
    return m.find() ? m.group(1) : null;
  }

  private static String findJarAssetUrl(final String json) {
    // Find browser_download_url entries that end in .jar but not .war
    Pattern p = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
    Matcher m = p.matcher(json);
    while (m.find()) {
      String url = m.group(1);
      if (!url.endsWith(".war")) {
        return url;
      }
    }
    return null;
  }

  // -------------------------------------------------------------------------
  // Version comparison
  // -------------------------------------------------------------------------

  static boolean isNewer(final String candidate, final String current) {
    try {
      int[] c = semver(candidate);
      int[] r = semver(current);
      for (int i = 0; i < 3; i++) {
        if (c[i] != r[i]) {
          return c[i] > r[i];
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  static int[] semver(final String v) {
    String numeric = v.replaceAll("[^0-9.].*$", "");
    if (numeric.isBlank()) {
      throw new IllegalArgumentException("Unparseable version: " + v);
    }
    String[] parts = numeric.split("\\.");
    int[] result = new int[3];
    for (int i = 0; i < 3 && i < parts.length; i++) {
      result[i] = parts[i].isBlank() ? 0 : Integer.parseInt(parts[i]);
    }
    return result;
  }

  // -------------------------------------------------------------------------
  // Download with safe swap
  // -------------------------------------------------------------------------

  static Path download(final ReleaseInfo release, final Path appDir, final Path oldJar) {
    Path newJarName = appDir.resolve("all-time-wrestling-rpg-" + release.version() + ".jar");
    Path tmpPath = appDir.resolve("all-time-wrestling-rpg-" + release.version() + ".jar.tmp");
    Path oldBackup =
        oldJar != null ? appDir.resolve(oldJar.getFileName().toString() + ".old") : null;

    System.out.println("[Launcher] Downloading v" + release.version() + "...");
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(release.jarUrl()))
              .header("User-Agent", "all-time-wrestling-rpg-launcher")
              .timeout(Duration.ofMinutes(10))
              .GET()
              .build();

      HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
      if (resp.statusCode() != 200) {
        System.err.println("[Launcher] Download failed with HTTP " + resp.statusCode());
        return null;
      }

      try (InputStream in = resp.body()) {
        Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
      }

      // Validate it's a valid ZIP/JAR before swapping
      try (ZipFile zf = new ZipFile(tmpPath.toFile())) {
        if (zf.size() == 0) {
          throw new IOException("Empty ZIP");
        }
      } catch (IOException e) {
        System.err.println("[Launcher] Downloaded file is corrupt — aborting update.");
        Files.deleteIfExists(tmpPath);
        return null;
      }

      // Safe swap: rename old → .old, then tmp → new
      if (oldJar != null && Files.exists(oldJar)) {
        Files.move(oldJar, oldBackup, StandardCopyOption.REPLACE_EXISTING);
      }
      Files.move(tmpPath, newJarName, StandardCopyOption.REPLACE_EXISTING);

      System.out.println("[Launcher] Download complete: " + newJarName.getFileName());
      return newJarName;

    } catch (Exception e) {
      System.err.println("[Launcher] Download error: " + e.getMessage());
      try {
        Files.deleteIfExists(tmpPath);
      } catch (IOException ignored) {
      }
      return null;
    }
  }

  // -------------------------------------------------------------------------
  // Launch the app JAR as a child process; wait briefly to detect a crash
  // -------------------------------------------------------------------------

  private static void launchApp(final Path jar, final String[] extraArgs) throws Exception {
    String javaExe =
        ProcessHandle.current()
            .info()
            .command()
            .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());

    List<String> cmd = new ArrayList<>();
    cmd.add(javaExe);
    cmd.add("-jar");
    cmd.add(jar.toAbsolutePath().toString());
    cmd.add("--atw.desktop.enabled=true");
    cmd.add("--spring.profiles.active=prod,h2");
    for (String arg : extraArgs) {
      cmd.add(arg);
    }

    System.out.println("[Launcher] Starting " + jar.getFileName());
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.inheritIO();
    Process process = pb.start();

    // Wait up to 5 s to catch an immediate crash (bad JAR, wrong Java version, etc.)
    boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
    if (exited && process.exitValue() != 0) {
      System.err.println(
          "[Launcher] Application exited immediately with code "
              + process.exitValue()
              + " — restoring previous version if available.");
      restoreBackupIfNeeded(jar.getParent());
      showError(
          "The application failed to start (exit code "
              + process.exitValue()
              + ").\n"
              + "The previous version has been restored. Please try again.");
      System.exit(process.exitValue());
    }

    // Normal case: app is running; wait for it to finish then exit with its code
    System.exit(process.waitFor());
  }

  // -------------------------------------------------------------------------
  // Cleanup helpers
  // -------------------------------------------------------------------------

  static void cleanStaleTmp(final Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Instant cutoff = Instant.now().minus(STALE_TMP_THRESHOLD);
    Files.list(dir)
        .filter(p -> p.getFileName().toString().endsWith(".tmp"))
        .filter(
            p -> {
              try {
                return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
              } catch (IOException e) {
                return false;
              }
            })
        .forEach(
            p -> {
              try {
                Files.delete(p);
              } catch (IOException e) {
                /* ignore */
              }
            });
  }

  static void restoreBackupIfNeeded(final Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    // If a .old backup exists but no current JAR, restore it
    Optional<Path> backup =
        Files.list(dir).filter(p -> p.getFileName().toString().endsWith(".jar.old")).findFirst();
    if (backup.isEmpty()) {
      return;
    }
    Optional<Path> current = findCurrentJar(dir);
    if (current.isEmpty()) {
      String name = backup.get().getFileName().toString().replace(".old", "");
      Files.move(backup.get(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
      System.out.println("[Launcher] Restored backup JAR: " + name);
    } else {
      // Current JAR exists and appears healthy — safe to remove old backup
      Files.deleteIfExists(backup.get());
    }
  }

  // -------------------------------------------------------------------------
  // UI helpers
  // -------------------------------------------------------------------------

  private static boolean promptUser(final String newVersion) {
    if (System.console() != null) {
      System.out.print("[Launcher] Update v" + newVersion + " available. Apply now? [y/N] ");
      String response = System.console().readLine();
      return response != null && response.trim().equalsIgnoreCase("y");
    }
    // GUI prompt for desktop installs
    int choice =
        JOptionPane.showConfirmDialog(
            null,
            "All Time Wrestling RPG v" + newVersion + " is available.\nDownload and apply update?",
            "Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
    return choice == JOptionPane.YES_OPTION;
  }

  private static void showError(final String message) {
    System.err.println("[Launcher] " + message);
    try {
      JOptionPane.showMessageDialog(null, message, "Launcher Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception ignored) {
    }
  }

  private Launcher() {}
}
