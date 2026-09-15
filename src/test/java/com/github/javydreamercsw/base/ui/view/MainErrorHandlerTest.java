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

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link MainErrorHandler} error routing (ATW-w9qd). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MainErrorHandler Tests")
class MainErrorHandlerTest {

  @Mock private NotificationService notificationService;

  private MainErrorHandler errorHandler;

  @BeforeEach
  void setUp() {
    errorHandler = new MainErrorHandler(notificationService);
  }

  @Test
  @DisplayName("Direct AIServiceException routes to the rich AI error dialog")
  void directAIServiceException_showsAIServiceError() {
    AIServiceException aiError =
        new AIServiceException(503, "Service Unavailable", "Gemini", "Gemini is down");
    when(notificationService.findAIServiceException(aiError)).thenReturn(aiError);

    errorHandler.showError(aiError);

    verify(notificationService).showAIServiceError(aiError);
    verify(notificationService, never()).showError(contains("An unexpected error"));
  }

  @Test
  @DisplayName("AIServiceException wrapped in the cause chain routes to the AI error dialog")
  void wrappedAIServiceException_showsAIServiceError() {
    AIServiceException aiError =
        new AIServiceException(429, "Too Many Requests", "OpenAI", "Rate limited");
    RuntimeException wrapper = new IllegalStateException("Narration failed", aiError);
    when(notificationService.findAIServiceException(wrapper)).thenReturn(aiError);

    errorHandler.showError(wrapper);

    verify(notificationService).showAIServiceError(wrapper);
    verify(notificationService, never()).showError(contains("An unexpected error"));
  }

  @Test
  @DisplayName("Non-AI exception shows a generic error with the real message")
  void nonAiException_showsGenericError() {
    IllegalStateException renderError = new IllegalStateException("Grid row exploded");

    errorHandler.showError(renderError);

    verify(notificationService, never()).showAIServiceError(renderError);
    verify(notificationService).showError("An unexpected error occurred: Grid row exploded");
  }

  @Test
  @DisplayName("Non-AI exception with an AI-free cause chain shows a generic error")
  void nonAiExceptionWithCause_showsGenericError() {
    IllegalStateException renderError =
        new IllegalStateException("Lazy init failed", new RuntimeException("session closed"));

    errorHandler.showError(renderError);

    verify(notificationService, never()).showAIServiceError(renderError);
    verify(notificationService).showError(eq("An unexpected error occurred: Lazy init failed"));
  }

  @Test
  @DisplayName("Null message still produces a generic error notification")
  void nullMessage_showsGenericError() {
    IllegalStateException renderError = new IllegalStateException();

    errorHandler.showError(renderError);

    verify(notificationService, never()).showAIServiceError(renderError);
    verify(notificationService).showError(eq("An unexpected error occurred: null"));
  }
}
