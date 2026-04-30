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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.JoinTable;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementation of DatabaseCleanup using Spring's ApplicationContext to find all repositories and
 * clear them in the correct order.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseCleaner implements DatabaseCleanup {

  private final ApplicationContext applicationContext;
  private final AccountInitializer accountInitializer;
  private final EntityDependencyAnalyzer dependencyAnalyzer;
  private final JdbcTemplate jdbcTemplate;

  @PersistenceContext private EntityManager entityManager;

  @Override
  public void clearRepositories() {
    log.info("🧹 Starting database cleanup...");

    // Ensure we have admin privileges for this manual execution
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          // 1. First break known circular dependencies and clear tables with many-to-many
          // relationships
          breakCircularDependencies();

          // 2. Discover all JPA repositories
          Map<String, JpaRepository<?, ?>> repositories = discoverRepositories();
          log.debug("📦 Discovered {} repositories", repositories.size());

          // 3. Get the correct synchronization/deletion order
          List<String> syncOrder = dependencyAnalyzer.determineSyncOrder(getEntityClasses());
          log.info("🔄 Deletion order: {}", syncOrder);

          // Entities that should not be cleared (static config, core roles, etc.)
          Set<String> protectedEntities =
              new HashSet<>(
                  Arrays.asList(
                      "showtype",
                      "segmenttype",
                      "segmentrule",
                      "cardset",
                      "card",
                      "campaignabilitycard",
                      "campaignupgrade",
                      "holiday",
                      "injurytype"));

          // Delete data in the correct order (reverse of sync order)
          int deletedCount = 0;
          for (int i = syncOrder.size() - 1; i >= 0; i--) {
            String entityName = syncOrder.get(i);
            if (protectedEntities.contains(entityName.toLowerCase())) {
              log.debug("🛡️ Skipping protected entity: {}", entityName);
              continue;
            }

            JpaRepository<?, ?> repository = repositories.get(entityName.toLowerCase());
            if (repository != null) {
              try {
                if (repository.count() > 0) {
                  repository.deleteAllInBatch();
                  resetSequence(entityName);
                  deletedCount++;
                }
              } catch (Exception e) {
                log.warn("Could not clear repository {}: {}", entityName, e.getMessage());
              }
            }
          }

          // Clean up any remaining repositories not in the entity list
          for (Map.Entry<String, JpaRepository<?, ?>> entry : repositories.entrySet()) {
            if (!syncOrder.contains(entry.getKey())
                && !protectedEntities.contains(entry.getKey().toLowerCase())) {
              try {
                if (entry.getValue().count() > 0) {
                  entry.getValue().deleteAllInBatch();
                  resetSequence(entry.getKey());
                  deletedCount++;
                }
              } catch (Exception e) {
                log.trace(
                    "Could not clear remaining repository {}: {}", entry.getKey(), e.getMessage());
              }
            }
          }

          // Re-initialize accounts to ensure admin, booker, etc. are available
          accountInitializer.init();
          entityManager.flush();
          entityManager.clear();
          log.info("✨ Database cleanup completed. Cleared {} repositories", deletedCount);
        });
  }

  /**
   * Manually breaks known circular dependencies by setting foreign keys to NULL. This is necessary
   * before batch deletion to avoid constraint violations.
   */
  private void breakCircularDependencies() {
    log.debug("💔 Breaking circular dependencies...");
    try {
      jdbcTemplate.execute(
          "UPDATE campaign_state SET active_storyline_id = NULL, current_match_id = NULL");
      log.debug("✅ Nullified campaign_state links");
    } catch (Exception e) {
      log.warn("Could not nullify campaign_state links: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute("UPDATE campaign_storyline SET current_milestone_id = NULL");
      log.debug("✅ Nullified campaign_storyline.current_milestone_id");
    } catch (Exception e) {
      log.warn("Could not nullify campaign_storyline.current_milestone_id: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute(
          "UPDATE storyline_milestone SET next_on_success_id = NULL, next_on_failure_id = NULL,"
              + " storyline_id = NULL");
      log.debug("✅ Nullified storyline_milestone links");
    } catch (Exception e) {
      log.warn("Could not nullify storyline_milestone links: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute("DELETE FROM storyline_milestone");
      jdbcTemplate.execute("DELETE FROM campaign_storyline");
      log.debug("✅ Manually cleared storyline tables");
    } catch (Exception e) {
      log.warn("Could not manually clear storyline tables: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute("UPDATE faction SET leader_id = NULL, manager_id = NULL");
      log.debug("✅ Nullified faction links");
    } catch (Exception e) {
      log.warn("Could not nullify faction links: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute("DELETE FROM league_roster");
      jdbcTemplate.execute("DELETE FROM league_membership");
      log.debug("✅ Cleared league_roster and league_membership");
    } catch (Exception e) {
      log.warn("Could not clear league tables: {}", e.getMessage());
    }
    try {
      jdbcTemplate.execute("DELETE FROM heat_event");
      jdbcTemplate.execute("DELETE FROM wrestler_state");
      jdbcTemplate.execute("DELETE FROM team");
      jdbcTemplate.execute("DELETE FROM wrestler_relationship");
      log.debug("✅ Cleared heat_event, wrestler_state, team and wrestler_relationship tables");
    } catch (Exception e) {
      log.warn("Could not clear relationship tables: {}", e.getMessage());
    }
  }

  /**
   * Discovers and clears all join tables defined with @JoinTable annotation. This is necessary for
   * Many-to-Many relationships that don't have a dedicated repository.
   */
  private void clearJoinTables() {
    Set<String> joinTableNames = new HashSet<>();
    joinTableNames.add("heat_event");
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entity : entities) {
      for (Attribute<?, ?> attribute : entity.getAttributes()) {
        if (attribute.isCollection()) {
          Member member = attribute.getJavaMember();
          if (member instanceof AnnotatedElement annotatedElement) {
            JoinTable joinTable = annotatedElement.getAnnotation(JoinTable.class);
            if (joinTable != null) {
              // Skip account_roles to prevent deleting user roles
              if (!"account_roles".equalsIgnoreCase(joinTable.name())) {
                joinTableNames.add(joinTable.name());
              }
            }
          }
        }
      }
    }

    if (!joinTableNames.isEmpty()) {
      log.debug("🧹 Clearing {} join tables...", joinTableNames.size());
      for (String tableName : joinTableNames) {
        try {
          log.debug("Deleting all records from join table {}", tableName);
          // Use uppercase to match database schema and no quotes to avoid case-sensitivity issues
          entityManager
              .createNativeQuery("DELETE FROM " + tableName.toUpperCase(Locale.ROOT))
              .executeUpdate();
          log.debug("✅ Deleted all records from join table {}", tableName);
        } catch (Exception e) {
          log.error("❌ Failed to delete from join table {}: {}", tableName, e.getMessage());
        }
      }
    }
  }

  private void resetSequence(String tableName) {
    try {
      String sql = "ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH 1";
      if (tableName.equalsIgnoreCase("wrestler")) {
        sql = "ALTER TABLE wrestler ALTER COLUMN wrestler_id RESTART WITH 1";
      } else if (tableName.equalsIgnoreCase("show")) {
        sql = "ALTER TABLE show ALTER COLUMN show_id RESTART WITH 1";
      }

      jdbcTemplate.execute(sql);
      log.trace("✅ Reset sequence for table: {}", tableName);
    } catch (Exception e) {
      log.trace("Could not reset sequence for table {}: {}", tableName, e.getMessage());
    }
  }

  private Set<Class<?>> getEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();
    Set<jakarta.persistence.metamodel.EntityType<?>> entities =
        entityManager.getMetamodel().getEntities();

    for (jakarta.persistence.metamodel.EntityType<?> entity : entities) {
      entityClasses.add(entity.getJavaType());
    }

    return entityClasses;
  }

  /**
   * Discovers all JpaRepository beans in the application context. Returns a map of entity name
   * (lowercase) to repository instance.
   */
  private Map<String, JpaRepository<?, ?>> discoverRepositories() {
    Map<String, JpaRepository<?, ?>> repositories = new HashMap<>();
    String[] beanNames = applicationContext.getBeanNamesForType(JpaRepository.class);

    for (String beanName : beanNames) {
      // Skip account and role repositories to avoid deleting test users
      if (beanName.equals("accountRepository") || beanName.equals("roleRepository")) {
        continue;
      }
      try {
        JpaRepository<?, ?> repository = applicationContext.getBean(beanName, JpaRepository.class);

        // Extract entity name from repository
        String entityName = extractEntityName(repository, beanName);
        if (entityName != null) {
          repositories.put(entityName.toLowerCase(), repository);
          log.debug("📍 Mapped repository {} to entity {}", beanName, entityName);
        }
      } catch (Exception e) {
        log.debug("⚠️ Skipping bean {}: {}", beanName, e.getMessage());
      }
    }

    return repositories;
  }

  /**
   * Extracts the entity name from a repository bean. Tries multiple strategies: bean name pattern,
   * repository interface generics.
   */
  private String extractEntityName(JpaRepository<?, ?> repository, String beanName) {
    // Strategy 1: Bean name pattern (e.g., "cardRepository" -> "card")
    if (beanName.endsWith("Repository")) {
      return beanName.substring(0, beanName.length() - "Repository".length());
    }

    // Strategy 2: Fallback to manual replacement if strategy 1 fails for some reason
    return beanName.replace("Repository", "").toLowerCase();
  }
}
