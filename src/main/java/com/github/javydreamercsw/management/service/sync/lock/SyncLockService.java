/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.sync.lock;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to manage global synchronization locks to prevent concurrent sync operations from
 * multiple sources (scheduler vs manual trigger).
 */
@Service
@Slf4j
public class SyncLockService {

  private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
  private String currentOperationId = null;

  /**
   * Attempts to acquire the global sync lock.
   *
   * @param operationId The operation ID requesting the lock
   * @return true if lock was acquired, false if another sync is in progress
   */
  public synchronized boolean acquireLock(String operationId) {
    if (syncInProgress.compareAndSet(false, true)) {
      currentOperationId = operationId;
      log.info("🔒 Sync lock acquired for operation: {}", operationId);
      return true;
    }
    log.warn(
        "⚠️ Sync lock acquisition failed for operation: {}. Current active operation: {}",
        operationId,
        currentOperationId);
    return false;
  }

  /**
   * Releases the global sync lock.
   *
   * @param operationId The operation ID releasing the lock
   */
  public synchronized void releaseLock(String operationId) {
    if (operationId.equals(currentOperationId)) {
      syncInProgress.set(false);
      currentOperationId = null;
      log.info("🔓 Sync lock released for operation: {}", operationId);
    } else {
      log.warn(
          "❌ Attempted to release lock for {} but it is held by {}",
          operationId,
          currentOperationId);
    }
  }

  /**
   * Checks if a sync is currently in progress.
   *
   * @return true if sync is in progress, false otherwise
   */
  public boolean isSyncInProgress() {
    return syncInProgress.get();
  }
}
