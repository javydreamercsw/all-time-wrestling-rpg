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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Service for managing database transactions during sync operations. Provides rollback capabilities
 * and transaction isolation for sync operations.
 */
@Slf4j
@Service
public class SyncTransactionManager {

  private final PlatformTransactionManager transactionManager;
  private final Map<String, SyncTransaction> activeTransactions = new ConcurrentHashMap<>();

  public SyncTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * Execute a sync operation within a transaction with rollback capability.
   *
   * @param operationId Unique identifier for the operation
   * @param entityType Type of entity being synced
   * @param operation The operation to execute
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws Exception if operation fails
   */
  public <T> T executeInTransaction(
      String operationId, String entityType, TransactionalSyncOperation<T> operation)
      throws Exception {

    SyncTransaction syncTransaction = startTransaction(operationId, entityType);

    try {
      log.debug("Starting transactional sync operation: {} for {}", operationId, entityType);

      T result = operation.execute(syncTransaction);

      // Commit transaction
      commitTransaction(syncTransaction);
      log.info("✅ Successfully committed sync transaction: {} for {}", operationId, entityType);

      return result;

    } catch (Exception e) {
      log.error(
          "❌ Sync operation failed, rolling back transaction: {} for {}",
          operationId,
          entityType,
          e);
      rollbackTransaction(syncTransaction);
      throw e;
    }
  }

  /**
   * Execute a sync operation with savepoint support for partial rollback.
   *
   * @param operationId Unique identifier for the operation
   * @param entityType Type of entity being synced
   * @param operation The operation to execute
   * @param <T> Return type of the operation
   * @return Result of the operation
   * @throws Exception if operation fails
   */
  public <T> T executeWithSavepoints(
      String operationId, String entityType, SavepointSyncOperation<T> operation) throws Exception {

    SyncTransaction syncTransaction = startTransaction(operationId, entityType);

    try {
      log.debug("Starting sync operation with savepoints: {} for {}", operationId, entityType);

      T result = operation.execute(syncTransaction);

      // Commit transaction
      commitTransaction(syncTransaction);
      log.info(
          "✅ Successfully committed sync transaction with savepoints: {} for {}",
          operationId,
          entityType);

      return result;

    } catch (Exception e) {
      log.error(
          "❌ Sync operation with savepoints failed, rolling back: {} for {}",
          operationId,
          entityType,
          e);
      rollbackTransaction(syncTransaction);
      throw e;
    }
  }

  /** Start a new sync transaction. */
  private SyncTransaction startTransaction(String operationId, String entityType) {
    DefaultTransactionDefinition def = new DefaultTransactionDefinition();
    def.setName("SyncTransaction-" + operationId);
    def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    def.setTimeout(300); // 5 minutes timeout

    TransactionStatus status = transactionManager.getTransaction(def);

    SyncTransaction syncTransaction = new SyncTransaction(operationId, entityType, status);
    activeTransactions.put(operationId, syncTransaction);

    log.debug("Started sync transaction: {} for {}", operationId, entityType);
    return syncTransaction;
  }

  /** Commit a sync transaction. */
  private void commitTransaction(SyncTransaction syncTransaction) {
    try {
      transactionManager.commit(syncTransaction.getStatus());
      syncTransaction.markCommitted();
      activeTransactions.remove(syncTransaction.getOperationId());

    } catch (Exception e) {
      log.error("Failed to commit sync transaction: {}", syncTransaction.getOperationId(), e);
      throw new RuntimeException("Transaction commit failed", e);
    }
  }

  /** Rollback a sync transaction. */
  private void rollbackTransaction(SyncTransaction syncTransaction) {
    try {
      if (!syncTransaction.getStatus().isCompleted()) {
        transactionManager.rollback(syncTransaction.getStatus());
        syncTransaction.markRolledBack();
        log.info(
            "🔄 Rolled back sync transaction: {} for {}",
            syncTransaction.getOperationId(),
            syncTransaction.getEntityType());
      }

      activeTransactions.remove(syncTransaction.getOperationId());

    } catch (Exception e) {
      log.error("Failed to rollback sync transaction: {}", syncTransaction.getOperationId(), e);
      // Don't throw here as we're already in error handling
    }
  }

  /** Get information about active transactions. */
  public List<TransactionInfo> getActiveTransactions() {
    List<TransactionInfo> info = new ArrayList<>();

    for (SyncTransaction transaction : activeTransactions.values()) {
      info.add(
          new TransactionInfo(
              transaction.getOperationId(),
              transaction.getEntityType(),
              transaction.getStartTime(),
              transaction.getSavepointCount()));
    }

    return info;
  }

  /** Force rollback of a specific transaction (emergency use). */
  public boolean forceRollback(String operationId) {
    SyncTransaction transaction = activeTransactions.get(operationId);
    if (transaction != null) {
      log.warn("🚨 Force rolling back sync transaction: {}", operationId);
      rollbackTransaction(transaction);
      return true;
    }
    return false;
  }

  /** Functional interface for transactional sync operations. */
  @FunctionalInterface
  public interface TransactionalSyncOperation<T> {
    T execute(SyncTransaction transaction) throws Exception;
  }

  /** Functional interface for sync operations with savepoint support. */
  @FunctionalInterface
  public interface SavepointSyncOperation<T> {
    T execute(SyncTransaction transaction) throws Exception;
  }

  /** Represents a sync transaction with savepoint capabilities. */
  public static class SyncTransaction {
    // Getters
    @Getter private final String operationId;
    @Getter private final String entityType;
    @Getter private final TransactionStatus status;
    @Getter private final LocalDateTime startTime;
    private final List<Object> savepoints = new ArrayList<>();
    @Getter private boolean committed = false;
    @Getter private boolean rolledBack = false;

    public SyncTransaction(String operationId, String entityType, TransactionStatus status) {
      this.operationId = operationId;
      this.entityType = entityType;
      this.status = status;
      this.startTime = LocalDateTime.now();
    }

    /** Create a savepoint for partial rollback. */
    public Object createSavepoint(String name) throws Exception {
      if (status.hasSavepoint()) {
        Object savepoint = status.createSavepoint();
        savepoints.add(savepoint);
        log.debug("Created savepoint '{}' for transaction: {}", name, operationId);
        return savepoint;
      } else {
        throw new UnsupportedOperationException(
            "Savepoints not supported by current transaction manager");
      }
    }

    /** Rollback to a specific savepoint. */
    public void rollbackToSavepoint(Object savepoint) throws Exception {
      if (savepoints.contains(savepoint)) {
        status.rollbackToSavepoint(savepoint);
        log.debug("Rolled back to savepoint for transaction: {}", operationId);
      } else {
        throw new IllegalArgumentException("Invalid savepoint for transaction: " + operationId);
      }
    }

    /** Release a savepoint (no longer needed). */
    public void releaseSavepoint(Object savepoint) throws Exception {
      if (savepoints.contains(savepoint)) {
        status.releaseSavepoint(savepoint);
        savepoints.remove(savepoint);
        log.debug("Released savepoint for transaction: {}", operationId);
      }
    }

    public int getSavepointCount() {
      return savepoints.size();
    }

    void markCommitted() {
      this.committed = true;
    }

    void markRolledBack() {
      this.rolledBack = true;
    }
  }

  /** Information about an active transaction. */
  @Getter
  public static class TransactionInfo {
    // Getters
    private final String operationId;
    private final String entityType;
    private final LocalDateTime startTime;
    private final int savepointCount;

    public TransactionInfo(
        String operationId, String entityType, LocalDateTime startTime, int savepointCount) {
      this.operationId = operationId;
      this.entityType = entityType;
      this.startTime = startTime;
      this.savepointCount = savepointCount;
    }
  }
}
