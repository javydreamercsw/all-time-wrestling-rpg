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
package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.service.sync.SyncEntityType;
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
    private EntityConfig Factions = new EntityConfig();
    private EntityConfig teams = new EntityConfig();
    private EntityConfig segments = new EntityConfig();
    private EntityConfig templates = new EntityConfig();
    private EntityConfig seasons = new EntityConfig();
    private EntityConfig showTypes = new EntityConfig();
    private EntityConfig Injuries = new EntityConfig();
    private EntityConfig npcs = new EntityConfig();
    private EntityConfig titles = new EntityConfig();
    private EntityConfig rivalries = new EntityConfig();
    private EntityConfig factionRivalries = new EntityConfig();
    private EntityConfig titleReigns = new EntityConfig();
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
    return SyncEntityType.fromKey(entityType)
        .map(this::getEntityConfig)
        .orElseGet(EntityConfig::new);
  }

  /** Get entity-specific configuration using SyncEntityType enum. */
  private EntityConfig getEntityConfig(SyncEntityType entityType) {
    return switch (entityType) {
      case SHOWS -> entities.getShows();
      case WRESTLERS -> entities.getWrestlers();
      case FACTIONS -> entities.getFactions();
      case TEAMS -> entities.getTeams();
      case SEGMENTS -> entities.getSegments();
      case TEMPLATES -> entities.getTemplates();
      case SEASONS -> entities.getSeasons();
      case SHOW_TYPES -> entities.getShowTypes();
      case INJURY_TYPES -> entities.getInjuries();
      case NPCS -> entities.getNpcs();
      case TITLES -> entities.getTitles();
      case RIVALRIES -> entities.getRivalries();
      case FACTION_RIVALRIES -> entities.getFactionRivalries();
      case TITLE_REIGN -> entities.getTitleReigns();
      default -> new EntityConfig(); // Default config
    };
  }
}
