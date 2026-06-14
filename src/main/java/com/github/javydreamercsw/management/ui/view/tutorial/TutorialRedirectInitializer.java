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
package com.github.javydreamercsw.management.ui.view.tutorial;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a {@code BeforeEnterListener} on every new UI that redirects first-time PLAYER users to
 * {@code /tutorial} before they can navigate anywhere else. Mirrors the pattern used by {@code
 * ThemeInitializer}.
 */
@Configuration
public class TutorialRedirectInitializer {

  @Value("${atw.tutorial.redirect.enabled:true}")
  private boolean redirectEnabled;

  @Bean
  public VaadinServiceInitListener tutorialRedirectInitListener(
      final TutorialService tutorialService,
      final SecurityUtils securityUtils,
      final AccountService accountService,
      final UniverseContextService universeContextService) {

    return event ->
        event
            .getSource()
            .addUIInitListener(
                uiEvent ->
                    uiEvent
                        .getUI()
                        .addBeforeEnterListener(
                            enterEvent -> {
                              if (!redirectEnabled) {
                                return;
                              }
                              if (TutorialView.class.equals(enterEvent.getNavigationTarget())) {
                                return;
                              }
                              if (!securityUtils.isAuthenticated() || !securityUtils.isPlayer()) {
                                return;
                              }

                              securityUtils
                                  .getCurrentAccountId()
                                  .flatMap(accountService::get)
                                  .ifPresent(
                                      account -> {
                                        // Once the player has a tutorial universe they are
                                        // actively working through the wizard — let them
                                        // navigate freely to complete each step's required action.
                                        if (tutorialService
                                            .findTutorialUniverse(account.getUsername())
                                            .isPresent()) {
                                          return;
                                        }

                                        // No tutorial universe yet: redirect to /tutorial so the
                                        // player picks a mode before doing anything else.
                                        Universe.UniverseType type =
                                            universeContextService
                                                .getCurrentUniverse()
                                                .map(Universe::getType)
                                                .orElse(Universe.UniverseType.GLOBAL);

                                        if (tutorialService.shouldShowTutorial(account, type)) {
                                          if (!tutorialService.hasBeenRedirected(account, type)) {
                                            tutorialService.recordFirstRedirect(account, type);
                                            enterEvent.rerouteTo(TutorialView.class);
                                          } else {
                                            Notification notification =
                                                Notification.show(
                                                    "You have an unfinished tutorial. Find it in"
                                                        + " your Profile drawer.",
                                                    5000,
                                                    Position.BOTTOM_START);
                                            notification.addThemeVariants(
                                                NotificationVariant.LUMO_PRIMARY);
                                          }
                                        }
                                      });
                            }));
  }
}
