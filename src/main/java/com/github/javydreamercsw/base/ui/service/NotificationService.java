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
package com.github.javydreamercsw.base.ui.service;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for displaying consistent and rich notifications across the application. */
@Service
@Slf4j
public class NotificationService {

  /**
   * Displays a success notification.
   *
   * @param message The message to display.
   */
  public void showSuccess(@NonNull String message) {
    Notification notification = Notification.show(message, 3_000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  /**
   * Displays an error notification.
   *
   * @param message The message to display.
   */
  public void showError(@NonNull String message) {
    Notification notification = Notification.show(message, 5_000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  /**
   * Displays a warning notification.
   *
   * @param message The message to display.
   */
  public void showWarning(@NonNull String message) {
    Notification notification = Notification.show(message, 4_000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
  }

  /**
   * Specifically handles AI service errors with rich details.
   *
   * @param t The throwable to analyze.
   */
  public void showAIServiceError(@NonNull Throwable t) {
    AIServiceException aiException = findAIServiceException(t);

    if (aiException != null) {
      showRichAIError(aiException);
    } else {
      showError("AI Service Error: " + t.getMessage());
    }
  }

  private AIServiceException findAIServiceException(Throwable t) {
    if (t == null) return null;
    if (t instanceof AIServiceException) return (AIServiceException) t;
    return findAIServiceException(t.getCause());
  }

  private void showRichAIError(@NonNull AIServiceException ex) {
    UI ui = UI.getCurrent();
    if (ui == null) return;

    ui.access(
        () -> {
          Notification notification = new Notification();
          notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
          notification.setPosition(Notification.Position.MIDDLE);
          notification.setDuration(0); // Manual close

          VerticalLayout layout = new VerticalLayout();
          layout.setPadding(false);
          layout.setSpacing(false);
          layout.addClassName(LumoUtility.MaxWidth.SCREEN_SMALL);

          H4 header = new H4("AI Generation Failed");
          header.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

          Span provider = new Span("Provider: " + ex.getProvider());
          provider.addClassNames(
              LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.ERROR);

          Div detail = new Div();
          detail.setText(getUserFriendlyMessage(ex));
          detail.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.FontSize.SMALL);

          com.vaadin.flow.component.button.Button closeBtn =
              new com.vaadin.flow.component.button.Button("Dismiss", e -> notification.close());
          closeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
          closeBtn.addClassName(LumoUtility.Margin.Top.MEDIUM);

          layout.add(header, provider, detail, closeBtn);
          notification.add(layout);
          notification.open();
        });
  }

  private String getUserFriendlyMessage(@NonNull AIServiceException ex) {
    String msg = ex.getMessage();
    if (msg == null) return "An unknown error occurred with the AI service.";

    // Simple heuristic: if it looks like Gemini's high demand error
    if (msg.contains("experiencing high demand") || msg.contains("UNAVAILABLE")) {
      return "The AI model is currently experiencing high demand. Please wait a few moments and"
          + " try again.";
    }

    if (ex.getStatusCode() == 429) {
      return "Rate limit exceeded. You've made too many requests in a short period.";
    }

    if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
      return "Authentication failed. Please check your AI API key in settings.";
    }

    if (ex.getStatusCode() >= 500) {
      return "The AI provider is currently having technical issues. Please try again later.";
    }

    // Return original message if no specific match, but maybe clean it up
    if (ex.getProvider() != null && msg.startsWith("[" + ex.getProvider() + "] ")) {
      return msg.substring(ex.getProvider().length() + 3);
    }

    return msg;
  }
}
