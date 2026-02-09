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

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Database optimization configuration that creates indexes and optimizes database performance. This
 * configuration runs after the application is ready to ensure all tables are created.
 */
@Configuration
@Slf4j
@Order(100)
@DependsOn("flyway")
public class DatabaseOptimizationConfig implements ApplicationRunner {

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseOptimizationConfig(DataSource dataSource, JdbcTemplate jdbcTemplate) {
    this.dataSource = dataSource;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // Run all post-startup tasks
    createDatabaseIndexes();
    optimizeConnectionSettings();
    analyzeQueryPerformance();
  }

  /**
   * Creates database indexes after the application is ready. This ensures all JPA entities have
   * been processed and tables created.
   */
  public void createDatabaseIndexes() {
    log.info("🚀 Starting database optimization - creating indexes...");

    try (Connection connection = dataSource.getConnection()) {
      // Execute the database optimization script
      ScriptUtils.executeSqlScript(
          connection, new ClassPathResource("db/optimization/indexes.sql"));

      log.info("✅ Database indexes created successfully");

      // Log current database statistics
      log.info("📊 Database Statistics: {}", getDatabaseStatistics());

    } catch (Exception e) {
      log.warn("⚠️ Failed to create some database indexes: {}", e.getMessage());
      log.debug("Index creation error details", e);
    }
  }

  /** Logs database statistics for monitoring performance. */
  public java.util.Map<String, Object> getDatabaseStatistics() {
    java.util.Map<String, Object> stats = new java.util.HashMap<>();
    try {
      // Get table counts for key entities
      stats.put(
          "Wrestlers", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wrestler", Integer.class));
      stats.put(
          "Shows",
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wrestling_show", Integer.class));
      stats.put(
          "Rivalries", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rivalry", Integer.class));
      stats.put(
          "Injury Types",
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM injury_type", Integer.class));
      stats.put(
          "Factions", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM faction", Integer.class));
      stats.put(
          "Show Templates",
          jdbcTemplate.queryForObject("SELECT COUNT(*) FROM show_template", Integer.class));
      stats.put("Decks", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deck", Integer.class));
      stats.put("Cards", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM card", Integer.class));
      stats.put(
          "Segments", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM segment", Integer.class));

      // Check if indexes exist (H2 specific query)
      try {
        Integer indexCount =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC'",
                Integer.class);
        stats.put("Database Indexes", indexCount);
      } catch (Exception e) {
        log.debug("Could not retrieve index count: {}", e.getMessage());
      }

    } catch (Exception e) {
      log.debug("Could not retrieve database statistics: {}", e.getMessage());
    }
    return stats;
  }

  /**
   * Analyzes query performance and suggests optimizations. This method can be called periodically
   * to monitor performance.
   *
   * @return A list of performance suggestions and warnings.
   */
  public java.util.List<String> analyzeQueryPerformance() {
    log.info("🔍 Analyzing query performance...");
    java.util.List<String> suggestions = new java.util.ArrayList<>();

    try {
      // Check for slow queries (H2 doesn't have built-in slow query log, but we can check table
      // sizes)
      suggestions.addAll(checkLargeTablePerformance());

      // Suggest optimizations based on data patterns
      suggestions.addAll(suggestOptimizations());

    } catch (Exception e) {
      log.warn("Failed to analyze query performance: {}", e.getMessage());
      suggestions.add("Error analyzing performance: " + e.getMessage());
    }
    return suggestions;
  }

  private java.util.List<String> checkLargeTablePerformance() {
    java.util.List<String> warnings = new java.util.ArrayList<>();
    try {
      // Check tables that might benefit from additional indexes
      String[] largeTables = {
        "wrestler", "wrestling_show", "rivalry", "injury", "multi_wrestler_feud"
      };

      for (String table : largeTables) {
        try {
          Integer count =
              jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);

          if (count != null && count > 1000) {
            String msg =
                String.format(
                    "⚡ Large table detected: %s (%d rows) - ensure proper indexing", table, count);
            log.info(msg);
            warnings.add(msg);
          }
        } catch (Exception e) {
          log.debug("Could not check table size for {}: {}", table, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.debug("Error checking large table performance: {}", e.getMessage());
    }
    return warnings;
  }

  private java.util.List<String> suggestOptimizations() {
    java.util.List<String> tips = new java.util.ArrayList<>();
    tips.add("💡 Performance Optimization Suggestions:");
    tips.add("   - Enable query caching for frequently accessed data");
    tips.add("   - Consider read replicas for heavy read workloads");
    tips.add("   - Monitor slow queries and add indexes as needed");
    tips.add("   - Use pagination for large result sets");
    tips.add("   - Implement connection pooling for high concurrency");

    tips.forEach(log::info);
    return tips;
  }

  /** Optimizes database connection settings for better performance. */
  public void optimizeConnectionSettings() {
    try {
      // Set optimal H2 database settings for performance
      jdbcTemplate.execute("SET DB_CLOSE_DELAY -1"); // Keep database open
      jdbcTemplate.execute("SET CACHE_SIZE 65536"); // 64MB cache
      jdbcTemplate.execute("SET LOCK_TIMEOUT 10000"); // 10 second lock timeout

      log.info("✅ Database connection settings optimized");

    } catch (Exception e) {
      log.debug("Could not optimize connection settings: {}", e.getMessage());
    }
  }
}
