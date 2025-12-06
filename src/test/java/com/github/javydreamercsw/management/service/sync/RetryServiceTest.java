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
package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.config.RetryConfig;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for RetryService to verify retry logic, exponential backoff, and error handling. */
@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

  private RetryService retryService;
  private RetryConfig retryConfig;

  @BeforeEach
  void setUp() {
    retryConfig = new RetryConfig();
    retryConfig.setMaxAttempts(3);
    retryConfig.setInitialDelayMs(100);
    retryConfig.setMaxDelayMs(1000);
    retryConfig.setBackoffMultiplier(2.0);
    retryConfig.setUseJitter(false); // Disable jitter for predictable tests

    retryService = new RetryService(retryConfig);
  }

  @Test
  void shouldSucceedOnFirstAttempt() throws Exception {
    // Given
    String expectedResult = "success";

    // When
    String result =
        retryService.executeWithRetry(
            "test",
            (RetryService.AttemptCallable<String>)
                (attemptNumber) -> {
                  assertThat(attemptNumber).isEqualTo(1);
                  return expectedResult;
                });

    // Then
    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void shouldRetryOnRetryableException() throws Exception {
    // Given
    AtomicInteger attemptCount = new AtomicInteger(0);
    String expectedResult = "success";

    // When
    String result =
        retryService.executeWithRetry(
            "test",
            (RetryService.AttemptCallable<String>)
                (attemptNumber) -> {
                  int currentAttempt = attemptCount.incrementAndGet();
                  if (currentAttempt < 3) {
                    throw new SocketTimeoutException("Connection timeout");
                  }
                  return expectedResult;
                });

    // Then
    assertThat(result).isEqualTo(expectedResult);
    assertThat(attemptCount.get()).isEqualTo(3);
  }

  @Test
  void shouldFailAfterMaxAttempts() {
    // Given
    AtomicInteger attemptCount = new AtomicInteger(0);

    // When & Then
    assertThatThrownBy(
            () ->
                retryService.executeWithRetry(
                    "test",
                    (RetryService.AttemptCallable<String>)
                        (attemptNumber) -> {
                          attemptCount.incrementAndGet();
                          throw new SocketTimeoutException("Connection timeout");
                        }))
        .isInstanceOf(SocketTimeoutException.class)
        .hasMessage("Connection timeout");

    assertThat(attemptCount.get()).isEqualTo(3); // Max attempts
  }

  @Test
  void shouldNotRetryOnNonRetryableException() {
    // Given
    AtomicInteger attemptCount = new AtomicInteger(0);

    // When & Then
    assertThatThrownBy(
            () ->
                retryService.executeWithRetry(
                    "test",
                    (RetryService.AttemptCallable<String>)
                        (attemptNumber) -> {
                          attemptCount.incrementAndGet();
                          throw new IllegalArgumentException("Bad request - 400");
                        }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Bad request - 400");

    assertThat(attemptCount.get()).isEqualTo(1); // Only one attempt
  }

  @Test
  void shouldUseEntitySpecificConfiguration() throws Exception {
    // Given
    retryConfig.getEntities().getShows().setMaxAttempts(5);
    AtomicInteger attemptCount = new AtomicInteger(0);
    String expectedResult = "success";

    // When
    String result =
        retryService.executeWithRetry(
            "shows",
            (RetryService.AttemptCallable<String>)
                (attemptNumber) -> {
                  int currentAttempt = attemptCount.incrementAndGet();
                  if (currentAttempt < 5) {
                    throw new SocketTimeoutException("Connection timeout");
                  }
                  return expectedResult;
                });

    // Then
    assertThat(result).isEqualTo(expectedResult);
    assertThat(attemptCount.get()).isEqualTo(5);
  }

  @Test
  void shouldCreateRetryContext() {
    // When
    RetryService.RetryContext context = retryService.createContext("test", "Test Operation");

    // Then
    assertThat(context.getEntityType()).isEqualTo("test");
    assertThat(context.getOperationName()).isEqualTo("Test Operation");
    assertThat(context.getMaxAttempts()).isEqualTo(3);
    assertThat(context.getCurrentAttempt()).isEqualTo(0);
    assertThat(context.hasMoreAttempts()).isTrue();
  }

  @Test
  void shouldTrackRetryContext() throws InterruptedException {
    // Given
    RetryService.RetryContext context = retryService.createContext("test", "Test Operation");
    Exception testException = new RuntimeException("Test error");

    // When
    Thread.sleep(1); // Ensure some time passes
    context.recordAttempt(2, testException);

    // Then
    assertThat(context.getCurrentAttempt()).isEqualTo(2);
    assertThat(context.getLastException()).isEqualTo(testException);
    assertThat(context.hasMoreAttempts()).isTrue();
    assertThat(context.getElapsedTime()).isGreaterThanOrEqualTo(0);
  }
}
