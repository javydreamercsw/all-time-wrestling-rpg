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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that all portable startup scripts activate the h2 profile alongside prod, ensuring
 * H2DataSourceConfig is loaded and data is persisted to a file rather than in-memory.
 *
 * <p>Regression test for: data loss between sessions when launched via installer (issue #290).
 */
class StartupScriptConfigTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "scripts/portable/start-windows.bat",
        "scripts/portable/start-linux.sh",
        "scripts/portable/start-macos.sh"
      })
  void startup_script_must_activate_h2_profile(@NonNull String scriptPath) throws IOException {
    Path path = Paths.get(scriptPath);
    assertTrue(Files.exists(path), "Startup script not found: " + scriptPath);

    String content = Files.readString(path);
    assertTrue(
        content.contains("--spring.profiles.active=prod,h2"),
        scriptPath
            + " must include '--spring.profiles.active=prod,h2' to activate file-based H2"
            + " persistence. Found: "
            + content
                .lines()
                .filter(l -> l.contains("spring.profiles.active"))
                .findFirst()
                .orElse("(no profiles.active line found)"));
  }
}
