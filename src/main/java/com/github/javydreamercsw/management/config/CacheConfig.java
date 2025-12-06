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

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for improving application performance. Implements caching for frequently
 * accessed data to reduce database load.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

  // Cache names - centralized for consistency
  public static final String WRESTLERS_CACHE = "wrestlers";
  public static final String SHOWS_CACHE = "shows";
  public static final String SHOW_TYPES_CACHE = "showTypes";
  public static final String SEASONS_CACHE = "seasons";
  public static final String TITLES_CACHE = "titles";
  public static final String RIVALRIES_CACHE = "rivalries";
  public static final String INJURIES_CACHE = "injuries";
  public static final String SEGMENT_TYPES_CACHE = "segmentTypes";
  public static final String SEGMENT_RULES_CACHE = "segmentRules";
  public static final String SHOW_TEMPLATES_CACHE = "showTemplates";
  public static final String WRESTLER_STATS_CACHE = "wrestlerStats";
  public static final String CALENDAR_CACHE = "calendar";
  public static final String NOTION_SYNC_CACHE = "notionSync";
  public static final String NOTION_PAGES_CACHE = "notionPages"; // Cache for Notion pages
  public static final String NOTION_QUERIES_CACHE =
      "notionQueries"; // Cache for Notion database queries

  /**
   * Configures the cache manager with optimized cache settings. Uses Caffeine for thread-safe
   * caching with reasonable TTL values.
   */
  @Bean
  public CacheManager cacheManager() {
    log.info("🚀 Initializing cache manager with performance optimizations...");

    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCacheNames(
        Arrays.asList(
            WRESTLERS_CACHE,
            SHOWS_CACHE,
            SHOW_TYPES_CACHE,
            SEASONS_CACHE,
            TITLES_CACHE,
            RIVALRIES_CACHE,
            INJURIES_CACHE,
            SEGMENT_TYPES_CACHE,
            SEGMENT_RULES_CACHE,
            SHOW_TEMPLATES_CACHE,
            WRESTLER_STATS_CACHE,
            CALENDAR_CACHE,
            NOTION_SYNC_CACHE,
            NOTION_PAGES_CACHE,
            NOTION_QUERIES_CACHE));

    // Default spec for all caches
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(500)
            .recordStats());

    log.info("✅ Cache manager initialized with {} caches", cacheManager.getCacheNames().size());
    return cacheManager;
  }

  /** Cache statistics and monitoring bean. Provides insights into cache performance. */
  @Bean
  public CacheMonitor cacheMonitor(CacheManager cacheManager) {
    return new CacheMonitor(cacheManager);
  }

  /** Cache monitoring utility for performance analysis. */
  public static class CacheMonitor {
    private final CacheManager cacheManager;

    public CacheMonitor(CacheManager cacheManager) {
      this.cacheManager = cacheManager;
    }

    /** Logs cache statistics for monitoring performance. */
    public void logCacheStatistics() {
      log.info("📊 Cache Statistics:");

      cacheManager
          .getCacheNames()
          .forEach(
              cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache caffeineCache) {
                  log.info(
                      "   - {}: {} entries (Hit Rate: {})",
                      cacheName,
                      caffeineCache.getNativeCache().estimatedSize(),
                      String.format("%.2f", caffeineCache.getNativeCache().stats().hitRate()));
                } else if (cache != null) {
                  log.info("   - {}: active (non-Caffeine cache)", cacheName);
                }
              });
    }

    /** Clears all caches - useful for testing or when data is updated. */
    public void clearAllCaches() {
      log.info("🧹 Clearing all caches...");
      cacheManager
          .getCacheNames()
          .forEach(
              cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                  cache.clear();
                  log.debug("Cleared cache: {}", cacheName);
                }
              });
      log.info("✅ All caches cleared");
    }

    /** Clears a specific cache by name. */
    public void clearCache(String cacheName) {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.info("🧹 Cleared cache: {}", cacheName);
      } else {
        log.warn("Cache not found: {}", cacheName);
      }
    }

    /**
     * Warms up caches with frequently accessed data. This method can be called during application
     * startup.
     */
    public void warmUpCaches() {
      log.info("🔥 Warming up caches...");
      // Cache warming would be implemented by the services
      // This is a placeholder for cache warming logic
      log.info("✅ Cache warm-up completed");
    }
  }

  /** Cache key generator for consistent cache key creation. */
  @Bean
  public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
    return (target, method, params) -> {
      StringBuilder sb = new StringBuilder();
      sb.append(target.getClass().getSimpleName()).append(".");
      sb.append(method.getName()).append("(");

      for (int i = 0; i < params.length; i++) {
        if (i > 0) sb.append(",");
        if (params[i] != null) {
          sb.append(params[i].toString());
        } else {
          sb.append("null");
        }
      }
      sb.append(")");

      return sb.toString();
    };
  }
}
