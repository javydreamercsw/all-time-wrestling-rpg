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
