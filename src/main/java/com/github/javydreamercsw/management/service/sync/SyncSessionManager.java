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

import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SyncSessionManager {

  // Session-based tracking to prevent duplicate syncing during batch operations
  private final ThreadLocal<Set<String>> currentSyncSession = ThreadLocal.withInitial(HashSet::new);

  /**
   * Checks if an entity has already been synced in the current session.
   *
   * @param entityName The name of the entity to check
   * @return true if already synced, false otherwise
   */
  public boolean isAlreadySyncedInSession(@NonNull String entityName) {
    return currentSyncSession.get().contains(entityName.toLowerCase());
  }

  /**
   * Marks an entity as synced in the current session.
   *
   * @param entityName The name of the entity to mark as synced
   */
  public void markAsSyncedInSession(@NonNull String entityName) {
    currentSyncSession.get().add(entityName.toLowerCase());
    log.debug("🏷️ Marked '{}' as synced in current session", entityName);
  }

  /** Clears the current sync session (should be called at the start of batch operations). */
  public void clearSyncSession() {
    currentSyncSession.get().clear();
    log.debug("🧹 Cleared sync session");
  }

  /**
   * Resets the sync status for a specific entity.
   *
   * @param entityName The name of the entity to reset
   */
  public void resetSyncStatus(@NonNull String entityName) {
    currentSyncSession.get().remove(entityName.toLowerCase());
    log.debug("🔄 Reset sync status for '{}'", entityName);
  }

  /** Cleans up the sync session thread local (should be called at the end of operations). */
  public void cleanupSyncSession() {
    currentSyncSession.remove();
    log.debug("🗑️ Cleaned up sync session thread local");
  }
}
