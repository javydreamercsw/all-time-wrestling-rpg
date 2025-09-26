package com.github.javydreamercsw.base.ai.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.AIServiceException;
import com.github.javydreamercsw.base.ai.AbstractSegmentNarrationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Anthropic Claude implementation of the SegmentNarrationService interface. Uses Anthropic's Claude
 * API for wrestling segment narration with rich context.
 */
@Service
@Slf4j
public class ClaudeSegmentNarrationService extends AbstractSegmentNarrationService {

  private static final Duration TIMEOUT = Duration.ofSeconds(60);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final ClaudeConfigProperties claudeConfigProperties;

  @Autowired
  public ClaudeSegmentNarrationService(ClaudeConfigProperties claudeConfigProperties) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.objectMapper = new ObjectMapper();
    this.apiKey = System.getenv("ANTHROPIC_API_KEY");
    this.claudeConfigProperties = claudeConfigProperties;
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

  @Override
  public String generateText(@NonNull String prompt) {
    return callClaude(prompt);
  }

  /** Makes a call to the Claude API with the given prompt. */
  private String callClaude(@NonNull String prompt) {
    try {
      String fullApiUrl = claudeConfigProperties.getApiUrl();
      String modelName = claudeConfigProperties.getModelName();

      Map<String, Object> requestBody =
          Map.of(
              "model",
              modelName,
              "max_tokens",
              4000,
              "messages",
              List.of(Map.of("role", "user", "content", prompt)));

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(fullApiUrl))
              .header("Content-Type", "application/json")
              .header("x-api-key", apiKey)
              .header("anthropic-version", "2023-06-01")
              .timeout(TIMEOUT)
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        return extractContentFromResponse(response.body());
      } else {
        throw new AIServiceException(
            response.statusCode(),
            "Claude API Error",
            getProviderName(),
            "Claude API returned an error: " + response.body());
      }

    } catch (Exception e) {
      log.error("Failed to call Claude API for segment narration", e);
      throw new AIServiceException(
          500, "Internal Server Error", getProviderName(), e.getMessage(), e);
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

      return "No content in AI response";
    } catch (Exception e) {
      log.error("Failed to parse Claude response", e);
      return "Error parsing AI response: " + e.getMessage();
    }
  }
}
