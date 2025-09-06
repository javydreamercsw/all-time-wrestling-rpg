package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.RetryConfig;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.exception.NotionAPIError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RetryService {
  private final RetryConfig config;
  private static final Pattern RETRY_AFTER_PATTERN =
      Pattern.compile("retry-after[:\\s]+([0-9]+)", Pattern.CASE_INSENSITIVE);

  @Autowired
  public RetryService(RetryConfig config) {
    this.config = config;
  }

  @FunctionalInterface
  public interface AttemptCallable<T> {
    T call(int attemptNumber) throws Exception;
  }

  public <T> T executeWithRetry(String entityType, AttemptCallable<T> callable) throws Exception {
    int maxAttempts = config.getMaxAttempts();
    // Support entity-specific config
    if ("shows".equals(entityType)
        && config.getEntities() != null
        && config.getEntities().getShows() != null) {
      maxAttempts = config.getEntities().getShows().getMaxAttempts();
    }
    long delay = config.getInitialDelayMs();
    double backoff = config.getBackoffMultiplier();
    Exception lastException = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return callable.call(attempt);
      } catch (SocketTimeoutException e) {
        lastException = e;
        if (attempt == maxAttempts) throw e;
        log.warn(
            "Attempt {}/{} failed for {} due to timeout, retrying in {}ms: {}",
            attempt,
            maxAttempts,
            entityType,
            delay,
            e.getMessage());
        Thread.sleep(delay);
        delay = Math.min((long) (delay * backoff), config.getMaxDelayMs());
      } catch (NotionAPIError e) {
        lastException = e;

        // Handle 429 rate limiting with Retry-After header
        if (isRateLimitError(e)) {
          if (attempt == maxAttempts) throw e;

          long retryAfterMs = extractRetryAfterDelay(e);
          if (retryAfterMs > 0) {
            log.warn(
                "Attempt {}/{} failed for {} due to rate limiting (429), respecting Retry-After:"
                    + " {}ms",
                attempt,
                maxAttempts,
                entityType,
                retryAfterMs);
            Thread.sleep(retryAfterMs);
          } else {
            log.warn(
                "Attempt {}/{} failed for {} due to rate limiting, retrying in {}ms: {}",
                attempt,
                maxAttempts,
                entityType,
                delay,
                e.getMessage());
            Thread.sleep(delay);
            delay = Math.min((long) (delay * backoff), config.getMaxDelayMs());
          }
        } else if (e.getMessage().contains("rate_limited")) {
          // Fallback for other rate limiting errors
          if (attempt == maxAttempts) throw e;
          log.warn(
              "Attempt {}/{} failed for {} due to rate limiting, retrying in {}ms: {}",
              attempt,
              maxAttempts,
              entityType,
              delay,
              e.getMessage());
          Thread.sleep(delay);
          delay = Math.min((long) (delay * backoff), config.getMaxDelayMs());
        } else {
          log.error("Non-retryable Notion API error for {}: {}", entityType, e.getMessage());
          throw e;
        }
      } catch (Exception e) {
        log.error("Unexpected error during {}: {}", entityType, e.getMessage());
        throw e;
      }
    }
    throw lastException != null
        ? lastException
        : new RuntimeException(
            "Failed to complete " + entityType + " after " + maxAttempts + " retries.");
  }

  /** Check if the error is a rate limiting error (HTTP 429) */
  private boolean isRateLimitError(@NonNull NotionAPIError error) {
    String message = error.getMessage().toLowerCase();
    return message.contains("429")
        || message.contains("rate limit")
        || message.contains("too many requests");
  }

  /**
   * Extract the Retry-After delay from the error message or headers Returns delay in milliseconds,
   * or 0 if not found
   */
  private long extractRetryAfterDelay(@NonNull NotionAPIError error) {
    String message = error.getMessage();

    Matcher matcher = RETRY_AFTER_PATTERN.matcher(message);
    if (matcher.find()) {
      try {
        int seconds = Integer.parseInt(matcher.group(1));
        return seconds * 1000L; // Convert to milliseconds
      } catch (NumberFormatException e) {
        log.warn("Failed to parse Retry-After value: {}", matcher.group(1));
      }
    }

    return 0;
  }

  public RetryContext createContext(@NonNull String entityType, @NonNull String operationName) {
    int maxAttempts = config.getMaxAttempts();
    if ("shows".equals(entityType)
        && config.getEntities() != null
        && config.getEntities().getShows() != null) {
      maxAttempts = config.getEntities().getShows().getMaxAttempts();
    }
    return new RetryContext(entityType, operationName, maxAttempts);
  }

  @Getter
  @Setter
  public static class RetryContext {
    private final String entityType;
    private final String operationName;
    private final int maxAttempts;
    private int currentAttempt = 0;
    private Exception lastException;
    private final long startTime = System.currentTimeMillis();

    public RetryContext(
        @NonNull String entityType, @NonNull String operationName, int maxAttempts) {
      this.entityType = entityType;
      this.operationName = operationName;
      this.maxAttempts = maxAttempts;
    }

    public long getElapsedTime() {
      return System.currentTimeMillis() - startTime;
    }

    public void recordAttempt(int attempt, Exception e) {
      this.currentAttempt = attempt;
      this.lastException = e;
    }

    public boolean hasMoreAttempts() {
      return currentAttempt < maxAttempts;
    }
  }
}
