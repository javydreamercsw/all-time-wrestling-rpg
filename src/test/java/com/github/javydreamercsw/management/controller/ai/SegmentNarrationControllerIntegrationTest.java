package com.github.javydreamercsw.management.controller.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for MatchNarrationController. Tests the complete flow from REST endpoints to AI
 * services using the mock provider.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Segment Narration Controller Integration Tests")
@TestPropertySource(properties = "notion.sync.enabled=true")
class SegmentNarrationControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("GET /api/segment-narration/limits should return provider information")
  void shouldReturnProviderInformation() throws Exception {
    mockMvc
        .perform(get("/api/segment-narration/limits"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.currentProvider").exists())
        .andExpect(jsonPath("$.availableServices").isArray())
        .andExpect(jsonPath("$.availableServices[?(@.providerName == 'Mock AI')]").exists())
        .andExpect(jsonPath("$.currentConfig.maxOutputTokens").exists())
        .andExpect(jsonPath("$.currentConfig.temperature").exists());
  }

  @Test
  @DisplayName("POST /api/segment-narration/sample should generate sample segment narration")
  void shouldGenerateSampleMatchNarration() throws Exception {
    mockMvc
        .perform(post("/api/segment-narration/test/mock"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.narration").isString())
        .andExpect(jsonPath("$.testProvider").value("mock"))
        .andExpect(jsonPath("$.estimatedCost").exists())
        .andExpect(jsonPath("$.context.segmentType").value("Singles Match"))
        .andExpect(jsonPath("$.context.wrestlers").isArray())
        .andExpect(jsonPath("$.context.wrestlers[0]").value("Rob Van Dam"))
        .andExpect(jsonPath("$.context.wrestlers[1]").value("Kurt Angle"))
        .andExpect(jsonPath("$.context.outcome").exists());
  }

  @Test
  @DisplayName("POST /api/segment-narration/test should use mock provider")
  void shouldUseMockProvider() throws Exception {
    mockMvc
        .perform(post("/api/segment-narration/test/mock"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").value("Mock AI"))
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.testProvider").value("mock"))
        .andExpect(jsonPath("$.estimatedCost").value(0.0));
  }

  @Test
  @DisplayName("POST /api/segment-narration/test/mock should test specific mock provider")
  void shouldTestSpecificMockProvider() throws Exception {
    mockMvc
        .perform(post("/api/segment-narration/test/mock"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").value("Mock AI"))
        .andExpect(jsonPath("$.testProvider").value("mock"))
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.estimatedCost").value(0.0));
  }

  @Test
  @DisplayName("POST /api/segment-narration/test/nonexistent should return error")
  void shouldReturnErrorForNonexistentProvider() throws Exception {
    mockMvc
        .perform(post("/api/segment-narration/test/nonexistent"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Provider 'nonexistent' not available or not found"));
  }

  @Test
  @DisplayName("POST /api/segment-narration/narrate should accept custom segment context")
  void shouldAcceptCustomSegmentContext() throws Exception {
    // Given
    SegmentNarrationContext customContext = super.createCustomSegmentContext();
    String requestBody = objectMapper.writeValueAsString(customContext);

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.segmentType").value("Hell in a Cell"))
        .andExpect(jsonPath("$.wrestlers").isArray())
        .andExpect(jsonPath("$.wrestlers[0]").value("The Undertaker"))
        .andExpect(jsonPath("$.wrestlers[1]").value("Mankind"))
        .andExpect(jsonPath("$.outcome").exists())
        .andExpect(jsonPath("$.estimatedCost").exists());
  }

  @Test
  @DisplayName("Should handle malformed JSON gracefully")
  void shouldHandleMalformedJsonGracefully() throws Exception {
    String malformedJson = "{ invalid json }";

    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should validate required fields in segment context")
  void shouldValidateRequiredFieldsInMatchContext() throws Exception {
    // Given - context missing required fields
    SegmentNarrationContext incompleteContext = new SegmentNarrationContext();
    String requestBody = objectMapper.writeValueAsString(incompleteContext);

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  @DisplayName("Should include cost estimates in limits response")
  void shouldIncludeCostEstimatesInLimitsResponse() throws Exception {
    mockMvc
        .perform(get("/api/segment-narration/limits"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.availableServices[?(@.providerName == 'Mock AI')].costPer1KTokens")
                .value(0.0))
        .andExpect(
            jsonPath("$.availableServices[?(@.providerName == 'Mock AI')].tier").value("FREE"))
        .andExpect(
            jsonPath("$.availableServices[?(@.providerName == 'Mock AI')].priority").value(10));
  }

  @Test
  @DisplayName("Should return consistent response structure across endpoints")
  void shouldReturnConsistentResponseStructureAcrossEndpoints() throws Exception {
    // Test sample endpoint
    mockMvc
        .perform(post("/api/segment-narration/test/mock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.estimatedCost").exists());

    // Test specific provider endpoint
    mockMvc
        .perform(post("/api/segment-narration/test/mock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.estimatedCost").exists());
  }
}
