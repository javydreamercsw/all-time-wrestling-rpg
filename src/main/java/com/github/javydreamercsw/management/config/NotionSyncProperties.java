package com.github.javydreamercsw.management.config;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Notion synchronization functionality. These properties control how
 * and when data is synchronized between Notion databases and local JSON files.
 */
@Component
@ConfigurationProperties(prefix = "notion.sync")
@Data
public class NotionSyncProperties {

  /**
   * Whether Notion sync functionality is enabled. When disabled, all sync operations will be
   * skipped.
   */
  private boolean enabled = false;

  /** Scheduler configuration for automatic synchronization. */
  private Scheduler scheduler = new Scheduler();

  /** Backup configuration for protecting original files. */
  private Backup backup = new Backup();

  /** Number of threads to use for parallel processing during sync. */
  private int parallelThreads = 3;

  /** Whether to load existing data from JSON files during sync. */
  private boolean loadFromJson = true;

  /** Map to store the last sync time for each entity. */
  private final Map<String, LocalDateTime> lastSyncTimes = new ConcurrentHashMap<>();

  /** Scheduler-specific configuration properties. */
  @Data
  public static class Scheduler {
    /**
     * Whether the sync scheduler is enabled. When disabled, automatic sync will not run, but manual
     * sync is still available.
     */
    private boolean enabled = false;

    /** Interval between automatic sync operations in milliseconds. Default: 1 hour (3600000ms) */
    private long interval = 3600000L; // 1 hour

    /**
     * Initial delay before the first sync operation in milliseconds. Default: 5 minutes (300000ms)
     */
    private long initialDelay = 300000L; // 5 minutes
  }

  /** Backup-specific configuration properties. */
  @Data
  public static class Backup {
    /**
     * Whether to create backups of original JSON files before sync. Recommended to keep enabled for
     * data safety.
     */
    private boolean enabled = true;

    /** Directory to store backup files. Relative to the application's working directory. */
    private String directory = "backups/notion-sync";

    /**
     * Maximum number of backup files to keep per entity. Older backups will be automatically
     * deleted.
     */
    private int maxFiles = 10;
  }

  /**
   * Check if a specific entity should be synchronized. With automatic dependency analysis, all
   * entities are enabled when sync is enabled.
   *
   * @param entityName The name of the entity to check
   * @return true if sync is enabled (all entities are automatically included)
   */
  public boolean isEntityEnabled(String entityName) {
    return enabled; // All entities are automatically included when sync is enabled
  }

  /**
   * Check if automatic scheduling is fully enabled.
   *
   * @return true if both sync and scheduler are enabled
   */
  public boolean isSchedulerEnabled() {
    return enabled && scheduler.enabled;
  }

  /**
   * Check if backup functionality is enabled.
   *
   * @return true if backup is enabled
   */
  public boolean isBackupEnabled() {
    return backup.enabled;
  }

  /**
   * Updates the last sync time for a specific entity.
   *
   * @param entityName The name of the entity.
   * @param syncTime The time of synchronization.
   */
  public void setLastSyncTime(String entityName, LocalDateTime syncTime) {
    lastSyncTimes.put(entityName, syncTime);
  }

  /**
   * Retrieves the last sync time for a specific entity.
   *
   * @param entityName The name of the entity.
   * @return The last sync time, or null if it has never been synced.
   */
  public LocalDateTime getLastSyncTime(String entityName) {
    return lastSyncTimes.get(entityName);
  }
}
