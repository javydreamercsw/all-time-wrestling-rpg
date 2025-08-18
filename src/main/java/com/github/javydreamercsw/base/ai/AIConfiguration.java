package com.github.javydreamercsw.base.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration properties for AI services. */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIConfiguration {

  /**
   * The preferred AI provider to use. Options: "auto", "openai", "notion" Default: "auto"
   * (automatically select the first available service)
   */
  private String provider = "auto";

  /** OpenAI-specific configuration. */
  private OpenAI openai = new OpenAI();

  /** Notion AI-specific configuration. */
  private NotionAI notion = new NotionAI();

  @Data
  public static class OpenAI {
    /** OpenAI API key. Can also be set via OPENAI_API_KEY environment variable. */
    private String apiKey;

    /** OpenAI model to use for requests. Default: "gpt-3.5-turbo" */
    private String model = "gpt-3.5-turbo";

    /** Maximum tokens for OpenAI responses. Default: 1000 */
    private int maxTokens = 1000;

    /**
     * Temperature for OpenAI requests (0.0 to 2.0). Higher values make output more random. Default:
     * 0.7
     */
    private double temperature = 0.7;

    /** Request timeout in seconds. Default: 30 */
    private int timeoutSeconds = 30;
  }

  @Data
  public static class NotionAI {
    /**
     * Whether to enable Notion AI service (when available). Default: false (since Notion AI API is
     * not yet available)
     */
    private boolean enabled = false;

    /** Notion AI API key (when it becomes available). */
    private String apiKey;

    /** Request timeout in seconds. Default: 30 */
    private int timeoutSeconds = 30;
  }
}
