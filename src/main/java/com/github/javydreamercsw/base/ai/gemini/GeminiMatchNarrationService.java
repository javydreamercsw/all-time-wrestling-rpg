package com.github.javydreamercsw.base.ai.gemini;

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
 * Google Gemini implementation of the MatchNarrationService interface. Uses Google's Gemini API for
 * wrestling match narration with rich context.
 */
@Service
@Slf4j
public class GeminiMatchNarrationService extends AbstractMatchNarrationService {

  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
  private static final Duration TIMEOUT =
      Duration.ofSeconds(60); // Longer timeout for match narration

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public GeminiMatchNarrationService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("GEMINI_API_KEY");
  }

  @Override
  protected String callAIProvider(@NonNull String prompt) {
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
  private String callGemini(@NonNull String prompt) {
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
                  0.8, // Higher creativity for storytelling
                  "topK",
                  40,
                  "topP",
                  0.95,
                  "maxOutputTokens",
                  4000, // Much longer output for detailed match narration (up to ~3000 words)
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
      log.error("Failed to call Gemini API for match narration", e);
      return "Error processing match narration request: " + e.getMessage();
    }
  }

  /** Extracts the content from Gemini API response. */
  private String extractContentFromResponse(@NonNull String responseBody) {
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
}
