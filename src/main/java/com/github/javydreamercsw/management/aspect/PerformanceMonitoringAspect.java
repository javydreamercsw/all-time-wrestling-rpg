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
package com.github.javydreamercsw.management.aspect;

import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring performance of critical operations. Automatically tracks execution times
 * and logs slow operations.
 */
// @Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringAspect {

  private final PerformanceMonitoringService performanceMonitoringService;

  /** Annotation to mark methods for performance monitoring. */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MonitorPerformance {
    /** Custom operation name for monitoring. If not specified, uses class.method format. */
    String value() default "";

    /** Threshold in milliseconds for logging slow operations. */
    long slowThreshold() default 1000;
  }

  /** Monitors performance of methods annotated with @MonitorPerformance. */
  @Around("@annotation(monitorPerformance)")
  public Object monitorMethodPerformance(
      ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) throws Throwable {
    String operationName = getOperationName(joinPoint, monitorPerformance);

    performanceMonitoringService.startOperation(operationName);

    try {
      Object result = joinPoint.proceed();
      performanceMonitoringService.endOperation(operationName);
      return result;
    } catch (Exception e) {
      performanceMonitoringService.incrementCounter("operations.failed." + operationName);
      performanceMonitoringService.endOperation(operationName);
      throw e;
    }
  }

  /**
   * Monitors performance of all service methods automatically. Excludes
   * PerformanceMonitoringService to avoid circular dependencies.
   */
  @Around(
      "execution(* com.github.javydreamercsw.management.service..*(..)) && !execution(*"
          + " com.github.javydreamercsw.management.service.performance..*(..))")
  public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operationName = className + "." + methodName;

    long startTime = System.currentTimeMillis();

    try {
      Object result = joinPoint.proceed();

      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer("service.methods." + operationName, duration);

      // Log slow service methods
      if (duration > 500) { // More than 500ms
        log.warn("⚠️ Slow service method: {} took {}ms", operationName, duration);
        performanceMonitoringService.incrementCounter("service.slow." + operationName);
      }

      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer(
          "service.methods." + operationName + ".failed", duration);
      performanceMonitoringService.incrementCounter("service.errors." + operationName);
      throw e;
    }
  }

  /** Monitors performance of repository methods (database operations). */
  @Around("execution(* com.github.javydreamercsw.management.domain..*Repository.*(..))")
  public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operationName = className + "." + methodName;

    long startTime = System.currentTimeMillis();

    try {
      Object result = joinPoint.proceed();

      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer("repository.methods." + operationName, duration);
      performanceMonitoringService.incrementCounter("repository.calls." + operationName);

      // Log slow database operations
      if (duration > 100) { // More than 100ms for database operations
        log.warn("⚠️ Slow database operation: {} took {}ms", operationName, duration);
        performanceMonitoringService.incrementCounter("repository.slow." + operationName);
      }

      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer(
          "repository.methods." + operationName + ".failed", duration);
      performanceMonitoringService.incrementCounter("repository.errors." + operationName);
      throw e;
    }
  }

  /**
   * Monitors performance of controller methods (API endpoints). Excludes performance-related
   * controllers to avoid circular dependencies.
   */
  @Around(
      "execution(* com.github.javydreamercsw.management.controller..*(..)) && !execution(*"
          + " com.github.javydreamercsw.management.controller.system.PerformanceController.*(..))")
  public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operationName = className + "." + methodName;

    long startTime = System.currentTimeMillis();

    try {
      Object result = joinPoint.proceed();

      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer("controller.methods." + operationName, duration);
      performanceMonitoringService.incrementCounter("controller.requests." + operationName);

      // Log slow API endpoints
      if (duration > 2000) { // More than 2 seconds for API calls
        log.warn("⚠️ Slow API endpoint: {} took {}ms", operationName, duration);
        performanceMonitoringService.incrementCounter("controller.slow." + operationName);
      }

      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer(
          "controller.methods." + operationName + ".failed", duration);
      performanceMonitoringService.incrementCounter("controller.errors." + operationName);
      throw e;
    }
  }

  /** Monitors Notion sync operations specifically. */
  @Around("execution(* com.github.javydreamercsw.management.service.sync..*(..))")
  public Object monitorSyncMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operationName = "sync." + className + "." + methodName;

    long startTime = System.currentTimeMillis();

    try {
      Object result = joinPoint.proceed();

      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer("sync.operations." + operationName, duration);
      performanceMonitoringService.incrementCounter("sync.success." + operationName);

      // Log sync operations (they can be long-running)
      if (duration > 10000) { // More than 10 seconds
        log.info("📊 Long sync operation: {} took {}ms", operationName, duration);
      }

      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      performanceMonitoringService.recordTimer(
          "sync.operations." + operationName + ".failed", duration);
      performanceMonitoringService.incrementCounter("sync.errors." + operationName);
      log.error("❌ Sync operation failed: {} after {}ms", operationName, duration);
      throw e;
    }
  }

  /** Gets the operation name from the join point and annotation. */
  private String getOperationName(
      ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) {
    if (!monitorPerformance.value().isEmpty()) {
      return monitorPerformance.value();
    }

    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    return className + "." + methodName;
  }
}
