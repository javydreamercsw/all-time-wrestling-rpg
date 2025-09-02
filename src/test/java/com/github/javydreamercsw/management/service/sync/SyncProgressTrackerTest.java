package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.service.sync.SyncProgressTracker.SyncProgress;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker.SyncProgressListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncProgressTrackerTest {

  private SyncProgressTracker progressTracker;

  @BeforeEach
  void setUp() {
    progressTracker = new SyncProgressTracker();
  }

  @Test
  @DisplayName("Should start and track sync operation")
  void shouldStartAndTrackSyncOperation() {
    // Given
    String operationId = "test-operation";
    String operationName = "Test Sync";
    int totalSteps = 5;

    // When
    SyncProgress progress = progressTracker.startOperation(operationId, operationName, totalSteps);

    // Then
    assertNotNull(progress);
    assertEquals(operationId, progress.getOperationId());
    assertEquals(operationName, progress.getOperationName());
    assertEquals(totalSteps, progress.getTotalSteps());
    assertEquals(0, progress.getCurrentStep());
    assertFalse(progress.isCompleted());
    assertTrue(progress.isInProgress());
  }

  @Test
  @DisplayName("Should update progress correctly")
  void shouldUpdateProgressCorrectly() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 4);

    // When
    progressTracker.updateProgress(operationId, 2, "Processing data...");

    // Then
    SyncProgress progress = progressTracker.getOperation(operationId);
    assertNotNull(progress);
    assertEquals(2, progress.getCurrentStep());
    assertEquals("Processing data...", progress.getCurrentStepDescription());
    assertEquals(0.5, progress.getProgressPercentage(), 0.01);
  }

  @Test
  @DisplayName("Should complete operation successfully")
  void shouldCompleteOperationSuccessfully() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);

    // When
    progressTracker.completeOperation(operationId, true, "Sync completed", 100);

    // Then
    SyncProgress progress = progressTracker.getOperation(operationId);
    assertNotNull(progress);
    assertTrue(progress.isCompleted());
    assertTrue(progress.isSuccess());
    assertEquals("Sync completed", progress.getResultMessage());
    assertEquals(100, progress.getItemsProcessed());
    assertEquals(3, progress.getCurrentStep()); // Should be set to total steps
    assertEquals(1.0, progress.getProgressPercentage(), 0.01);
  }

  @Test
  @DisplayName("Should fail operation with error message")
  void shouldFailOperationWithErrorMessage() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);

    // When
    progressTracker.failOperation(operationId, "Connection failed");

    // Then
    SyncProgress progress = progressTracker.getOperation(operationId);
    assertNotNull(progress);
    assertTrue(progress.isCompleted());
    assertFalse(progress.isSuccess());
    assertEquals("Connection failed", progress.getResultMessage());
    assertEquals(0, progress.getItemsProcessed());
  }

  @Test
  @DisplayName("Should notify listeners of progress updates")
  void shouldNotifyListenersOfProgressUpdates() throws InterruptedException {
    // Given
    CountDownLatch startedLatch = new CountDownLatch(1);
    CountDownLatch updatedLatch = new CountDownLatch(1);
    CountDownLatch completedLatch = new CountDownLatch(1);

    AtomicInteger startedCount = new AtomicInteger(0);
    AtomicInteger updatedCount = new AtomicInteger(0);
    AtomicInteger completedCount = new AtomicInteger(0);

    SyncProgressListener listener =
        new SyncProgressListener() {
          @Override
          public void onOperationStarted(SyncProgress progress) {
            startedCount.incrementAndGet();
            startedLatch.countDown();
          }

          @Override
          public void onProgressUpdated(SyncProgress progress) {
            updatedCount.incrementAndGet();
            updatedLatch.countDown();
          }

          @Override
          public void onOperationCompleted(SyncProgress progress) {
            completedCount.incrementAndGet();
            completedLatch.countDown();
          }
        };

    progressTracker.addProgressListener(listener);

    // When
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);
    progressTracker.updateProgress(operationId, 1, "Step 1");
    progressTracker.completeOperation(operationId, true, "Done", 50);

    // Then
    assertTrue(startedLatch.await(1, TimeUnit.SECONDS));
    assertTrue(updatedLatch.await(1, TimeUnit.SECONDS));
    assertTrue(completedLatch.await(1, TimeUnit.SECONDS));

    assertEquals(1, startedCount.get());
    assertEquals(1, updatedCount.get());
    assertEquals(1, completedCount.get());
  }

  @Test
  @DisplayName("Should track multiple concurrent operations")
  void shouldTrackMultipleConcurrentOperations() {
    // Given
    String operation1 = "operation-1";
    String operation2 = "operation-2";

    // When
    progressTracker.startOperation(operation1, "Sync 1", 3);
    progressTracker.startOperation(operation2, "Sync 2", 5);

    progressTracker.updateProgress(operation1, 1, "Step 1 of Sync 1");
    progressTracker.updateProgress(operation2, 2, "Step 2 of Sync 2");

    // Then
    List<SyncProgress> activeOperations = progressTracker.getActiveOperations();
    assertEquals(2, activeOperations.size());
    assertTrue(progressTracker.hasActiveOperations());

    SyncProgress progress1 = progressTracker.getOperation(operation1);
    SyncProgress progress2 = progressTracker.getOperation(operation2);

    assertNotNull(progress1);
    assertNotNull(progress2);
    assertEquals(1, progress1.getCurrentStep());
    assertEquals(2, progress2.getCurrentStep());
    assertEquals("Step 1 of Sync 1", progress1.getCurrentStepDescription());
    assertEquals("Step 2 of Sync 2", progress2.getCurrentStepDescription());
  }

  @Test
  @DisplayName("Should calculate progress percentage correctly")
  void shouldCalculateProgressPercentageCorrectly() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 10);

    // Test various progress levels
    progressTracker.updateProgress(operationId, 0, "Starting");
    assertEquals(0.0, progressTracker.getOperation(operationId).getProgressPercentage(), 0.01);

    progressTracker.updateProgress(operationId, 5, "Half way");
    assertEquals(0.5, progressTracker.getOperation(operationId).getProgressPercentage(), 0.01);

    progressTracker.updateProgress(operationId, 10, "Complete");
    assertEquals(1.0, progressTracker.getOperation(operationId).getProgressPercentage(), 0.01);
  }

  @Test
  @DisplayName("Should handle zero total steps")
  void shouldHandleZeroTotalSteps() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 0);

    // When
    SyncProgress progress = progressTracker.getOperation(operationId);

    // Then
    assertEquals(0.0, progress.getProgressPercentage(), 0.01);

    // When completed
    progressTracker.completeOperation(operationId, true, "Done", 0);
    assertEquals(1.0, progress.getProgressPercentage(), 0.01);
  }

  @Test
  @DisplayName("Should generate correct status strings")
  void shouldGenerateCorrectStatusStrings() {
    // Given
    String operationId = "test-operation";
    SyncProgress progress = progressTracker.startOperation(operationId, "Test Sync", 5);

    // Test in progress status
    progressTracker.updateProgress(operationId, 2, "Processing");
    assertEquals("In Progress (2/5)", progress.getStatusString());

    // Test completed successfully
    progressTracker.completeOperation(operationId, true, "Success", 100);
    assertEquals("Completed Successfully", progress.getStatusString());

    // Test failed operation
    String failedOperationId = "failed-operation";
    SyncProgress failedProgress =
        progressTracker.startOperation(failedOperationId, "Failed Sync", 3);
    progressTracker.failOperation(failedOperationId, "Error occurred");
    assertEquals("Failed", failedProgress.getStatusString());
  }

  @Test
  @DisplayName("Should remove listener correctly")
  void shouldRemoveListenerCorrectly() throws InterruptedException {
    // Given
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger callCount = new AtomicInteger(0);

    SyncProgressListener listener =
        new SyncProgressListener() {
          @Override
          public void onOperationStarted(SyncProgress progress) {
            callCount.incrementAndGet();
            latch.countDown();
          }
        };

    progressTracker.addProgressListener(listener);

    // When - first operation should trigger listener
    progressTracker.startOperation("operation-1", "Test 1", 3);
    assertTrue(latch.await(1, TimeUnit.SECONDS));
    assertEquals(1, callCount.get());

    // Remove listener and start another operation
    progressTracker.removeProgressListener(listener);
    progressTracker.startOperation("operation-2", "Test 2", 3);

    // Give some time for potential listener calls
    Thread.sleep(100);

    // Then - call count should still be 1 (listener was removed)
    assertEquals(1, callCount.get());
  }

  @Test
  @DisplayName("Should send log messages to listeners")
  void shouldSendLogMessagesToListeners() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);

    AtomicReference<String> receivedOperationId = new AtomicReference<>();
    AtomicReference<String> receivedMessage = new AtomicReference<>();
    AtomicReference<String> receivedLevel = new AtomicReference<>();

    SyncProgressListener listener =
        new SyncProgressListener() {
          @Override
          public void onLogMessage(String logOperationId, String message, String level) {
            receivedOperationId.set(logOperationId);
            receivedMessage.set(message);
            receivedLevel.set(level);
          }
        };

    progressTracker.addProgressListener(listener);

    // When
    String testMessage = "📥 Retrieving data from Notion...";
    String testLevel = "INFO";
    progressTracker.addLogMessage(operationId, testMessage, testLevel);

    // Then
    assertEquals(operationId, receivedOperationId.get());
    assertEquals(testMessage, receivedMessage.get());
    assertEquals(testLevel, receivedLevel.get());
  }

  @Test
  @DisplayName("Should not send log messages for non-existent operations")
  void shouldNotSendLogMessagesForNonExistentOperations() {
    // Given
    String operationId = "test-operation";
    String nonExistentOperationId = "non-existent-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);

    AtomicInteger callCount = new AtomicInteger(0);
    SyncProgressListener listener =
        new SyncProgressListener() {
          @Override
          public void onLogMessage(String logOperationId, String message, String level) {
            callCount.incrementAndGet();
          }
        };

    progressTracker.addProgressListener(listener);

    // When
    progressTracker.addLogMessage(nonExistentOperationId, "Test message", "INFO");

    // Then
    assertEquals(0, callCount.get());
  }

  @Test
  @DisplayName("Should send log messages with different levels")
  void shouldSendLogMessagesWithDifferentLevels() {
    // Given
    String operationId = "test-operation";
    progressTracker.startOperation(operationId, "Test Sync", 3);

    AtomicInteger infoCount = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    SyncProgressListener listener =
        new SyncProgressListener() {
          @Override
          public void onLogMessage(String logOperationId, String message, String level) {
            switch (level) {
              case "INFO" -> infoCount.incrementAndGet();
              case "SUCCESS" -> successCount.incrementAndGet();
              case "ERROR" -> errorCount.incrementAndGet();
            }
          }
        };

    progressTracker.addProgressListener(listener);

    // When
    progressTracker.addLogMessage(operationId, "Starting operation", "INFO");
    progressTracker.addLogMessage(operationId, "Operation completed", "SUCCESS");
    progressTracker.addLogMessage(operationId, "Operation failed", "ERROR");

    // Then
    assertEquals(1, infoCount.get());
    assertEquals(1, successCount.get());
    assertEquals(1, errorCount.get());
  }
}
