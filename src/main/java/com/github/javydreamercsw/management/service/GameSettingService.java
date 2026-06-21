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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.event.dto.GameDateChangedEvent;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  // Tutorial enable/disable settings (gameplay keys, default true)
  public static final String TUTORIAL_ENABLED_CAMPAIGN_KEY = "tutorial.enabled.CAMPAIGN";
  public static final String TUTORIAL_ENABLED_LEAGUE_KEY = "tutorial.enabled.LEAGUE";
  public static final String TUTORIAL_ENABLED_GLOBAL_KEY = "tutorial.enabled.GLOBAL";

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

  /**
   * Keys that are strictly per-universe credentials. They are NEVER inherited from the global
   * defaults — each universe must configure its own. Reading without an active universe returns
   * empty.
   */
  private static final Set<String> CREDENTIAL_KEYS =
      Set.of(
          "AI_OPENAI_API_KEY", "AI_CLAUDE_API_KEY", "AI_GEMINI_API_KEY", "AI_POLLINATIONS_API_KEY");

  /**
   * Keys that are always global (system-level). They are never scoped per-universe regardless of
   * the active universe context.
   */
  private static final Set<String> SYSTEM_KEYS = Set.of("default_theme");

  private final GameSettingRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final UniverseContextService universeContextService;

  // ── Internal resolution ───────────────────────────────────────────────────

  /**
   * Resolves a setting value respecting the three-tier hierarchy:
   *
   * <ul>
   *   <li>System keys → always global (universe_id IS NULL)
   *   <li>Credential keys → universe-scoped only, no fallback
   *   <li>Gameplay keys → universe-scoped with fallback to global
   * </ul>
   */
  private Optional<String> resolveValue(final String key) {
    if (SYSTEM_KEYS.contains(key)) {
      return repository.findGlobal(key).map(GameSetting::getValue);
    }

    Long universeId = universeContextService.getCurrentUniverseId();

    if (CREDENTIAL_KEYS.contains(key)) {
      if (universeId == null) {
        return Optional.empty();
      }
      return repository.findBySettingKeyAndUniverseId(key, universeId).map(GameSetting::getValue);
    }

    // Gameplay: universe-scoped first, fall back to global
    if (universeId != null) {
      Optional<String> universeValue =
          repository.findBySettingKeyAndUniverseId(key, universeId).map(GameSetting::getValue);
      if (universeValue.isPresent()) {
        return universeValue;
      }
    }
    return repository.findGlobal(key).map(GameSetting::getValue);
  }

  /**
   * Saves a setting scoped to the correct context:
   *
   * <ul>
   *   <li>System keys → always saves as global
   *   <li>Credential keys → saves to current universe (throws if none active)
   *   <li>Gameplay keys → saves to current universe if one is active, else global
   * </ul>
   */
  private void saveInternal(final String key, final String value) {
    Long universeId = null;

    if (!SYSTEM_KEYS.contains(key)) {
      universeId = universeContextService.getCurrentUniverseId();
      if (CREDENTIAL_KEYS.contains(key) && universeId == null) {
        throw new IllegalStateException(
            "Cannot save credential setting '" + key + "' without an active universe.");
      }
    }

    final Long finalUniverseId = universeId;
    GameSetting setting =
        (finalUniverseId != null
                ? repository.findBySettingKeyAndUniverseId(key, finalUniverseId)
                : repository.findGlobal(key))
            .orElseGet(GameSetting::new);
    setting.setSettingKey(key);
    setting.setValue(value);
    setting.setUniverseId(finalUniverseId);
    repository.save(setting);
  }

  // ── Gameplay settings ────────────────────────────────────────────────────

  @PreAuthorize("permitAll()")
  public boolean isWearAndTearEnabled() {
    return resolveValue(WEAR_AND_TEAR_ENABLED_KEY).map(Boolean::parseBoolean).orElse(true);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setWearAndTearEnabled(final boolean enabled) {
    saveInternal(WEAR_AND_TEAR_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public boolean isStatusCardsEnabled() {
    return resolveValue(STATUS_CARDS_ENABLED_KEY).map(Boolean::parseBoolean).orElse(true);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void setStatusCardsEnabled(final boolean enabled) {
    saveInternal(STATUS_CARDS_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public boolean isTutorialEnabled(final Universe.UniverseType type) {
    String key =
        switch (type) {
          case CAMPAIGN -> TUTORIAL_ENABLED_CAMPAIGN_KEY;
          case LEAGUE -> TUTORIAL_ENABLED_LEAGUE_KEY;
          case GLOBAL -> TUTORIAL_ENABLED_GLOBAL_KEY;
        };
    return resolveValue(key).map(Boolean::parseBoolean).orElse(true);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setTutorialEnabled(final Universe.UniverseType type, final boolean enabled) {
    String key =
        switch (type) {
          case CAMPAIGN -> TUTORIAL_ENABLED_CAMPAIGN_KEY;
          case LEAGUE -> TUTORIAL_ENABLED_LEAGUE_KEY;
          case GLOBAL -> TUTORIAL_ENABLED_GLOBAL_KEY;
        };
    saveInternal(key, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public boolean isAiNewsEnabled() {
    return resolveValue(AI_NEWS_ENABLED_KEY).map(Boolean::parseBoolean).orElse(true);
  }

  @PreAuthorize("permitAll()")
  public int getNewsRumorChance() {
    return resolveValue(NEWS_RUMOR_CHANCE_KEY).map(Integer::parseInt).orElse(20);
  }

  @PreAuthorize("permitAll()")
  public String getNewsStrategy() {
    return resolveValue(NEWS_STRATEGY_KEY).orElse("SEGMENT");
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setAiNewsEnabled(final boolean enabled) {
    saveInternal(AI_NEWS_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setNewsRumorChance(final int chance) {
    saveInternal(NEWS_RUMOR_CHANCE_KEY, String.valueOf(chance));
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setNewsStrategy(final String strategy) {
    saveInternal(NEWS_STRATEGY_KEY, strategy);
  }

  @PreAuthorize("permitAll()")
  public LocalDate getCurrentGameDate() {
    return resolveValue(CURRENT_GAME_DATE_KEY).map(LocalDate::parse).orElse(LocalDate.now());
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void saveCurrentGameDate(final LocalDate date) {
    LocalDate oldDate = getCurrentGameDate();
    saveInternal(CURRENT_GAME_DATE_KEY, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    if (!date.equals(oldDate)) {
      log.info("Game date changed from {} to {}", oldDate, date);
      eventPublisher.publishEvent(new GameDateChangedEvent(this, oldDate, date));
    }
  }

  @PreAuthorize("permitAll()")
  public int getRivalryResolutionThresholdPle() {
    return resolveValue(RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY).map(Integer::parseInt).orElse(30);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionThresholdPle(final int threshold) {
    saveInternal(RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY, String.valueOf(threshold));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryResolutionThresholdRegular() {
    return resolveValue(RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY).map(Integer::parseInt).orElse(35);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionThresholdRegular(final int threshold) {
    saveInternal(RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY, String.valueOf(threshold));
  }

  @PreAuthorize("permitAll()")
  public boolean isRivalryResolutionOnRegularShowsEnabled() {
    return resolveValue(RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY)
        .map(Boolean::parseBoolean)
        .orElse(false);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryResolutionOnRegularShowsEnabled(final boolean enabled) {
    saveInternal(RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryMaxDurationDays() {
    return resolveValue(RIVALRY_MAX_DURATION_DAYS_KEY).map(Integer::parseInt).orElse(90);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryMaxDurationDays(final int days) {
    saveInternal(RIVALRY_MAX_DURATION_DAYS_KEY, String.valueOf(days));
  }

  @PreAuthorize("permitAll()")
  public boolean isRivalryHeatDecayEnabled() {
    return resolveValue(RIVALRY_HEAT_DECAY_ENABLED_KEY).map(Boolean::parseBoolean).orElse(true);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayEnabled(final boolean enabled) {
    saveInternal(RIVALRY_HEAT_DECAY_ENABLED_KEY, String.valueOf(enabled));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryHeatDecayPerInterval() {
    return resolveValue(RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY).map(Integer::parseInt).orElse(1);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayPerInterval(final int amount) {
    saveInternal(RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY, String.valueOf(amount));
  }

  @PreAuthorize("permitAll()")
  public int getRivalryHeatDecayIntervalDays() {
    return resolveValue(RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY).map(Integer::parseInt).orElse(7);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void setRivalryHeatDecayIntervalDays(final int days) {
    saveInternal(RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY, String.valueOf(days));
  }

  // ── Generic access (used by AiSettingsService and other consumers) ────────

  /**
   * Finds a setting by key for an explicit universe ID, bypassing the session context. Use this
   * when the caller already knows which universe to query (e.g. from a background thread or when
   * the session universe may differ from the target universe).
   */
  @PreAuthorize("permitAll()")
  public Optional<GameSetting> findByKeyForUniverse(final String key, final Long universeId) {
    if (universeId != null) {
      Optional<GameSetting> scoped = repository.findBySettingKeyAndUniverseId(key, universeId);
      if (scoped.isPresent()) {
        return scoped;
      }
    }
    return repository.findGlobal(key);
  }

  /**
   * Finds a setting by key using the standard resolution hierarchy. Callers that need raw access
   * (e.g. AiSettingsService) should use this instead of the old repository.findById(key).
   */
  @PreAuthorize("permitAll()")
  public Optional<GameSetting> findById(final String key) {
    Long universeId =
        SYSTEM_KEYS.contains(key) ? null : universeContextService.getCurrentUniverseId();

    if (CREDENTIAL_KEYS.contains(key)) {
      if (universeId == null) {
        return Optional.empty();
      }
      return repository.findBySettingKeyAndUniverseId(key, universeId);
    }

    if (universeId != null) {
      Optional<GameSetting> scoped = repository.findBySettingKeyAndUniverseId(key, universeId);
      if (scoped.isPresent()) {
        return scoped;
      }
    }
    return repository.findGlobal(key);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public GameSetting save(final GameSetting gameSetting) {
    log.debug(
        "Saving game setting: {} = {}",
        gameSetting.getSettingKey(),
        gameSetting.getSettingKey() != null && gameSetting.getSettingKey().contains("KEY")
            ? "********"
            : gameSetting.getValue());
    return repository.save(gameSetting);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void save(final String key, final String value) {
    saveInternal(key, value);
  }

  @PreAuthorize("permitAll()")
  public List<GameSetting> findAll() {
    return repository.findAll();
  }

  /** Returns all global (shared) settings — used by admin views. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public List<GameSetting> findAllGlobal() {
    return repository.findAllGlobal();
  }

  /** Returns all universe-scoped overrides for a specific universe. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public List<GameSetting> findAllForUniverse(final Long universeId) {
    return repository.findAllByUniverseId(universeId);
  }
}
