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
package com.github.javydreamercsw.base.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalAIStatusServiceTest {

  @Mock private LocalAIConfigProperties config;
  @Mock private HttpClient httpClient;
  private LocalAIStatusService statusService;

  @BeforeEach
  void setUp() {
    statusService = new LocalAIStatusService(config, httpClient);
  }

  @Test
  void testCheckHealthWhenDisabled() {
    when(config.isEnabled()).thenReturn(false);

    LocalAIStatusService.Status result = statusService.checkHealth();

    assertEquals(LocalAIStatusService.Status.NOT_STARTED, result);
    assertEquals(LocalAIStatusService.Status.NOT_STARTED, statusService.getStatus());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCheckHealthSuccess() throws IOException, InterruptedException {
    when(config.isEnabled()).thenReturn(true);
    when(config.getBaseUrl()).thenReturn("http://localhost:8080");

    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(httpClient.<String>send(any(), any())).thenReturn(response);

    LocalAIStatusService.Status result = statusService.checkHealth();

    assertEquals(LocalAIStatusService.Status.READY, result);
    assertEquals(LocalAIStatusService.Status.READY, statusService.getStatus());
    assertEquals("LocalAI is ready.", statusService.getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCheckHealthFailure() throws IOException, InterruptedException {
    when(config.isEnabled()).thenReturn(true);
    when(config.getBaseUrl()).thenReturn("http://localhost:8080");

    when(httpClient.send(any(), any())).thenThrow(new IOException("Connection refused"));

    LocalAIStatusService.Status result = statusService.checkHealth();

    assertEquals(LocalAIStatusService.Status.FAILED, result);
    assertEquals(LocalAIStatusService.Status.FAILED, statusService.getStatus());
    assertTrue(statusService.getMessage().contains("Connection refused"));
  }
}
