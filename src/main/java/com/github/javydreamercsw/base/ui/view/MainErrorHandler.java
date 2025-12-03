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
package com.github.javydreamercsw.base.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServiceInitListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class MainErrorHandler {

  @Bean
  public VaadinServiceInitListener errorHandlerInitializer() {
    return (event) ->
        event
            .getSource()
            .addSessionInitListener(
                sessionInitEvent ->
                    sessionInitEvent
                        .getSession()
                        .setErrorHandler(
                            errorEvent -> {
                              log.error("An unexpected error occurred", errorEvent.getThrowable());
                              errorEvent
                                  .getComponent()
                                  .flatMap(Component::getUI)
                                  .ifPresent(
                                      ui -> {
                                        var notification =
                                            new Notification(
                                                "An unexpected error has occurred. Please try again"
                                                    + " later.");
                                        notification.addThemeVariants(
                                            NotificationVariant.LUMO_ERROR);
                                        notification.setPosition(Notification.Position.TOP_CENTER);
                                        notification.setDuration(3000);
                                        ui.access(notification::open);
                                      });
                            }));
  }
}
