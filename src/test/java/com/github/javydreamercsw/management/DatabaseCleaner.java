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

import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.Method;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatically discovers and clears all JPA repositories in the correct dependency order. Uses
 * EntityDependencyAnalyzer to determine the safe deletion order based on entity relationships.
 */
@Service
@Slf4j
public class DatabaseCleaner {

  @Autowired private ApplicationContext applicationContext;
  @Autowired private EntityDependencyAnalyzer dependencyAnalyzer;
  @Autowired private EntityManager entityManager;

  /**
   * Clears all repositories in the correct dependency order. Automatically discovers all
   * JpaRepository beans and deletes their data in reverse dependency order (children before
   * parents).
   */
  @Transactional
  public void clearRepositories() {
    log.info("🧹 Starting database cleanup...");

    // Discover all repositories
    Map<String, JpaRepository<?, ?>> repositories = discoverRepositories();
    log.info("📦 Discovered {} repositories", repositories.size());

    // Get entity classes and determine deletion order
    Set<Class<?>> entityClasses = getEntityClasses();
    List<String> syncOrder = dependencyAnalyzer.determineSyncOrder(entityClasses);

    // Reverse the order for deletion (delete children before parents)
    Collections.reverse(syncOrder);

    log.info("🔄 Deletion order: {}", syncOrder);

    // Delete data in the correct order
    int deletedCount = 0;
    for (String entityName : syncOrder) {
      JpaRepository<?, ?> repository = repositories.get(entityName.toLowerCase());
      if (repository != null) {
        long count = repository.count();
        if (count > 0) {
          try {
            repository.deleteAll();
            log.debug("✅ Deleted {} records from {}", count, entityName);
            deletedCount++;
          } catch (Exception e) {
            log.warn("⚠️ Error deleting from {}: {}", entityName, e.getMessage());
            // Try deleteAllInBatch as fallback
            try {
              repository.deleteAllInBatch();
              log.debug("✅ Deleted (batch) {} records from {}", count, entityName);
              deletedCount++;
            } catch (Exception e2) {
              log.error("❌ Failed to delete from {}: {}", entityName, e2.getMessage());
            }
          }
        }
      }
    }

    // Clean up any remaining repositories not in the entity list
    for (Map.Entry<String, JpaRepository<?, ?>> entry : repositories.entrySet()) {
      if (!syncOrder.contains(entry.getKey())) {
        try {
          long count = entry.getValue().count();
          if (count > 0) {
            entry.getValue().deleteAll();
            log.debug(
                "✅ Deleted {} records from {} (not in dependency graph)", count, entry.getKey());
            deletedCount++;
          }
        } catch (Exception e) {
          log.warn("⚠️ Error deleting from {}: {}", entry.getKey(), e.getMessage());
        }
      }
    }

    log.info("✨ Database cleanup completed. Cleared {} repositories", deletedCount);
  }

  /**
   * Discovers all JpaRepository beans in the application context. Returns a map of entity name
   * (lowercase) to repository instance.
   */
  private Map<String, JpaRepository<?, ?>> discoverRepositories() {
    Map<String, JpaRepository<?, ?>> repositories = new HashMap<>();
    String[] beanNames = applicationContext.getBeanNamesForType(JpaRepository.class);

    for (String beanName : beanNames) {
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

    // Strategy 2: Try to find the entity class from repository methods
    try {
      Method findAllMethod = repository.getClass().getMethod("findAll");
      Class<?> returnType = findAllMethod.getReturnType();
      if (returnType != null) {
        // This is a fallback, might not always work perfectly
        return returnType.getSimpleName().toLowerCase();
      }
    } catch (Exception e) {
      // Ignore
    }

    return null;
  }

  /** Gets all entity classes registered with the EntityManager. */
  private Set<Class<?>> getEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();
    Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();

    for (EntityType<?> entity : entities) {
      entityClasses.add(entity.getJavaType());
    }

    return entityClasses;
  }
}
