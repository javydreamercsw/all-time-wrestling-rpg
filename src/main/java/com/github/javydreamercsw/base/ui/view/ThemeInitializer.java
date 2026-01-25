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
package com.github.javydreamercsw.base.ui.view;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.theme.ThemeService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThemeInitializer {

  @Bean
  public VaadinServiceInitListener themeServiceInitListener(
      ThemeService themeService, SecurityUtils securityUtils) {
    return event ->
        event
            .getSource()
            .addUIInitListener(
                uiEvent -> {
                  UI ui = uiEvent.getUI();
                  ui.addAfterNavigationListener(
                      afterNavigationEvent -> {
                        securityUtils
                            .getAuthenticatedUser()
                            .ifPresentOrElse(
                                user -> {
                                  String theme = themeService.getEffectiveTheme(user.getAccount());
                                  applyTheme(ui, theme);
                                },
                                () -> {
                                  String theme =
                                      themeService.getGlobalDefaultTheme().orElse("light");
                                  applyTheme(ui, theme);
                                });
                      });
                });
  }

  private void applyTheme(UI ui, String theme) {
    if ("light".equals(theme)) {
      ui.getElement().setAttribute("theme", "");
    } else {
      ui.getElement().setAttribute("theme", theme);
    }
  }
}
