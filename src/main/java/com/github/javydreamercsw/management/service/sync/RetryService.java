package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.RetryConfig;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling retry logic with exponential backoff and jitter. Provides configurable retry
 * mechanisms for sync operations.
 */
@Slf4j
@Service
public class RetryService {

  private final RetryConfig retryConfig;
  private final Random random = new Random();

  public RetryService(RetryConfig retryConfig) {
    this.retryConfig = retryConfig;
  }

  /**
   * Execute an operation with retry logic.
   *
   * @param entityType The type of entity being processed
   * @param operation The operation to execute
   * @param retryableExceptionPredicate Predicate to determine if exception is retryable
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws Exception if all retry attempts fail
   */
  public <T> T executeWithRetry(
      String entityType,
      RetryableOperation<T> operation,
      Predicate<Exception> retryableExceptionPredicate)
      throws Exception {

    int maxAttempts = retryConfig.getMaxAttempts(entityType);
    long initialDelay = retryConfig.getInitialDelayMs(entityType);
    long maxDelay = retryConfig.getMaxDelayMs(entityType);

    Exception lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        log.debug("Executing {} operation, attempt {}/{}", entityType, attempt, maxAttempts);
        return operation.execute(attempt);

      } catch (Exception e) {
        lastException = e;

        // Check if this is the last attempt
        if (attempt == maxAttempts) {
          log.error("All {} retry attempts failed for {}", maxAttempts, entityType, e);
          break;
        }

        // Check if exception is retryable
        if (!retryableExceptionPredicate.test(e)) {
          log.warn("Non-retryable exception for {}, not retrying", entityType, e);
          throw e;
        }

        // Calculate delay for next attempt
        long delay = calculateDelay(attempt, initialDelay, maxDelay);

        log.warn(
            "Attempt {}/{} failed for {}, retrying in {}ms: {}",
            attempt,
            maxAttempts,
            entityType,
            delay,
            e.getMessage());

        try {
          TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Retry interrupted", ie);
        }
      }
    }

    throw lastException;
  }

  /** Execute an operation with retry logic using default retryable exception predicate. */
  public <T> T executeWithRetry(String entityType, RetryableOperation<T> operation)
      throws Exception {
    return executeWithRetry(entityType, operation, this::isRetryableException);
  }

  /** Calculate delay for next retry attempt using exponential backoff with jitter. */
  private long calculateDelay(int attempt, long initialDelay, long maxDelay) {
    // Calculate exponential backoff
    long delay = (long) (initialDelay * Math.pow(retryConfig.getBackoffMultiplier(), attempt - 1));

    // Apply maximum delay limit
    delay = Math.min(delay, maxDelay);

    // Apply jitter if enabled
    if (retryConfig.isUseJitter()) {
      double jitterFactor = retryConfig.getJitterFactor();
      double jitter =
          1.0
              + (random.nextDouble() * 2 - 1)
                  * jitterFactor; // Random between (1-jitter) and (1+jitter)
      delay = (long) (delay * jitter);
    }

    return Math.max(delay, 0);
  }

  /**
   * Default predicate to determine if an exception is retryable. Generally, network issues,
   * timeouts, and temporary service unavailability are retryable.
   */
  private boolean isRetryableException(Exception e) {
    String message = e.getMessage();
    if (message == null) {
      message = "";
    }

    // Network and connection issues
    if (e instanceof java.net.SocketTimeoutException
        || e instanceof java.net.ConnectException
        || e instanceof java.net.UnknownHostException
        || e instanceof java.io.IOException) {
      return true;
    }

    // HTTP-related errors that are typically retryable
    if (message.contains("timeout")
        || message.contains("connection")
        || message.contains("network")
        || message.contains("503")
        || // Service Unavailable
        message.contains("502")
        || // Bad Gateway
        message.contains("504")
        || // Gateway Timeout
        message.contains("429")) { // Too Many Requests
      return true;
    }

    // Notion API specific errors that might be retryable
    if (message.contains("rate limit")
        || message.contains("temporarily unavailable")
        || message.contains("internal server error")) {
      return true;
    }

    // Authentication and authorization errors are generally not retryable
    if (message.contains("401")
        || // Unauthorized
        message.contains("403")
        || // Forbidden
        message.contains("invalid token")
        || message.contains("authentication")) {
      return false;
    }

    // Client errors (4xx) are generally not retryable except for specific cases above
    if (message.contains("400")
        || // Bad Request
        message.contains("404")
        || // Not Found
        message.contains("422")) { // Unprocessable Entity
      return false;
    }

    // Default to retryable for unknown exceptions
    return true;
  }

  /** Create a retry context for tracking retry attempts. */
  public RetryContext createContext(String entityType, String operationName) {
    return new RetryContext(entityType, operationName, retryConfig.getMaxAttempts(entityType));
  }

  /** Functional interface for retryable operations. */
  @FunctionalInterface
  public interface RetryableOperation<T> {
    T execute(int attemptNumber) throws Exception;
  }

  /** Context for tracking retry attempts. */
  public static class RetryContext {
    private final String entityType;
    private final String operationName;
    private final int maxAttempts;
    private int currentAttempt = 0;
    private long startTime = System.currentTimeMillis();
    private Exception lastException;

    public RetryContext(String entityType, String operationName, int maxAttempts) {
      this.entityType = entityType;
      this.operationName = operationName;
      this.maxAttempts = maxAttempts;
    }

    public void recordAttempt(int attemptNumber, Exception exception) {
      this.currentAttempt = attemptNumber;
      this.lastException = exception;
    }

    public boolean hasMoreAttempts() {
      return currentAttempt < maxAttempts;
    }

    public long getElapsedTime() {
      return System.currentTimeMillis() - startTime;
    }

    // Getters
    public String getEntityType() {
      return entityType;
    }

    public String getOperationName() {
      return operationName;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public int getCurrentAttempt() {
      return currentAttempt;
    }

    public Exception getLastException() {
      return lastException;
    }
  }
}
