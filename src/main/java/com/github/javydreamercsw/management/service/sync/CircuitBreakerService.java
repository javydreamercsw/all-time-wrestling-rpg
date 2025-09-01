package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.RetryConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Circuit breaker implementation for sync operations. Prevents cascading failures by temporarily
 * stopping calls to failing services.
 */
@Slf4j
@Service
public class CircuitBreakerService {

  private final RetryConfig retryConfig;
  private final ConcurrentHashMap<String, CircuitBreakerState> circuitStates =
      new ConcurrentHashMap<>();

  public CircuitBreakerService(RetryConfig retryConfig) {
    this.retryConfig = retryConfig;
  }

  /**
   * Execute an operation with circuit breaker protection.
   *
   * @param entityType The type of entity being synced
   * @param operation The operation to execute
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws CircuitBreakerOpenException if circuit is open
   */
  public <T> T execute(String entityType, SyncOperation<T> operation) throws Exception {
    CircuitBreakerState state = getOrCreateState(entityType);

    // Check if circuit is open
    if (state.isOpen()) {
      if (state.shouldAttemptReset()) {
        log.info("Circuit breaker for {} attempting reset", entityType);
        state.halfOpen();
      } else {
        log.warn("Circuit breaker for {} is OPEN - rejecting call", entityType);
        throw new CircuitBreakerOpenException("Circuit breaker is open for " + entityType);
      }
    }

    try {
      T result = operation.execute();
      state.recordSuccess();

      if (state.isHalfOpen() && state.shouldClose()) {
        log.info("Circuit breaker for {} closing after successful recovery", entityType);
        state.close();
      }

      return result;
    } catch (Exception e) {
      state.recordFailure();

      if (state.shouldOpen()) {
        log.warn("Circuit breaker for {} opening due to failures", entityType);
        state.open();
      }

      throw e;
    }
  }

  /** Get the current state of a circuit breaker. */
  public CircuitBreakerStatus getStatus(String entityType) {
    CircuitBreakerState state = circuitStates.get(entityType);
    if (state == null) {
      return CircuitBreakerStatus.CLOSED;
    }
    return state.getStatus();
  }

  /** Manually reset a circuit breaker. */
  public void reset(String entityType) {
    CircuitBreakerState state = circuitStates.get(entityType);
    if (state != null) {
      state.reset();
      log.info("Circuit breaker for {} manually reset", entityType);
    }
  }

  /** Get or create circuit breaker state for an entity type. */
  private CircuitBreakerState getOrCreateState(String entityType) {
    return circuitStates.computeIfAbsent(
        entityType, k -> new CircuitBreakerState(retryConfig.getCircuitBreaker()));
  }

  /** Functional interface for sync operations. */
  @FunctionalInterface
  public interface SyncOperation<T> {
    T execute() throws Exception;
  }

  /** Circuit breaker states. */
  public enum CircuitBreakerStatus {
    CLOSED, // Normal operation
    OPEN, // Rejecting calls
    HALF_OPEN // Testing if service has recovered
  }

  /** Exception thrown when circuit breaker is open. */
  public static class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
      super(message);
    }
  }

  /** Internal state management for circuit breaker. */
  private static class CircuitBreakerState {
    private final RetryConfig.CircuitBreakerConfig config;
    private volatile CircuitBreakerStatus status = CircuitBreakerStatus.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger callCount = new AtomicInteger(0);

    public CircuitBreakerState(RetryConfig.CircuitBreakerConfig config) {
      this.config = config;
    }

    public boolean isOpen() {
      return status == CircuitBreakerStatus.OPEN;
    }

    public boolean isHalfOpen() {
      return status == CircuitBreakerStatus.HALF_OPEN;
    }

    public boolean shouldAttemptReset() {
      return isOpen()
          && (System.currentTimeMillis() - lastFailureTime.get()) > config.getRecoveryTimeoutMs();
    }

    public boolean shouldOpen() {
      return status == CircuitBreakerStatus.CLOSED
          && failureCount.get() >= config.getFailureThreshold();
    }

    public boolean shouldClose() {
      if (!isHalfOpen()) {
        return false;
      }

      int totalCalls = callCount.get();
      if (totalCalls < config.getEvaluationWindow()) {
        return false;
      }

      double successRate = (double) successCount.get() / totalCalls;
      return successRate >= config.getSuccessThreshold();
    }

    public void recordSuccess() {
      if (isHalfOpen()) {
        successCount.incrementAndGet();
        callCount.incrementAndGet();
      } else if (status == CircuitBreakerStatus.CLOSED) {
        failureCount.set(0); // Reset failure count on success
      }
    }

    public void recordFailure() {
      failureCount.incrementAndGet();
      lastFailureTime.set(System.currentTimeMillis());

      if (isHalfOpen()) {
        callCount.incrementAndGet();
      }
    }

    public void open() {
      status = CircuitBreakerStatus.OPEN;
      lastFailureTime.set(System.currentTimeMillis());
    }

    public void halfOpen() {
      status = CircuitBreakerStatus.HALF_OPEN;
      successCount.set(0);
      callCount.set(0);
    }

    public void close() {
      status = CircuitBreakerStatus.CLOSED;
      failureCount.set(0);
      successCount.set(0);
      callCount.set(0);
    }

    public void reset() {
      close();
    }

    public CircuitBreakerStatus getStatus() {
      return status;
    }
  }
}
