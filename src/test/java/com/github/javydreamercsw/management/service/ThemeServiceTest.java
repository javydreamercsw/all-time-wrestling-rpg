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
package com.github.javydreamercsw.management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

  @Mock private GameSettingRepository gameSettingRepository;

  @InjectMocks private ThemeService themeService;

  @Test
  void testGetEffectiveTheme_UserPreference() {
    Account account = new Account();
    account.setThemePreference("dark");

    String theme = themeService.getEffectiveTheme(account);
    assertEquals("dark", theme);
  }

  @Test
  void testGetEffectiveTheme_GlobalDefault() {
    Account account = new Account();
    // themePreference is null

    GameSetting setting = new GameSetting();
    setting.setId("default_theme");
    setting.setValue("neon");

    when(gameSettingRepository.findById("default_theme")).thenReturn(Optional.of(setting));

    String theme = themeService.getEffectiveTheme(account);
    assertEquals("neon", theme);
  }

  @Test
  void testGetEffectiveTheme_Fallback() {
    Account account = new Account();
    when(gameSettingRepository.findById("default_theme")).thenReturn(Optional.empty());

    String theme = themeService.getEffectiveTheme(account);
    assertEquals("light", theme); // Default fallback
  }

  @Test
  void testUpdateGlobalDefaultTheme() {
    String newTheme = "retro";
    themeService.updateGlobalDefaultTheme(newTheme);

    ArgumentCaptor<GameSetting> captor = ArgumentCaptor.forClass(GameSetting.class);
    verify(gameSettingRepository).save(captor.capture());
    GameSetting captured = captor.getValue();
    assertEquals("default_theme", captured.getId());
    assertEquals("retro", captured.getValue());
  }
}
