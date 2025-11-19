package com.github.javydreamercsw.base.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractSegmentNarrationService implements SegmentNarrationService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  protected String getSystemMessage(@NonNull String prompt) {
    if (prompt.contains("Summarize the following segment narration")) {
      return "You are a wrestling expert. Your task is to provide a concise summary of a wrestling"
          + " segment narration.";
    } else {
      return "You are a professional wrestling commentator and storyteller. You have deep knowledge"
          + " of wrestling history, storytelling techniques, and segment psychology. Create"
          + " vivid, engaging segment narrations that capture the drama and excitement of"
          + " professional wrestling.";
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
    prompt.append("You are a professional wrestling commentator and storyteller.\n");
    prompt.append("You will be provided with a context object in JSON format.\n");
    prompt.append(
        "Generate a compelling wrestling narration based on the data in the JSON object.\n");
    prompt.append("The JSON object contains instructions that you must follow.\n");
    prompt.append(
        "If a segmentChampionship is provided, use it as the title of the segment narration.\n\n");
    prompt.append(
        "If the segmentType is \"Promo\", narrate a promo segment. Focus on the dialogue,"
            + " interviews, and interactions between the participants. Do not describe a wrestling"
            + " match.\n\n");
    prompt.append(
        "If the match is a championship match, the context will include the current champion. Make"
            + " sure to acknowledge the current champion in your narration.\n\n");
    prompt.append("Here is the JSON context:\n\n");
    prompt.append(jsonContext);

    log.debug("Generated AI Prompt: {}", prompt);
    return prompt.toString();
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
      return Arrays.asList(429, 502, 503).contains(aiException.getStatusCode());
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
}
