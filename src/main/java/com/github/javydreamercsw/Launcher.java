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

import java.awt.GraphicsEnvironment;
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
import java.util.concurrent.TimeUnit;
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
    System.exit(run(args));
  }

  /**
   * Launcher flow; returns the process exit code (0 = the app JAR ran, 1 = fatal). Visible for
   * testing — {@link #main} turns the return value into {@code System.exit}.
   */
  static int run(final String[] args) throws Exception {
    Path appDir = resolveAppDir();
    Files.createDirectories(appDir);

    cleanStaleTmp(appDir);
    restoreBackupIfNeeded(appDir);
    seedBundledJar(appDir);

    Path currentJar = findCurrentJar(appDir).orElse(null);
    String currentVersion =
        currentJar == null ? null : extractVersion(currentJar.getFileName().toString());

    ReleaseInfo release = fetchLatestRelease(currentVersion);

    if (release == null) {
      if (currentJar == null) {
        showError(
            "Could not connect to GitHub to download the application.\n"
                + "Check your network and try again.");
        return 1;
      }
      System.out.println(
          "[Launcher] Update check failed — launching existing version " + currentVersion);
      return launchApp(currentJar, args);
    }

    boolean needsDownload = currentJar == null || isNewer(release.version(), currentVersion);

    // Floor: prefer the bundled/local version over a release older than the installer that
    // shipped this launcher — /releases/latest can lag the installer (e.g. RC installed while
    // latest stable is older). The floor NEVER blocks the only path to a runnable app: with no
    // local JAR at all, downloading something older beats failing (ATW-ykmu).
    String installerVersion = installerVersion();
    boolean floorBlocks =
        needsDownload
            && currentJar != null
            && installerVersion != null
            && isNewer(installerVersion, release.version());
    if (floorBlocks) {
      System.out.println(
          "[Launcher] Release v"
              + release.version()
              + " is older than this installer (v"
              + installerVersion
              + ") — keeping the bundled/local version.");
      needsDownload = false;
    }

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
      return 1;
    }

    return launchApp(currentJar, args);
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

  private static String releasesListApiUrl() {
    String override = System.getProperty("atw.launcher.releases-api");
    if (override != null) {
      // Tests and CI point the override at a single-release JSON document; keep it.
      return override;
    }
    // /releases returns the list including prereleases (newest first), unlike
    // /releases/latest which by GitHub's definition excludes them.
    return "https://api.github.com/repos/" + REPO + "/releases?per_page=10";
  }

  /**
   * Prerelease channel: forced by property, implicit when the install itself is an RC, or implicit
   * when the *installer* that placed this launcher is a prerelease (first run — no app JAR yet).
   */
  static boolean allowPreRelease(final String currentVersion) {
    return Boolean.getBoolean("atw.launcher.allow-prerelease")
        || isPreRelease(currentVersion)
        || isPreRelease(installerVersion());
  }

  /**
   * The version of the installer that shipped this launcher, parsed from the launcher JAR's own
   * filename ({@code atw-launcher-2.10.0-RC2.jar} in the jpackage app dir). Null when unknown —
   * bare runs during development. {@code atw.launcher.self} overrides the launcher location
   * (tests); production never sets it.
   */
  static String installerVersion() {
    try {
      Path launcher = selfLocation();
      Matcher m =
          Pattern.compile("atw-launcher-(.+)\\.jar").matcher(launcher.getFileName().toString());
      return m.matches() ? m.group(1) : null;
    } catch (Exception e) {
      return null;
    }
  }

  /** Where this launcher JAR lives; {@code atw.launcher.self} property overrides (tests). */
  private static Path selfLocation() throws Exception {
    String override = System.getProperty("atw.launcher.self");
    if (override != null) {
      return Path.of(override);
    }
    return Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  /**
   * Picks the newest release from a GitHub releases-list JSON array. Pre-releases are only eligible
   * when the channel allows them; otherwise the newest stable release wins. Falls back to the
   * newest stable when a pre-release carries no JAR asset.
   */
  static ReleaseInfo newestReleaseFromList(final String json, final String currentVersion) {
    // The list is sorted newest-first by GitHub; walk it in order.
    Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    Matcher tagMatcher = tagPattern.matcher(json);
    ReleaseInfo best = null;
    int bestRank = Integer.MIN_VALUE;
    int cursor = 0;
    while (tagMatcher.find()) {
      int start = tagMatcher.start();
      int end =
          json.indexOf("\"tag_name\"", tagMatcher.end()) == -1
              ? json.length()
              : json.indexOf("\"tag_name\"", tagMatcher.end());
      String body = json.substring(cursor, Math.max(end, start));
      cursor = Math.max(end, start);

      String tag = tagMatcher.group(1);
      String version = tag.startsWith("v") ? tag.substring(1) : tag;
      String jarUrl = findJarAssetUrl(body);
      if (jarUrl == null) {
        continue;
      }
      boolean allowed = !isPreRelease(version) || allowPreRelease(currentVersion);
      if (!allowed) {
        continue;
      }
      int rank = 0;
      try {
        int[] s = semver(version);
        rank = s[0] * 1_000_000 + s[1] * 1_000 + s[2];
      } catch (Exception e) {
        continue;
      }
      // Two-digit pre-release slot: finals get 99, RCn gets min(n, 98). A finals
      // rank of Integer.MAX_VALUE would overflow the *100 fold and flip the order.
      int slot =
          preReleaseRank(version) == Integer.MAX_VALUE ? 99 : Math.min(preReleaseRank(version), 98);
      rank = rank * 100 + slot;
      if (best == null || rank > bestRank) {
        best = new ReleaseInfo(version, jarUrl);
        bestRank = rank;
      }
    }
    return best;
  }

  static ReleaseInfo fetchLatestRelease() {
    return fetchLatestRelease(null);
  }

  static ReleaseInfo fetchLatestRelease(final String currentVersion) {
    try {
      HttpClient client =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(10))
              // GitHub API and asset URLs redirect (302); default policy NEVER fails them.
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();
      // The prerelease channel must hit the LIST endpoint: /releases/latest
      // never returns prereleases, so an RC install could otherwise only ever
      // discover final releases.
      boolean preChannel = allowPreRelease(currentVersion);
      String apiUrl =
          preChannel && System.getProperty("atw.launcher.releases-api") == null
              ? releasesListApiUrl()
              : releasesApiUrl();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(apiUrl))
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
      if (preChannel) {
        // List endpoint returns an array; the single-release override returns one object.
        String listBody = body.trim().startsWith("[") ? body : "[" + body + "]";
        return newestReleaseFromList(listBody, currentVersion);
      }

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

  /**
   * True when the version carries a pre-release suffix (RC, beta, snapshot...). The stable channel
   * must never see pre-release builds, and RC installs must be able to update to a higher RC of
   * themselves, so suffix ordering matters ({@code 2.10.0-RC2 > 2.10.0-RC1}, both {@code <
   * 2.10.0}).
   */
  static boolean isPreRelease(final String version) {
    return version != null && version.matches(".*-(?i)(rc|beta|alpha|snapshot|milestone).*");
  }

  /** Numeric part only — the ordering key for release comparison. */
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

  /**
   * Ordering weight: any numeric version (final {@code 2.10.0}) outranks a pre-release of the same
   * numeric ({@code 2.10.0-RC1}); higher RC numbers outrank lower ones. -1 marks a version too
   * malformed to compare.
   */
  static int preReleaseRank(final String version) {
    if (!isPreRelease(version)) {
      return Integer.MAX_VALUE;
    }
    Matcher m = Pattern.compile("(?i)-rc[\\s_-]?(\\d+)").matcher(version);
    return m.find() ? Integer.parseInt(m.group(1)) : -1;
  }

  static boolean isNewer(final String candidate, final String current) {
    try {
      int[] c = semver(candidate);
      int[] r = semver(current);
      for (int i = 0; i < 3; i++) {
        if (c[i] != r[i]) {
          return c[i] > r[i];
        }
      }
      // Same numeric version: a final beats an RC, a higher RC beats a lower one.
      return preReleaseRank(candidate) > preReleaseRank(current);
    } catch (Exception e) {
      return false;
    }
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
      HttpClient client =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(15))
              // GitHub asset URLs redirect to release-assets.githubusercontent.com.
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();
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

  /**
   * Spawns the app JAR as a child process and returns its exit code. Waits up to 5 s to detect an
   * immediate crash (bad JAR, wrong Java version, etc.); a still-running child keeps waiting
   * normally.
   */
  static int launchApp(final Path jar, final String[] extraArgs) throws Exception {
    String javaExe = resolveJavaExecutable();

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
    boolean exited = process.waitFor(5, TimeUnit.SECONDS);
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
      return process.exitValue();
    }

    // Normal case: app is running; wait for it to finish then propagate its code
    return process.waitFor();
  }

  // -------------------------------------------------------------------------
  // Cleanup helpers
  // -------------------------------------------------------------------------

  /**
   * Seeds the app dir from the bundled JAR that ships inside the installer (ATW-ykmu). jpackage
   * wraps {@code atw-app-VERSION.jar} into the same directory as the launcher, so first run works
   * offline and is version-exact — never whatever {@code /releases/latest} happens to return. The
   * bundled JAR is copied in when the app dir has no JAR, or when the bundled one is newer than
   * what is there (e.g. the installer was reinstalled/upgraded without the launcher downloading
   * yet). Returns the version of the seeded JAR, or null when nothing was seeded.
   */
  static String seedBundledJar(final Path appDir) throws IOException {
    Path bundled = findBundledAppJar();
    if (bundled == null) {
      return null;
    }
    // Version comes from the bundled file's own name (atw-app-2.10.0-RC2.jar) — the
    // app JAR pattern does not match this filename, so extractVersion cannot be used.
    Matcher bundledMatcher =
        Pattern.compile("atw-app-(.+)\\.jar").matcher(bundled.getFileName().toString());
    if (!bundledMatcher.matches()) {
      return null;
    }
    String bundledVersion = bundledMatcher.group(1);

    Optional<Path> existing = findCurrentJar(appDir);
    if (existing.isPresent()) {
      String existingVersion = extractVersion(existing.get().getFileName().toString());
      // Newer numeric version always wins; a bundled prerelease of the same numeric does NOT
      // overwrite an installed final (2.10.0-RC2 bundled vs 2.10.0 installed → keep installed).
      boolean bundledStrictlyNewer;
      try {
        bundledStrictlyNewer = isNewer(bundledVersion, existingVersion);
      } catch (Exception e) {
        bundledStrictlyNewer = false;
      }
      if (!bundledStrictlyNewer) {
        return null;
      }
      // Keep the old JAR as a backup before replacing it with the bundled one.
      Path backup = appDir.resolve(existing.get().getFileName().toString() + ".old");
      Files.move(existing.get(), backup, StandardCopyOption.REPLACE_EXISTING);
    }

    String targetName = "all-time-wrestling-rpg-" + bundledVersion + ".jar";
    Files.copy(bundled, appDir.resolve(targetName), StandardCopyOption.REPLACE_EXISTING);
    System.out.println("[Launcher] Seeded app dir from bundled JAR: " + targetName);
    return bundledVersion;
  }

  /** The {@code atw-app-*.jar} bundled next to this launcher by jpackage, or null. */
  static Path findBundledAppJar() {
    try {
      Path dir = selfLocation().getParent();
      if (dir == null || !Files.isDirectory(dir)) {
        return null;
      }
      try (var stream = Files.list(dir)) {
        return stream
            .filter(
                p ->
                    p.getFileName().toString().startsWith("atw-app-")
                        && p.getFileName().toString().endsWith(".jar"))
            .findFirst()
            .orElse(null);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /** {@code java.exe} on Windows, {@code java} everywhere else. */
  static String javaBinaryName(final String osName) {
    return osName != null && osName.toLowerCase().contains("win") ? "java.exe" : "java";
  }

  /**
   * Resolves a real {@code java} executable for the child-process launch. The packaged runtime
   * (java.home) is preferred — it is the JVM this process is already running on. The current
   * process command is only used when it actually is a java executable; under jpackage it is the
   * app launcher binary, which does not accept {@code -jar} (ATW-mcwe).
   */
  static String resolveJavaExecutable() {
    String javaHome = System.getProperty("java.home");
    if (javaHome != null) {
      Path packaged = Path.of(javaHome, "bin", javaBinaryName(System.getProperty("os.name", "")));
      if (Files.isRegularFile(packaged)) {
        return packaged.toString();
      }
    }
    return ProcessHandle.current()
        .info()
        .command()
        .filter(cmd -> cmd.endsWith("java") || cmd.endsWith("java.exe"))
        .orElse("java");
  }

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

  /**
   * Whether a modal Swing dialog can be shown. False when headless (CI, servers) or when the {@code
   * atw.launcher.headless} system property is set — the property is the deterministic seam tests
   * use, since {@link GraphicsEnvironment#isHeadless()} caches its answer per JVM.
   */
  static boolean isGuiAvailable() {
    return !GraphicsEnvironment.isHeadless() && !Boolean.getBoolean("atw.launcher.headless");
  }

  static boolean promptUser(final String newVersion) {
    if (System.console() != null) {
      System.out.print("[Launcher] Update v" + newVersion + " available. Apply now? [y/N] ");
      String response = System.console().readLine();
      return response != null && response.trim().equalsIgnoreCase("y");
    }
    if (!isGuiAvailable()) {
      // No display to attach a modal to (CI, tests); refuse the update so the existing JAR keeps
      // launching instead of dying with HeadlessException.
      return false;
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

  static void showError(final String message) {
    System.err.println("[Launcher] " + message);
    if (!isGuiAvailable()) {
      // stderr above carries the message when no dialog can be shown.
      return;
    }
    try {
      JOptionPane.showMessageDialog(null, message, "Launcher Error", JOptionPane.ERROR_MESSAGE);
    } catch (Exception ignored) {
    }
  }

  private Launcher() {}
}
