package com.github.javydreamercsw.base.ai.openai;

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
 * OpenAI implementation of the AIService interface. Uses OpenAI's GPT models to analyze wrestling
 * data and generate insights.
 */
@Service
@Slf4j
public class OpenAIService implements AIService {

  private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
  private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  private static final int MAX_TOKENS = 500; // Reduced to save costs
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public OpenAIService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("OPENAI_API_KEY");
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

    return callOpenAI(prompt);
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

    return callOpenAI(prompt);
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

    return callOpenAI(prompt);
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

    return callOpenAI(prompt);
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

    return callOpenAI(prompt);
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

    return callOpenAI(fullPrompt);
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

    return callOpenAI(prompt);
  }

  @Override
  public String getProviderName() {
    return "OpenAI";
  }

  @Override
  public boolean isAvailable() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the OpenAI API with the given prompt. */
  private String callOpenAI(String prompt) {
    if (!isAvailable()) {
      log.warn("OpenAI API key not configured");
      return "OpenAI service is not available. Please configure OPENAI_API_KEY environment"
          + " variable.";
    }

    try {
      // Create request body
      Map<String, Object> requestBody =
          Map.of(
              "model",
              DEFAULT_MODEL,
              "messages",
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      "You are a professional wrestling analyst and creative writer with deep"
                          + " knowledge of wrestling storylines, character development, and match"
                          + " psychology."),
                  Map.of("role", "user", "content", prompt)),
              "max_tokens",
              MAX_TOKENS,
              "temperature",
              0.7);

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      // Create HTTP request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(OPENAI_API_URL))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .timeout(TIMEOUT)
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      // Send request and get response
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
        return "Error calling OpenAI API: " + response.statusCode();
      }

    } catch (Exception e) {
      log.error("Failed to call OpenAI API", e);
      return "Error processing AI request: " + e.getMessage();
    }
  }

  /** Extracts the content from OpenAI API response. */
  private String extractContentFromResponse(String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

      if (choices != null && !choices.isEmpty()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
      }

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse OpenAI response", e);
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
