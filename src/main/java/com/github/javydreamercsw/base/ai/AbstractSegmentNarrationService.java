/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.base.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractSegmentNarrationService implements SegmentNarrationService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpClient httpClient;

  @Autowired(required = false)
  @Setter
  @Getter
  private PerformanceMonitoringService performanceMonitoringService;

  protected String getSystemMessage(@NonNull String prompt) {
    if (prompt.contains("Summarize the following segment narration")) {
      return "You are a wrestling expert. Your task is to provide a concise summary of a wrestling"
          + " segment narration.";
    } else {
      return "You are a team of professional wrestling commentators and a match narrator. Your"
          + " task is to provide a transcript of the match, alternating between vivid"
          + " descriptions of the action (as 'Narrator') and character-driven commentary from"
          + " the commentators. Each commentator has a unique voice, alignment (Face/Heel), and"
          + " style. Commentators MUST show bias based on their alignment and the alignment of the"
          + " wrestlers they are describing (Face vs Heel dynamics). IMPORTANT: Every line of"
          + " output MUST follow the format: 'Name: Text'.";
    }
  }

  protected String buildSegmentNarrationPrompt(
      @NonNull SegmentNarrationService.SegmentNarrationContext context) {

    String jsonContext;
    try {
      jsonContext = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    } catch (JsonProcessingException e) {
      log.error("Error serializing context to JSON", e);
      throw new RuntimeException("Error serializing context to JSON", e);
    }

    StringBuilder prompt = new StringBuilder();
    prompt.append("You are a team of professional wrestling commentators.\n");
    prompt.append("You will be provided with a context object in JSON format.\n");
    prompt.append(
        "Generate a compelling wrestling narration as a DIALOGUE between the commentators provided"
            + " in the JSON.\n");
    prompt.append("The JSON object contains instructions that you must follow.\n");
    prompt.append(
        "If commentators are provided, use their names, styles, and alignments to drive the"
            + " conversation.\n");
    prompt.append(
        "A 'FACE' commentator should be supportive of rule-abiding wrestlers and optimistic.\n");
    prompt.append(
        "A 'HEEL' commentator should be snarky, support rule-breakers, and offer critical or"
            + " controversial takes.\n");
    prompt.append("COMMENTARY INTERACTION RULES:\n");
    prompt.append(
        "- FACE Commentator vs. FACE Wrestler: Enthusiastic support, praise for skill and"
            + " integrity.\n");
    prompt.append(
        "- FACE Commentator vs. HEEL Wrestler: Critical of dirty tactics, focuses on the"
            + " importance of rules and fair play.\n");
    prompt.append(
        "- HEEL Commentator vs. FACE Wrestler: Mockery, dismissal of skills as 'boring' or 'just"
            + " luck', finds their moral high ground annoying.\n");
    prompt.append(
        "- HEEL Commentator vs. HEEL Wrestler: Justifies rule-breaking as 'strategy', praises"
            + " ruthlessness and 'doing what it takes to win'.\n");
    prompt.append(
        "- NEUTRAL (Wrestler or Commentator): Focus strictly on technical execution, stats, and"
            + " the importance of the match outcome without moral bias.\n\n");
    prompt.append(
        "IMPORTANT: You MUST format the narration as a transcript of dialogue and action.\n");
    prompt.append(
        "For each sequence of events, start the line with 'Narrator:' followed by a vivid,"
            + " objective description of the wrestling action and moves performed.\n");
    prompt.append(
        "Follow the action lines with reactions and analysis from the commentators provided in the"
            + " JSON.\n");
    prompt.append(
        "Each line MUST start with the speaker's name (either 'Narrator' or a commentator's name)"
            + " followed immediately by a colon and their text.\n");
    prompt.append(
        "Example: 'Narrator: Jax Felix scales the ropes and connects with a springboard"
            + " moonsault!'\n");
    prompt.append("Example: 'Dara Hoshiko: Incredible athleticism! Jax is taking control!'\n");
    prompt.append(
        "Example: 'Lord Bastian Von Crowe: A flashy move, but Eddie Guerrero is far from"
            + " finished. He's just baiting the boy in.'\n");
    prompt.append("DO NOT use square brackets like [SPEAKER:Name] around the names.\n");
    prompt.append("DO NOT include any text that is not a tagged line.\n\n");
    prompt.append(
        "If a segmentChampionship is provided, use it as the title of the segment narration.\n\n");
    prompt.append(
        "If the segmentType is \"Promo\", narrate a promo segment. Focus on the dialogue,"
            + " interviews, and interactions between the participants. Do not describe a wrestling"
            + " match.\n\n");
    prompt.append(
        "If the match is a championship match, the context will include the current champion. Make"
            + " sure to acknowledge the current champion in your narration.\n\n");
    prompt.append(
        "If campaignContext is present, incorporate the wrestler's alignment (FACE/HEEL), "
            + "current chapter, and any injuries into the narrative tone. "
            + "A high HEEL alignment should result in more aggressive behavior. "
            + "Injuries should be mentioned if they might affect performance.\n\n");
    prompt.append("Here is the JSON context:\n\n");
    prompt.append(jsonContext);

    log.debug("Generated AI Prompt: {}", prompt);
    return prompt.toString();
  }

  @Override
  public String generateText(@NonNull String prompt) {
    return callAIProvider(prompt);
  }

  /**
   * Abstract method that each provider must implement to call their specific AI service.
   *
   * @param prompt The formatted prompt for segment narration
   * @return The AI-generated segment narration
   */
  protected abstract String callAIProvider(@NonNull String prompt);

  @Override
  public String narrateSegment(
      @NonNull SegmentNarrationService.SegmentNarrationContext segmentContext) {
    if (!isAvailable()) {
      throw new AIServiceException(
          503,
          "Service Unavailable",
          getProviderName(),
          getProviderName()
              + " AI service is not available. Please configure the required API key.");
    }

    String prompt = buildSegmentNarrationPrompt(segmentContext);
    return executeWithRetry(prompt);
  }

  @Override
  public String summarizeNarration(@NonNull String narration) {
    if (!isAvailable()) {
      throw new RuntimeException(
          getProviderName()
              + " AI service is not available. Please configure the required API key.");
    }

    String prompt = buildSummaryPrompt(narration);
    return executeWithRetry(prompt);
  }

  protected String buildSummaryPrompt(@NonNull String narration) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        "You are a wrestling expert. Summarize the following segment narration in 2-3 sentences,"
            + " focusing on the key events, turning points, and the final outcome. The summary"
            + " should be suitable for a show planning context, providing a quick overview of what"
            + " happened in the segment.\n\n");
    prompt.append("Narration:\n");
    prompt.append(narration);
    prompt.append("\n\nSummary:");
    return prompt.toString();
  }

  /**
   * Gets the retry policies specific to this AI provider. Each implementation can override this to
   * provide custom retry behavior.
   */
  protected List<RetryPolicyConfig> getRetryPolicies() {
    // Default retry policies - can be overridden by implementations
    return Arrays.asList(
        RetryPolicyConfig.fixedDelay(
            3, Duration.ofSeconds(2), this::isRetryableException, "Standard retry policy"),
        RetryPolicyConfig.exponentialBackoff(
            2,
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            this::isRetryableException,
            "Extended backoff policy"));
  }

  /**
   * Determines if an exception should trigger a retry. Can be overridden by implementations for
   * provider-specific logic.
   */
  protected boolean isRetryableException(@NonNull Exception exception) {
    // Check for custom AI service exception with retryable status codes
    if (exception instanceof AIServiceException aiException) {
      return Arrays.asList(429, 502, 503, 504).contains(aiException.getStatusCode());
    }

    // Fallback to message-based checking for generic exceptions
    String message = exception.getMessage();
    if (message == null) {
      return false;
    }

    // Default retryable conditions
    return message.contains("429")
        || // Rate limiting
        message.contains("503")
        || // Service unavailable
        message.contains("502")
        || // Bad gateway
        message.contains("timeout")
        || message.contains("connection");
  }

  /** Executes AI provider call with retry logic using the provider's retry policies. */
  protected String executeWithRetry(@NonNull String prompt) {
    List<RetryPolicyConfig> policies = getRetryPolicies();
    Exception lastException = null;

    String operationName = "AI.Narration." + getProviderName();
    if (performanceMonitoringService != null) {
      performanceMonitoringService.startOperation(operationName);
    }

    try {
      for (RetryPolicyConfig policy : policies) {
        try {
          return callAIProviderWithRetry(prompt, policy);
        } catch (AIServiceException e) {
          lastException = e;
          if (!isRetryableException(e)) {
            throw e; // Re-throw non-retryable AI exceptions directly
          }
          log.warn(
              "Retryable AI error for {}: {} - {}",
              getProviderName(),
              e.getStatusCode(),
              e.getMessage());
        } catch (Exception e) {
          lastException = e;
          log.warn(
              "Retry policy '{}' failed for {}: {}",
              policy.getDescription(),
              getProviderName(),
              e.getMessage());
        }
      }
    } finally {
      if (performanceMonitoringService != null) {
        performanceMonitoringService.endOperation(operationName);
      }
    }

    throw new AIServiceException(
        503,
        "Service Unavailable",
        getProviderName(),
        "All retry policies exhausted for " + getProviderName(),
        lastException);
  }

  /** Executes the AI provider call with a specific retry policy. */
  private String callAIProviderWithRetry(
      @NonNull String prompt, @NonNull RetryPolicyConfig policy) {
    Exception lastException = null;
    Duration currentDelay = policy.getBaseDelay();

    for (int attempt = 0; attempt <= policy.getMaxRetries(); attempt++) {
      try {
        return callAIProvider(prompt);
      } catch (Exception e) {
        lastException = e;

        if (attempt < policy.getMaxRetries() && policy.getShouldRetry().test(e)) {
          Duration delay = calculateRetryDelay(e, currentDelay);
          log.warn(
              "Attempt {} failed for {} ({}), retrying in {}ms: {}",
              attempt + 1,
              getProviderName(),
              policy.getDescription(),
              delay.toMillis(),
              e.getMessage());

          try {
            Thread.sleep(delay.toMillis());
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry delay", ie);
          }

          // Exponential backoff
          if (!policy.getBaseDelay().equals(policy.getMaxDelay())) {
            currentDelay =
                Duration.ofMillis(
                    Math.min(currentDelay.toMillis() * 2, policy.getMaxDelay().toMillis()));
          }
        } else {
          break;
        }
      }
    }

    throw new RuntimeException(
        "Max retries exceeded for "
            + getProviderName()
            + " with policy: "
            + policy.getDescription(),
        lastException);
  }

  /**
   * Calculates retry delay, potentially extracting it from API error response. Can be overridden by
   * implementations to parse provider-specific retry hints.
   */
  protected Duration calculateRetryDelay(
      @NonNull Exception exception, @NonNull Duration defaultDelay) {
    return defaultDelay;
  }

  protected HttpClient getHttpClient(int timeout) {
    if (httpClient == null) {
      httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeout)).build();
    }
    return httpClient;
  }
}
