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
package com.github.javydreamercsw.base.ai.claude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class ClaudeSegmentNarrationServiceTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private ClaudeConfigProperties claudeConfigProperties;
  @Mock private Environment environment;
  @Mock private HttpClient httpClient;
  @Mock private PerformanceMonitoringService performanceMonitoringService;

  private ClaudeSegmentNarrationService service;

  @BeforeEach
  void setUp() {
    service =
        new ClaudeSegmentNarrationService(claudeConfigProperties, environment, aiSettingsService) {
          @Override
          protected HttpClient getHttpClient(final int timeout) {
            return httpClient;
          }
        };
    service.setPerformanceMonitoringService(performanceMonitoringService);
  }

  @Test
  void getProviderName_returnsAnthropicClaude() {
    assertEquals("Anthropic Claude", service.getProviderName());
  }

  @Test
  void isAvailable_enabledWithKey_returnsTrue() {
    when(aiSettingsService.isClaudeEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");

    assertTrue(service.isAvailable());
  }

  @Test
  void isAvailable_disabled_returnsFalse() {
    when(aiSettingsService.isClaudeEnabled()).thenReturn(false);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_noApiKey_returnsFalse() {
    when(aiSettingsService.isClaudeEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(claudeConfigProperties.getApiKey()).thenReturn("");

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_nullApiKey_returnsFalse() {
    when(aiSettingsService.isClaudeEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(claudeConfigProperties.getApiKey()).thenReturn(null);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_testProfileActive_returnsFalse() {
    when(aiSettingsService.isClaudeEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});

    assertFalse(service.isAvailable());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_successfulResponse_returnsNarration() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "content": [{"type": "text", "text": "Epic wrestling narration!"}],
          "usage": {"input_tokens": 100, "output_tokens": 200}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a wrestling match");

    assertEquals("Epic wrestling narration!", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_apiError_throwsAIServiceException() throws IOException, InterruptedException {
    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("{\"error\": \"Unauthorized\"}");
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("invalid-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    AIServiceException ex =
        assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));

    // The catch(Exception e) block wraps the AIServiceException as 500
    assertEquals(500, ex.getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_rateLimitError_throwsAIServiceException()
      throws IOException, InterruptedException {
    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(429);
    when(httpResponse.body()).thenReturn("{\"error\": \"Rate limit exceeded\"}");
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_timeoutException_throwsAIServiceExceptionWith504()
      throws IOException, InterruptedException {
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new HttpTimeoutException("Connection timed out"));

    AIServiceException ex =
        assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));

    assertEquals(504, ex.getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_successWithUsage_recordsTokenUsage() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "content": [{"type": "text", "text": "Narration text"}],
          "usage": {"input_tokens": 150, "output_tokens": 300}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    verify(performanceMonitoringService).recordTokenUsage("Anthropic Claude", 150, 300);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_emptyContent_returnsNoContentMessage()
      throws IOException, InterruptedException {
    String responseJson =
        """
        {"content": []}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-test-key");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a match");

    assertEquals("No content in AI response", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_sendsCorrectHeaders() throws IOException, InterruptedException {
    String responseJson =
        """
        {"content": [{"type": "text", "text": "narration"}], "usage": {}}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(claudeConfigProperties.getApiUrl()).thenReturn("https://api.anthropic.com/v1/messages");
    when(claudeConfigProperties.getModelName()).thenReturn("claude-3-haiku-20240307");
    when(claudeConfigProperties.getApiKey()).thenReturn("sk-abc123");
    when(claudeConfigProperties.getTimeout()).thenReturn(30);

    org.mockito.ArgumentCaptor<HttpRequest> requestCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    HttpRequest captured = requestCaptor.getValue();
    assertTrue(captured.headers().firstValue("x-api-key").isPresent());
    assertEquals("sk-abc123", captured.headers().firstValue("x-api-key").get());
    assertTrue(captured.headers().firstValue("anthropic-version").isPresent());
    assertEquals("application/json", captured.headers().firstValue("Content-Type").get());
  }
}
