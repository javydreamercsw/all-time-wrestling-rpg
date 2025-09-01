package com.github.javydreamercsw.base.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration properties for match narration AI services. */
@Configuration
@ConfigurationProperties(prefix = "match-narration")
@Data
public class MatchNarrationConfig {

  /** AI provider settings for match narration. */
  private AI ai = new AI();

  @Data
  public static class AI {
    /**
     * Maximum output tokens for match narration. Gemini: Up to 8,192 tokens (~6,000 words) OpenAI
     * GPT-3.5: Up to 4,096 tokens (~3,000 words) OpenAI GPT-4: Up to 8,192 tokens (~6,000 words)
     */
    private int maxOutputTokens = 4000;

    /**
     * Temperature for AI creativity (0.0 to 2.0). 0.0 = Very deterministic 0.8 = Good balance for
     * storytelling 1.0 = More creative 2.0 = Very creative/random
     */
    private double temperature = 0.8;

    /** Request timeout in seconds for match narration. Longer narrations need more time. */
    private int timeoutSeconds = 90;

    /** Target word count for match narrations. */
    private WordCount wordCount = new WordCount();

    /** Rate limiting settings. */
    private RateLimit rateLimit = new RateLimit();
  }

  @Data
  public static class WordCount {
    /** Minimum words for a complete match narration. */
    private int minimum = 1500;

    /** Maximum words for a complete match narration. */
    private int maximum = 2500;

    /** Target words for optimal match length. */
    private int target = 2000;
  }

  @Data
  public static class RateLimit {
    /** Maximum requests per minute (to avoid hitting provider limits). */
    private int requestsPerMinute = 10;

    /** Maximum requests per hour. */
    private int requestsPerHour = 100;

    /** Whether to enable rate limiting. */
    private boolean enabled = true;
  }

  /** Provider-specific limits and capabilities. */
  public enum ProviderLimits {
    GEMINI_FREE(
        "Gemini Free",
        15, // requests per minute
        1500, // requests per day
        8192, // max output tokens
        1000000 // max input tokens
        ),
    GEMINI_PAID(
        "Gemini Paid",
        1000, // requests per minute
        -1, // unlimited daily requests
        8192, // max output tokens
        1000000 // max input tokens
        ),
    OPENAI_GPT35(
        "OpenAI GPT-3.5",
        3500, // requests per minute (tier dependent)
        -1, // unlimited daily requests
        4096, // max output tokens
        16385 // max total tokens (input + output)
        ),
    OPENAI_GPT4(
        "OpenAI GPT-4",
        500, // requests per minute (tier dependent)
        -1, // unlimited daily requests
        8192, // max output tokens
        32768 // max total tokens (input + output)
        );

    private final String displayName;
    private final int requestsPerMinute;
    private final int requestsPerDay;
    private final int maxOutputTokens;
    private final int maxInputTokens;

    ProviderLimits(
        String displayName,
        int requestsPerMinute,
        int requestsPerDay,
        int maxOutputTokens,
        int maxInputTokens) {
      this.displayName = displayName;
      this.requestsPerMinute = requestsPerMinute;
      this.requestsPerDay = requestsPerDay;
      this.maxOutputTokens = maxOutputTokens;
      this.maxInputTokens = maxInputTokens;
    }

    public String getDisplayName() {
      return displayName;
    }

    public int getRequestsPerMinute() {
      return requestsPerMinute;
    }

    public int getRequestsPerDay() {
      return requestsPerDay;
    }

    public int getMaxOutputTokens() {
      return maxOutputTokens;
    }

    public int getMaxInputTokens() {
      return maxInputTokens;
    }
  }
}
