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
package com.github.javydreamercsw.base.ai;

import java.time.Duration;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * Configuration for retry policies used by AI segment narration services. Allows each provider to
 * define custom retry behavior based on their specific error conditions.
 */
@Data
@AllArgsConstructor
public class RetryPolicyConfig {

  /** Maximum number of retry attempts */
  private final int maxRetries;

  /** Base delay between retries */
  private final Duration baseDelay;

  /** Maximum delay between retries (for exponential backoff) */
  private final Duration maxDelay;

  /** Predicate to determine if an exception should trigger a retry */
  private final Predicate<Exception> shouldRetry;

  /** Description of this retry policy for logging purposes */
  private final String description;

  /** Creates a simple retry policy with fixed delay. */
  public static RetryPolicyConfig fixedDelay(
      int maxRetries,
      Duration delay,
      @NonNull Predicate<Exception> shouldRetry,
      @NonNull String description) {
    return new RetryPolicyConfig(maxRetries, delay, delay, shouldRetry, description);
  }

  /** Creates a retry policy with exponential backoff. */
  public static RetryPolicyConfig exponentialBackoff(
      int maxRetries,
      Duration baseDelay,
      Duration maxDelay,
      @NonNull Predicate<Exception> shouldRetry,
      @NonNull String description) {
    return new RetryPolicyConfig(maxRetries, baseDelay, maxDelay, shouldRetry, description);
  }
}
