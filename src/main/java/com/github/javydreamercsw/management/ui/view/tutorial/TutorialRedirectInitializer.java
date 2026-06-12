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
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a {@code BeforeEnterListener} on every new UI that redirects first-time PLAYER users to
 * {@code /tutorial} before they can navigate anywhere else. Mirrors the pattern used by {@code
 * ThemeInitializer}.
 */
@Configuration
public class TutorialRedirectInitializer {

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
                              if (TutorialView.class.equals(enterEvent.getNavigationTarget())) {
                                return;
                              }
                              if (!securityUtils.isAuthenticated() || !securityUtils.isPlayer()) {
                                return;
                              }

                              Universe.UniverseType type =
                                  universeContextService
                                      .getCurrentUniverse()
                                      .map(Universe::getType)
                                      .orElse(Universe.UniverseType.GLOBAL);

                              securityUtils
                                  .getCurrentAccountId()
                                  .flatMap(accountService::get)
                                  .ifPresent(
                                      account -> {
                                        if (tutorialService.shouldShowTutorial(account, type)) {
                                          enterEvent.rerouteTo(TutorialView.class);
                                        }
                                      });
                            }));
  }
}
