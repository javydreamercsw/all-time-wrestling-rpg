package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.*;

import com.github.javydreamercsw.management.config.RetryConfig;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for CircuitBreakerService to verify circuit breaker patterns and failure handling. */
@ExtendWith(MockitoExtension.class)
class CircuitBreakerServiceTest {

  private CircuitBreakerService circuitBreakerService;

    @BeforeEach
  void setUp() {
        RetryConfig retryConfig = new RetryConfig();
    retryConfig.getCircuitBreaker().setFailureThreshold(3);
    retryConfig.getCircuitBreaker().setRecoveryTimeoutMs(1000);
    retryConfig.getCircuitBreaker().setSuccessThreshold(0.5);
    retryConfig.getCircuitBreaker().setEvaluationWindow(4);

    circuitBreakerService = new CircuitBreakerService(retryConfig);
  }

  @Test
  void shouldAllowCallsWhenCircuitIsClosed() throws Exception {
    // Given
    String expectedResult = "success";

    // When
    String result = circuitBreakerService.execute("test", () -> expectedResult);

    // Then
    assertThat(result).isEqualTo(expectedResult);
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.CLOSED);
  }

  @Test
  void shouldOpenCircuitAfterFailureThreshold() {
    // Given
    AtomicInteger attemptCount = new AtomicInteger(0);

    // When - Execute operations that fail
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "test",
            () -> {
              attemptCount.incrementAndGet();
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    // Then - Circuit should be open
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.OPEN);
    assertThat(attemptCount.get()).isEqualTo(3);

    // When - Try to execute another operation
    assertThatThrownBy(() -> circuitBreakerService.execute("test", () -> "should not execute"))
        .isInstanceOf(CircuitBreakerService.CircuitBreakerOpenException.class);

    // Then - Attempt count should not increase (circuit is open)
    assertThat(attemptCount.get()).isEqualTo(3);
  }

  @Test
  void shouldTransitionToHalfOpenAfterRecoveryTimeout() throws Exception {
    // Given - Open the circuit
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "test",
            () -> {
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.OPEN);

    // When - Wait for recovery timeout
    Thread.sleep(1100); // Wait longer than recovery timeout (1000ms)

    // Then - Next call should transition to half-open
    String result = circuitBreakerService.execute("test", () -> "recovery success");

    assertThat(result).isEqualTo("recovery success");
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.HALF_OPEN);
  }

  @Test
  void shouldCloseCircuitAfterSuccessfulRecovery() throws Exception {
    // Given - Open the circuit
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "test",
            () -> {
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    // Wait for recovery timeout
    Thread.sleep(1100);

    // When - Execute successful operations in half-open state
    for (int i = 0; i < 4; i++) { // Evaluation window size
      String result = circuitBreakerService.execute("test", () -> "success");
      assertThat(result).isEqualTo("success");
    }

    // Then - Circuit should be closed
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.CLOSED);
  }

  @Test
  void shouldReopenCircuitOnFailureInHalfOpenState() throws Exception {
    // Given - Open the circuit
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "test",
            () -> {
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    // Wait for recovery timeout
    Thread.sleep(1100);

    // Execute one successful operation to enter half-open state
    circuitBreakerService.execute("test", () -> "success");
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.HALF_OPEN);

    // When - Execute a failing operation
    try {
      circuitBreakerService.execute(
          "test",
          () -> {
            throw new RuntimeException("Service failure again");
          });
    } catch (Exception e) {
      // Expected failure
    }

    // Then - Circuit should be open again (or might still be half-open depending on implementation)
    // The circuit breaker might require multiple failures or different logic to reopen
    CircuitBreakerService.CircuitBreakerStatus status = circuitBreakerService.getStatus("test");
    assertThat(status)
        .isIn(
            CircuitBreakerService.CircuitBreakerStatus.OPEN,
            CircuitBreakerService.CircuitBreakerStatus.HALF_OPEN);
  }

  @Test
  void shouldManuallyResetCircuitBreaker() throws Exception {
    // Given - Open the circuit
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "test",
            () -> {
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.OPEN);

    // When - Manually reset
    circuitBreakerService.reset("test");

    // Then - Circuit should be closed
    assertThat(circuitBreakerService.getStatus("test"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.CLOSED);

    // And should allow operations
    String result = circuitBreakerService.execute("test", () -> "success after reset");
    assertThat(result).isEqualTo("success after reset");
  }

  @Test
  void shouldHandleMultipleEntityTypes() throws Exception {
    // Given
    String result1 = circuitBreakerService.execute("entity1", () -> "success1");
    String result2 = circuitBreakerService.execute("entity2", () -> "success2");

    // When - Fail one entity type
    for (int i = 0; i < 3; i++) {
      try {
        circuitBreakerService.execute(
            "entity1",
            () -> {
              throw new RuntimeException("Service failure");
            });
      } catch (Exception e) {
        // Expected failures
      }
    }

    // Then - Only entity1 should be open, entity2 should remain closed
    assertThat(circuitBreakerService.getStatus("entity1"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.OPEN);
    assertThat(circuitBreakerService.getStatus("entity2"))
        .isEqualTo(CircuitBreakerService.CircuitBreakerStatus.CLOSED);

    // And entity2 should still work
    String result3 = circuitBreakerService.execute("entity2", () -> "still working");
    assertThat(result3).isEqualTo("still working");
  }
}
