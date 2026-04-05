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
package com.github.javydreamercsw.base.image;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Image source that resolves images from remote URLs. If the provided name starts with http:// or
 * https://, it is returned as the resolved path.
 */
@Component
@Slf4j
public class RemoteUrlImageSource implements ImageSource {

  @Override
  public Optional<String> resolveImage(String name, ImageCategory category) {
    if (name != null && (name.startsWith("http://") || name.startsWith("https://"))) {
      log.debug("Resolved remote URL: {}", name);
      return Optional.of(name);
    }
    return Optional.empty();
  }

  @Override
  public int getPriority() {
    return 10; // Checked before local sources if provided name is a URL
  }
}
