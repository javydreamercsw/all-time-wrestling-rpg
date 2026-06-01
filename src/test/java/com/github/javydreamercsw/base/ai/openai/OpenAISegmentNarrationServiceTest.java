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
package com.github.javydreamercsw.base.ai.openai;

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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAISegmentNarrationServiceTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private Environment environment;
  @Mock private HttpClient httpClient;
  @Mock private PerformanceMonitoringService performanceMonitoringService;

  private OpenAISegmentNarrationService service;

  @BeforeEach
  void setUp() {
    service =
        new OpenAISegmentNarrationService(environment, aiSettingsService) {
          @Override
          protected synchronized HttpClient getHttpClient(final int timeout) {
            return httpClient;
          }
        };
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    service.setPerformanceMonitoringService(performanceMonitoringService);
  }

  @Test
  void getProviderName_defaultModel_returnsGpt35() {
    // getModel() falls back to default; premiumModel "gpt-4" is not contained in "gpt-3.5-turbo"
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn(null);
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("gpt-3.5-turbo");

    assertEquals("OpenAI GPT-3.5", service.getProviderName());
  }

  @Test
  void getProviderName_premiumModel_returnsGpt4() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");

    assertEquals("OpenAI GPT-4", service.getProviderName());
  }

  @Test
  void getProviderName_nullModel_returnsUnknown() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn(null);
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn(null);

    assertEquals("OpenAI Unknown", service.getProviderName());
  }

  @Test
  void isAvailable_enabledWithKey_returnsTrue() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");

    assertTrue(service.isAvailable());
  }

  @Test
  void isAvailable_disabled_returnsFalse() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(false);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_noApiKey_returnsFalse() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("");

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_nullApiKey_returnsFalse() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn(null);

    assertFalse(service.isAvailable());
  }

  @Test
  void isAvailable_testProfileActive_returnsFalse() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});

    assertFalse(service.isAvailable());
  }

  @Test
  void generateText_notAvailable_throwsAIServiceException() {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(false);
    // callOpenAI checks isAvailable() first — which itself checks isOpenAIEnabled
    // We need to also set up for the isAvailable call path
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("gpt-3.5-turbo");

    AIServiceException ex =
        assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));

    assertEquals(400, ex.getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_successfulResponse_returnsNarration() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "choices": [{
            "message": {"role": "assistant", "content": "Epic wrestling narration!"},
            "finish_reason": "stop"
          }],
          "usage": {"prompt_tokens": 100, "completion_tokens": 200}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
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
    when(httpResponse.body()).thenReturn("{\"error\": {\"message\": \"Invalid API key\"}}");
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("invalid-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
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
    when(httpResponse.body()).thenReturn("{\"error\": {\"message\": \"Rate limit exceeded\"}}");
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThrows(AIServiceException.class, () -> service.generateText("Narrate a match"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_timeoutException_throwsAIServiceExceptionWith504()
      throws IOException, InterruptedException {
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
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
          "choices": [{
            "message": {"role": "assistant", "content": "Narration text"},
            "finish_reason": "stop"
          }],
          "usage": {"prompt_tokens": 150, "completion_tokens": 300}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    verify(performanceMonitoringService).recordTokenUsage("OpenAI GPT-4", 150, 300);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_emptyChoices_returnsNoContentMessage()
      throws IOException, InterruptedException {
    String responseJson =
        """
        {"choices": []}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    String result = service.generateText("Narrate a match");

    assertEquals("No content in AI response", result);
  }

  @Test
  void isUsingPremiumModel_withPremiumModel_returnsTrue() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");

    assertTrue(service.isUsingPremiumModel());
  }

  @Test
  void isUsingPremiumModel_withDefaultModel_returnsFalse() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn(null);
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("gpt-3.5-turbo");

    assertFalse(service.isUsingPremiumModel());
  }

  @Test
  void getCostPer1KTokens_premiumModel_returnsHigherCost() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");

    assertEquals(10.0, service.getCostPer1KTokens(), 0.001);
  }

  @Test
  void getCostPer1KTokens_defaultModel_returnsLowerCost() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn(null);
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("gpt-3.5-turbo");

    assertEquals(0.50, service.getCostPer1KTokens(), 0.001);
  }

  @Test
  void getModel_withConfiguredModel_returnsConfiguredModel() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4-turbo-preview");

    assertEquals("gpt-4-turbo-preview", service.getModel());
  }

  @Test
  void getModel_withEmptyConfig_returnsDefaultModel() {
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("  ");
    when(aiSettingsService.getOpenAIDefaultModel()).thenReturn("gpt-3.5-turbo");

    assertEquals("gpt-3.5-turbo", service.getModel());
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateText_sendsAuthorizationHeader() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "choices": [{"message": {"content": "ok"}, "finish_reason": "stop"}],
          "usage": {}
        }
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettingsService.isOpenAIEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
    when(aiSettingsService.getOpenAIApiKey()).thenReturn("sk-abc123");
    when(aiSettingsService.getOpenAIPremiumModel()).thenReturn("gpt-4");
    when(aiSettingsService.getOpenAIApiUrl())
        .thenReturn("https://api.openai.com/v1/chat/completions");
    when(aiSettingsService.getOpenAIMaxTokens()).thenReturn(1000);
    when(aiSettingsService.getAiTimeout()).thenReturn(30);

    org.mockito.ArgumentCaptor<HttpRequest> requestCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateText("Narrate a match");

    HttpRequest captured = requestCaptor.getValue();
    assertTrue(captured.headers().firstValue("Authorization").isPresent());
    assertEquals("Bearer sk-abc123", captured.headers().firstValue("Authorization").get());
  }
}
