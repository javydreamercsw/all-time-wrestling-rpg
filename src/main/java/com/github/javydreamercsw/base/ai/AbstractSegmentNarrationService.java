package com.github.javydreamercsw.base.ai;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for segment narration services. Contains common logic for building prompts
 * from segment context that can be used by any AI provider.
 */
@Slf4j
public abstract class AbstractSegmentNarrationService implements SegmentNarrationService {

  /**
   * Builds a comprehensive prompt for segment narration based on the provided context. This method
   * is provider-agnostic and can be used by any AI service implementation.
   */
  protected String buildSegmentNarrationPrompt(
      @NonNull SegmentNarrationService.SegmentNarrationContext context) {
    // Check if this is a promo or a segment
    boolean isPromo = isPromoType(context);

    if (isPromo) {
      return buildPromoNarrationPrompt(context);
    } else {
      return buildSegmentNarrationPromptInternal(context);
    }
  }

  /** Determines if the context is for a promo rather than a segment. */
  private boolean isPromoType(@NonNull SegmentNarrationService.SegmentNarrationContext context) {
    if (context.getSegmentType() == null || context.getSegmentType().getSegmentType() == null) {
      return false;
    }

    String segmentType = context.getSegmentType().getSegmentType().toLowerCase();
    return segmentType.contains("promo")
        || (context.getWrestlers() != null && context.getWrestlers().size() == 1);
  }

  /** Builds a prompt specifically for promo narration. */
  private String buildPromoNarrationPrompt(
      @NonNull SegmentNarrationService.SegmentNarrationContext context) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are a professional wrestling commentator and storyteller. ");
    prompt.append(
        "Narrate a compelling wrestling promo segment with vivid detail, authentic character voice,"
            + " and dramatic storytelling. ");
    prompt.append(
        "Focus on the wrestler's personality, motivations, and storyline development.\n\n");

    return buildPromoPromptInternal(context, prompt);
  }

  /** Builds the internal segment narration prompt (non-promo). */
  private String buildSegmentNarrationPromptInternal(
      @NonNull SegmentNarrationService.SegmentNarrationContext context) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are a professional wrestling play-by-play commentator and storyteller. ");
    prompt.append(
        "Narrate a complete wrestling segment with vivid detail, compelling storytelling, and"
            + " authentic wrestling commentary. ");
    prompt.append("Include crowd reactions, move descriptions, and dramatic moments.\n\n");

    return buildSegmentPromptInternal(context, prompt);
  }

  /** Builds the internal segment prompt content. */
  private String buildSegmentPromptInternal(
      @NonNull SegmentNarrationService.SegmentNarrationContext context, StringBuilder prompt) {
    // Segment Setup
    prompt.append("SEGMENT SETUP:\n");
    prompt.append("Segment Type: ").append(context.getSegmentType().getSegmentType()).append("\n");
    prompt.append("Stipulation: ").append(context.getSegmentType().getStipulation()).append("\n");
    if (context.getSegmentType().getRules() != null
        && !context.getSegmentType().getRules().isEmpty()) {
      prompt
          .append("Special Rules: ")
          .append(String.join(", ", context.getSegmentType().getRules()))
          .append("\n");
    }
    if (context.getSegmentType().getTimeLimit() > 0) {
      prompt
          .append("Time Limit: ")
          .append(context.getSegmentType().getTimeLimit())
          .append(" minutes\n");
    }

    // Venue Details
    if (context.getVenue() != null) {
      prompt.append("Venue: ").append(context.getVenue().getName()).append("\n");
      prompt.append("Location: ").append(context.getVenue().getLocation()).append("\n");
      prompt.append("Type: ").append(context.getVenue().getType()).append("\n");
      prompt.append("Capacity: ").append(context.getVenue().getCapacity()).append("\n");
      prompt.append("Description: ").append(context.getVenue().getDescription()).append("\n");
      prompt.append("Atmosphere: ").append(context.getVenue().getAtmosphere()).append("\n");
      prompt.append("Significance: ").append(context.getVenue().getSignificance()).append("\n");
      if (context.getVenue().getNotableSegments() != null
          && !context.getVenue().getNotableSegments().isEmpty()) {
        prompt
            .append("Notable Segments: ")
            .append(String.join(", ", context.getVenue().getNotableSegments()))
            .append("\n");
      }
    }

    prompt.append("Audience: ").append(context.getAudience()).append("\n\n");

    // Wrestlers
    prompt.append("WRESTLERS:\n");
    for (WrestlerContext wrestler : context.getWrestlers()) {
      prompt.append("- ").append(wrestler.getName()).append(":\n");
      prompt.append("  Description: ").append(wrestler.getDescription()).append("\n");

      if (wrestler.getMoveSet() != null) {
        if (wrestler.getMoveSet().getFinishers() != null
            && !wrestler.getMoveSet().getFinishers().isEmpty()) {
          prompt.append("  Finishers: ");
          prompt.append(
              wrestler.getMoveSet().getFinishers().stream()
                  .map(move -> move.getName() + " (" + move.getDescription() + ")")
                  .collect(Collectors.joining(", ")));
          prompt.append("\n");
        }

        if (wrestler.getMoveSet().getTrademarks() != null
            && !wrestler.getMoveSet().getTrademarks().isEmpty()) {
          prompt.append("  Trademark Moves: ");
          prompt.append(
              wrestler.getMoveSet().getTrademarks().stream()
                  .map(move -> move.getName() + " (" + move.getDescription() + ")")
                  .collect(Collectors.joining(", ")));
          prompt.append("\n");
        }
      }

      if (wrestler.getFeudsAndHeat() != null && !wrestler.getFeudsAndHeat().isEmpty()) {
        prompt
            .append("  Current Feuds/Heat: ")
            .append(String.join(", ", wrestler.getFeudsAndHeat()))
            .append("\n");
      }

      if (wrestler.getRecentSegments() != null && !wrestler.getRecentSegments().isEmpty()) {
        prompt
            .append("  Recent Segment History: ")
            .append(String.join("; ", wrestler.getRecentSegments()))
            .append("\n");
      }
      prompt.append("\n");
    }

    // Referee
    if (context.getReferee() != null) {
      prompt.append("REFEREE:\n");
      prompt.append("- ").append(context.getReferee().getName()).append(":\n");
      prompt.append("  Description: ").append(context.getReferee().getDescription()).append("\n");
      prompt.append("  Personality: ").append(context.getReferee().getPersonality()).append("\n\n");
    }

    // NPCs
    if (context.getNpcs() != null && !context.getNpcs().isEmpty()) {
      prompt.append("SUPPORTING CHARACTERS:\n");
      for (NPCContext npc : context.getNpcs()) {
        prompt.append("- ").append(npc.getName()).append(" (").append(npc.getRole()).append("):\n");
        prompt.append("  Description: ").append(npc.getDescription()).append("\n");
        prompt.append("  Personality: ").append(npc.getPersonality()).append("\n");
      }
      prompt.append("\n");
    }

    // Recent Segment Context
    if (context.getRecentSegmentNarrations() != null
        && !context.getRecentSegmentNarrations().isEmpty()) {
      prompt.append("RECENT SEGMENT CONTEXT (for continuity and avoiding repetition):\n");
      for (int i = 0; i < Math.min(3, context.getRecentSegmentNarrations().size()); i++) {
        prompt.append("Recent Segment ").append(i + 1).append(": ");
        prompt.append(
            context.getRecentSegmentNarrations().get(i),
            0,
            Math.min(200, context.getRecentSegmentNarrations().get(i).length()));
        prompt.append("...\n");
      }
      prompt.append("\n");
    }

    // Predetermined Outcome
    prompt.append("PREDETERMINED OUTCOME:\n");
    prompt.append(context.getDeterminedOutcome()).append("\n\n");

    // Instructions
    prompt.append("NARRATION INSTRUCTIONS:\n");
    prompt.append(
        "1. Create a compelling 3-act structure: Opening/Setup, Middle/Action, Climax/Finish\n");
    prompt.append("2. Use the wrestlers' signature moves and personalities authentically\n");
    prompt.append("3. Include realistic crowd reactions and atmosphere\n");
    prompt.append("4. Incorporate the referee's personality into key moments\n");
    prompt.append("5. Reference the feuds/heat to add emotional weight to the action\n");
    prompt.append("6. Build to the predetermined outcome naturally and dramatically\n");
    prompt.append("7. Include commentary from the NPCs when appropriate\n");
    prompt.append("8. Make it feel like a real wrestling segment with proper pacing\n");
    prompt.append("9. Use vivid, descriptive language that captures the excitement\n");
    prompt.append("10. Create a detailed, comprehensive segment narration of 1500-2500 words\n");
    prompt.append(
        "11. Include multiple segment parts: opening, early action, mid-segment drama, climax, and"
            + " finish\n");
    prompt.append("12. Don't rush - take time to build drama and tell the complete story\n\n");

    prompt.append("Begin the segment narration now:");

    return prompt.toString();
  }

  /** Builds the internal promo prompt content. */
  private String buildPromoPromptInternal(
      @NonNull SegmentNarrationService.SegmentNarrationContext context, StringBuilder prompt) {
    // Promo Setup
    prompt.append("PROMO SETUP:\n");
    prompt.append("Segment Type: ").append(context.getSegmentType().getSegmentType()).append("\n");
    if (context.getSegmentType().getStipulation() != null) {
      prompt.append("Purpose: ").append(context.getSegmentType().getStipulation()).append("\n");
    }
    if (context.getSegmentType().getTimeLimit() > 0) {
      prompt
          .append("Time Limit: ")
          .append(context.getSegmentType().getTimeLimit())
          .append(" minutes\n");
    }

    // Venue Details
    if (context.getVenue() != null) {
      prompt.append("Venue: ").append(context.getVenue().getName());
      if (context.getVenue().getLocation() != null) {
        prompt.append(" (").append(context.getVenue().getLocation()).append(")");
      }
      prompt.append("\n");

      if (context.getVenue().getDescription() != null) {
        prompt
            .append("Venue Description: ")
            .append(context.getVenue().getDescription())
            .append("\n");
      }
      if (context.getVenue().getAtmosphere() != null) {
        prompt.append("Atmosphere: ").append(context.getVenue().getAtmosphere()).append("\n");
      }
    }

    // Wrestler Details (usually just one for promos)
    if (context.getWrestlers() != null && !context.getWrestlers().isEmpty()) {
      prompt.append("\nWRESTLER DETAILS:\n");
      for (WrestlerContext wrestler : context.getWrestlers()) {
        prompt.append("Name: ").append(wrestler.getName()).append("\n");
        if (wrestler.getDescription() != null) {
          prompt.append("Character: ").append(wrestler.getDescription()).append("\n");
        }
        if (wrestler.getFeudsAndHeat() != null && !wrestler.getFeudsAndHeat().isEmpty()) {
          prompt
              .append("Current Storylines: ")
              .append(String.join(", ", wrestler.getFeudsAndHeat()))
              .append("\n");
        }
        if (wrestler.getRecentSegments() != null && !wrestler.getRecentSegments().isEmpty()) {
          prompt
              .append("Recent Activity: ")
              .append(String.join(", ", wrestler.getRecentSegments()))
              .append("\n");
        }
        prompt.append("\n");
      }
    }

    // Audience
    if (context.getAudience() != null) {
      prompt.append("Audience: ").append(context.getAudience()).append("\n\n");
    }

    // Predetermined Outcome/Message
    if (context.getDeterminedOutcome() != null) {
      prompt.append("PROMO OBJECTIVE:\n");
      prompt.append(context.getDeterminedOutcome()).append("\n\n");
    }

    // Instructions for Promo
    prompt.append("PROMO NARRATION INSTRUCTIONS:\n");
    prompt.append("1. Create an engaging promo segment with authentic character voice\n");
    prompt.append("2. Focus on the wrestler's personality, motivations, and current storylines\n");
    prompt.append("3. Include realistic crowd reactions and atmosphere\n");
    prompt.append("4. Build emotional connection between wrestler and audience\n");
    prompt.append("5. Reference current feuds and storylines naturally\n");
    prompt.append("6. Show character development and progression\n");
    prompt.append("7. Use wrestling terminology and authentic dialogue\n");
    prompt.append("8. Create dramatic moments and memorable quotes\n");
    prompt.append("9. Make it feel like a real wrestling promo with proper pacing\n");
    prompt.append("10. Create a detailed promo narration of 800-1200 words\n");
    prompt.append("11. Include setup, main message delivery, and crowd reaction\n");
    prompt.append("12. Focus on storytelling and character rather than physical action\n\n");

    prompt.append("Begin the promo narration now:");

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
      return getProviderName()
          + " AI service is not available. Please configure the required API key.";
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
