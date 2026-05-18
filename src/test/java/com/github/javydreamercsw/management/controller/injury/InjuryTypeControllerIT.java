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
package com.github.javydreamercsw.management.controller.injury;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for InjuryTypeController. Tests the complete REST API for injury type
 * management.
 */
@DisplayName("InjuryTypeController Integration Tests")
@Transactional
class InjuryTypeControllerIT extends AbstractRestControllerIT {

  @Autowired private InjuryTypeService injuryTypeService;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new InjuryTypeController(injuryTypeService)).build();
  }

  private InjuryType createTestInjuryType(final String name) {
    return injuryTypeService.createInjuryType(name, -5, -3, 0, "Test special effect");
  }

  @Test
  @DisplayName("Should return page of injury types")
  void shouldReturnPageOfInjuryTypes() throws Exception {
    createTestInjuryType("Sprained Ankle");

    mockMvc
        .perform(get("/api/injury-types").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("Should return injury type by ID when found")
  void shouldReturnInjuryTypeByIdWhenFound() throws Exception {
    InjuryType injuryType = createTestInjuryType("Torn Muscle");

    mockMvc
        .perform(get("/api/injury-types/{id}", injuryType.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.injuryName").value("Torn Muscle"));
  }

  @Test
  @DisplayName("Should return 404 when injury type ID not found")
  void shouldReturn404WhenInjuryTypeIdNotFound() throws Exception {
    mockMvc.perform(get("/api/injury-types/{id}", 999_999L)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return injury type by name when found")
  void shouldReturnInjuryTypeByNameWhenFound() throws Exception {
    createTestInjuryType("Broken Rib");

    mockMvc
        .perform(get("/api/injury-types/by-name/{name}", "Broken Rib"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.injuryName").value("Broken Rib"));
  }

  @Test
  @DisplayName("Should return 404 when injury type name not found")
  void shouldReturn404WhenInjuryTypeNameNotFound() throws Exception {
    mockMvc
        .perform(get("/api/injury-types/by-name/{name}", "NonExistentInjury"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return injury types with special effects")
  void shouldReturnInjuryTypesWithSpecialEffects() throws Exception {
    createTestInjuryType("Concussion");

    mockMvc
        .perform(get("/api/injury-types/with-special-effects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return injury type statistics")
  void shouldReturnInjuryTypeStatistics() throws Exception {
    createTestInjuryType("Knee Injury");

    mockMvc.perform(get("/api/injury-types/stats")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should create injury type successfully")
  void shouldCreateInjuryTypeSuccessfully() throws Exception {
    InjuryTypeController.CreateInjuryTypeRequest request =
        new InjuryTypeController.CreateInjuryTypeRequest(
            "Hamstring Pull", -4, -2, 0, "Reduces speed temporarily");

    mockMvc
        .perform(
            post("/api/injury-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.injuryName").value("Hamstring Pull"));
  }

  @Test
  @DisplayName("Should return 400 when creating duplicate injury type")
  void shouldReturn400WhenCreatingDuplicateInjuryType() throws Exception {
    createTestInjuryType("Duplicate Injury");

    InjuryTypeController.CreateInjuryTypeRequest request =
        new InjuryTypeController.CreateInjuryTypeRequest("Duplicate Injury", -3, -1, 0, null);

    mockMvc
        .perform(
            post("/api/injury-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update injury type successfully when found")
  void shouldUpdateInjuryTypeSuccessfullyWhenFound() throws Exception {
    InjuryType injuryType = createTestInjuryType("Old Injury Name");

    InjuryTypeController.UpdateInjuryTypeRequest request =
        new InjuryTypeController.UpdateInjuryTypeRequest(
            "Updated Injury Name", -6, -4, -1, "Updated special effects");

    mockMvc
        .perform(
            put("/api/injury-types/{id}", injuryType.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.injuryName").value("Updated Injury Name"));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent injury type")
  void shouldReturn404WhenUpdatingNonExistentInjuryType() throws Exception {
    InjuryTypeController.UpdateInjuryTypeRequest request =
        new InjuryTypeController.UpdateInjuryTypeRequest("Ghost Injury", -1, -1, 0, null);

    mockMvc
        .perform(
            put("/api/injury-types/{id}", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should delete injury type successfully when found")
  void shouldDeleteInjuryTypeSuccessfullyWhenFound() throws Exception {
    InjuryType injuryType = createTestInjuryType("Temporary Injury");

    mockMvc
        .perform(delete("/api/injury-types/{id}", injuryType.getId()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent injury type")
  void shouldReturn404WhenDeletingNonExistentInjuryType() throws Exception {
    mockMvc.perform(delete("/api/injury-types/{id}", 999_999L)).andExpect(status().isNotFound());
  }
}
