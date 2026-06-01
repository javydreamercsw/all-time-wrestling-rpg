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

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.event.dto.GameDateChangedEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSettingService {

  public static final String CURRENT_GAME_DATE_KEY = "current_game_date";
  public static final String AI_NEWS_ENABLED_KEY = "ai_news_enabled";
  public static final String NEWS_RUMOR_CHANCE_KEY = "news_rumor_chance";
  public static final String NEWS_STRATEGY_KEY = "news_strategy"; // "SEGMENT" or "SHOW"
  public static final String WEAR_AND_TEAR_ENABLED_KEY = "wear_and_tear_enabled";
  public static final String STATUS_CARDS_ENABLED_KEY = "status_cards_enabled";
  public static final String NOTION_TOKEN_KEY = "notion_token";

  // Rivalry lifecycle settings
  public static final String RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY =
      "rivalry_resolution_threshold_ple";
  public static final String RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY =
      "rivalry_resolution_threshold_regular";
  public static final String RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY =
      "rivalry_resolution_on_regular_shows";
  public static final String RIVALRY_MAX_DURATION_DAYS_KEY = "rivalry_max_duration_days";
  public static final String RIVALRY_HEAT_DECAY_ENABLED_KEY = "rivalry_heat_decay_enabled";
  public static final String RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY =
      "rivalry_heat_decay_per_interval";
  public static final String RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY =
      "rivalry_heat_decay_interval_days";
  private final GameSettingRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public String getNotionToken() {
    return repository.findById(NOTION_TOKEN_KEY).map(GameSetting::getValue).orElse(null);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setNotionToken(final String token) {
    save(NOTION_TOKEN_KEY, token);
  }

  @PreAuthorize("permitAll()")
  public boolean isWearAndTearEnabled() {
    return repository
        .findById(WEAR_AND_TEAR_ENABLED_KEY)
        .map(GameSetting::getValue)
        .map(Boolean::parseBoolean)
        .orElse(true); // Enabled by default
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setWearAndTearEnabled(final boolean enabled) {
    save(WEAR_AND_TEAR_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public boolean isStatusCardsEnabled() {
    return repository
        .findById(STATUS_CARDS_ENABLED_KEY)
        .map(GameSetting::getValue)
        .map(Boolean::parseBoolean)
        .orElse(true); // Enabled by default
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void setStatusCardsEnabled(final boolean enabled) {
    save(STATUS_CARDS_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public boolean isAiNewsEnabled() {
    return repository
        .findById(AI_NEWS_ENABLED_KEY)
        .map(GameSetting::getValue)
        .map(Boolean::parseBoolean)
        .orElse(true); // Enabled by default
  }

  @PreAuthorize("permitAll()")
  public int getNewsRumorChance() {
    return repository
        .findById(NEWS_RUMOR_CHANCE_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(20); // 20% default
  }

  @PreAuthorize("permitAll()")
  public String getNewsStrategy() {
    return repository.findById(NEWS_STRATEGY_KEY).map(GameSetting::getValue).orElse("SEGMENT");
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setAiNewsEnabled(final boolean enabled) {
    save(AI_NEWS_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setNewsRumorChance(final int chance) {
    save(NEWS_RUMOR_CHANCE_KEY, String.valueOf(chance));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setNewsStrategy(final String strategy) {
    save(NEWS_STRATEGY_KEY, strategy);
  }

  @PreAuthorize("permitAll()")
  public LocalDate getCurrentGameDate() {
    return repository
        .findById(CURRENT_GAME_DATE_KEY)
        .map(GameSetting::getValue)
        .map(LocalDate::parse)
        .orElse(LocalDate.now()); // Fallback to real date if not set
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void saveCurrentGameDate(final LocalDate date) {
    LocalDate oldDate = getCurrentGameDate();
    GameSetting setting = repository.findById(CURRENT_GAME_DATE_KEY).orElseGet(GameSetting::new);
    setting.setId(CURRENT_GAME_DATE_KEY);
    setting.setValue(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    repository.save(setting);

    if (!date.equals(oldDate)) {
      log.info("Game date changed from {} to {}", oldDate, date);
      eventPublisher.publishEvent(new GameDateChangedEvent(this, oldDate, date));
    }
  }

  @PreAuthorize("permitAll()")
  public int getRivalryResolutionThresholdPle() {
    return repository
        .findById(RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(30);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionThresholdPle(final int threshold) {
    save(RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY, String.valueOf(threshold));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryResolutionThresholdRegular() {
    return repository
        .findById(RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(35);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionThresholdRegular(final int threshold) {
    save(RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY, String.valueOf(threshold));
  }

  @PreAuthorize("permitAll()")
  public boolean isRivalryResolutionOnRegularShowsEnabled() {
    return repository
        .findById(RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY)
        .map(GameSetting::getValue)
        .map(Boolean::parseBoolean)
        .orElse(false);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionOnRegularShowsEnabled(final boolean enabled) {
    save(RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryMaxDurationDays() {
    return repository
        .findById(RIVALRY_MAX_DURATION_DAYS_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(0);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryMaxDurationDays(final int days) {
    save(RIVALRY_MAX_DURATION_DAYS_KEY, String.valueOf(days));
  }

  @PreAuthorize("permitAll()")
  public boolean isRivalryHeatDecayEnabled() {
    return repository
        .findById(RIVALRY_HEAT_DECAY_ENABLED_KEY)
        .map(GameSetting::getValue)
        .map(Boolean::parseBoolean)
        .orElse(false);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayEnabled(final boolean enabled) {
    save(RIVALRY_HEAT_DECAY_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryHeatDecayPerInterval() {
    return repository
        .findById(RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(1);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayPerInterval(final int amount) {
    save(RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY, String.valueOf(amount));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryHeatDecayIntervalDays() {
    return repository
        .findById(RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY)
        .map(GameSetting::getValue)
        .map(Integer::parseInt)
        .orElse(7);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayIntervalDays(final int days) {
    save(RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY, String.valueOf(days));
  }

  @PreAuthorize("permitAll()")
  public Optional<GameSetting> findById(final String key) {
    return repository.findById(key);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public GameSetting save(final GameSetting gameSetting) {
    log.debug(
        "Saving game setting: {} = {}",
        gameSetting.getId(),
        gameSetting.getId().contains("KEY") ? "********" : gameSetting.getValue());
    return repository.save(gameSetting);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void save(final String key, final String value) {
    GameSetting setting = repository.findById(key).orElseGet(GameSetting::new);
    setting.setId(key);
    setting.setValue(value);
    save(setting);
  }

  @PreAuthorize("permitAll()")
  public List<GameSetting> findAll() {
    return repository.findAll();
  }
}
