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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Optional;
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
  private final ThemeService themeService;
  private final NotionHandler notionHandler;
  private DatePicker gameDatePicker;
  private ComboBox<String> defaultThemeSelection;
  private PasswordField notionTokenField;

  @Autowired
  public GameSettingsView(
      final GameSettingService gameSettingService,
      final ThemeService themeService,
      final Optional<NotionHandler> notionHandler) {
    this.gameSettingService = gameSettingService;
    this.themeService = themeService;
    this.notionHandler = notionHandler.orElse(null);
    init();
  }

  private void init() {
    gameDatePicker = new DatePicker("Current Game Date");
    gameDatePicker.setValue(gameSettingService.getCurrentGameDate());
    gameDatePicker.addValueChangeListener(
        event -> {
          gameSettingService.saveCurrentGameDate(event.getValue());
          Notification.show("Game date updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

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

    notionTokenField = new PasswordField("Notion Integration Token");
    try {
      notionTokenField.setValue(
          gameSettingService.getNotionToken() != null ? gameSettingService.getNotionToken() : "");
    } catch (Exception e) {
      log.warn(
          "Could not retrieve Notion token (likely due to missing authentication): {}",
          e.getMessage());
      notionTokenField.setValue("");
    }
    notionTokenField.setPlaceholder("Enter your Notion API token");
    notionTokenField.setWidthFull();
    notionTokenField.addValueChangeListener(
        event -> {
          gameSettingService.setNotionToken(event.getValue());
          if (notionHandler != null) {
            notionHandler.reinitialize();
          }
          Notification.show("Notion token updated")
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox aiNewsEnabled = new Checkbox("Enable AI-Powered News Feed");

    aiNewsEnabled.setValue(gameSettingService.isAiNewsEnabled());

    aiNewsEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setAiNewsEnabled(event.getValue());

          Notification.show("AI News Feed " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox wearAndTearEnabled = new Checkbox("Enable Persistent Wear & Tear");
    wearAndTearEnabled.setValue(gameSettingService.isWearAndTearEnabled());
    wearAndTearEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setWearAndTearEnabled(event.getValue());
          Notification.show("Persistent Wear & Tear " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    Checkbox statusCardsEnabled = new Checkbox("Enable Status Cards Mechanic");
    statusCardsEnabled.setValue(gameSettingService.isStatusCardsEnabled());
    statusCardsEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setStatusCardsEnabled(event.getValue());
          Notification.show("Status Cards Mechanic " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField rumorChance = new IntegerField("Daily Rumor Chance (%)");

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

    newsStrategy.setItems(List.of("SEGMENT", "SHOW"));

    newsStrategy.setValue(gameSettingService.getNewsStrategy());

    newsStrategy.addValueChangeListener(
        event -> {
          gameSettingService.setNewsStrategy(event.getValue());

          Notification.show("News strategy updated to: " + event.getValue())
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    // ── Rivalry Configuration ──────────────────────────────────────────────────
    IntegerField pleThreshold = new IntegerField("PLE Resolution Threshold (2d20 must exceed)");
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
    resolutionOnRegular.setValue(gameSettingService.isRivalryResolutionOnRegularShowsEnabled());
    resolutionOnRegular.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryResolutionOnRegularShowsEnabled(event.getValue());
          Notification.show(
                  "Regular show resolution " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField maxDuration = new IntegerField("Max Rivalry Duration (days, 0 = unlimited)");
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
    heatDecayEnabled.setValue(gameSettingService.isRivalryHeatDecayEnabled());
    heatDecayEnabled.addValueChangeListener(
        event -> {
          gameSettingService.setRivalryHeatDecayEnabled(event.getValue());
          Notification.show("Heat decay " + (event.getValue() ? "enabled" : "disabled"))
              .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

    IntegerField decayAmount = new IntegerField("Heat decay per interval");
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
            new H3("Rivalry Configuration"),
            pleThreshold,
            regularThreshold,
            resolutionOnRegular,
            maxDuration,
            heatDecayEnabled,
            decayAmount,
            decayInterval);
    rivalrySection.setPadding(false);

    VerticalLayout layout =
        new VerticalLayout(
            gameDatePicker,
            defaultThemeSelection,
            notionTokenField,
            aiNewsEnabled,
            wearAndTearEnabled,
            rumorChance,
            newsStrategy,
            rivalrySection);

    add(layout);
  }
}
