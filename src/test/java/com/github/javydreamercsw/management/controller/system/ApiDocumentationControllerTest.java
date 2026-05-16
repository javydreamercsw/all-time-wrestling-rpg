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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN"})
class ApiDocumentationControllerTest extends AbstractControllerTest {

  @Test
  void getApiInfo_returnsInfo() throws Exception {
    mockMvc
        .perform(get("/api/system/info").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("All Time Wrestling RPG Management API"))
        .andExpect(jsonPath("$.version").isNotEmpty())
        .andExpect(jsonPath("$.capabilities").exists())
        .andExpect(jsonPath("$.endpoints").exists());
  }

  @Test
  void getHealthStatus_returnsUp() throws Exception {
    mockMvc
        .perform(get("/api/system/health").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void getCapabilities_returnsCapabilities() throws Exception {
    mockMvc
        .perform(get("/api/system/capabilities").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiProviders").exists())
        .andExpect(jsonPath("$.dataFormats").exists());
  }

  @Test
  void getEndpoints_returnsEndpointList() throws Exception {
    mockMvc
        .perform(get("/api/system/endpoints").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wrestlerManagement").exists())
        .andExpect(jsonPath("$.showManagement").exists());
  }

  @Test
  void getApiStats_returnsStats() throws Exception {
    mockMvc
        .perform(get("/api/system/stats").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalEndpoints").value(50))
        .andExpect(jsonPath("$.uptime").exists());
  }
}
