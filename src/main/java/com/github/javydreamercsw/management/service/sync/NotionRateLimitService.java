package com.github.javydreamercsw.management.service.sync;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Rate limiting service for Notion API calls. Implements token bucket algorithm to respect Notion's
 * rate limits.
 */
@Slf4j
@Service
public class NotionRateLimitService {

  // Notion's documented rate limits (conservative values)
  private static final int MAX_REQUESTS_PER_SECOND = 3; // Conservative limit
  private static final int BURST_CAPACITY = 10; // Allow small bursts
  private static final long REFILL_INTERVAL_MS = 1000; // 1 second

  private final Semaphore permits;
  private final AtomicLong lastRefillTime;
  private volatile int availableTokens;

  public NotionRateLimitService() {
    this.permits = new Semaphore(BURST_CAPACITY, true); // Fair semaphore
    this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    this.availableTokens = BURST_CAPACITY;
  }

  /**
   * Acquire a permit for making a Notion API call. This method will block if rate limit is
   * exceeded.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void acquirePermit() throws InterruptedException {
    refillTokens();

    // Try to acquire permit with timeout to avoid infinite blocking
    boolean acquired = permits.tryAcquire(10, TimeUnit.SECONDS);
    if (!acquired) {
      log.warn("Failed to acquire rate limit permit within 10 seconds");
      throw new RuntimeException("Rate limit timeout - unable to acquire permit");
    }

    log.debug("Rate limit permit acquired. Available permits: {}", permits.availablePermits());
  }

  /**
   * Acquire a permit with a custom timeout.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return true if permit was acquired, false if timeout occurred
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean tryAcquirePermit(long timeout, TimeUnit unit) throws InterruptedException {
    refillTokens();
    return permits.tryAcquire(timeout, unit);
  }

  /**
   * Handle a 429 response by implementing exponential backoff and respecting the Retry-After
   * header.
   *
   * @param retryAfterSeconds the Retry-After value from the 429 response, or 0 if not available
   * @throws InterruptedException if interrupted while waiting
   */
  public void handle429Response(int retryAfterSeconds) throws InterruptedException {
    long delayMs;

    if (retryAfterSeconds > 0) {
      // Respect the Retry-After header
      delayMs = retryAfterSeconds * 1000L;
      log.warn("Received 429 response, respecting Retry-After: {} seconds", retryAfterSeconds);
    } else {
      // Use exponential backoff as fallback
      delayMs = calculateBackoffDelay();
      log.warn("Received 429 response, applying exponential backoff: {}ms", delayMs);
    }

    // Drain remaining permits to slow down requests
    permits.drainPermits();

    // Wait for the specified delay
    Thread.sleep(delayMs);

    // Refill tokens after waiting
    refillTokens();
  }

  /** Get the current number of available permits. */
  public int getAvailablePermits() {
    return permits.availablePermits();
  }

  /** Reset the rate limiter (useful for testing or manual intervention). */
  public void reset() {
    permits.drainPermits();
    permits.release(BURST_CAPACITY);
    lastRefillTime.set(System.currentTimeMillis());
    availableTokens = BURST_CAPACITY;
    log.info("Rate limiter reset");
  }

  /** Refill tokens based on elapsed time (token bucket algorithm). */
  private void refillTokens() {
    long now = System.currentTimeMillis();
    long lastRefill = lastRefillTime.get();
    long elapsed = now - lastRefill;

    if (elapsed >= REFILL_INTERVAL_MS) {
      // Calculate how many tokens to add
      int tokensToAdd = (int) (elapsed / REFILL_INTERVAL_MS) * MAX_REQUESTS_PER_SECOND;

      if (tokensToAdd > 0) {
        // Release permits up to the burst capacity
        int currentPermits = permits.availablePermits();
        int permitsToRelease = Math.min(tokensToAdd, BURST_CAPACITY - currentPermits);

        if (permitsToRelease > 0) {
          permits.release(permitsToRelease);
          log.debug(
              "Refilled {} rate limit tokens. Total available: {}",
              permitsToRelease,
              permits.availablePermits());
        }

        // Update last refill time
        lastRefillTime.set(now);
      }
    }
  }

  /** Calculate exponential backoff delay for rate limiting. */
  private long calculateBackoffDelay() {
    // Start with 1 second, can be made configurable
    long baseDelay = 1000;

    // Add some jitter to avoid thundering herd
    double jitter = 0.1 + (Math.random() * 0.2); // 10-30% jitter

    return (long) (baseDelay * (1 + jitter));
  }
}
