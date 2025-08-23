package com.github.javydreamercsw.management.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for retrieving configuration values from system properties and environment
 * variables. This class provides a consistent way to access configuration values with fallback
 * mechanisms.
 *
 * <p>The retrieval order is:
 *
 * <ol>
 *   <li>System property (e.g., -Dnotion.token=value)
 *   <li>Environment variable (e.g., NOTION_TOKEN=value)
 *   <li>Default value (if provided)
 * </ol>
 */
@Slf4j
public final class EnvironmentVariableUtil {

  private EnvironmentVariableUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Retrieves a configuration value with fallback from system property to environment variable.
   *
   * @param systemPropertyKey the system property key (e.g., "notion.token")
   * @param environmentVariableKey the environment variable key (e.g., "NOTION_TOKEN")
   * @return the configuration value, or null if not found
   */
  public static String getValue(
      @NonNull String systemPropertyKey, @NonNull String environmentVariableKey) {
    return getValue(systemPropertyKey, environmentVariableKey, null);
  }

  /**
   * Retrieves a configuration value with fallback from system property to environment variable to
   * default.
   *
   * @param systemPropertyKey the system property key (e.g., "notion.token")
   * @param environmentVariableKey the environment variable key (e.g., "NOTION_TOKEN")
   * @param defaultValue the default value to return if neither property nor environment variable is
   *     set
   * @return the configuration value, or the default value if not found
   */
  public static String getValue(
      @NonNull String systemPropertyKey,
      @NonNull String environmentVariableKey,
      String defaultValue) {
    // First try system property
    String value = System.getProperty(systemPropertyKey);
    if (value != null && !value.trim().isEmpty()) {
      log.debug("Found configuration value from system property: {}", systemPropertyKey);
      return value.trim();
    }

    // Then try environment variable
    value = System.getenv(environmentVariableKey);
    if (value != null && !value.trim().isEmpty()) {
      log.debug("Found configuration value from environment variable: {}", environmentVariableKey);
      return value.trim();
    }

    // Return default value
    if (defaultValue != null) {
      log.debug("Using default value for {}/{}", systemPropertyKey, environmentVariableKey);
    } else {
      log.debug("No value found for {}/{}", systemPropertyKey, environmentVariableKey);
    }
    return defaultValue;
  }

  /**
   * Retrieves the Notion token from system property or environment variable. This is a convenience
   * method for the most common use case.
   *
   * @return the Notion token, or null if not found
   */
  public static String getNotionToken() {
    return getValue("notion.token", "NOTION_TOKEN");
  }

  /**
   * Retrieves an OpenAI API key from system property or environment variable.
   *
   * @return the OpenAI API key, or null if not found
   */
  public static String getOpenAiApiKey() {
    return getValue("openai.api.key", "OPENAI_API_KEY");
  }

  /**
   * Retrieves a Claude API key from system property or environment variable.
   *
   * @return the Claude API key, or null if not found
   */
  public static String getClaudeApiKey() {
    return getValue("claude.api.key", "CLAUDE_API_KEY");
  }

  /**
   * Retrieves a Gemini API key from system property or environment variable.
   *
   * @return the Gemini API key, or null if not found
   */
  public static String getGeminiApiKey() {
    return getValue("gemini.api.key", "GEMINI_API_KEY");
  }

  /**
   * Checks if a configuration value is available (not null and not empty).
   *
   * @param systemPropertyKey the system property key
   * @param environmentVariableKey the environment variable key
   * @return true if a value is available, false otherwise
   */
  public static boolean isAvailable(String systemPropertyKey, String environmentVariableKey) {
    String value = getValue(systemPropertyKey, environmentVariableKey);
    return value != null && !value.trim().isEmpty();
  }

  /**
   * Checks if the Notion token is available.
   *
   * @return true if the Notion token is available, false otherwise
   */
  public static boolean isNotionTokenAvailable() {
    return isAvailable("notion.token", "NOTION_TOKEN");
  }

  /**
   * Retrieves a boolean configuration value with fallback.
   *
   * @param systemPropertyKey the system property key
   * @param environmentVariableKey the environment variable key
   * @param defaultValue the default boolean value
   * @return the boolean configuration value
   */
  public static boolean getBooleanValue(
      String systemPropertyKey, String environmentVariableKey, boolean defaultValue) {
    String value = getValue(systemPropertyKey, environmentVariableKey);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  /**
   * Retrieves an integer configuration value with fallback.
   *
   * @param systemPropertyKey the system property key
   * @param environmentVariableKey the environment variable key
   * @param defaultValue the default integer value
   * @return the integer configuration value
   * @throws NumberFormatException if the value cannot be parsed as an integer
   */
  public static int getIntValue(
      String systemPropertyKey, String environmentVariableKey, int defaultValue) {
    String value = getValue(systemPropertyKey, environmentVariableKey);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn(
          "Invalid integer value '{}' for {}/{}, using default: {}",
          value,
          systemPropertyKey,
          environmentVariableKey,
          defaultValue);
      return defaultValue;
    }
  }

  /**
   * Logs the availability status of common configuration values for debugging purposes. This method
   * does NOT log the actual values for security reasons.
   */
  public static void logConfigurationStatus() {
    log.info("Configuration Status:");
    log.info("  Notion Token: {}", isNotionTokenAvailable() ? "Available" : "Not Available");
    log.info(
        "  OpenAI API Key: {}",
        isAvailable("openai.api.key", "OPENAI_API_KEY") ? "Available" : "Not Available");
    log.info(
        "  Claude API Key: {}",
        isAvailable("claude.api.key", "CLAUDE_API_KEY") ? "Available" : "Not Available");
    log.info(
        "  Gemini API Key: {}",
        isAvailable("gemini.api.key", "GEMINI_API_KEY") ? "Available" : "Not Available");
  }
}
