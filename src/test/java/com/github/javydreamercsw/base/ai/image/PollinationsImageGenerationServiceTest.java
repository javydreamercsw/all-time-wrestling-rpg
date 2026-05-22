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
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PollinationsImageGenerationServiceTest {

  @Mock private AiSettingsService aiSettingsService;
  @Mock private HttpClient httpClient;

  private PollinationsImageGenerationService service;

  @BeforeEach
  void setUp() {
    service =
        new PollinationsImageGenerationService(aiSettingsService) {
          @Override
          protected HttpClient getHttpClient(final int timeoutSeconds) {
            return httpClient;
          }
        };
    when(aiSettingsService.getAiTimeout()).thenReturn(30);
    when(aiSettingsService.getPollinationsApiKey()).thenReturn("");
  }

  // --- no-API-key path (returns URL directly) ---

  @Test
  void generateImage_noApiKey_returnsDirectUrl() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestling champion").build();

    String result = service.generateImage(request);

    assertThat(result)
        .isEqualTo(
            "https://gen.pollinations.ai/image/A+wrestling+champion?nologo=true&model=flux&width=1024&height=1024");
  }

  @Test
  void generateImage_customSize_includesWidthHeight() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A belt").size("512x512").build();

    String result = service.generateImage(request);

    assertThat(result)
        .isEqualTo(
            "https://gen.pollinations.ai/image/A+belt?nologo=true&model=flux&width=512&height=512");
  }

  @Test
  void generateImage_customModel_includesModelParam() {
    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A champion").model("turbo").build();

    String result = service.generateImage(request);

    assertThat(result).contains("model=turbo");
  }

  // --- API-key path (makes HTTP request) ---

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_withApiKey_success_returnsDataUri() throws IOException, InterruptedException {
    byte[] fakeImage = new byte[] {1, 2, 3, 4};
    HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(fakeImage);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.firstValue("Content-Type")).thenReturn(Optional.of("image/png"));
    when(httpResponse.headers()).thenReturn(headers);
    when(aiSettingsService.getPollinationsApiKey()).thenReturn("poll-api-key");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    String result = service.generateImage(request);

    assertThat(result).startsWith("data:image/png;base64,");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_withApiKey_httpError_throwsAIServiceException()
      throws IOException, InterruptedException {
    HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(403);
    when(aiSettingsService.getPollinationsApiKey()).thenReturn("poll-api-key");
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
  void generateImage_withApiKey_networkError_throwsAIServiceException500()
      throws IOException, InterruptedException {
    when(aiSettingsService.getPollinationsApiKey()).thenReturn("poll-api-key");
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("Connection reset"));

    ImageGenerationService.ImageRequest request =
        ImageGenerationService.ImageRequest.builder().prompt("A wrestler").build();

    assertThatThrownBy(() -> service.generateImage(request))
        .isInstanceOf(AIServiceException.class)
        .satisfies(ex -> assertThat(((AIServiceException) ex).getStatusCode()).isEqualTo(500));
  }

  // --- isAvailable / getProviderName ---

  @Test
  void isAvailable_whenEnabled_returnsTrue() {
    when(aiSettingsService.isPollinationsEnabled()).thenReturn(true);
    assertThat(service.isAvailable()).isTrue();
  }

  @Test
  void isAvailable_whenDisabled_returnsFalse() {
    when(aiSettingsService.isPollinationsEnabled()).thenReturn(false);
    assertThat(service.isAvailable()).isFalse();
  }

  @Test
  void getProviderName_returnspollinations() {
    assertThat(service.getProviderName()).isEqualTo("Pollinations");
  }
}
