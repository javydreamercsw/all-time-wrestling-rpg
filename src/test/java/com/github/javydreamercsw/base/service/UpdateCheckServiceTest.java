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
package com.github.javydreamercsw.base.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.event.UpdateAvailableEvent;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;

class UpdateCheckServiceTest {

  @AfterEach
  void tearDown() {
    System.clearProperty("atw.update-check.releases-api");
  }

  @Test
  void checkForUpdates_skipsWhenBuildPropertiesAbsent() throws Exception {
    UpdateCheckService service =
        new UpdateCheckService(
            mock(ApplicationEventPublisher.class), new ObjectMapper(), Optional.empty());
    // currentVersion == null → early return, no HTTP call or event published
    service.checkForUpdates();
  }

  @Test
  void checkForUpdates_skipsForSnapshotBuild() throws Exception {
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.6.0-SNAPSHOT");
    UpdateCheckService service =
        new UpdateCheckService(
            mock(ApplicationEventPublisher.class),
            new ObjectMapper(),
            Optional.of(buildProperties));
    // SNAPSHOT version → early return
    service.checkForUpdates();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMinor() {
    assertThat(UpdateCheckService.isNewer("2.6.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherPatch() {
    assertThat(UpdateCheckService.isNewer("2.5.3", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsTrueWhenCandidateHasHigherMajor() {
    assertThat(UpdateCheckService.isNewer("3.0.0", "2.5.2")).isTrue();
  }

  @Test
  void isNewer_returnsFalseForSameVersion() {
    assertThat(UpdateCheckService.isNewer("2.5.2", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseWhenCandidateIsOlder() {
    assertThat(UpdateCheckService.isNewer("2.4.0", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_handlesPreReleaseSuffix() {
    // Pre-release suffix stripped; numeric part compared only
    assertThat(UpdateCheckService.isNewer("2.6.0-RC1", "2.5.2")).isTrue();
    assertThat(UpdateCheckService.isNewer("2.5.2-RC1", "2.5.2")).isFalse();
  }

  @Test
  void isNewer_returnsFalseForUnparseableVersions() {
    assertThat(UpdateCheckService.isNewer("not-a-version", "2.5.2")).isFalse();
    assertThat(UpdateCheckService.isNewer("2.6.0", "not-a-version")).isFalse();
  }

  @Test
  void checkForUpdates_publishesEventWhenNewerVersionAvailable() throws Exception {
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.5.0");

    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    UpdateCheckService service =
        new UpdateCheckService(publisher, new ObjectMapper(), Optional.of(buildProperties));

    HttpServer server = startJsonServer(200, releaseJson("v2.6.0"));
    System.setProperty(
        "atw.update-check.releases-api",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
    try {
      service.checkForUpdates();
      ArgumentCaptor<UpdateAvailableEvent> captor =
          ArgumentCaptor.forClass(UpdateAvailableEvent.class);
      verify(publisher).publishEvent(captor.capture());
      assertThat(captor.getValue().getNewVersion()).isEqualTo("2.6.0");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkForUpdates_doesNotPublishWhenVersionIsSameOrOlder() throws Exception {
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.6.0");

    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    UpdateCheckService service =
        new UpdateCheckService(publisher, new ObjectMapper(), Optional.of(buildProperties));

    HttpServer server = startJsonServer(200, releaseJson("v2.6.0"));
    System.setProperty(
        "atw.update-check.releases-api",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
    try {
      service.checkForUpdates();
      verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkForUpdates_silentlyIgnoresNon200Response() throws Exception {
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.5.0");

    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    UpdateCheckService service =
        new UpdateCheckService(publisher, new ObjectMapper(), Optional.of(buildProperties));

    HttpServer server = startJsonServer(503, "Service Unavailable");
    System.setProperty(
        "atw.update-check.releases-api",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
    try {
      service.checkForUpdates();
      verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkForUpdates_silentlyIgnoresBlankTagName() throws Exception {
    BuildProperties buildProperties = mock(BuildProperties.class);
    when(buildProperties.getVersion()).thenReturn("2.5.0");

    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    UpdateCheckService service =
        new UpdateCheckService(publisher, new ObjectMapper(), Optional.of(buildProperties));

    HttpServer server = startJsonServer(200, "{\"tag_name\": \"\", \"html_url\": \"\"}");
    System.setProperty(
        "atw.update-check.releases-api",
        "http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
    try {
      service.checkForUpdates();
      verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    } finally {
      server.stop(0);
    }
  }

  private static String releaseJson(final String tag) {
    return """
    {"tag_name": "%s", "html_url": "https://example.com/releases/%s"}
    """
        .formatted(tag, tag);
  }

  private static HttpServer startJsonServer(final int status, final String body)
      throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    byte[] bytes = body.getBytes();
    server.createContext(
        "/releases",
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    server.start();
    return server;
  }
}
