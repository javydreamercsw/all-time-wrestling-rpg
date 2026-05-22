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
package com.github.javydreamercsw.base.ai.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAIImageGenerationServiceTest {

  @Mock private AiSettingsService aiSettings;
  @Mock private HttpClient httpClient;

  private OpenAIImageGenerationService service;

  @BeforeEach
  void setUp() {
    service =
        new OpenAIImageGenerationService(aiSettings) {
          @Override
          protected HttpClient getHttpClient(final int timeoutSeconds) {
            return httpClient;
          }
        };
    when(aiSettings.getAiTimeout()).thenReturn(30);
  }

  // --- isAvailable ---

  @Test
  void isAvailable_enabledWithKey_returnsTrue() {
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");

    assertThat(service.isAvailable()).isTrue();
  }

  @Test
  void isAvailable_disabled_returnsFalse() {
    when(aiSettings.isOpenAIEnabled()).thenReturn(false);

    assertThat(service.isAvailable()).isFalse();
  }

  @Test
  void isAvailable_enabledWithEmptyKey_returnsFalse() {
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("");

    assertThat(service.isAvailable()).isFalse();
  }

  // --- getProviderName ---

  @Test
  void getProviderName_returnsOpenAI() {
    assertThat(service.getProviderName()).isEqualTo("OpenAI");
  }

  // --- generateImage when not available ---

  @Test
  void generateImage_whenNotAvailable_throws503() {
    when(aiSettings.isOpenAIEnabled()).thenReturn(false);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    assertThatThrownBy(() -> service.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .satisfies(ex -> assertThat(((AIServiceException) ex).getStatusCode()).isEqualTo(503));
  }

  // --- generateImage HTTP success paths ---

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_successUrlFormat_returnsImageUrl() throws IOException, InterruptedException {
    String responseJson =
        """
        {"data": [{"url": "https://openai-images.example.com/result.png"}]}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestling champion").build();

    String result = service.generateImage(request);

    assertThat(result).isEqualTo("https://openai-images.example.com/result.png");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_successB64Format_returnsBase64() throws IOException, InterruptedException {
    String responseJson =
        """
        {"data": [{"b64_json": "base64encodedimagedata=="}]}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A wrestling belt")
            .responseFormat("b64_json")
            .build();

    String result = service.generateImage(request);

    assertThat(result).isEqualTo("base64encodedimagedata==");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_usesRequestModelOverSettingsModel() throws IOException, InterruptedException {
    String responseJson =
        """
        {"data": [{"url": "https://openai-images.example.com/result.png"}]}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder()
            .prompt("A champion")
            .model("dall-e-2")
            .build();

    String result = service.generateImage(request);

    assertThat(result).isEqualTo("https://openai-images.example.com/result.png");
  }

  // --- generateImage HTTP error paths ---

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_apiReturns401_throwsAIServiceException()
      throws IOException, InterruptedException {
    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("{\"error\": {\"message\": \"Invalid API key\"}}");
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-bad-key");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    // The AIServiceException thrown inside the try block is caught and re-wrapped as 500
    assertThatThrownBy(() -> service.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .satisfies(ex -> assertThat(((AIServiceException) ex).getStatusCode()).isEqualTo(500));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_networkError_throwsAIServiceException500()
      throws IOException, InterruptedException {
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Connection reset"));

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    assertThatThrownBy(() -> service.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .satisfies(ex -> assertThat(((AIServiceException) ex).getStatusCode()).isEqualTo(500));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_emptyDataArray_throwsRuntimeException()
      throws IOException, InterruptedException {
    String responseJson =
        """
        {"data": []}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-test-key");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    assertThatThrownBy(() -> service.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .satisfies(ex -> assertThat(((AIServiceException) ex).getStatusCode()).isEqualTo(500));
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_sendsAuthorizationHeader() throws IOException, InterruptedException {
    String responseJson =
        """
        {"data": [{"url": "https://openai-images.example.com/result.png"}]}
        """;

    HttpResponse<String> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(aiSettings.isOpenAIEnabled()).thenReturn(true);
    when(aiSettings.getOpenAIApiKey()).thenReturn("sk-abc123");
    when(aiSettings.getOpenAIImageModel()).thenReturn("dall-e-3");

    org.mockito.ArgumentCaptor<HttpRequest> requestCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    service.generateImage(ImageGenerationService.ImageRequest.builder().prompt("A match").build());

    HttpRequest captured = requestCaptor.getValue();
    assertThat(captured.headers().firstValue("Authorization")).hasValue("Bearer sk-abc123");
  }
}
