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
package com.github.javydreamercsw.management.controller.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for ApiDocumentationController. */
@DisplayName("ApiDocumentationController Integration Tests")
@Transactional
class ApiDocumentationControllerIT extends AbstractRestControllerIT {

  @Autowired(required = false)
  private BuildProperties buildProperties;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ApiDocumentationController(buildProperties)).build();
  }

  @Test
  @DisplayName("GET /api/system/info should return 200")
  void shouldReturnApiInfo() throws Exception {
    mockMvc.perform(get("/api/system/info")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/system/health should return 200")
  void shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/system/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/system/capabilities should return 200")
  void shouldReturnCapabilities() throws Exception {
    mockMvc.perform(get("/api/system/capabilities")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/system/endpoints should return 200")
  void shouldReturnEndpoints() throws Exception {
    mockMvc.perform(get("/api/system/endpoints")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/system/stats should return 200")
  void shouldReturnApiStats() throws Exception {
    mockMvc.perform(get("/api/system/stats")).andExpect(status().isOk());
  }
}
