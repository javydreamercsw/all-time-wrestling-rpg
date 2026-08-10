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
package com.github.javydreamercsw.management.service.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.service.challenge.ChallengeUpdateService.UpdateResult;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChallengeUpdateServiceTest {

  @TempDir Path contentDir;
  @Mock ChallengeService challengeService;

  private ChallengeUpdateService updateService;

  private static final String MINIMAL_CHALLENGE_JSON =
      "[{\"id\":\"test_01\",\"active\":true,\"expansionCode\":\"BASE_GAME\","
          + "\"requiredExpansions\":[],\"requiredWrestlerNames\":[],"
          + "\"conditions\":[],\"modifiers\":[]}]";

  @BeforeEach
  void setUp() {
    lenient().when(challengeService.getContentDir()).thenReturn(contentDir);
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    updateService = new ChallengeUpdateService(challengeService, mapper);
  }

  @Test
  void checkAndApply_whenManifestUnreachable_returnsNetworkError() {
    ReflectionTestUtils.setField(updateService, "manifestUrl", "bogus://unreachable");
    UpdateResult result = updateService.checkAndApply();
    assertEquals("Could not reach the content server. Check your network.", result.message());
    assertEquals(0, result.downloaded());
    assertEquals(0, result.skipped());
    assertNull(updateService.getLastChecked());
  }

  @Test
  void checkAndApply_whenEmptyManifest_alreadyUpToDate() throws Exception {
    String manifest = "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":[]}";
    HttpServer server = startServer("/manifest.json", 200, manifest);
    try {
      setManifestUrl(server, "/manifest.json");
      UpdateResult result = updateService.checkAndApply();
      assertEquals("Already up to date.", result.message());
      assertEquals(0, result.downloaded());
      assertEquals(0, result.skipped());
      assertNotNull(updateService.getLastChecked());
      verify(challengeService, never()).reload();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_whenPackageDownloads_callsReloadAndReturnsCount() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"pkg1\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/pkg1.json\",\"images\":[]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/pkg1.json", 200, MINIMAL_CHALLENGE_JSON);
    server.start();

    try {
      setManifestUrl(server, "/manifest.json");
      UpdateResult result = updateService.checkAndApply();
      assertEquals("1 package(s) updated.", result.message());
      assertEquals(1, result.downloaded());
      assertEquals(0, result.skipped());
      assertNotNull(updateService.getLastChecked());
      verify(challengeService).reload();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_whenPackageJsonReturns404_skipsPackage() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"missing\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/missing.json\",\"images\":[]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/missing.json", 404, "");
    server.start();

    try {
      setManifestUrl(server, "/manifest.json");
      UpdateResult result = updateService.checkAndApply();
      assertEquals(0, result.downloaded());
      assertEquals(1, result.skipped());
      verify(challengeService, never()).reload();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_downloadsNewImagesAndSkipsExisting() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    Path imagesDir = contentDir.resolve("images");
    Files.createDirectories(imagesDir);
    Files.write(imagesDir.resolve("existing.png"), new byte[] {1, 2, 3});

    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"pkg1\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/pkg1.json\","
            + "\"images\":["
            + "{\"name\":\"existing.png\",\"url\":\"http://localhost:"
            + port
            + "/existing.png\"},"
            + "{\"name\":\"new.png\",\"url\":\"http://localhost:"
            + port
            + "/new.png\"}"
            + "]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/pkg1.json", 200, MINIMAL_CHALLENGE_JSON);
    serve(server, "/new.png", 200, "PNG");
    server.start();

    try {
      setManifestUrl(server, "/manifest.json");
      updateService.checkAndApply();
      assertEquals(3, Files.readAllBytes(imagesDir.resolve("existing.png")).length);
      assertEquals("PNG", new String(Files.readAllBytes(imagesDir.resolve("new.png"))));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void scheduledCheck_delegatesToCheckAndApply() {
    ChallengeUpdateService spied = spy(updateService);
    doReturn(new UpdateResult(0, 0, "ok")).when(spied).checkAndApply();
    spied.scheduledCheck();
    verify(spied).checkAndApply();
  }

  private HttpServer startServer(String path, int status, String body) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    serve(server, path, status, body);
    server.start();
    return server;
  }

  private void serve(HttpServer server, String path, int status, String body) throws Exception {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    server.createContext(
        path,
        ex -> {
          ex.sendResponseHeaders(status, bytes.length);
          if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
          }
          ex.close();
        });
  }

  private void setManifestUrl(HttpServer server, String path) {
    int port = server.getAddress().getPort();
    ReflectionTestUtils.setField(updateService, "manifestUrl", "http://localhost:" + port + path);
  }
}
