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
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  @Mock private SeedReviewGate seedReviewGate;

  @InjectMocks private GameSettingService service;

  @BeforeEach
  void setUp() {
    // Return null universeId so tests resolve against global (NULL) rows by default
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(null);
  }

  private GameSetting setting(final String key, final String value) {
    GameSetting s = new GameSetting();
    s.setSettingKey(key);
    s.setValue(value);
    return s;
  }

  @Test
  void isIntergenderMatchesEnabled_settingMissing_returnsDefaultFalse() {
    when(repository.findGlobal(GameSettingService.INTERGENDER_MATCHES_ENABLED_KEY))
        .thenReturn(Optional.empty());

    assertThat(service.isIntergenderMatchesEnabled()).isFalse();
  }

  @Test
  void isIntergenderMatchesEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.INTERGENDER_MATCHES_ENABLED_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.INTERGENDER_MATCHES_ENABLED_KEY, "true")));

    assertThat(service.isIntergenderMatchesEnabled()).isTrue();
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

  // ── Rivalry lifecycle settings ────────────────────────────────────────────

  @Test
  void getRivalryResolutionThresholdPle_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY, "35")));
    assertThat(service.getRivalryResolutionThresholdPle()).isEqualTo(35);
  }

  @Test
  void getRivalryResolutionThresholdPle_settingMissing_returnsDefault30() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryResolutionThresholdPle()).isEqualTo(30);
  }

  @Test
  void getRivalryResolutionThresholdRegular_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY))
        .thenReturn(
            Optional.of(
                setting(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY, "30")));
    assertThat(service.getRivalryResolutionThresholdRegular()).isEqualTo(30);
  }

  @Test
  void getRivalryResolutionThresholdRegular_settingMissing_returnsDefault25() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryResolutionThresholdRegular()).isEqualTo(25);
  }

  @Test
  void isRivalryResolutionOnRegularShowsEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY))
        .thenReturn(
            Optional.of(
                setting(GameSettingService.RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY, "false")));
    assertThat(service.isRivalryResolutionOnRegularShowsEnabled()).isFalse();
  }

  @Test
  void isRivalryResolutionOnRegularShowsEnabled_settingMissing_returnsDefaultTrue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isRivalryResolutionOnRegularShowsEnabled()).isTrue();
  }

  @Test
  void getRivalryMaxDurationDays_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_MAX_DURATION_DAYS_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.RIVALRY_MAX_DURATION_DAYS_KEY, "60")));
    assertThat(service.getRivalryMaxDurationDays()).isEqualTo(60);
  }

  @Test
  void getRivalryMaxDurationDays_settingMissing_returnsDefault90() {
    when(repository.findGlobal(GameSettingService.RIVALRY_MAX_DURATION_DAYS_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryMaxDurationDays()).isEqualTo(90);
  }

  @Test
  void isRivalryHeatDecayEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_ENABLED_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.RIVALRY_HEAT_DECAY_ENABLED_KEY, "false")));
    assertThat(service.isRivalryHeatDecayEnabled()).isFalse();
  }

  @Test
  void isRivalryHeatDecayEnabled_settingMissing_returnsDefaultTrue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_ENABLED_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isRivalryHeatDecayEnabled()).isTrue();
  }

  @Test
  void getRivalryHeatDecayPerInterval_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY, "3")));
    assertThat(service.getRivalryHeatDecayPerInterval()).isEqualTo(3);
  }

  @Test
  void getRivalryHeatDecayPerInterval_settingMissing_returnsDefault1() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryHeatDecayPerInterval()).isEqualTo(1);
  }

  @Test
  void getRivalryHeatDecayIntervalDays_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY, "14")));
    assertThat(service.getRivalryHeatDecayIntervalDays()).isEqualTo(14);
  }

  @Test
  void getRivalryHeatDecayIntervalDays_settingMissing_returnsDefault7() {
    when(repository.findGlobal(GameSettingService.RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryHeatDecayIntervalDays()).isEqualTo(7);
  }

  @Test
  void getRivalryResolutionMinHeat_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_MIN_HEAT_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.RIVALRY_RESOLUTION_MIN_HEAT_KEY, "15")));
    assertThat(service.getRivalryResolutionMinHeat()).isEqualTo(15);
  }

  @Test
  void getRivalryResolutionMinHeat_settingMissing_returnsDefault10() {
    when(repository.findGlobal(GameSettingService.RIVALRY_RESOLUTION_MIN_HEAT_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getRivalryResolutionMinHeat()).isEqualTo(10);
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

  // ── Briefcase expiry (ATW-8p72) ──────────────────────────────────────────

  @Test
  void getBriefcaseExpiryDays_settingMissing_returnsDefault365() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_EXPIRY_DAYS_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.getBriefcaseExpiryDays()).isEqualTo(365);
  }

  @Test
  void getBriefcaseExpiryDays_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_EXPIRY_DAYS_KEY))
        .thenReturn(Optional.of(setting(GameSettingService.BRIEFCASE_EXPIRY_DAYS_KEY, "180")));
    assertThat(service.getBriefcaseExpiryDays()).isEqualTo(180);
  }

  @Test
  void setBriefcaseExpiryDays_savesGlobalSetting() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_EXPIRY_DAYS_KEY))
        .thenReturn(Optional.empty());

    service.setBriefcaseExpiryDays(120);

    verify(repository)
        .save(
            assertThatCaptorMatches(
                s ->
                    GameSettingService.BRIEFCASE_EXPIRY_DAYS_KEY.equals(s.getSettingKey())
                        && "120".equals(s.getValue())
                        && s.getUniverseId() == null));
  }

  // ── NPC briefcase cash-in advisor (ATW-2b9u) ──────────────────────────────

  @Test
  void npcBriefcaseCashInEnabled_defaultsTrue() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_NPC_CASH_IN_ENABLED_KEY))
        .thenReturn(Optional.empty());
    assertThat(service.isNpcBriefcaseCashInEnabled()).isTrue();
  }

  @Test
  void npcBriefcaseCashInEnabled_settingExists_returnsValue() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_NPC_CASH_IN_ENABLED_KEY))
        .thenReturn(
            Optional.of(setting(GameSettingService.BRIEFCASE_NPC_CASH_IN_ENABLED_KEY, "false")));
    assertThat(service.isNpcBriefcaseCashInEnabled()).isFalse();
  }

  @Test
  void setNpcBriefcaseCashInEnabled_savesGlobalSetting() {
    when(repository.findGlobal(GameSettingService.BRIEFCASE_NPC_CASH_IN_ENABLED_KEY))
        .thenReturn(Optional.empty());

    service.setNpcBriefcaseCashInEnabled(false);

    verify(repository)
        .save(
            assertThatCaptorMatches(
                s ->
                    GameSettingService.BRIEFCASE_NPC_CASH_IN_ENABLED_KEY.equals(s.getSettingKey())
                        && "false".equals(s.getValue())
                        && s.getUniverseId() == null));
  }

  private GameSetting assertThatCaptorMatches(Predicate<GameSetting> match) {
    return ArgumentMatchers.argThat(s -> match.test(s));
  }

  // ── Credential fallback (beads ATW-p2y2) ────────────────────────────────────

  @Test
  void findById_credentialEmptyScopedRowFallsBackToGlobal() {
    // The sandbox bug: AiSettingsSync (or an admin save) left an EMPTY universe-scoped row; that
    // must not shadow a working global key — blank is not "this universe has its own key".
    Long universeId = 1L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting emptyScoped = setting("AI_GEMINI_API_KEY", "");
    emptyScoped.setUniverseId(universeId);
    GameSetting globalKey = setting("AI_GEMINI_API_KEY", "global-working-key");
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_GEMINI_API_KEY", universeId))
        .thenReturn(Optional.of(emptyScoped));
    Mockito.when(repository.findGlobal("AI_GEMINI_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findById("AI_GEMINI_API_KEY");

    assertThat(result).isPresent().contains(globalKey);
  }

  @Test
  void findById_credentialBlankScopedRowFallsBackToGlobal() {
    Long universeId = 2L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting blankScoped = setting("AI_GEMINI_API_KEY", "   ");
    blankScoped.setUniverseId(universeId);
    GameSetting globalKey = setting("AI_GEMINI_API_KEY", "global-working-key");
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_GEMINI_API_KEY", universeId))
        .thenReturn(Optional.of(blankScoped));
    Mockito.when(repository.findGlobal("AI_GEMINI_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findById("AI_GEMINI_API_KEY");

    assertThat(result).isPresent().contains(globalKey);
  }

  @Test
  void findById_credentialNonEmptyScopedRowStaysAuthoritative() {
    // A configured per-universe key still wins — the fallback only rescues blank rows.
    Long universeId = 3L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting scopedKey = setting("AI_GEMINI_API_KEY", "universe-specific-key");
    scopedKey.setUniverseId(universeId);
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_GEMINI_API_KEY", universeId))
        .thenReturn(Optional.of(scopedKey));

    Optional<GameSetting> result = service.findById("AI_GEMINI_API_KEY");

    assertThat(result).isPresent().contains(scopedKey);
  }

  @Test
  void findById_credentialNoUniverseStillReturnsEmpty() {
    // The CREDENTIAL_KEYS safety property is unchanged: no active universe → no credential read.
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(null);
    Mockito.when(repository.findGlobal("AI_GEMINI_API_KEY"))
        .thenReturn(Optional.of(setting("AI_GEMINI_API_KEY", "global-key")));

    assertThat(service.findById("AI_GEMINI_API_KEY")).isEmpty();
  }

  @Test
  void findByKeyForUniverse_configuredScopedRowWinsOverGlobal() {
    // The positive half of the blank-falls-through rule: a configured scoped row IS
    // authoritative for an explicit-universe lookup (used by background threads).
    Long universeId = 7L;
    GameSetting scopedKey = setting("AI_CLAUDE_API_KEY", "universe-7-key");
    scopedKey.setUniverseId(universeId);
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_CLAUDE_API_KEY", universeId))
        .thenReturn(Optional.of(scopedKey));

    Optional<GameSetting> result = service.findByKeyForUniverse("AI_CLAUDE_API_KEY", universeId);

    assertThat(result).isPresent().contains(scopedKey);
  }

  @Test
  void findById_genericKeyConfiguredScopedRowWinsOverGlobal() {
    // Non-credential, non-system keys: a configured scoped row wins over the global value.
    Long universeId = 4L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting scopedSetting = setting("narration_tone", "gritty");
    scopedSetting.setUniverseId(universeId);
    Mockito.when(repository.findBySettingKeyAndUniverseId("narration_tone", universeId))
        .thenReturn(Optional.of(scopedSetting));

    Optional<GameSetting> result = service.findById("narration_tone");

    assertThat(result).isPresent().contains(scopedSetting);
  }

  @Test
  void findById_credentialNullValuedScopedRowFallsBackToGlobal() {
    // hasText(null) is the third "not configured" shape: a row that exists but never had a
    // value written (distinct from empty and whitespace) — it must also fall through.
    Long universeId = 9L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting nullScoped = setting("AI_GEMINI_API_KEY", null);
    nullScoped.setUniverseId(universeId);
    GameSetting globalKey = setting("AI_GEMINI_API_KEY", "global-working-key");
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_GEMINI_API_KEY", universeId))
        .thenReturn(Optional.of(nullScoped));
    Mockito.when(repository.findGlobal("AI_GEMINI_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findById("AI_GEMINI_API_KEY");

    assertThat(result).isPresent().contains(globalKey);
  }

  @Test
  void findByKeyForUniverseNullValuedScopedRowFallsBackToGlobal() {
    // Same null-value shape on the explicit-universe lookup path.
    Long universeId = 9L;
    GameSetting nullScoped = setting("AI_CLAUDE_API_KEY", null);
    nullScoped.setUniverseId(universeId);
    GameSetting globalKey = setting("AI_CLAUDE_API_KEY", "global-key");
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_CLAUDE_API_KEY", universeId))
        .thenReturn(Optional.of(nullScoped));
    Mockito.when(repository.findGlobal("AI_CLAUDE_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findByKeyForUniverse("AI_CLAUDE_API_KEY", universeId);

    assertThat(result).isPresent().contains(globalKey);
  }

  @Test
  void findByKeyForUniverseBlankScopedRowFallsBackToGlobal() {
    // Blank-scoped on the explicit-universe path mirrors the findById credential rule.
    Long universeId = 5L;
    GameSetting blankScoped = setting("AI_CLAUDE_API_KEY", "  ");
    blankScoped.setUniverseId(universeId);
    GameSetting globalKey = setting("AI_CLAUDE_API_KEY", "global-key");
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_CLAUDE_API_KEY", universeId))
        .thenReturn(Optional.of(blankScoped));
    Mockito.when(repository.findGlobal("AI_CLAUDE_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findByKeyForUniverse("AI_CLAUDE_API_KEY", universeId);

    assertThat(result).isPresent().contains(globalKey);
  }

  @Test
  void findById_genericBlankScopedRowFallsBackToGlobal() {
    // Blank-scoped on the generic-key path: blank never shadows the global value.
    Long universeId = 6L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    GameSetting blankScoped = setting("narration_tone", "");
    blankScoped.setUniverseId(universeId);
    GameSetting globalSetting = setting("narration_tone", "cinematic");
    Mockito.when(repository.findBySettingKeyAndUniverseId("narration_tone", universeId))
        .thenReturn(Optional.of(blankScoped));
    Mockito.when(repository.findGlobal("narration_tone")).thenReturn(Optional.of(globalSetting));

    Optional<GameSetting> result = service.findById("narration_tone");

    assertThat(result).isPresent().contains(globalSetting);
  }

  @Test
  void findById_genericAbsentScopedRowFallsBackToGlobal() {
    // Absent-scoped on the generic-key path completes the truth table.
    Long universeId = 8L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    Mockito.when(repository.findBySettingKeyAndUniverseId("narration_tone", universeId))
        .thenReturn(Optional.empty());
    GameSetting globalSetting = setting("narration_tone", "cinematic");
    Mockito.when(repository.findGlobal("narration_tone")).thenReturn(Optional.of(globalSetting));

    assertThat(service.findById("narration_tone")).isPresent().contains(globalSetting);
  }

  @Test
  void findByKeyForUniverseNullUniverseIdGoesStraightToGlobal() {
    // Explicit-universe lookup with no universe: the scoped branch is skipped entirely.
    GameSetting globalKey = setting("AI_CLAUDE_API_KEY", "global-key");
    Mockito.when(repository.findGlobal("AI_CLAUDE_API_KEY")).thenReturn(Optional.of(globalKey));

    Optional<GameSetting> result = service.findByKeyForUniverse("AI_CLAUDE_API_KEY", null);

    assertThat(result).isPresent().contains(globalKey);
    Mockito.verify(repository, Mockito.never())
        .findBySettingKeyAndUniverseId(Mockito.anyString(), Mockito.any());
  }

  @Test
  void findById_credentialAbsentScopedRowFallsBackToGlobal() {
    // The absent arm of the credential condition: no scoped row at all → global.
    Long universeId = 11L;
    Mockito.when(universeContextService.getCurrentUniverseId()).thenReturn(universeId);
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_GEMINI_API_KEY", universeId))
        .thenReturn(Optional.empty());
    GameSetting globalKey = setting("AI_GEMINI_API_KEY", "global-working-key");
    Mockito.when(repository.findGlobal("AI_GEMINI_API_KEY")).thenReturn(Optional.of(globalKey));

    assertThat(service.findById("AI_GEMINI_API_KEY")).isPresent().contains(globalKey);
  }

  @Test
  void findByKeyForUniverseAbsentScopedRowFallsBackToGlobal() {
    // The absent arm of the explicit-universe lookup condition → global.
    Long universeId = 12L;
    Mockito.when(repository.findBySettingKeyAndUniverseId("AI_CLAUDE_API_KEY", universeId))
        .thenReturn(Optional.empty());
    GameSetting globalKey = setting("AI_CLAUDE_API_KEY", "global-key");
    Mockito.when(repository.findGlobal("AI_CLAUDE_API_KEY")).thenReturn(Optional.of(globalKey));

    assertThat(service.findByKeyForUniverse("AI_CLAUDE_API_KEY", universeId))
        .isPresent()
        .contains(globalKey);
  }
}
