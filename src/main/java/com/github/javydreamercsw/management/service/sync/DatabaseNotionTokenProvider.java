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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.ai.notion.NotionTokenProvider;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Implementation of {@link NotionTokenProvider} that retrieves the token from game settings. */
@Component
public class DatabaseNotionTokenProvider implements NotionTokenProvider {

  private final GameSettingService gameSettingService;

  @Autowired
  public DatabaseNotionTokenProvider(GameSettingService gameSettingService) {
    this.gameSettingService = gameSettingService;
  }

  @Override
  public Optional<String> getToken() {
    return GeneralSecurityUtils.runAsAdmin(
        (Supplier<Optional<String>>)
            () -> {
              String token = gameSettingService.getNotionToken();
              return (token != null && !token.trim().isEmpty())
                  ? Optional.of(token)
                  : Optional.empty();
            });
  }
}
