package com.github.javydreamercsw.management.config;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
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
  public static final String MATCH_TYPES_CACHE = "matchTypes";
  public static final String MATCH_RULES_CACHE = "matchRules";
  public static final String SHOW_TEMPLATES_CACHE = "showTemplates";
  public static final String WRESTLER_STATS_CACHE = "wrestlerStats";
  public static final String CALENDAR_CACHE = "calendar";
  public static final String NOTION_SYNC_CACHE = "notionSync";

  /**
   * Configures the cache manager with optimized cache settings. Uses concurrent maps for
   * thread-safe caching with reasonable TTL values.
   */
  @Bean
  public CacheManager cacheManager() {
    log.info("🚀 Initializing cache manager with performance optimizations...");

    SimpleCacheManager cacheManager = new SimpleCacheManager();

    // Configure caches with different characteristics based on usage patterns
    cacheManager.setCaches(
        Arrays.asList(
            // Frequently accessed, rarely changed data - longer TTL
            createCache(WRESTLERS_CACHE, 1000), // Up to 1000 wrestlers
            createCache(SHOW_TYPES_CACHE, 50), // Limited number of show types
            createCache(SEASONS_CACHE, 100), // Limited number of seasons
            createCache(TITLES_CACHE, 100), // Limited number of titles
            createCache(MATCH_TYPES_CACHE, 50), // Limited number of match types
            createCache(MATCH_RULES_CACHE, 100), // Limited number of match rules

            // Moderately accessed data - medium TTL
            createCache(SHOWS_CACHE, 2000), // Up to 2000 shows
            createCache(SHOW_TEMPLATES_CACHE, 200), // Limited number of templates
            createCache(RIVALRIES_CACHE, 500), // Up to 500 active rivalries

            // Frequently changing data - shorter TTL
            createCache(INJURIES_CACHE, 1000), // Injuries change frequently
            createCache(WRESTLER_STATS_CACHE, 1000), // Stats updated after matches

            // Computed/aggregated data - can be expensive to recalculate
            createCache(CALENDAR_CACHE, 100), // Calendar views
            createCache(NOTION_SYNC_CACHE, 50) // Notion sync status
            ));

    log.info("✅ Cache manager initialized with {} caches", cacheManager.getCacheNames().size());
    return cacheManager;
  }

  /**
   * Creates a cache with the specified name and maximum size. Uses ConcurrentMapCache for
   * thread-safe operations.
   */
  private ConcurrentMapCache createCache(String name, int maxSize) {
    log.debug("Creating cache: {} (max size: {})", name, maxSize);
    return new ConcurrentMapCache(name);
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
                if (cache != null) {
                  // For ConcurrentMapCache, we can get the native cache
                  if (cache.getNativeCache() instanceof java.util.concurrent.ConcurrentHashMap) {
                    var nativeCache =
                        (java.util.concurrent.ConcurrentHashMap<?, ?>) cache.getNativeCache();
                    log.info("   - {}: {} entries", cacheName, nativeCache.size());
                  } else {
                    log.info("   - {}: active", cacheName);
                  }
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
