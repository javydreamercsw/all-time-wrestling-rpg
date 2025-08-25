package com.github.javydreamercsw.management.controller.injury;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.controller.injury.InjuryTypeController.CreateInjuryTypeRequest;
import com.github.javydreamercsw.management.controller.injury.InjuryTypeController.UpdateInjuryTypeRequest;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for InjuryTypeController. */
@WebMvcTest(InjuryTypeController.class)
@DisplayName("InjuryType Controller Tests")
class InjuryTypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private InjuryTypeService injuryTypeService;

  @Test
  @DisplayName("Should get all injury types with pagination")
  void shouldGetAllInjuryTypesWithPagination() throws Exception {
    // Given
    InjuryType injuryType = createSampleInjuryType();
    Page<InjuryType> page = new PageImpl<>(List.of(injuryType), PageRequest.of(0, 20), 1);
    when(injuryTypeService.getAllInjuryTypes(any())).thenReturn(page);

    // When & Then
    mockMvc
        .perform(get("/api/injury-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].injuryName").value("Head injury"))
        .andExpect(jsonPath("$.content[0].healthEffect").value(-3))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("Should get injury type by ID")
  void shouldGetInjuryTypeById() throws Exception {
    // Given
    InjuryType injuryType = createSampleInjuryType();
    when(injuryTypeService.getInjuryTypeById(1L)).thenReturn(Optional.of(injuryType));

    // When & Then
    mockMvc
        .perform(get("/api/injury-types/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.injuryName").value("Head injury"))
        .andExpect(jsonPath("$.healthEffect").value(-3));
  }

  @Test
  @DisplayName("Should return 404 when injury type not found")
  void shouldReturn404WhenInjuryTypeNotFound() throws Exception {
    // Given
    when(injuryTypeService.getInjuryTypeById(999L)).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/injury-types/999")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should create injury type successfully")
  void shouldCreateInjuryTypeSuccessfully() throws Exception {
    // Given
    CreateInjuryTypeRequest request =
        new CreateInjuryTypeRequest("New Injury", -2, 0, -1, "Special effect");
    InjuryType createdInjuryType = createSampleInjuryType();
    createdInjuryType.setInjuryName("New Injury");

    when(injuryTypeService.createInjuryType(anyString(), any(), any(), any(), anyString()))
        .thenReturn(createdInjuryType);

    // When & Then
    mockMvc
        .perform(
            post("/api/injury-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.injuryName").value("New Injury"));
  }

  @Test
  @DisplayName("Should handle validation errors when creating injury type")
  void shouldHandleValidationErrorsWhenCreatingInjuryType() throws Exception {
    // Given - Invalid request with empty name
    CreateInjuryTypeRequest request = new CreateInjuryTypeRequest("", -2, 0, -1, "Special effect");

    // When & Then
    mockMvc
        .perform(
            post("/api/injury-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update injury type successfully")
  void shouldUpdateInjuryTypeSuccessfully() throws Exception {
    // Given
    UpdateInjuryTypeRequest request =
        new UpdateInjuryTypeRequest("Updated Injury", -3, -1, -2, "Updated special effect");
    InjuryType updatedInjuryType = createSampleInjuryType();
    updatedInjuryType.setInjuryName("Updated Injury");

    when(injuryTypeService.updateInjuryType(eq(1L), anyString(), any(), any(), any(), anyString()))
        .thenReturn(Optional.of(updatedInjuryType));

    // When & Then
    mockMvc
        .perform(
            put("/api/injury-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.injuryName").value("Updated Injury"));
  }

  @Test
  @DisplayName("Should return 404 when updating non-existent injury type")
  void shouldReturn404WhenUpdatingNonExistentInjuryType() throws Exception {
    // Given
    UpdateInjuryTypeRequest request =
        new UpdateInjuryTypeRequest("Updated Injury", -3, -1, -2, "Updated special effect");

    when(injuryTypeService.updateInjuryType(
            eq(999L), anyString(), any(), any(), any(), anyString()))
        .thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(
            put("/api/injury-types/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should delete injury type successfully")
  void shouldDeleteInjuryTypeSuccessfully() throws Exception {
    // Given
    when(injuryTypeService.deleteInjuryType(1L)).thenReturn(true);

    // When & Then
    mockMvc.perform(delete("/api/injury-types/1")).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Should return 404 when deleting non-existent injury type")
  void shouldReturn404WhenDeletingNonExistentInjuryType() throws Exception {
    // Given
    when(injuryTypeService.deleteInjuryType(999L)).thenReturn(false);

    // When & Then
    mockMvc.perform(delete("/api/injury-types/999")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should get injury types with special effects")
  void shouldGetInjuryTypesWithSpecialEffects() throws Exception {
    // Given
    InjuryType injuryType = createSampleInjuryType();
    injuryType.setSpecialEffects("Maximum handsize reduced by 1");
    when(injuryTypeService.findWithSpecialEffects()).thenReturn(List.of(injuryType));

    // When & Then
    mockMvc
        .perform(get("/api/injury-types/with-special-effects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].specialEffects").value("Maximum handsize reduced by 1"));
  }

  @Test
  @DisplayName("Should get injury type statistics")
  void shouldGetInjuryTypeStatistics() throws Exception {
    // Given
    InjuryTypeService.InjuryTypeStats stats = new InjuryTypeService.InjuryTypeStats(5, 3, 4, 2);
    when(injuryTypeService.getInjuryTypeStats()).thenReturn(stats);

    // When & Then
    mockMvc
        .perform(get("/api/injury-types/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.healthEffectCount").value(5))
        .andExpect(jsonPath("$.staminaEffectCount").value(3))
        .andExpect(jsonPath("$.cardEffectCount").value(4))
        .andExpect(jsonPath("$.specialEffectCount").value(2));
  }

  /** Helper method to create a sample injury type for testing. */
  private InjuryType createSampleInjuryType() {
    InjuryType injuryType = new InjuryType();
    injuryType.setId(1L);
    injuryType.setInjuryName("Head injury");
    injuryType.setHealthEffect(-3);
    injuryType.setStaminaEffect(0);
    injuryType.setCardEffect(-2);
    injuryType.setSpecialEffects("Reduced concentration");
    injuryType.setExternalId("test-external-id");
    return injuryType;
  }
}
