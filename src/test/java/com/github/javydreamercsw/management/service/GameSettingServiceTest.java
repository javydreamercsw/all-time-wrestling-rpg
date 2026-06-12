/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.event.dto.GameDateChangedEvent;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameSettingServiceTest {

  @Mock private GameSettingRepository repository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private UniverseContextService universeContextService;

  @InjectMocks private GameSettingService service;

  @BeforeEach
  void setUp() {
    // Return null universeId so tests resolve against global (NULL) rows by default
    org.mockito.Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(null);
  }

  private GameSetting setting(final String key, final String value) {
    GameSetting s = new GameSetting();
    s.setSettingKey(key);
    s.setValue(value);
    return s;
  }

  @Test
  void isWearAndTearEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.WEAR_AND_TEAR_ENABLED_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.WEAR_AND_TEAR_ENABLED_KEY, "false")));

    assertThat(service.isWearAndTearEnabled()).isFalse();
  }

  @Test
  void isWearAndTearEnabled_settingMissing_returnsDefaultTrue() {
    when(repository.findGlobal(GameSettingService.WEAR_AND_TEAR_ENABLED_KEY))
        .thenReturn(Optional.empty());

    assertThat(service.isWearAndTearEnabled()).isTrue();
  }

  @Test
  void isStatusCardsEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.STATUS_CARDS_ENABLED_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.STATUS_CARDS_ENABLED_KEY, "false")));

    assertThat(service.isStatusCardsEnabled()).isFalse();
  }

  @Test
  void isStatusCardsEnabled_settingMissing_returnsDefaultTrue() {
    when(repository.findGlobal(GameSettingService.STATUS_CARDS_ENABLED_KEY))
        .thenReturn(Optional.empty());

    assertThat(service.isStatusCardsEnabled()).isTrue();
  }

  @Test
  void isAiNewsEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.AI_NEWS_ENABLED_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.AI_NEWS_ENABLED_KEY, "false")));

    assertThat(service.isAiNewsEnabled()).isFalse();
  }

  @Test
  void isAiNewsEnabled_settingMissing_returnsDefaultTrue() {
    when(repository.findGlobal(GameSettingService.AI_NEWS_ENABLED_KEY))
        .thenReturn(Optional.empty());

    assertThat(service.isAiNewsEnabled()).isTrue();
  }

  @Test
  void getNewsRumorChance_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.NEWS_RUMOR_CHANCE_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.NEWS_RUMOR_CHANCE_KEY, "35")));

    assertThat(service.getNewsRumorChance()).isEqualTo(35);
  }

  @Test
  void getNewsRumorChance_settingMissing_returnsDefault20() {
    when(repository.findGlobal(GameSettingService.NEWS_RUMOR_CHANCE_KEY))
        .thenReturn(Optional.empty());

    assertThat(service.getNewsRumorChance()).isEqualTo(20);
  }

  @Test
  void getNewsStrategy_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.NEWS_STRATEGY_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.NEWS_STRATEGY_KEY, "SHOW")));

    assertThat(service.getNewsStrategy()).isEqualTo("SHOW");
  }

  @Test
  void getNewsStrategy_settingMissing_returnsDefaultSegment() {
    when(repository.findGlobal(GameSettingService.NEWS_STRATEGY_KEY)).thenReturn(Optional.empty());

    assertThat(service.getNewsStrategy()).isEqualTo("SEGMENT");
  }

  @Test
  void getCurrentGameDate_settingExists_returnsParsedDate() {
    LocalDate date = LocalDate.of(2025, 6, 15);
    when(repository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.CURRENT_GAME_DATE_KEY, "2025-06-15")));

    assertThat(service.getCurrentGameDate()).isEqualTo(date);
  }

  @Test
  void getCurrentGameDate_settingMissing_returnsToday() {
    when(repository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY))
        .thenReturn(Optional.empty());

    LocalDate result = service.getCurrentGameDate();

    // Should return today's date (or close to it)
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(LocalDate.now());
  }

  @Test
  void saveCurrentGameDate_newDate_publishesEvent() {
    LocalDate oldDate = LocalDate.of(2025, 1, 1);
    LocalDate newDate = LocalDate.of(2025, 6, 15);

    // getCurrentGameDate() -> old date
    when(repository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.CURRENT_GAME_DATE_KEY, "2025-01-01")));
    GameSetting existingSetting = setting(GameSettingService.CURRENT_GAME_DATE_KEY, "2025-01-01");
    when(repository.save(any(GameSetting.class))).thenReturn(existingSetting);

    service.saveCurrentGameDate(newDate);

    verify(eventPublisher).publishEvent(any(GameDateChangedEvent.class));
  }

  @Test
  void saveCurrentGameDate_sameDateAsExisting_doesNotPublishEvent() {
    LocalDate date = LocalDate.of(2025, 6, 15);

    when(repository.findGlobal(GameSettingService.CURRENT_GAME_DATE_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.CURRENT_GAME_DATE_KEY, "2025-06-15")));
    GameSetting existingSetting = setting(GameSettingService.CURRENT_GAME_DATE_KEY, "2025-06-15");
    when(repository.save(any(GameSetting.class))).thenReturn(existingSetting);

    service.saveCurrentGameDate(date);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void findById_delegatesToRepository() {
    GameSetting gs = setting("some_key", "some_value");
    when(repository.findGlobal("some_key")).thenReturn(Optional.of(gs));

    Optional<GameSetting> result = service.findById("some_key");

    assertThat(result).isPresent().contains(gs);
  }

  @Test
  void findAll_delegatesToRepository() {
    GameSetting gs1 = setting("key1", "val1");
    GameSetting gs2 = setting("key2", "val2");
    when(repository.findAll()).thenReturn(List.of(gs1, gs2));

    List<GameSetting> result = service.findAll();

    assertThat(result).containsExactly(gs1, gs2);
  }

  @Test
  void save_gameSetting_delegatesToRepository() {
    GameSetting gs = setting("my_key", "my_value");
    when(repository.save(gs)).thenReturn(gs);

    GameSetting result = service.save(gs);

    verify(repository).save(gs);
    assertThat(result).isSameAs(gs);
  }

  // ── Tutorial settings ─────────────────────────────────────────────────────

  @Test
  void isTutorialEnabled_campaign_defaultsToTrue() {
    when(repository.findGlobal(GameSettingService.TUTORIAL_ENABLED_CAMPAIGN_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isTutorialEnabled(Universe.UniverseType.CAMPAIGN)).isTrue();
  }

  @Test
  void isTutorialEnabled_league_defaultsToTrue() {
    when(repository.findGlobal(GameSettingService.TUTORIAL_ENABLED_LEAGUE_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isTutorialEnabled(Universe.UniverseType.LEAGUE)).isTrue();
  }

  @Test
  void isTutorialEnabled_global_defaultsToTrue() {
    when(repository.findGlobal(GameSettingService.TUTORIAL_ENABLED_GLOBAL_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isTutorialEnabled(Universe.UniverseType.GLOBAL)).isTrue();
  }
}
