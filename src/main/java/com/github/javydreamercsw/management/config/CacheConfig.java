package com.github.javydreamercsw.management.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
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

    SimpleCacheManager cacheManager = new SimpleCacheManager();

    // Configure caches with different characteristics based on usage patterns
    cacheManager.setCaches(
        Arrays.asList(
            // Frequently accessed, rarely changed data - longer TTL
            createCaffeineCache(WRESTLERS_CACHE, 100, 60), // Up to 1000 wrestlers, 10 min TTL
            createCaffeineCache(
                SHOW_TYPES_CACHE, 1_000, 60), // Limited number of show types, 60 min TTL
            createCaffeineCache(SEASONS_CACHE, 100, 60), // Limited number of seasons, 60 min TTL
            createCaffeineCache(TITLES_CACHE, 100, 60), // Limited number of titles, 60 min TTL
            createCaffeineCache(
                SEGMENT_TYPES_CACHE, 50, 60), // Limited number of segment types, 60 min TTL
            createCaffeineCache(
                SEGMENT_RULES_CACHE, 100, 60), // Limited number of segment rules, 60 min TTL

            // Moderately accessed data - medium TTL
            createCaffeineCache(SHOWS_CACHE, 2000, 5), // Up to 2000 shows, 5 min TTL
            createCaffeineCache(
                SHOW_TEMPLATES_CACHE, 100, 30), // Limited number of templates, 30 min TTL
            createCaffeineCache(RIVALRIES_CACHE, 500, 15), // Up to 500 active rivalries, 15 min TTL

            // Frequently changing data - shorter TTL
            createCaffeineCache(INJURIES_CACHE, 1000, 2), // Injuries change frequently, 2 min TTL
            createCaffeineCache(
                WRESTLER_STATS_CACHE, 1000, 1), // Stats updated after segments, 1 min TTL

            // Computed/aggregated data - can be expensive to recalculate
            createCaffeineCache(CALENDAR_CACHE, 100, 5), // Calendar views, 5 min TTL
            createCaffeineCache(NOTION_SYNC_CACHE, 50, 1), // Notion sync status, 1 min TTL

            // Notion API Caches
            createCaffeineCache(NOTION_PAGES_CACHE, 5000, 5), // Cache Notion pages for 5 minutes
            createCaffeineCache(NOTION_QUERIES_CACHE, 1000, 2) // Cache Notion queries for 2 minutes
            ));

    log.info("✅ Cache manager initialized with {} caches", cacheManager.getCacheNames().size());
    return cacheManager;
  }

  /** Creates a Caffeine cache with the specified name, maximum size, and TTL. */
  private Cache createCaffeineCache(String name, int maxSize, int ttlMinutes) {
    log.debug("Creating Caffeine cache: {} (max size: {}, TTL: {} min)", name, maxSize, ttlMinutes);
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build());
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
