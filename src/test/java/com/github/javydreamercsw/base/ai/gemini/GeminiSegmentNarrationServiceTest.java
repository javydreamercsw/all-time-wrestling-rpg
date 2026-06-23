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
package com.github.javydreamercsw.base.ai.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GeminiSegmentNarrationServiceTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private Environment environment;
  @Mock private HttpClient httpClient;
  @Mock private PerformanceMonitoringService performanceMonitoringService;

  private GeminiSegmentNarrationService service;

  @BeforeEach
  void setUp() {
    service =
        new GeminiSegmentNarrationService(environment, aiSettingsService) {
          @Override
          protected synchronized HttpClient getHttpClient(final int timeout) {
            return httpClient;
          }
        };
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    service.setPerformanceMonitoringService(performanceMonitoringService);
  }

  /** Returns an empty HttpHeaders to satisfy the trace-log call in callGemini(). */
  private HttpHeaders emptyHeaders() {
    return HttpHeaders.of(Map.of(), (k, v) -> true);
  }

  @Test
  void getProviderName_returnsGoogleGemini() {
    assertEquals("Google Gemini", service.getProviderName());
  }

  @Test
  void isAvailable_enabledWithKey_returnsTrue() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");

    assertTrue(service.isAvailable());
  }

  @Test
  void isAvailable_disabled_returnsFalse() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(false);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_noApiKey_returnsFalse() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getGeminiApiKey()).thenReturn("");

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_nullApiKey_returnsFalse() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getGeminiApiKey()).thenReturn(null);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_testProfileActive_returnsFalse() {
    when(aiSettingsService.isGeminiEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});

    assertFalse(service.isAvailable());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_successfulResponse_returnsNarration() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "candidates": [{
            "content": {"parts": [{"text": "Epic wrestling narration!"}]},
            "finishReason": "STOP"
          }],
          "usageMetadata": {"promptTokenCount": 100, "candidatesTokenCount": 200}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a wrestling match");

    assertEquals("Epic wrestling narration!", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_multipleTextParts_concatenatesAll() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "candidates": [{
            "content": {"parts": [{"text": "Part one. "}, {"text": "Part two."}]},
            "finishReason": "STOP"
          }]
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a match");

    assertEquals("Part one. Part two.", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_apiError_throwsAIServiceException() throws IOException, InterruptedException {
    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(403);
    when(httpResponse.body()).thenReturn("{\"error\": \"API key invalid\"}");
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("bad-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    // The catch(Exception e) block wraps the AIServiceException as 500
    AIServiceException ex =
        assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));

    assertEquals(500, ex.getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_rateLimitError_throwsAIServiceException()
      throws IOException, InterruptedException {
    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(429);
    when(httpResponse.body())
        .thenReturn("{\"error\": {\"code\": 429, \"message\": \"Resource exhausted\"}}");
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_timeoutException_throwsAIServiceExceptionWith504()
      throws IOException, InterruptedException {
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
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
          "candidates": [{
            "content": {"parts": [{"text": "Narration text"}]},
            "finishReason": "STOP"
          }],
          "usageMetadata": {"promptTokenCount": 150, "candidatesTokenCount": 300}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    verify(performanceMonitoringService).recordTokenUsage("Google Gemini", 150, 300);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_emptyCandidates_returnsNoContentMessage()
      throws IOException, InterruptedException {
    String responseJson =
        """
        {"candidates": []}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-test-key");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a match");

    assertEquals("No content in AI response", result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_appendsApiKeyToUrl() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "candidates": [{"content": {"parts": [{"text": "ok"}]}, "finishReason": "STOP"}]
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpResponse.headers()).thenReturn(emptyHeaders());
    when(aiSettingsService.getGeminiModelName()).thenReturn("gemini-3.1-flash-lite-preview");
    when(aiSettingsService.getGeminiApiUrl())
        .thenReturn("https://generativelanguage.googleapis.com/v1beta/models/");
    when(aiSettingsService.getGeminiApiKey()).thenReturn("AIzaSy-abc123");
    when(aiSettingsService.getAiTimeout()).thenReturn(30);

    org.mockito.ArgumentCaptor<HttpRequest> requestCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    String uri = requestCaptor.getValue().uri().toString();
    assertTrue(uri.contains("key=AIzaSy-abc123"), "URL should contain the API key as query param");
    assertTrue(uri.contains("gemini-3.1-flash-lite-preview"), "URL should contain the model name");
    assertTrue(uri.contains(":generateContent"), "URL should end with :generateContent");
  }
}
