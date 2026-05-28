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
package com.github.javydreamercsw.management.extension;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.http.OkHttp4Client;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that archives Notion pages created during tests.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @ExtendWith(NotionTestCleanupExtension.class)
 * class MyNotionTest {
 *   void myTest() {
 *     String pageId = notionClient.createPage(...).getId();
 *     NotionTestCleanupExtension.trackPageId(pageId);
 *     // ... assertions ...
 *   }
 * }
 * }</pre>
 *
 * <p>Cleanup only runs when a real {@code NOTION_TOKEN} is present in the environment, so mocked
 * tests are unaffected.
 */
@Slf4j
public class NotionTestCleanupExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Set<String> TRACKED_PAGE_IDS = ConcurrentHashMap.newKeySet();

  /**
   * Registers a Notion page ID for cleanup after the test class completes. Safe to call even when
   * running with a mocked Notion client — cleanup is skipped if no real token is available.
   */
  public static void trackPageId(final String pageId) {
    if (pageId != null && !pageId.isBlank()) {
      TRACKED_PAGE_IDS.add(pageId);
    }
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    TRACKED_PAGE_IDS.clear();
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    if (TRACKED_PAGE_IDS.isEmpty()) {
      return;
    }

    String token = EnvironmentVariableUtil.getNotionToken();
    if (token == null || token.isBlank()) {
      log.debug(
          "Skipping Notion test cleanup — no NOTION_TOKEN found ({} page IDs tracked but not"
              + " archived)",
          TRACKED_PAGE_IDS.size());
      TRACKED_PAGE_IDS.clear();
      return;
    }

    log.info("Archiving {} test Notion page(s) created during tests", TRACKED_PAGE_IDS.size());
    try (NotionClient client = new NotionClient(token)) {
      client.setHttpClient(new OkHttp4Client(30_000, 30_000, 30_000));
      for (String pageId : TRACKED_PAGE_IDS) {
        try {
          client.updatePage(new UpdatePageRequest(pageId, null, true, null, null));
          log.info("Archived test Notion page: {}", pageId);
        } catch (Exception e) {
          log.warn("Failed to archive test Notion page {}: {}", pageId, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to create Notion client for cleanup: {}", e.getMessage());
    } finally {
      TRACKED_PAGE_IDS.clear();
    }
  }
}
