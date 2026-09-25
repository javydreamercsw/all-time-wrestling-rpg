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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ResourceBanner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/**
 * The startup banner must always show the application version.
 *
 * <p>Spring Boot's {@code ${application.version}} placeholder resolves from the JAR manifest only,
 * so it rendered an empty {@code (v)} whenever the app ran as exploded classes (./mvnw, IDE). The
 * banner is therefore version-substituted at build time (Maven resource filtering with {@code
 * @project.version@}); these tests fail if that wiring regresses.
 */
class StartupBannerTest {

  /** The filtered classpath banner carries a concrete version, not a placeholder. */
  @Test
  void banner_containsBuildTimeSubstitutedVersion() {
    String rendered = renderBanner();

    assertThat(rendered).matches("(?s).*\\(v\\d+\\.\\d+\\.\\d+.*\\).*");
  }

  /** Maven filtering already replaced the placeholder — no raw token may leak into startup. */
  @Test
  void banner_containsNoUnsubstitutedPlaceholders() {
    String rendered = renderBanner();

    assertThat(rendered).doesNotContain("@project.version@");
    assertThat(rendered).doesNotContain("${");
  }

  /** Renders banner.txt exactly the way SpringApplication does at startup. */
  private static String renderBanner() {
    ResourceBanner banner = new ResourceBanner(new ClassPathResource("banner.txt"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    banner.printBanner(
        new MockEnvironment(),
        StartupBannerTest.class,
        new PrintStream(out, true, StandardCharsets.UTF_8));
    return out.toString(StandardCharsets.UTF_8);
  }
}
