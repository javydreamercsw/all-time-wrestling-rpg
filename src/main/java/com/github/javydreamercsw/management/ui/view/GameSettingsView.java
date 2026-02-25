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
import com.github.javydreamercsw.management.service.GameSettingService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@PageTitle("Game Settings")
@Component
@RolesAllowed("ADMIN")
@Lazy
@UIScope
public class GameSettingsView extends VerticalLayout {

  private final GameSettingService gameSettingService;
  private final ThemeService themeService;
  private DatePicker gameDatePicker;
  private ComboBox<String> defaultThemeSelection;

  @Autowired
  public GameSettingsView(GameSettingService gameSettingService, ThemeService themeService) {
    this.gameSettingService = gameSettingService;
    this.themeService = themeService;
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

    VerticalLayout layout =
        new VerticalLayout(
            gameDatePicker,
            defaultThemeSelection,
            aiNewsEnabled,
            wearAndTearEnabled,
            rumorChance,
            newsStrategy);

    add(layout);
  }
}
