package com.github.javydreamercsw.base.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for MatchNarrationController. Tests the complete flow from REST endpoints to AI
 * services using the mock provider.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Match Narration Controller Integration Tests")
class MatchNarrationControllerIntegrationTest {

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
        .perform(post("/api/segment-narration/sample"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.narration").isString())
        .andExpect(jsonPath("$.sampleMatch").value(true))
        .andExpect(jsonPath("$.estimatedCost").exists())
        .andExpect(jsonPath("$.context.matchType").value("Singles Match"))
        .andExpect(jsonPath("$.context.wrestlers").isArray())
        .andExpect(jsonPath("$.context.wrestlers[0]").value("Rob Van Dam"))
        .andExpect(jsonPath("$.context.wrestlers[1]").value("Kurt Angle"))
        .andExpect(jsonPath("$.context.outcome").exists());
  }

  @Test
  @DisplayName("POST /api/segment-narration/test should use mock provider")
  void shouldUseMockProvider() throws Exception {
    mockMvc
        .perform(post("/api/segment-narration/test"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.provider").value("Mock AI"))
        .andExpect(jsonPath("$.narration").exists())
        .andExpect(jsonPath("$.testMatch").value(true))
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
  void shouldAcceptCustomMatchContext() throws Exception {
    // Given
    SegmentNarrationContext customContext = createCustomMatchContext();
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
        .andExpect(jsonPath("$.matchType").value("Hell in a Cell"))
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
        .perform(post("/api/segment-narration/sample"))
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

  /** Creates a custom segment context for testing. */
  private SegmentNarrationContext createCustomMatchContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Match Type
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("King of the Ring 1998");
    matchType.setRules(Arrays.asList("No Disqualification", "Falls Count Anywhere"));
    context.setSegmentType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Civic Arena");
    venue.setLocation("Pittsburgh, Pennsylvania");
    venue.setType("Indoor Arena");
    venue.setCapacity(17000);
    venue.setDescription("Historic venue for legendary matches");
    venue.setAtmosphere("Intense and foreboding");
    venue.setSignificance("Site of the most famous Hell in a Cell segment");
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext undertaker = new WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman - Phenom of WWE");

    WrestlerContext mankind = new WrestlerContext();
    mankind.setName("Mankind");
    mankind.setDescription("Hardcore legend Mick Foley");

    context.setWrestlers(Arrays.asList(undertaker, mankind));

    // Context
    context.setAudience("Shocked and horrified crowd of 17,000");
    context.setDeterminedOutcome(
        "The Undertaker wins after Mankind is thrown off the Hell in a Cell");

    return context;
  }
}
