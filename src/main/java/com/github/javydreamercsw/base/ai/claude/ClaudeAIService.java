package com.github.javydreamercsw.base.ai.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude implementation of the AIService interface. Uses Claude's API for wrestling
 * analysis and content generation.
 *
 * <p>Enable with: ai.provider=claude Requires: CLAUDE_API_KEY environment variable
 */
@Service
@Slf4j
public class ClaudeAIService implements AIService {

  private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
  private static final String DEFAULT_MODEL = "claude-3-haiku-20240307"; // Cheapest option
  private static final int MAX_TOKENS = 1000;
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public ClaudeAIService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("CLAUDE_API_KEY");
  }

  @Override
  public String analyzeWrestler(Map<String, Object> wrestlerData) {
    String prompt =
        String.format(
            "Analyze this wrestler's data and provide insights about their performance, strengths,"
                + " and areas for improvement:\n\n"
                + "%s\n\n"
                + "Focus on wrestling-specific metrics like heat, fan engagement, match"
                + " performance, and storyline potential.",
            formatDataForPrompt(wrestlerData));

    return callClaude(prompt);
  }

  @Override
  public String predictMatch(Map<String, Object> wrestler1Data, Map<String, Object> wrestler2Data) {
    String prompt =
        String.format(
            "Predict the outcome of a wrestling match between these two wrestlers. Consider their"
                + " stats, heat levels, fan support, and storyline context:\n\n"
                + "Wrestler 1:\n"
                + "%s\n\n"
                + "Wrestler 2:\n"
                + "%s\n\n"
                + "Provide a prediction with reasoning, potential match quality, and storyline"
                + " implications.",
            formatDataForPrompt(wrestler1Data), formatDataForPrompt(wrestler2Data));

    return callClaude(prompt);
  }

  @Override
  public String analyzeShow(Map<String, Object> showData, List<Map<String, Object>> matchesData) {
    StringBuilder matchesInfo = new StringBuilder();
    for (int i = 0; i < matchesData.size(); i++) {
      matchesInfo.append(
          String.format("Match %d:\n%s\n\n", i + 1, formatDataForPrompt(matchesData.get(i))));
    }

    String prompt =
        String.format(
            "Analyze this wrestling show and provide insights about the card quality, storyline"
                + " progression, and overall entertainment value:\n\n"
                + "Show Details:\n"
                + "%s\n\n"
                + "Matches:\n"
                + "%s\n\n"
                + "Provide analysis on pacing, storyline coherence, match quality, and suggestions"
                + " for improvement.",
            formatDataForPrompt(showData), matchesInfo.toString());

    return callClaude(prompt);
  }

  @Override
  public String generateStorylineSuggestions(Map<String, Object> heatData) {
    String prompt =
        String.format(
            "Based on this wrestling heat/storyline data, generate creative storyline suggestions"
                + " and booking ideas:\n\n"
                + "%s\n\n"
                + "Consider the wrestlers involved, their characters, current heat level, and"
                + " potential for compelling storytelling. Suggest match types, promos, and story"
                + " developments.",
            formatDataForPrompt(heatData));

    return callClaude(prompt);
  }

  @Override
  public String analyzeFaction(
      Map<String, Object> factionData, List<Map<String, Object>> membersData) {
    StringBuilder membersInfo = new StringBuilder();
    for (int i = 0; i < membersData.size(); i++) {
      membersInfo.append(
          String.format("Member %d:\n%s\n\n", i + 1, formatDataForPrompt(membersData.get(i))));
    }

    String prompt =
        String.format(
            "Analyze this wrestling faction and provide insights about group dynamics, storyline"
                + " potential, and booking suggestions:\n\n"
                + "Faction Details:\n"
                + "%s\n\n"
                + "Members:\n"
                + "%s\n\n"
                + "Focus on chemistry, roles within the group, potential conflicts, and storyline"
                + " opportunities.",
            formatDataForPrompt(factionData), membersInfo.toString());

    return callClaude(prompt);
  }

  @Override
  public String generateCreativeContent(String prompt, Map<String, Object> context) {
    String fullPrompt =
        String.format(
            "Generate creative wrestling content based on this prompt: %s\n\n"
                + "Context:\n"
                + "%s\n\n"
                + "Make it engaging, authentic to wrestling storytelling, and appropriate for the"
                + " context provided.",
            prompt, formatDataForPrompt(context));

    return callClaude(fullPrompt);
  }

  @Override
  public String summarizeData(Map<String, Object> data, SummaryType summaryType) {
    String instruction =
        switch (summaryType) {
          case BRIEF -> "Provide a brief, bullet-point summary of the key information";
          case DETAILED -> "Provide a comprehensive overview with all important details";
          case ANALYTICAL ->
              "Provide a deep analytical summary with insights, patterns, and recommendations";
        };

    String prompt =
        String.format("%s for this wrestling data:\n\n%s", instruction, formatDataForPrompt(data));

    return callClaude(prompt);
  }

  @Override
  public String getProviderName() {
    return "Claude AI";
  }

  @Override
  public boolean isAvailable() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the Claude API with the given prompt. */
  private String callClaude(String prompt) {
    if (!isAvailable()) {
      log.warn("Claude API key not configured");
      return "Claude AI service is not available. Please configure CLAUDE_API_KEY environment"
          + " variable.";
    }

    try {
      // Create request body for Claude API
      Map<String, Object> requestBody =
          Map.of(
              "model",
              DEFAULT_MODEL,
              "max_tokens",
              MAX_TOKENS,
              "messages",
              List.of(
                  Map.of(
                      "role",
                      "user",
                      "content",
                      "You are a professional wrestling analyst and creative writer with deep"
                          + " knowledge of wrestling storylines, character development, and match"
                          + " psychology. "
                          + prompt)));

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      // Create HTTP request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(CLAUDE_API_URL))
              .header("Content-Type", "application/json")
              .header("x-api-key", apiKey)
              .header("anthropic-version", "2023-06-01")
              .timeout(TIMEOUT)
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      // Send request and get response
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        log.error("Claude API error: {} - {}", response.statusCode(), response.body());
        return "Error calling Claude API: " + response.statusCode();
      }

    } catch (Exception e) {
      log.error("Failed to call Claude API", e);
      return "Error processing AI request: " + e.getMessage();
    }
  }

  /** Extracts the content from Claude API response. */
  private String extractContentFromResponse(String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

      if (content != null && !content.isEmpty()) {
        return (String) content.get(0).get("text");
      }

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse Claude response", e);
      return "Error parsing AI response: " + e.getMessage();
    }
  }

  /** Formats data map into a readable string for AI prompts. */
  private String formatDataForPrompt(Map<String, Object> data) {
    if (data == null || data.isEmpty()) {
      return "No data provided";
    }

    StringBuilder formatted = new StringBuilder();
    data.forEach(
        (key, value) -> {
          formatted.append(
              String.format("- %s: %s\n", key, value != null ? value.toString() : "null"));
        });

    return formatted.toString();
  }
}
