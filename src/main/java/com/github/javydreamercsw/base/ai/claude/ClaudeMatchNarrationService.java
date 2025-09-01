package com.github.javydreamercsw.base.ai.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AbstractMatchNarrationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude implementation of the MatchNarrationService interface. Uses Claude's API for
 * wrestling match narration with rich context.
 *
 * <p>Enable by setting: CLAUDE_API_KEY environment variable
 */
@Service
@Slf4j
public class ClaudeMatchNarrationService extends AbstractMatchNarrationService {

  private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
  private static final String DEFAULT_MODEL = "claude-3-haiku-20240307"; // Cheapest option
  private static final int MAX_TOKENS = 4_000; // Longer output for detailed match narration
  private static final Duration TIMEOUT =
      Duration.ofSeconds(90); // Longer timeout for match narration

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public ClaudeMatchNarrationService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("CLAUDE_API_KEY");
  }

  @Override
  protected String callAIProvider(@NonNull String prompt) {
    return callClaude(prompt);
  }

  @Override
  public String getProviderName() {
    return "Anthropic Claude";
  }

  @Override
  public boolean isAvailable() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the Claude API with the given prompt. */
  private String callClaude(@NonNull String prompt) {
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
        return "Error calling Claude API: " + response.statusCode() + " - " + response.body();
      }

    } catch (Exception e) {
      log.error("Failed to call Claude API for match narration", e);
      return "Error processing match narration request: " + e.getMessage();
    }
  }

  /** Extracts the content from Claude API response. */
  private String extractContentFromResponse(@NonNull String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

      if (content != null && !content.isEmpty()) {
        return (String) content.get(0).get("text");
      }

      return "No content in Claude AI response";
    } catch (Exception e) {
      log.error("Failed to parse Claude response", e);
      return "Error parsing Claude AI response: " + e.getMessage();
    }
  }
}
