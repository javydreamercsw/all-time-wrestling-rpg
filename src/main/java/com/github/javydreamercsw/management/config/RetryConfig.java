package com.github.javydreamercsw.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for retry mechanisms in sync operations. Provides configurable retry
 * policies for different types of operations.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sync.retry")
public class RetryConfig {

  /** Maximum number of retry attempts for sync operations */
  private int maxAttempts = 3;

  /** Initial delay between retry attempts in milliseconds */
  private long initialDelayMs = 1000;

  /** Maximum delay between retry attempts in milliseconds */
  private long maxDelayMs = 30000;

  /** Multiplier for exponential backoff */
  private double backoffMultiplier = 2.0;

  /** Whether to use jitter in retry delays to avoid thundering herd */
  private boolean useJitter = true;

  /** Maximum jitter percentage (0.0 to 1.0) */
  private double jitterFactor = 0.1;

  /** Timeout for individual sync operations in milliseconds */
  private long operationTimeoutMs = 300000; // 5 minutes

  /** Circuit breaker configuration */
  private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

  @Data
  public static class CircuitBreakerConfig {
    /** Number of consecutive failures before opening circuit */
    private int failureThreshold = 5;

    /** Time to wait before attempting to close circuit (milliseconds) */
    private long recoveryTimeoutMs = 60000; // 1 minute

    /** Percentage of successful calls needed to close circuit */
    private double successThreshold = 0.5; // 50%

    /** Number of calls to evaluate for success threshold */
    private int evaluationWindow = 10;
  }

  /** Retry configuration for specific entity types */
  private EntityRetryConfig entities = new EntityRetryConfig();

  @Data
  public static class EntityRetryConfig {
    private EntityConfig shows = new EntityConfig();
    private EntityConfig wrestlers = new EntityConfig();
    private EntityConfig factions = new EntityConfig();
    private EntityConfig teams = new EntityConfig();
    private EntityConfig matches = new EntityConfig();
    private EntityConfig templates = new EntityConfig();
  }

  @Data
  public static class EntityConfig {
    /** Override max attempts for this entity type */
    private Integer maxAttempts;

    /** Override initial delay for this entity type */
    private Long initialDelayMs;

    /** Override max delay for this entity type */
    private Long maxDelayMs;

    /** Whether this entity supports partial success */
    private boolean allowPartialSuccess = true;

    /** Batch size for processing large datasets */
    private int batchSize = 100;
  }

  /**
   * Get effective max attempts for an entity type. Falls back to global config if entity-specific
   * config is not set.
   */
  public int getMaxAttempts(String entityType) {
    EntityConfig entityConfig = getEntityConfig(entityType);
    return entityConfig.getMaxAttempts() != null ? entityConfig.getMaxAttempts() : maxAttempts;
  }

  /**
   * Get effective initial delay for an entity type. Falls back to global config if entity-specific
   * config is not set.
   */
  public long getInitialDelayMs(String entityType) {
    EntityConfig entityConfig = getEntityConfig(entityType);
    return entityConfig.getInitialDelayMs() != null
        ? entityConfig.getInitialDelayMs()
        : initialDelayMs;
  }

  /**
   * Get effective max delay for an entity type. Falls back to global config if entity-specific
   * config is not set.
   */
  public long getMaxDelayMs(String entityType) {
    EntityConfig entityConfig = getEntityConfig(entityType);
    return entityConfig.getMaxDelayMs() != null ? entityConfig.getMaxDelayMs() : maxDelayMs;
  }

  /** Get entity-specific configuration. */
  private EntityConfig getEntityConfig(String entityType) {
    switch (entityType.toLowerCase()) {
      case "shows":
        return entities.getShows();
      case "wrestlers":
        return entities.getWrestlers();
      case "factions":
        return entities.getFactions();
      case "teams":
        return entities.getTeams();
      case "matches":
        return entities.getMatches();
      case "templates":
        return entities.getTemplates();
      default:
        return new EntityConfig(); // Default config
    }
  }
}
