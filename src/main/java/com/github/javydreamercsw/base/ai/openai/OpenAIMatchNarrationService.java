package com.github.javydreamercsw.base.ai.openai;

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
 * OpenAI implementation of the MatchNarrationService interface. Uses OpenAI's GPT models for
 * wrestling match narration with rich context.
 *
 * <p>Enable by setting: OPENAI_API_KEY environment variable
 *
 * <p>Supports both GPT-3.5-turbo (default, cost-effective) and GPT-4 (premium quality). Model can
 * be configured via OPENAI_MODEL environment variable.
 */
@Service
@Slf4j
public class OpenAIMatchNarrationService extends AbstractMatchNarrationService {

  private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
  private static final String DEFAULT_MODEL = "gpt-3.5-turbo"; // Cost-effective default
  private static final String PREMIUM_MODEL = "gpt-4"; // Premium quality option
  private static final int MAX_TOKENS = 4000; // Longer output for detailed match narration
  private static final Duration TIMEOUT =
      Duration.ofSeconds(90); // Longer timeout for match narration

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public OpenAIMatchNarrationService() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("OPENAI_API_KEY");

    // Allow model configuration via environment variable
    String configuredModel = System.getenv("OPENAI_MODEL");
    this.model =
        (configuredModel != null && !configuredModel.trim().isEmpty())
            ? configuredModel.trim()
            : DEFAULT_MODEL;
  }

  @Override
  protected String callAIProvider(@NonNull String prompt) {
    return callOpenAI(prompt);
  }

  @Override
  public String getProviderName() {
    return "OpenAI " + (model.contains("gpt-4") ? "GPT-4" : "GPT-3.5");
  }

  @Override
  public boolean isAvailable() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /** Makes a call to the OpenAI API with the given prompt. */
  private String callOpenAI(@NonNull String prompt) {
    if (!isAvailable()) {
      log.warn("OpenAI API key not configured");
      return "OpenAI AI service is not available. Please configure OPENAI_API_KEY environment"
          + " variable.";
    }

    try {
      // Create request body for OpenAI Chat Completions API
      Map<String, Object> requestBody =
          Map.of(
              "model",
              model,
              "messages",
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      "You are a professional wrestling play-by-play commentator and creative"
                          + " writer. You have deep knowledge of wrestling history, storytelling"
                          + " techniques, and match psychology. Create vivid, engaging match"
                          + " narrations that capture the drama and excitement of professional"
                          + " wrestling."),
                  Map.of("role", "user", "content", prompt)),
              "max_tokens",
              MAX_TOKENS,
              "temperature",
              0.8, // Good balance for creative storytelling
              "top_p",
              0.95,
              "frequency_penalty",
              0.1, // Slight penalty to avoid repetition
              "presence_penalty",
              0.1 // Encourage diverse vocabulary
              );

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
        return "Error calling OpenAI API: " + response.statusCode() + " - " + response.body();
      }

    } catch (Exception e) {
      log.error("Failed to call OpenAI API for match narration", e);
      return "Error processing match narration request: " + e.getMessage();
    }
  }

  /** Extracts the content from OpenAI API response. */
  private String extractContentFromResponse(@NonNull String responseBody) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

      if (choices != null && !choices.isEmpty()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        if (message != null) {
          String content = (String) message.get("content");
          if (content != null) {
            return content.trim();
          }
        }
      }

      return "No content in OpenAI response";
    } catch (Exception e) {
      log.error("Failed to parse OpenAI response", e);
      return "Error parsing OpenAI response: " + e.getMessage();
    }
  }

  /**
   * Gets the current model being used.
   *
   * @return The OpenAI model name (e.g., "gpt-3.5-turbo", "gpt-4")
   */
  public String getModel() {
    return model;
  }

  /**
   * Checks if using the premium GPT-4 model.
   *
   * @return true if using GPT-4, false if using GPT-3.5
   */
  public boolean isUsingPremiumModel() {
    return model.contains("gpt-4");
  }

  /**
   * Gets estimated cost per 1K tokens based on the current model.
   *
   * @return Cost per 1K input tokens in USD
   */
  public double getCostPer1KTokens() {
    if (model.contains("gpt-4")) {
      return 10.0; // GPT-4: $10/1K input, $30/1K output
    } else {
      return 0.50; // GPT-3.5: $0.50/1K input, $1.50/1K output
    }
  }
}
