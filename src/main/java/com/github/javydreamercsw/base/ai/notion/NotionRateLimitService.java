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
package com.github.javydreamercsw.base.ai.notion;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Rate limiting service for Notion API calls. Implements token bucket algorithm to respect Notion's
 * rate limits.
 */
@Slf4j
@Service
public class NotionRateLimitService {

  private static final int MAX_REQUESTS_PER_SECOND = 3; // Notion's documented rate limit
  private static final int BURST_CAPACITY = 10; // Allow small bursts

  private final Semaphore permits;
  private final ScheduledExecutorService scheduler;

  public NotionRateLimitService() {
    this.permits = new Semaphore(BURST_CAPACITY, true); // Fair semaphore
    this.scheduler = Executors.newScheduledThreadPool(1);
    // Schedule a task to add permits every second, starting after an initial delay
    this.scheduler.scheduleAtFixedRate(this::refillPermits, 1, 1, TimeUnit.SECONDS);
  }

  /**
   * Acquire a permit for making a Notion API call. This method will block if rate limit is
   * exceeded.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void acquirePermit() throws InterruptedException {
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
  }

  /** Get the current number of available permits. */
  public int getAvailablePermits() {
    return permits.availablePermits();
  }

  /**
   * Check if the rate limiter is currently active (i.e., has no available permits).
   *
   * @return true if rate limited, false otherwise
   */
  public boolean isRateLimited() {
    return permits.availablePermits() == 0;
  }

  /** Reset the rate limiter (useful for testing or manual intervention). */
  public void reset() {
    permits.drainPermits();
    permits.release(BURST_CAPACITY);
    log.info("Rate limiter reset");
  }

  /** Refill permits periodically. */
  private void refillPermits() {
    try {
      int permitsToRelease = MAX_REQUESTS_PER_SECOND;
      int currentAvailable = permits.availablePermits();

      // Do not exceed burst capacity
      if (currentAvailable + permitsToRelease > BURST_CAPACITY) {
        permitsToRelease = BURST_CAPACITY - currentAvailable;
      }

      if (permitsToRelease > 0) {
        permits.release(permitsToRelease);
        log.debug(
            "Refilled {} rate limit tokens. Total available: {}",
            permitsToRelease,
            permits.availablePermits());
      }
    } catch (Exception e) {
      log.error("Error refilling rate limit permits", e);
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

  /** Shutdown the scheduler (should be called during application shutdown). */
  @PreDestroy
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
