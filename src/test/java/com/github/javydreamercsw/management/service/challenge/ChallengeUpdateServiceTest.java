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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Achievement;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.service.challenge.ChallengeUpdateService.UpdateResult;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChallengeUpdateServiceTest {

  @TempDir Path contentDir;
  @Mock ChallengeService challengeService;
  @Mock AchievementRepository achievementRepository;
  @Mock CacheManager cacheManager;
  @Mock Cache scriptedAchievementsCache;

  private ChallengeUpdateService updateService;

  private static final String MINIMAL_CHALLENGE_JSON =
      "[{\"id\":\"test_01\",\"active\":true,\"expansionCode\":\"BASE_GAME\","
          + "\"requiredExpansions\":[],\"requiredWrestlerNames\":[],"
          + "\"conditions\":[],\"modifiers\":[]}]";

  @BeforeEach
  void setUp() {
    lenient().when(challengeService.getContentDir()).thenReturn(contentDir);
    lenient()
        .when(cacheManager.getCache(CacheConfig.SCRIPTED_ACHIEVEMENTS_CACHE))
        .thenReturn(scriptedAchievementsCache);
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    updateService =
        new ChallengeUpdateService(challengeService, achievementRepository, mapper, cacheManager);
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
  void checkAndApply_downloadsChallengeAndAchievementContent() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    String challengeJson =
        "[{\"id\":\"week_05_new\",\"title\":\"New Challenge\","
            + "\"imageUrl\":\"challenge-content/images/week5.png\",\"active\":true,"
            + "\"expansionCode\":\"EDDIE\",\"requiredExpansions\":[],"
            + "\"requiredWrestlerNames\":[],\"conditions\":[],\"modifiers\":[]}]";
    String achievementsJson =
        "[{\"key\":\"CHALLENGE_WEEK_05\",\"name\":\"New Achievement\","
            + "\"description\":\"Desc\",\"xpValue\":100,"
            + "\"category\":\"CHALLENGE\"}]";
    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-21\",\"packages\":["
            + "{\"id\":\"season_1_weekly\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/weekly.json\",\"achievementsUrl\":\"http://localhost:"
            + port
            + "/achievements.json\",\"images\":[{\"name\":\"week5.png\","
            + "\"url\":\"http://localhost:"
            + port
            + "/week5.png\"}]}]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/weekly.json", 200, challengeJson);
    serve(server, "/achievements.json", 200, achievementsJson);
    serve(server, "/week5.png", 200, "PNG");
    server.start();

    when(achievementRepository.findByKey("CHALLENGE_WEEK_05")).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    try {
      setManifestUrl(server, "/manifest.json");
      UpdateResult result = updateService.checkAndApply();

      assertEquals(1, result.downloaded());
      assertEquals(challengeJson, Files.readString(contentDir.resolve("season_1_weekly.json")));
      assertEquals("PNG", Files.readString(contentDir.resolve("images/week5.png")));
      verify(challengeService).reload();
      verify(achievementRepository).saveAll(any(List.class));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_withAchievementsUrl_insertsNewAchievement() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    String achievementsJson =
        "[{\"key\":\"TEST_ACH\",\"name\":\"Test\",\"description\":\"Desc\","
            + "\"xpValue\":50,\"category\":\"CHALLENGE\",\"unlockCondition\":\"true\"}]";
    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"pkg1\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/pkg1.json\","
            + "\"achievementsUrl\":\"http://localhost:"
            + port
            + "/achievements.json\","
            + "\"images\":[]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/pkg1.json", 200, MINIMAL_CHALLENGE_JSON);
    serve(server, "/achievements.json", 200, achievementsJson);
    server.start();

    when(achievementRepository.findByKey("TEST_ACH")).thenReturn(Optional.empty());
    when(achievementRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    try {
      setManifestUrl(server, "/manifest.json");
      updateService.checkAndApply();
      @SuppressWarnings("unchecked")
      var captor = ArgumentCaptor.forClass(List.class);
      verify(achievementRepository).saveAll(captor.capture());
      Achievement saved = (Achievement) captor.getValue().get(0);
      assertEquals("true", saved.getUnlockCondition());
      verify(scriptedAchievementsCache).clear();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_withAchievementsUrl_updatesExistingAchievement() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    String achievementsJson =
        "[{\"key\":\"EXISTING_ACH\",\"name\":\"Updated Name\",\"description\":\"New"
            + " Desc\",\"xpValue\":75,\"category\":\"CHALLENGE\",\"unlockCondition\":\"wrestlers.size()"
            + " >= 5\"}]";
    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"pkg1\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/pkg1.json\","
            + "\"achievementsUrl\":\"http://localhost:"
            + port
            + "/achievements.json\","
            + "\"images\":[]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/pkg1.json", 200, MINIMAL_CHALLENGE_JSON);
    serve(server, "/achievements.json", 200, achievementsJson);
    server.start();

    Achievement existing = new Achievement();
    existing.setKey("EXISTING_ACH");
    existing.setName("Old Name");
    existing.setDescription("Old Desc");
    existing.setXpValue(50);
    existing.setUnlockCondition(null);
    when(achievementRepository.findByKey("EXISTING_ACH")).thenReturn(Optional.of(existing));
    when(achievementRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    try {
      setManifestUrl(server, "/manifest.json");
      updateService.checkAndApply();
      assertEquals("Updated Name", existing.getName());
      assertEquals("New Desc", existing.getDescription());
      assertEquals(75, existing.getXpValue());
      assertEquals("wrestlers.size() >= 5", existing.getUnlockCondition());
      verify(achievementRepository).saveAll(any(List.class));
      verify(scriptedAchievementsCache).clear();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void checkAndApply_withAchievementsUrl_handlesUnreachableAchievementsGracefully()
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();

    String manifest =
        "{\"schemaVersion\":1,\"lastUpdated\":\"2026-08-01\",\"packages\":["
            + "{\"id\":\"pkg1\",\"jsonUrl\":\"http://localhost:"
            + port
            + "/pkg1.json\","
            + "\"achievementsUrl\":\"http://localhost:"
            + port
            + "/achievements.json\","
            + "\"images\":[]}"
            + "]}";

    serve(server, "/manifest.json", 200, manifest);
    serve(server, "/pkg1.json", 200, MINIMAL_CHALLENGE_JSON);
    serve(server, "/achievements.json", 404, "");
    server.start();

    try {
      setManifestUrl(server, "/manifest.json");
      UpdateResult result = updateService.checkAndApply();
      assertEquals(1, result.downloaded());
      verify(achievementRepository, never()).saveAll(any());
      verify(scriptedAchievementsCache, never()).clear();
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
