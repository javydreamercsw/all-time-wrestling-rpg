package com.github.javydreamercsw.management.service.sync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for tracking sync operation progress and providing real-time updates to UI components.
 * Supports multiple concurrent sync operations with individual progress tracking.
 */
@Service
@Slf4j
public class SyncProgressTracker {

  private final List<SyncProgressListener> listeners = new CopyOnWriteArrayList<>();
  private final List<SyncProgress> activeOperations = new CopyOnWriteArrayList<>();

  /**
   * Register a listener for sync progress updates.
   *
   * @param listener The listener to register
   */
  public void addProgressListener(SyncProgressListener listener) {
    listeners.add(listener);
    log.debug("Added progress listener: {}", listener.getClass().getSimpleName());
  }

  /**
   * Remove a progress listener.
   *
   * @param listener The listener to remove
   */
  public void removeProgressListener(SyncProgressListener listener) {
    listeners.remove(listener);
    log.debug("Removed progress listener: {}", listener.getClass().getSimpleName());
  }

  /**
   * Start tracking a new sync operation.
   *
   * @param operationId Unique identifier for the operation
   * @param operationName Human-readable name for the operation
   * @param totalSteps Total number of steps in the operation
   * @return SyncProgress object for tracking this operation
   */
  public SyncProgress startOperation(String operationId, String operationName, int totalSteps) {
    SyncProgress progress = new SyncProgress(operationId, operationName, totalSteps);
    activeOperations.add(progress);

    log.info("Started tracking sync operation: {} ({})", operationName, operationId);
    notifyListeners(listener -> listener.onOperationStarted(progress));

    return progress;
  }

  /**
   * Update progress for an existing operation.
   *
   * @param operationId The operation ID
   * @param currentStep Current step number
   * @param stepDescription Description of current step
   */
  public void updateProgress(String operationId, int currentStep, String stepDescription) {
    activeOperations.stream()
        .filter(op -> op.getOperationId().equals(operationId))
        .findFirst()
        .ifPresent(
            progress -> {
              progress.setCurrentStep(currentStep);
              progress.setCurrentStepDescription(stepDescription);
              progress.setLastUpdated(LocalDateTime.now());

              log.debug(
                  "Updated progress for {}: {}/{} - {}",
                  operationId,
                  currentStep,
                  progress.getTotalSteps(),
                  stepDescription);

              notifyListeners(listener -> listener.onProgressUpdated(progress));
            });
  }

  /**
   * Complete a sync operation.
   *
   * @param operationId The operation ID
   * @param success Whether the operation was successful
   * @param resultMessage Final result message
   * @param itemsProcessed Number of items processed
   */
  public void completeOperation(
      String operationId, boolean success, String resultMessage, int itemsProcessed) {
    activeOperations.stream()
        .filter(op -> op.getOperationId().equals(operationId))
        .findFirst()
        .ifPresent(
            progress -> {
              progress.setCompleted(true);
              progress.setSuccess(success);
              progress.setResultMessage(resultMessage);
              progress.setItemsProcessed(itemsProcessed);
              progress.setCompletedAt(LocalDateTime.now());
              progress.setCurrentStep(progress.getTotalSteps());

              log.info(
                  "Completed sync operation: {} - {} (processed {} items)",
                  operationId,
                  success ? "SUCCESS" : "FAILED",
                  itemsProcessed);

              notifyListeners(listener -> listener.onOperationCompleted(progress));

              // Remove from active operations after a delay to allow UI updates
              removeOperationAfterDelay(operationId);
            });
  }

  /**
   * Fail a sync operation with an error.
   *
   * @param operationId The operation ID
   * @param errorMessage Error message
   */
  public void failOperation(String operationId, String errorMessage) {
    completeOperation(operationId, false, errorMessage, 0);
  }

  /**
   * Get all currently active operations.
   *
   * @return List of active sync operations
   */
  public List<SyncProgress> getActiveOperations() {
    return new ArrayList<>(activeOperations);
  }

  /**
   * Get a specific operation by ID.
   *
   * @param operationId The operation ID
   * @return SyncProgress if found, null otherwise
   */
  public SyncProgress getOperation(String operationId) {
    return activeOperations.stream()
        .filter(op -> op.getOperationId().equals(operationId))
        .findFirst()
        .orElse(null);
  }

  /**
   * Check if any sync operations are currently active.
   *
   * @return true if any operations are in progress
   */
  public boolean hasActiveOperations() {
    return activeOperations.stream().anyMatch(op -> !op.isCompleted());
  }

  private void notifyListeners(Consumer<SyncProgressListener> action) {
    listeners.forEach(
        listener -> {
          try {
            action.accept(listener);
          } catch (Exception e) {
            log.warn("Error notifying progress listener: {}", e.getMessage());
          }
        });
  }

  private void removeOperationAfterDelay(String operationId) {
    // Remove completed operation after 30 seconds to allow UI to show completion
    new Thread(
            () -> {
              try {
                Thread.sleep(30000); // 30 seconds
                activeOperations.removeIf(op -> op.getOperationId().equals(operationId));
                log.debug("Removed completed operation from tracking: {}", operationId);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            })
        .start();
  }

  /** Data class representing the progress of a sync operation. */
  @Data
  public static class SyncProgress {
    private final String operationId;
    private final String operationName;
    private final int totalSteps;
    private final LocalDateTime startedAt;

    private int currentStep = 0;
    private String currentStepDescription = "Initializing...";
    private boolean completed = false;
    private boolean success = false;
    private String resultMessage = "";
    private int itemsProcessed = 0;
    private LocalDateTime lastUpdated;
    private LocalDateTime completedAt;

    public SyncProgress(String operationId, String operationName, int totalSteps) {
      this.operationId = operationId;
      this.operationName = operationName;
      this.totalSteps = totalSteps;
      this.startedAt = LocalDateTime.now();
      this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Get progress as a percentage (0.0 to 1.0).
     *
     * @return Progress percentage
     */
    public double getProgressPercentage() {
      if (totalSteps == 0) return completed ? 1.0 : 0.0;
      return Math.min(1.0, (double) currentStep / totalSteps);
    }

    /**
     * Get estimated time remaining based on current progress.
     *
     * @return Estimated seconds remaining, or -1 if cannot estimate
     */
    public long getEstimatedSecondsRemaining() {
      if (completed || currentStep == 0) return -1;

      long elapsedSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
      if (elapsedSeconds == 0) return -1;

      double progressRate = (double) currentStep / elapsedSeconds;
      int remainingSteps = totalSteps - currentStep;

      return Math.round(remainingSteps / progressRate);
    }

    /**
     * Check if the operation is currently in progress.
     *
     * @return true if in progress
     */
    public boolean isInProgress() {
      return !completed;
    }

    /**
     * Get a human-readable status string.
     *
     * @return Status string
     */
    public String getStatusString() {
      if (!completed) {
        return String.format("In Progress (%d/%d)", currentStep, totalSteps);
      } else if (success) {
        return "Completed Successfully";
      } else {
        return "Failed";
      }
    }
  }

  /** Interface for listening to sync progress updates. */
  public interface SyncProgressListener {

    /**
     * Called when a new sync operation starts.
     *
     * @param progress The sync progress object
     */
    default void onOperationStarted(SyncProgress progress) {}

    /**
     * Called when sync progress is updated.
     *
     * @param progress The updated sync progress object
     */
    default void onProgressUpdated(SyncProgress progress) {}

    /**
     * Called when a sync operation completes.
     *
     * @param progress The completed sync progress object
     */
    default void onOperationCompleted(SyncProgress progress) {}
  }
}
