package com.github.javydreamercsw.management.config;

import java.util.List;
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

  // Entities to sync are now automatically determined based on database relationships
  // Keeping deprecated methods for backward compatibility with existing tests
  @Deprecated(forRemoval = true)
  private List<String> entities = List.of(); // Empty list - not used anymore

  /** Scheduler configuration for automatic synchronization. */
  private Scheduler scheduler = new Scheduler();

  /** Backup configuration for protecting original files. */
  private Backup backup = new Backup();

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

  // ==================== DEPRECATED METHODS FOR BACKWARD COMPATIBILITY ====================
  // These methods are kept for existing tests but should not be used in new code

  /**
   * @deprecated Entities are now automatically determined based on database relationships. This
   *     method returns an empty list and should not be used.
   */
  @Deprecated(forRemoval = true)
  public List<String> getEntities() {
    return entities; // Returns empty list
  }

  /**
   * @deprecated Entities are now automatically determined based on database relationships. This
   *     method does nothing and should not be used.
   */
  @Deprecated(forRemoval = true)
  public void setEntities(List<String> entities) {
    // Do nothing - entities are automatically determined
  }
}
