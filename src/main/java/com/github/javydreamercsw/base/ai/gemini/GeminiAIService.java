package com.github.javydreamercsw.base.ai.gemini;

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
 * Google Gemini implementation of the AIService interface. Uses Google's Gemini API for wrestling
 * analysis and content generation.
 *
 * <p>Enable with: ai.provider=gemini Requires: GEMINI_API_KEY environment variable Get API key
 * from: https://makersuite.google.com/app/apikey
 */
@Service
@Slf4j
public class GeminiAIService implements AIService {

  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public GeminiAIService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("GEMINI_API_KEY");
  }

  @Override
  public String analyzeWrestler(Map<String, Object> wrestlerData) {
    String prompt =
        String.format(
            "As a professional wrestling analyst, analyze this wrestler's data and provide insights"
                + " about their performance, strengths, and areas for improvement:\n\n"
                + "%s\n\n"
                + "Focus on wrestling-specific metrics like heat, fan engagement, match"
                + " performance, and storyline potential.",
            formatDataForPrompt(wrestlerData));

    return callGemini(prompt);
  }

  @Override
  public String predictMatch(Map<String, Object> wrestler1Data, Map<String, Object> wrestler2Data) {
    String prompt =
        String.format(
            "As a wrestling expert, predict the outcome of a match between these wrestlers."
                + " Consider their stats, heat levels, fan support, and storyline context:\n\n"
                + "Wrestler 1:\n"
                + "%s\n\n"
                + "Wrestler 2:\n"
                + "%s\n\n"
                + "Provide a prediction with reasoning, potential match quality, and storyline"
                + " implications.",
            formatDataForPrompt(wrestler1Data), formatDataForPrompt(wrestler2Data));

    return callGemini(prompt);
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
            "As a wrestling show analyst, analyze this wrestling show and provide insights about"
                + " card quality, storyline progression, and entertainment value:\n\n"
                + "Show Details:\n"
                + "%s\n\n"
                + "Matches:\n"
                + "%s\n\n"
                + "Provide analysis on pacing, storyline coherence, match quality, and improvement"
                + " suggestions.",
            formatDataForPrompt(showData), matchesInfo.toString());

    return callGemini(prompt);
  }

  @Override
  public String generateStorylineSuggestions(Map<String, Object> heatData) {
    String prompt =
        String.format(
            "As a wrestling creative writer, generate storyline suggestions and booking ideas based"
                + " on this heat/storyline data:\n\n"
                + "%s\n\n"
                + "Consider the wrestlers involved, their characters, current heat level, and"
                + " storytelling potential. Suggest match types, promos, and story developments.",
            formatDataForPrompt(heatData));

    return callGemini(prompt);
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
            "As a wrestling analyst, analyze this faction and provide insights about group"
                + " dynamics, storyline potential, and booking suggestions:\n\n"
                + "Faction Details:\n"
                + "%s\n\n"
                + "Members:\n"
                + "%s\n\n"
                + "Focus on chemistry, roles, potential conflicts, and storyline opportunities.",
            formatDataForPrompt(factionData), membersInfo.toString());

    return callGemini(prompt);
  }

  @Override
  public String generateCreativeContent(String prompt, Map<String, Object> context) {
    String fullPrompt =
        String.format(
            "As a wrestling creative writer, generate content based on this prompt: %s\n\n"
                + "Context:\n"
                + "%s\n\n"
                + "Make it engaging, authentic to wrestling storytelling, and appropriate for the"
                + " context.",
            prompt, formatDataForPrompt(context));

    return callGemini(fullPrompt);
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
        String.format(
            "As a data analyst, %s for this wrestling data:\n\n%s",
            instruction, formatDataForPrompt(data));

    return callGemini(prompt);
  }

  @Override
  public String getProviderName() {
    return "Google Gemini";
  }

  @Override
  public boolean isAvailable() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the Gemini API with the given prompt. */
  private String callGemini(String prompt) {
    if (!isAvailable()) {
      log.warn("Gemini API key not configured");
      return "Gemini AI service is not available. Please configure GEMINI_API_KEY environment"
          + " variable.";
    }

    try {
      String url = GEMINI_API_URL + "?key=" + apiKey;

      // Create request body for Gemini API
      Map<String, Object> requestBody =
          Map.of(
              "contents",
              List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
              "generationConfig",
              Map.of(
                  "temperature",
                  0.7,
                  "topK",
                  1,
                  "topP",
                  1,
                  "maxOutputTokens",
                  1000,
                  "stopSequences",
                  List.of()),
              "safetySettings",
              List.of(
                  Map.of(
                      "category",
                      "HARM_CATEGORY_HARASSMENT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_HATE_SPEECH",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE"),
                  Map.of(
                      "category",
                      "HARM_CATEGORY_DANGEROUS_CONTENT",
                      "threshold",
                      "BLOCK_MEDIUM_AND_ABOVE")));

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      // Create HTTP request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .timeout(TIMEOUT)
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      // Send request and get response
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
        return "Error calling Gemini API: " + response.statusCode();
      }

    } catch (Exception e) {
      log.error("Failed to call Gemini API", e);
      return "Error processing AI request: " + e.getMessage();
    }
  }

  /** Extracts the content from Gemini API response. */
  private String extractContentFromResponse(String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

      if (candidates != null && !candidates.isEmpty()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        if (parts != null && !parts.isEmpty()) {
          return (String) parts.get(0).get("text");
        }
      }

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse Gemini response", e);
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
