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
package com.github.javydreamercsw.base.service.theme;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ThemeService {

  private final GameSettingRepository gameSettingRepository;
  public static final String DEFAULT_THEME_KEY = "default_theme";
  public static final String FALLBACK_THEME = "light";

  public java.util.List<String> getAvailableThemes() {
    return java.util.List.of("light", "dark", "retro", "high-contrast", "neon");
  }

  public String getEffectiveTheme(Account account) {
    if (account != null
        && account.getThemePreference() != null
        && !account.getThemePreference().isBlank()) {
      return account.getThemePreference();
    }
    return getGlobalDefaultTheme().orElse(FALLBACK_THEME);
  }

  public Optional<String> getGlobalDefaultTheme() {
    return gameSettingRepository.findById(DEFAULT_THEME_KEY).map(GameSetting::getValue);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void updateGlobalDefaultTheme(String theme) {
    GameSetting setting =
        gameSettingRepository.findById(DEFAULT_THEME_KEY).orElse(new GameSetting());
    setting.setId(DEFAULT_THEME_KEY);
    setting.setValue(theme);
    gameSettingRepository.save(setting);
  }
}
