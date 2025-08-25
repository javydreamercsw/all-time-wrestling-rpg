package com.github.javydreamercsw.management.service.sync;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Analyzes JPA entity relationships to automatically determine sync order. Uses reflection to
 * examine entity annotations and build a dependency graph.
 */
@Component
@Slf4j
public class EntityDependencyAnalyzer {

  @Autowired private ApplicationContext applicationContext;

  /**
   * Automatically determines sync order based on known entity relationships. Uses hardcoded
   * knowledge of entity dependencies for now, but can be enhanced with full reflection-based
   * analysis later.
   *
   * @return List of sync method names in dependency order
   */
  public List<String> getAutomaticSyncOrder() {
    // For now, use known dependency relationships
    // TODO: Enhance with full reflection-based analysis
    return getKnownDependencyOrder();
  }

  /**
   * Returns the known dependency order based on current entity relationships. This is a simplified
   * version that can be enhanced with full reflection later.
   */
  private List<String> getKnownDependencyOrder() {
    // TODO replace with smart code, not hardcoded
    List<String> order = new ArrayList<>();

    // Base entities with no dependencies
    order.add("templates"); // ShowTemplate (no dependencies)
    order.add("seasons"); // Season (no dependencies)
    order.add("injury-types"); // InjuryType (no dependencies, reference data)

    // Entities that depend on base entities
    order.add("shows"); // Show (depends on ShowTemplate, Season)
    order.add("wrestlers"); // Wrestler (may depend on Faction, but can be synced independently)

    // Entities that depend on wrestlers
    order.add("factions"); // Faction (depends on Wrestler for leader and members)
    order.add("teams"); // Team (depends on Wrestler for members)

    // Complex entities that depend on multiple others
    order.add("matches"); // Match (depends on Show, Wrestler, Team, Faction)

    log.info("🎯 Using known dependency order: {}", order);
    return order;
  }

  /** Discovers all JPA entity classes in the application. */
  private Set<Class<?>> discoverEntityClasses() {
    Set<Class<?>> entityClasses = new HashSet<>();

    // Get all beans and check if they're entities
    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      try {
        Class<?> beanClass = applicationContext.getType(beanName);
        if (beanClass != null && beanClass.isAnnotationPresent(Entity.class)) {
          entityClasses.add(beanClass);
        }
      } catch (Exception e) {
        // Skip beans that can't be analyzed
        log.debug("Skipping bean {} during entity discovery: {}", beanName, e.getMessage());
      }
    }

    // Also scan domain packages directly
    entityClasses.addAll(scanDomainPackages());

    log.info(
        "🔍 Discovered {} entity classes: {}",
        entityClasses.size(),
        entityClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));

    return entityClasses;
  }

  /** Scans domain packages for entity classes. */
  private Set<Class<?>> scanDomainPackages() {
    Set<Class<?>> entities = new HashSet<>();

    // List of known domain packages to scan
    String[] domainPackages = {
      "com.github.javydreamercsw.management.domain.show",
      "com.github.javydreamercsw.management.domain.wrestler",
      "com.github.javydreamercsw.management.domain.faction",
      "com.github.javydreamercsw.management.domain.team",
      "com.github.javydreamercsw.management.domain.match",
      "com.github.javydreamercsw.management.domain.season"
    };

    for (String packageName : domainPackages) {
      try {
        // This is a simplified approach - in a real implementation you might use
        // ClassPathScanningCandidateComponentProvider or similar
        log.debug("Scanning package: {}", packageName);
      } catch (Exception e) {
        log.debug("Error scanning package {}: {}", packageName, e.getMessage());
      }
    }

    return entities;
  }

  /**
   * Analyzes entity classes and returns them in dependency order (dependencies first). Uses
   * topological sorting based on JPA relationship annotations.
   *
   * @param entityClasses Set of entity classes to analyze
   * @return List of entity names in sync order (dependencies first)
   */
  public List<String> determineSyncOrder(Set<Class<?>> entityClasses) {
    log.info("🔍 Analyzing {} entity classes for dependency relationships", entityClasses.size());

    // Build dependency graph
    Map<String, Set<String>> dependencies = buildDependencyGraph(entityClasses);

    // Perform topological sort
    List<String> syncOrder = topologicalSort(dependencies);

    log.info("📋 Determined sync order: {}", syncOrder);
    return syncOrder;
  }

  /**
   * Builds a dependency graph by analyzing JPA annotations. Key = entity name, Value = set of
   * entities this entity depends on
   */
  private Map<String, Set<String>> buildDependencyGraph(Set<Class<?>> entityClasses) {
    Map<String, Set<String>> dependencies = new HashMap<>();
    Map<String, Class<?>> entityNameToClass =
        entityClasses.stream().collect(Collectors.toMap(this::getEntityName, clazz -> clazz));

    for (Class<?> entityClass : entityClasses) {
      String entityName = getEntityName(entityClass);
      Set<String> entityDependencies = new HashSet<>();

      // Analyze all fields for relationship annotations
      for (Field field : getAllFields(entityClass)) {
        analyzeForeignKeyDependencies(field, entityDependencies, entityNameToClass);
      }

      dependencies.put(entityName, entityDependencies);
      log.debug("Entity '{}' depends on: {}", entityName, entityDependencies);
    }

    return dependencies;
  }

  /** Analyzes a field for foreign key relationships that create dependencies. */
  private void analyzeForeignKeyDependencies(
      Field field, Set<String> dependencies, Map<String, Class<?>> entityNameToClass) {
    // @ManyToOne - this entity depends on the referenced entity
    if (field.isAnnotationPresent(ManyToOne.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @ManyToOne dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }

    // @JoinColumn - indicates a foreign key dependency
    if (field.isAnnotationPresent(JoinColumn.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @JoinColumn dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }

    // @OneToOne with @JoinColumn - this entity owns the relationship
    if (field.isAnnotationPresent(OneToOne.class) && field.isAnnotationPresent(JoinColumn.class)) {
      String referencedEntity = getEntityNameFromFieldType(field, entityNameToClass);
      if (referencedEntity != null) {
        dependencies.add(referencedEntity);
        log.debug("Found @OneToOne dependency: {} -> {}", field.getName(), referencedEntity);
      }
    }
  }

  /** Gets the entity name from a field's type. */
  private String getEntityNameFromFieldType(Field field, Map<String, Class<?>> entityNameToClass) {
    Class<?> fieldType = field.getType();

    // Handle Collection types (OneToMany, ManyToMany)
    if (Collection.class.isAssignableFrom(fieldType)) {
      // For collections, we don't create dependencies (they're usually bidirectional)
      return null;
    }

    // Check if the field type is one of our entities
    for (Map.Entry<String, Class<?>> entry : entityNameToClass.entrySet()) {
      if (entry.getValue().equals(fieldType)) {
        return entry.getKey();
      }
    }

    return null;
  }

  /** Gets all fields from a class including inherited fields. */
  private List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> currentClass = clazz;

    while (currentClass != null && currentClass != Object.class) {
      fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
      currentClass = currentClass.getSuperclass();
    }

    return fields;
  }

  /** Gets the entity name from a class (uses @Entity name or class simple name). */
  private String getEntityName(Class<?> entityClass) {
    Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
    if (entityAnnotation != null && !entityAnnotation.name().isEmpty()) {
      return entityAnnotation.name().toLowerCase();
    }
    return entityClass.getSimpleName().toLowerCase();
  }

  /**
   * Performs topological sort on the dependency graph. Returns entities in dependency order
   * (dependencies first).
   */
  private List<String> topologicalSort(Map<String, Set<String>> dependencies) {
    List<String> result = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();

    for (String entity : dependencies.keySet()) {
      if (!visited.contains(entity)) {
        topologicalSortVisit(entity, dependencies, visited, visiting, result);
      }
    }

    return result;
  }

  /** Recursive helper for topological sort with cycle detection. */
  private void topologicalSortVisit(
      String entity,
      Map<String, Set<String>> dependencies,
      Set<String> visited,
      Set<String> visiting,
      List<String> result) {
    if (visiting.contains(entity)) {
      log.warn("⚠️ Circular dependency detected involving entity: {}", entity);
      return;
    }

    if (visited.contains(entity)) {
      return;
    }

    visiting.add(entity);

    // Visit all dependencies first
    Set<String> entityDeps = dependencies.getOrDefault(entity, Collections.emptySet());
    for (String dependency : entityDeps) {
      if (dependencies.containsKey(dependency)) {
        topologicalSortVisit(dependency, dependencies, visited, visiting, result);
      }
    }

    visiting.remove(entity);
    visited.add(entity);
    result.add(entity);
  }

  /** Maps common entity names to sync method names. */
  public String mapEntityToSyncMethod(String entityName) {
    return switch (entityName.toLowerCase()) {
      case "showtemplate" -> "templates";
      case "season" -> "seasons";
      case "show" -> "shows";
      case "wrestler" -> "wrestlers";
      case "faction" -> "factions";
      case "team" -> "teams";
      case "match" -> "matches";
      default -> entityName.toLowerCase() + "s"; // Default pluralization
    };
  }
}
