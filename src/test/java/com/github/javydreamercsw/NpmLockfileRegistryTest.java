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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards against corporate npm proxy leaks in {@code package-lock.json}. Local Maven runs can make
 * the Vaadin plugin re-sync the lockfile through a corporate registry (e.g. an internal
 * Artifactory); CI runners cannot resolve those hosts, so the frontend build fails with {@code
 * getaddrinfo ENOTFOUND}. Every resolved URL must point at the public npm registry.
 */
class NpmLockfileRegistryTest {

  private static final Pattern RESOLVED_HOST =
      Pattern.compile("\"resolved\":\\s*\"(https?://[^/\"]+)");

  @Test
  void lockfileOnlyResolvesFromPublicNpmRegistry() throws IOException {
    Path lockfile = Path.of("package-lock.json");
    assertThat(lockfile).exists();

    Matcher matcher = RESOLVED_HOST.matcher(Files.readString(lockfile));
    List<String> offendingHosts =
        matcher
            .results()
            .map(r -> r.group(1))
            .distinct()
            .filter(host -> !"https://registry.npmjs.org".equals(host))
            .collect(Collectors.toList());

    assertThat(offendingHosts)
        .as(
            "package-lock.json resolves packages from non-public registries. A local Maven run"
                + " likely re-synced it through a corporate npm proxy. Restore it with:"
                + " git checkout -- package.json package-lock.json")
        .isEmpty();
  }
}
