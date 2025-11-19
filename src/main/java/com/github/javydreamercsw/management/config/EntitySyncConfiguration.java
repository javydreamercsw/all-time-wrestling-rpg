package com.github.javydreamercsw.management.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for entity-specific sync settings providing fine-grained control over
 * synchronization behavior for each entity type.
 */
@Component
@ConfigurationProperties(prefix = "notion.sync.entities")
@Data
public class EntitySyncConfiguration {

  /** Default configuration for all entities */
  private EntitySyncSettings defaults = new EntitySyncSettings();

  /** Entity-specific configurations */
  private Map<String, EntitySyncSettings> specific = new ConcurrentHashMap<>();

  /**
   * Gets the effective configuration for a specific entity, merging defaults with specific
   * settings.
   *
   * @param entityType The entity type (e.g., "wrestlers", "factions")
   * @return Effective configuration for the entity
   */
  public EntitySyncSettings getEffectiveSettings(String entityType) {
    EntitySyncSettings entitySettings = specific.get(entityType.toLowerCase());
    if (entitySettings == null) {
      return defaults;
    }

    // Merge with defaults
    EntitySyncSettings effective = new EntitySyncSettings();
    effective.setDatabaseId(
        entitySettings.getDatabaseId() != null
            ? entitySettings.getDatabaseId()
            : defaults.getDatabaseId());
    effective.setEnabled(
        entitySettings.getEnabled() != null ? entitySettings.getEnabled() : defaults.getEnabled());
    effective.setBatchSize(
        entitySettings.getBatchSize() != null
            ? entitySettings.getBatchSize()
            : defaults.getBatchSize());
    effective.setParallelProcessing(
        entitySettings.getParallelProcessing() != null
            ? entitySettings.getParallelProcessing()
            : defaults.getParallelProcessing());
    effective.setMaxThreads(
        entitySettings.getMaxThreads() != null
            ? entitySettings.getMaxThreads()
            : defaults.getMaxThreads());
    effective.setTimeoutSeconds(
        entitySettings.getTimeoutSeconds() != null
            ? entitySettings.getTimeoutSeconds()
            : defaults.getTimeoutSeconds());
    effective.setRetryAttempts(
        entitySettings.getRetryAttempts() != null
            ? entitySettings.getRetryAttempts()
            : defaults.getRetryAttempts());
    effective.setRetryDelayMs(
        entitySettings.getRetryDelayMs() != null
            ? entitySettings.getRetryDelayMs()
            : defaults.getRetryDelayMs());
    effective.setValidationEnabled(
        entitySettings.getValidationEnabled() != null
            ? entitySettings.getValidationEnabled()
            : defaults.getValidationEnabled());
    effective.setSkipOnError(
        entitySettings.getSkipOnError() != null
            ? entitySettings.getSkipOnError()
            : defaults.getSkipOnError());

    return effective;
  }

  /**
   * Checks if an entity type is enabled for synchronization.
   *
   * @param entityType The entity type to check
   * @return true if the entity is enabled for sync
   */
  public boolean isEntityEnabled(String entityType) {
    return getEffectiveSettings(entityType).getEnabled();
  }

  @Data
  public static class EntitySyncSettings {
    /** The Notion database ID for this entity. */
    private String databaseId;

    /** Whether sync is enabled for this entity */
    private Boolean enabled = true;

    /** Batch size for processing entities */
    private Integer batchSize = 50;

    /** Whether to use parallel processing */
    private Boolean parallelProcessing = true;

    /** Maximum number of threads for parallel processing */
    private Integer maxThreads = 4;

    /** Timeout for sync operations in seconds */
    private Integer timeoutSeconds = 300;

    /** Number of retry attempts on failure */
    private Integer retryAttempts = 3;

    /** Delay between retry attempts in milliseconds */
    private Integer retryDelayMs = 1000;

    /** Whether to enable validation after sync */
    private Boolean validationEnabled = true;

    /** Whether to skip entity on error or fail entire sync */
    private Boolean skipOnError = true;
  }
}
