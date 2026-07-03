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
package com.github.javydreamercsw.management.ui.component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the set of valid icon names from {@code /static/icons/manifest.json} once at class-init
 * time. Unknown names referenced in guide text are caught here rather than silently broken.
 */
@Slf4j
public final class GuideIconRegistry {

  private static final Set<String> VALID_ICONS;

  static {
    Set<String> icons = new HashSet<>();
    try (InputStream is =
        GuideIconRegistry.class.getResourceAsStream("/static/icons/manifest.json")) {
      if (is != null) {
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        // Parse the "icons": [...] array with a simple regex — avoids a Jackson dependency here.
        Matcher m = Pattern.compile("\"([a-z0-9_-]+)\"").matcher(json);
        boolean inArray = false;
        while (m.find()) {
          String token = m.group(1);
          if ("icons".equals(token)) {
            inArray = true;
          } else if (inArray) {
            icons.add(token);
          }
        }
      } else {
        log.warn(
            "Icon manifest not found at /static/icons/manifest.json — icon rendering disabled");
      }
    } catch (Exception e) {
      log.warn("Failed to load icon manifest", e);
    }
    VALID_ICONS = Collections.unmodifiableSet(icons);
    if (!VALID_ICONS.isEmpty()) {
      log.debug("Guide icon registry loaded {} icons: {}", VALID_ICONS.size(), VALID_ICONS);
    }
  }

  private GuideIconRegistry() {}

  public static boolean isValid(final String name) {
    return VALID_ICONS.contains(name);
  }

  public static Set<String> validNames() {
    return VALID_ICONS;
  }
}
