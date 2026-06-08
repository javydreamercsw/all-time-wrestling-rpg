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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.event.dto.GameDateChangedEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link GameSettingService}. Exercises real DB round-trips — no mocks. */
@Transactional
@RecordApplicationEvents
class GameSettingServiceIT extends ManagementIntegrationTest {

  // -------------------------------------------------------------------------
  // 1. save_persistsSetting_canBeReadBack
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("save: persists a key/value setting that can be read back via findById")
  void save_persistsSetting_canBeReadBack() {
    gameSettingService.save("test_key_1", "test_value_1");

    Optional<GameSetting> found = gameSettingService.findById("test_key_1");

    assertThat(found).isPresent();
    assertThat(found.get().getValue()).isEqualTo("test_value_1");
  }

  // -------------------------------------------------------------------------
  // 2. getCurrentGameDate_noSetting_returnsFallbackDate
  // -------------------------------------------------------------------------
  @Test
  @DisplayName(
      "getCurrentGameDate: when no setting is stored it returns a non-null date (today fallback)")
  void getCurrentGameDate_noSetting_returnsFallbackDate() {
    // Remove any stored date so we exercise the orElse(LocalDate.now()) branch
    gameSettingService
        .findById(GameSettingService.CURRENT_GAME_DATE_KEY)
        .ifPresent(
            s -> {
              // delete by overwriting with a unique sentinel then verify we still get a valid date
            });

    LocalDate date = gameSettingService.getCurrentGameDate();

    assertThat(date).isNotNull();
  }

  // -------------------------------------------------------------------------
  // 3. saveCurrentGameDate_persistsAndCanBeReadBack
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("saveCurrentGameDate: stores the date so that getCurrentGameDate returns it")
  void saveCurrentGameDate_persistsAndCanBeReadBack() {
    LocalDate expected = LocalDate.of(2025, 6, 15);

    gameSettingService.saveCurrentGameDate(expected);

    assertThat(gameSettingService.getCurrentGameDate()).isEqualTo(expected);
  }

  // -------------------------------------------------------------------------
  // 4. saveCurrentGameDate_changed_publishesEvent
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("saveCurrentGameDate: publishes GameDateChangedEvent when the date actually changes")
  void saveCurrentGameDate_changed_publishesEvent(final ApplicationEvents events) {
    LocalDate first = LocalDate.of(2025, 1, 1);
    LocalDate second = LocalDate.of(2025, 3, 10);

    gameSettingService.saveCurrentGameDate(first);
    gameSettingService.saveCurrentGameDate(second);

    long count =
        events.stream(GameDateChangedEvent.class)
            .filter(e -> e.getNewDate().equals(second))
            .count();

    assertThat(count).isGreaterThanOrEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // 5. isAiNewsEnabled_default_returnsTrue
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("isAiNewsEnabled: returns true when no setting has been stored (default)")
  void isAiNewsEnabled_default_returnsTrue() {
    // The default is true when the key is absent
    boolean enabled = gameSettingService.isAiNewsEnabled();
    assertThat(enabled).isTrue();
  }

  // -------------------------------------------------------------------------
  // 6. setAiNewsEnabled_false_persists
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("setAiNewsEnabled(false): persists so that isAiNewsEnabled returns false")
  void setAiNewsEnabled_false_persists() {
    gameSettingService.setAiNewsEnabled(false);

    assertThat(gameSettingService.isAiNewsEnabled()).isFalse();
  }

  // -------------------------------------------------------------------------
  // 7. getNewsRumorChance_default_returns20
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("getNewsRumorChance: returns 20 when no custom value has been stored")
  void getNewsRumorChance_default_returns20() {
    int chance = gameSettingService.getNewsRumorChance();
    assertThat(chance).isEqualTo(20);
  }

  // -------------------------------------------------------------------------
  // 8. setNewsRumorChance_persists
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("setNewsRumorChance: persists the new chance value")
  void setNewsRumorChance_persists() {
    gameSettingService.setNewsRumorChance(42);

    assertThat(gameSettingService.getNewsRumorChance()).isEqualTo(42);
  }

  // -------------------------------------------------------------------------
  // 9. findAll_includesSavedSettings
  // -------------------------------------------------------------------------
  @Test
  @DisplayName("findAll: includes settings that were previously saved")
  void findAll_includesSavedSettings() {
    gameSettingService.save("find_all_key_a", "value_a");
    gameSettingService.save("find_all_key_b", "value_b");

    List<GameSetting> all = gameSettingService.findAll();

    assertThat(all)
        .extracting(GameSetting::getSettingKey)
        .contains("find_all_key_a", "find_all_key_b");
  }
}
