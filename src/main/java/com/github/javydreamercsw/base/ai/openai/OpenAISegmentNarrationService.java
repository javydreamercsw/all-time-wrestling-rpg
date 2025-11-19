package com.github.javydreamercsw.base.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * OpenAI implementation of the SegmentNarrationService interface. Uses OpenAI's GPT models for
 * wrestling segment narration with rich context.
 *
 * <p>Enable by setting: OPENAI_API_KEY environment variable
 *
 * <p>Supports both GPT-3.5-turbo (default, cost-effective) and GPT-4 (premium quality). Model can
 * be configured via OPENAI_MODEL environment variable.
 */
@Service
@Slf4j
public class OpenAISegmentNarrationService extends AbstractSegmentNarrationService {

  private static final Duration TIMEOUT =
      Duration.ofSeconds(90); // Longer timeout for segment narration

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  @Getter private final String model;
  private final OpenAIConfigProperties openAIConfigProperties;
  private final Environment environment;

  @Autowired
  public OpenAISegmentNarrationService(
      OpenAIConfigProperties openAIConfigProperties, Environment environment) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("OPENAI_API_KEY");
    this.environment = environment;

    // Allow model configuration via environment variable
    String configuredModel = System.getenv("OPENAI_MODEL");
    this.model =
        (configuredModel != null && !configuredModel.trim().isEmpty())
            ? configuredModel.trim()
            : openAIConfigProperties.getDefaultModel(); // Use configured default model
    this.openAIConfigProperties = openAIConfigProperties;
  }

  @Override
  protected String callAIProvider(@NonNull String prompt) {
    return callOpenAI(prompt);
  }

  @Override
  public String getProviderName() {
    return "OpenAI "
        + (model.contains(openAIConfigProperties.getPremiumModel())
            ? "GPT-4"
            : "GPT-3.5"); // Use configured premium model
  }

  @Override
  public boolean isAvailable() {
    if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
      return false;
    }
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  @Override
  public String generateText(@NonNull String prompt) {
    return callOpenAI(prompt);
  }

  /** Makes a call to the OpenAI API with the given prompt. */
  private String callOpenAI(@NonNull String prompt) {
    if (!isAvailable()) {
      throw new com.github.javydreamercsw.base.ai.AIServiceException(
          400,
          "Bad Request",
          getProviderName(),
          "OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.");
    }

    try {
      // Create request body for OpenAI Chat Completions API
      Map<String, Object> requestBody =
          Map.of(
              "model",
              model,
              "messages",
              List.of(
                  Map.of("role", "system", "content", getSystemMessage(prompt)),
                  Map.of("role", "user", "content", prompt)),
              "max_tokens",
              openAIConfigProperties.getMaxTokens(), // Use configured max tokens
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
              .uri(URI.create(openAIConfigProperties.getApiUrl())) // Use configured API URL
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
        throw new com.github.javydreamercsw.base.ai.AIServiceException(
            response.statusCode(),
            "OpenAI API Error",
            getProviderName(),
            "OpenAI API returned an error: " + response.body());
      }

    } catch (Exception e) {
      log.error("Failed to call OpenAI API for segment narration", e);
      throw new com.github.javydreamercsw.base.ai.AIServiceException(
          500, "Internal Server Error", getProviderName(), e.getMessage(), e);
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

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse OpenAI response", e);
      return "Error parsing AI response: " + e.getMessage();
    }
  }

  /**
   * Checks if using the premium GPT-4 model.
   *
   * @return true if using GPT-4, false if using GPT-3.5
   */
  public boolean isUsingPremiumModel() {
    return model.contains(openAIConfigProperties.getPremiumModel());
  }

  /**
   * Gets estimated cost per 1K tokens based on the current model.
   *
   * @return Cost per 1K input tokens in USD
   */
  public double getCostPer1KTokens() {
    if (model.contains(openAIConfigProperties.getPremiumModel())) {
      return 10.0; // GPT-4: $10/1K input, $30/1K output
    } else {
      return 0.50; // GPT-3.5: $0.50/1K input, $1.50/1K output
    }
  }
}
