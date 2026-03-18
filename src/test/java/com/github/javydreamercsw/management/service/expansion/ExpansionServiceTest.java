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
package com.github.javydreamercsw.management.service.expansion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExpansionServiceTest {

  @Mock private GameSettingService gameSettingService;

  private ExpansionService expansionService;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    expansionService = new ExpansionService(gameSettingService, objectMapper);
  }

  @Test
  void testGetExpansions() {
    when(gameSettingService.findById(anyString())).thenReturn(Optional.empty());

    List<Expansion> expansions = expansionService.getExpansions();

    assertFalse(expansions.isEmpty());
    assertTrue(expansions.stream().anyMatch(e -> e.getCode().equals("BASE_GAME")));
    // Default should be enabled
    assertTrue(expansions.get(0).isEnabled());
  }

  @Test
  void testIsExpansionEnabled() {
    String code = "TEST_EXT";
    String key = ExpansionService.SET_ENABLED_PREFIX + code;

    // Case 1: Not in DB (Default to true)
    when(gameSettingService.findById(key)).thenReturn(Optional.empty());
    assertTrue(expansionService.isExpansionEnabled(code));

    // Case 2: Explicitly disabled in DB
    GameSetting disabledSetting = new GameSetting();
    disabledSetting.setId(key);
    disabledSetting.setValue("false");
    when(gameSettingService.findById(key)).thenReturn(Optional.of(disabledSetting));
    assertFalse(expansionService.isExpansionEnabled(code));

    // Case 3: Explicitly enabled in DB
    GameSetting enabledSetting = new GameSetting();
    enabledSetting.setId(key);
    enabledSetting.setValue("true");
    when(gameSettingService.findById(key)).thenReturn(Optional.of(enabledSetting));
    assertTrue(expansionService.isExpansionEnabled(code));
  }

  @Test
  void testSetExpansionEnabled() {
    String code = "TEST_EXT";
    String key = ExpansionService.SET_ENABLED_PREFIX + code;

    expansionService.setExpansionEnabled(code, false);
    verify(gameSettingService).save(key, "false");

    expansionService.setExpansionEnabled(code, true);
    verify(gameSettingService).save(key, "true");
  }

  @Test
  void testGetEnabledExpansionCodes() {
    // Mock BASE_GAME as enabled and EXTREME as disabled
    GameSetting extremeDisabled = new GameSetting();
    extremeDisabled.setId(ExpansionService.SET_ENABLED_PREFIX + "EXTREME");
    extremeDisabled.setValue("false");

    when(gameSettingService.findById(ExpansionService.SET_ENABLED_PREFIX + "EXTREME"))
        .thenReturn(Optional.of(extremeDisabled));
    when(gameSettingService.findById(ExpansionService.SET_ENABLED_PREFIX + "BASE_GAME"))
        .thenReturn(Optional.empty()); // Defaults to true
    // Mock others to empty/true for simplicity in this test

    List<String> enabledCodes = expansionService.getEnabledExpansionCodes();

    assertTrue(enabledCodes.contains("BASE_GAME"));
    assertFalse(enabledCodes.contains("EXTREME"));
  }
}
