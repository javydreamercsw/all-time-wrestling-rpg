/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.controller.sync;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link NotionSyncController}. Uses real Spring beans from the test
 * application context (with mock Notion infrastructure from {@code TestNotionConfiguration}).
 */
@DisplayName("NotionSyncController Integration Tests")
@Transactional
class NotionSyncControllerIT extends AbstractRestControllerIT {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private NotionSyncProperties syncProperties;
  @Autowired private EntityDependencyAnalyzer dependencyAnalyzer;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new NotionSyncController(
                    notionSyncService,
                    null, // NotionSyncScheduler is conditional — not available in tests
                    syncProperties,
                    dependencyAnalyzer))
            .build();
  }

  @Test
  @DisplayName("GET /api/sync/notion/status returns 200 with sync configuration")
  void getSyncStatus_returns200WithConfig() throws Exception {
    mockMvc
        .perform(get("/api/sync/notion/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.enabled").isBoolean())
        .andExpect(jsonPath("$.schedulerEnabled").isBoolean())
        .andExpect(jsonPath("$.entities").isArray())
        .andExpect(jsonPath("$.detailedStatus").value("Scheduler not available"));
  }

  @Test
  @DisplayName("GET /api/sync/notion/entities returns 200 with entity list")
  void getSupportedEntities_returns200WithEntities() throws Exception {
    mockMvc
        .perform(get("/api/sync/notion/entities"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.syncEntities").isArray())
        .andExpect(jsonPath("$.configuredEntities").isArray())
        .andExpect(jsonPath("$.syncOrder").isString())
        .andExpect(jsonPath("$.description").isMap());
  }

  @Test
  @DisplayName("GET /api/sync/notion/entities returns a non-empty list of entity keys")
  void getSupportedEntities_returnsNonEmptyList() throws Exception {
    mockMvc
        .perform(get("/api/sync/notion/entities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.syncEntities.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  @Test
  @DisplayName("GET /api/sync/notion/health returns 200 or 503 with health fields")
  void healthCheck_returnsHealthFields() throws Exception {
    mockMvc
        .perform(get("/api/sync/notion/health"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").isString())
        .andExpect(jsonPath("$.syncEnabled").isBoolean())
        .andExpect(jsonPath("$.schedulerEnabled").isBoolean())
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  @Test
  @DisplayName("POST /api/sync/notion/trigger returns 503 when scheduler is unavailable")
  void triggerManualSync_returns503WhenSchedulerUnavailable() throws Exception {
    // NotionSyncScheduler is null in standaloneSetup (not conditional in test profile)
    mockMvc
        .perform(post("/api/sync/notion/trigger"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Notion sync scheduler is not available"));
  }

  @Test
  @DisplayName("POST /api/sync/notion/trigger/{entity} returns 400 for an invalid entity name")
  void triggerEntitySync_returns400ForInvalidEntity() throws Exception {
    mockMvc
        .perform(post("/api/sync/notion/trigger/nonexistententity"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Invalid entity name: nonexistententity"))
        .andExpect(jsonPath("$.validEntities").isArray());
  }

  @Test
  @DisplayName(
      "POST /api/sync/notion/trigger/{entity} returns 503 when scheduler is unavailable for valid"
          + " entity")
  void triggerEntitySync_returns503ForValidEntityWhenSchedulerUnavailable() throws Exception {
    // Determine a valid entity key from the analyzer so the name check passes
    String validKey = dependencyAnalyzer.getAutomaticSyncOrder().get(0).getKey();

    mockMvc
        .perform(post("/api/sync/notion/trigger/{entity}", validKey))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Notion sync scheduler is not available"));
  }
}
