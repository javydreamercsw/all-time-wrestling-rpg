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
package com.github.javydreamercsw.management.ui;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.service.GameClock;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.ai.AiOnboardingService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.component.AiOnboardingDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting context bean injected into Vaadin views in place of the individual notification,
 * security, universe, and expansion services that every view needs.
 */
@Component
@RequiredArgsConstructor
@Getter
public class ViewContext {

  private final NotificationService notificationService;
  private final SecurityUtils securityUtils;
  private final UniverseContextService universeContextService;
  private final ExpansionService expansionService;
  private final GameSettingService gameSettingService;
  private final GameClock gameClock;
  private final AiOnboardingService aiOnboardingService;

  /** Prototype-style factory: the dialog is stateful per open, so every call builds a new one. */
  public AiOnboardingDialog createAiOnboardingDialog() {
    return new AiOnboardingDialog(aiOnboardingService, securityUtils);
  }

  /**
   * Surfaces a no-AI error as a component-content notification whose "Set up AI…" button opens the
   * onboarding wizard. Shared by every notification-based AI dead end so the composition (error
   * span + setup button) lives in exactly one place.
   */
  public void showAiSetupNotification(String reason) {
    Span errorSpan = new Span(reason);
    Button setupAi = new Button("Set up AI…", e -> createAiOnboardingDialog().open());
    setupAi.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    VerticalLayout content = new VerticalLayout(errorSpan, setupAi);
    content.setPadding(false);
    content.setSpacing(false);

    Notification notification = new Notification();
    notification.add(content);
    notification.setDuration(10_000);
    notification.open();
  }
}
