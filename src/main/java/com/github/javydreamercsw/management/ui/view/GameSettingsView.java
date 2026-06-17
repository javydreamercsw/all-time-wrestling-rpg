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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.domain.GameSettingRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@PageTitle("Game Settings")
@Component
@RolesAllowed("ADMIN")
@Lazy
@UIScope
@Slf4j
public class GameSettingsView extends VerticalLayout {

  private final GameSettingService gameSettingService;
  private final GameSettingRepository gameSettingRepository;
  private final ThemeService themeService;
  private final UniverseContextService universeContextService;
  private DatePicker gameDatePicker;
  private ComboBox<String> defaultThemeSelection;

  @Autowired
  public GameSettingsView(
      final GameSettingService gameSettingService,
      final GameSettingRepository gameSettingRepository,
      final ThemeService themeService,
      final UniverseContextService universeContextService) {
    this.gameSettingService = gameSettingService;
    this.gameSettingRepository = gameSettingRepository;
    this.themeService = themeService;
    this.universeContextService = universeContextService;
    init();
  }

  private void init() {
    Long universeId = universeContextService.getCurrentUniverseId();
    boolean hasUniverse = universeId != null;

    // ── Universe context banner ──────────────────────────────────────────────
    if (hasUniverse) {
      Span universeBadge = new Span("Editing settings for universe ID: " + universeId);
      universeBadge
          .getStyle()
          .set("font-style", "italic")
          .set("color", "var(--lumo-secondary-text-color)");
      add(universeBadge);
    } else {
      Span globalBadge = new Span("Editing global default settings (no universe selected)");
      globalBadge
          .getStyle()
          .set("font-style", "italic")
          .set("color", "var(--lumo-secondary-text-color)");
      add(globalBadge);
    }

    // ── System settings (always global) ─────────────────────────────────────
    add(new H3("System Settings"));

    defaultThemeSelection = new ComboBox<>("Default Application Theme");
    defaultThemeSelection.setId("default-theme-selection");
    defaultThemeSelection.setItems(themeService.getAvailableThemes());
    defaultThemeSelection.setValue(themeService.getGlobalDefaultTheme().orElse("light"));
    defaultThemeSelection.addValueChangeListener(
        event -> {
          themeService.updateGlobalDefaultTheme(event.getValue());
          Notification.show("Default theme updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
    add(defaultThemeSelection);

    // ── Gameplay settings (global default + per-universe override) ───────────
    add(new H3("Gameplay Settings"));
    if (hasUniverse) {
      Span note =
          new Span(
              "Settings marked '(inherited)' use the global default. Save a value to override for"
                  + " this universe.");
      note.getStyle().set("font-style", "italic").set("color", "var(--lumo-secondary-text-color)");
      add(note);
    }

    gameDatePicker = new DatePicker("Current Game Date");
    gameDatePicker.setHelperText(
        universeInheritanceLabel(GameSettingService.CURRENT_GAME_DATE_KEY, universeId));
    gameDatePicker.setValue(gameSettingService.getCurrentGameDate());
    gameDatePicker.addValueChangeListener(
        event -> {
          gameSettingService.saveCurrentGameDate(event.getValue());
          Notification.show("Game date updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox aiNewsEnabled = new Checkbox("Enable AI-Powered News Feed");
    aiNewsEnabled.setHelperText(
        universeInheritanceLabel(GameSettingService.AI_NEWS_ENABLED_KEY, universeId));
    aiNewsEnabled.setValue(gameSettingService.isAiNewsEnabled());
    aiNewsEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setAiNewsEnabled(event.getValue());
          Notification.show("AI News Feed " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox wearAndTearEnabled = new Checkbox("Enable Persistent Wear & Tear");
    wearAndTearEnabled.setHelperText(
        universeInheritanceLabel(GameSettingService.WEAR_AND_TEAR_ENABLED_KEY, universeId));
    wearAndTearEnabled.setValue(gameSettingService.isWearAndTearEnabled());
    wearAndTearEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setWearAndTearEnabled(event.getValue());
          Notification.show("Persistent Wear & Tear " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox statusCardsEnabled = new Checkbox("Enable Status Cards Mechanic");
    statusCardsEnabled.setHelperText(
        universeInheritanceLabel(GameSettingService.STATUS_CARDS_ENABLED_KEY, universeId));
    statusCardsEnabled.setValue(gameSettingService.isStatusCardsEnabled());
    statusCardsEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setStatusCardsEnabled(event.getValue());
          Notification.show("Status Cards Mechanic " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField rumorChance = new IntegerField("Daily Rumor Chance (%)");
    rumorChance.setHelperText(
        universeInheritanceLabel(GameSettingService.NEWS_RUMOR_CHANCE_KEY, universeId));
    rumorChance.setMin(0);
    rumorChance.setMax(100);
    rumorChance.setValue(gameSettingService.getNewsRumorChance());
    rumorChance.setStepButtonsVisible(true);
    rumorChance.addValueChangeListener(
        event -> {
          gameSettingService.setNewsRumorChance(event.getValue());
          Notification.show("Rumor chance updated to: " + event.getValue() + "%")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    ComboBox<String> newsStrategy = new ComboBox<>("News Generation Strategy");
    newsStrategy.setHelperText(
        universeInheritanceLabel(GameSettingService.NEWS_STRATEGY_KEY, universeId));
    newsStrategy.setItems(List.of("SEGMENT", "SHOW"));
    newsStrategy.setValue(gameSettingService.getNewsStrategy());
    newsStrategy.addValueChangeListener(
        event -> {
          gameSettingService.setNewsStrategy(event.getValue());
          Notification.show("News strategy updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    // ── Rivalry Configuration ────────────────────────────────────────────────
    H4 rivalryHeader = new H4("Rivalry Configuration");

    IntegerField pleThreshold = new IntegerField("PLE Resolution Threshold (2d20 must exceed)");
    pleThreshold.setHelperText(
        universeInheritanceLabel(
            GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_PLE_KEY, universeId));
    pleThreshold.setMin(2);
    pleThreshold.setMax(40);
    pleThreshold.setStepButtonsVisible(true);
    pleThreshold.setValue(gameSettingService.getRivalryResolutionThresholdPle());
    pleThreshold.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryResolutionThresholdPle(event.getValue());
          Notification.show("PLE resolution threshold updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField regularThreshold =
        new IntegerField("Regular Show Resolution Threshold (2d20 must exceed)");
    regularThreshold.setHelperText(
        universeInheritanceLabel(
            GameSettingService.RIVALRY_RESOLUTION_THRESHOLD_REGULAR_KEY, universeId));
    regularThreshold.setMin(2);
    regularThreshold.setMax(40);
    regularThreshold.setStepButtonsVisible(true);
    regularThreshold.setValue(gameSettingService.getRivalryResolutionThresholdRegular());
    regularThreshold.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryResolutionThresholdRegular(event.getValue());
          Notification.show("Regular show resolution threshold updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox resolutionOnRegular =
        new Checkbox("Allow rivalry resolution on regular (non-PLE) shows");
    resolutionOnRegular.setHelperText(
        universeInheritanceLabel(
            GameSettingService.RIVALRY_RESOLUTION_ON_REGULAR_SHOWS_KEY, universeId));
    resolutionOnRegular.setValue(gameSettingService.isRivalryResolutionOnRegularShowsEnabled());
    resolutionOnRegular.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryResolutionOnRegularShowsEnabled(event.getValue());
          Notification.show(
                  "Regular show resolution " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField maxDuration = new IntegerField("Max Rivalry Duration (days, 0 = unlimited)");
    maxDuration.setHelperText(
        universeInheritanceLabel(GameSettingService.RIVALRY_MAX_DURATION_DAYS_KEY, universeId));
    maxDuration.setMin(0);
    maxDuration.setStepButtonsVisible(true);
    maxDuration.setValue(gameSettingService.getRivalryMaxDurationDays());
    maxDuration.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryMaxDurationDays(event.getValue());
          Notification.show(
                  event.getValue() == 0
                      ? "Max rivalry duration disabled"
                      : "Max rivalry duration set to " + event.getValue() + " days")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox heatDecayEnabled = new Checkbox("Enable automatic heat decay");
    heatDecayEnabled.setHelperText(
        universeInheritanceLabel(GameSettingService.RIVALRY_HEAT_DECAY_ENABLED_KEY, universeId));
    heatDecayEnabled.setValue(gameSettingService.isRivalryHeatDecayEnabled());
    heatDecayEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryHeatDecayEnabled(event.getValue());
          Notification.show("Heat decay " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField decayAmount = new IntegerField("Heat decay per interval");
    decayAmount.setHelperText(
        universeInheritanceLabel(
            GameSettingService.RIVALRY_HEAT_DECAY_PER_INTERVAL_KEY, universeId));
    decayAmount.setMin(1);
    decayAmount.setMax(10);
    decayAmount.setStepButtonsVisible(true);
    decayAmount.setValue(gameSettingService.getRivalryHeatDecayPerInterval());
    decayAmount.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryHeatDecayPerInterval(event.getValue());
          Notification.show("Heat decay amount updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField decayInterval = new IntegerField("Heat decay interval (days)");
    decayInterval.setHelperText(
        universeInheritanceLabel(
            GameSettingService.RIVALRY_HEAT_DECAY_INTERVAL_DAYS_KEY, universeId));
    decayInterval.setMin(1);
    decayInterval.setMax(30);
    decayInterval.setStepButtonsVisible(true);
    decayInterval.setValue(gameSettingService.getRivalryHeatDecayIntervalDays());
    decayInterval.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryHeatDecayIntervalDays(event.getValue());
          Notification.show("Heat decay interval updated to: " + event.getValue() + " days")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    VerticalLayout rivalrySection =
        new VerticalLayout(
            rivalryHeader,
            pleThreshold,
            regularThreshold,
            resolutionOnRegular,
            maxDuration,
            heatDecayEnabled,
            decayAmount,
            decayInterval);
    rivalrySection.setPadding(false);

    // ── Tutorial Configuration ────────────────────────────────────────────────
    H4 tutorialHeader = new H4("Tutorial Settings");

    Checkbox campaignTutorial = new Checkbox("Enable Campaign Tutorial");
    campaignTutorial.setHelperText(
        universeInheritanceLabel(GameSettingService.TUTORIAL_ENABLED_CAMPAIGN_KEY, universeId));
    campaignTutorial.setValue(gameSettingService.isTutorialEnabled(Universe.UniverseType.CAMPAIGN));
    campaignTutorial.addValueChangeListener(
        event -> {
          gameSettingService.setTutorialEnabled(Universe.UniverseType.CAMPAIGN, event.getValue());
          Notification.show("Campaign tutorial " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox leagueTutorial = new Checkbox("Enable League Tutorial");
    leagueTutorial.setHelperText(
        universeInheritanceLabel(GameSettingService.TUTORIAL_ENABLED_LEAGUE_KEY, universeId));
    leagueTutorial.setValue(gameSettingService.isTutorialEnabled(Universe.UniverseType.LEAGUE));
    leagueTutorial.addValueChangeListener(
        event -> {
          gameSettingService.setTutorialEnabled(Universe.UniverseType.LEAGUE, event.getValue());
          Notification.show("League tutorial " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox globalTutorial = new Checkbox("Enable Universe (Global) Tutorial");
    globalTutorial.setHelperText(
        universeInheritanceLabel(GameSettingService.TUTORIAL_ENABLED_GLOBAL_KEY, universeId));
    globalTutorial.setValue(gameSettingService.isTutorialEnabled(Universe.UniverseType.GLOBAL));
    globalTutorial.addValueChangeListener(
        event -> {
          gameSettingService.setTutorialEnabled(Universe.UniverseType.GLOBAL, event.getValue());
          Notification.show("Universe tutorial " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    VerticalLayout tutorialSection =
        new VerticalLayout(tutorialHeader, campaignTutorial, leagueTutorial, globalTutorial);
    tutorialSection.setPadding(false);

    add(
        gameDatePicker,
        aiNewsEnabled,
        wearAndTearEnabled,
        statusCardsEnabled,
        rumorChance,
        newsStrategy,
        rivalrySection,
        tutorialSection);
  }

  /**
   * Returns helper text indicating whether a gameplay setting is universe-specific or inherited
   * from the global default.
   */
  private String universeInheritanceLabel(final String key, final Long universeId) {
    if (universeId == null) {
      return "Global default";
    }
    boolean hasOverride =
        gameSettingRepository.findBySettingKeyAndUniverseId(key, universeId).isPresent();
    return hasOverride ? "Universe override" : "(inherited from global default)";
  }
}
