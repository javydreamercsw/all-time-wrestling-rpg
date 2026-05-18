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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncLockServiceTest {

  private SyncLockService syncLockService;

  @BeforeEach
  void setUp() {
    syncLockService = new SyncLockService();
  }

  @Test
  void isSyncInProgress_initially_returnsFalse() {
    assertFalse(syncLockService.isSyncInProgress());
  }

  @Test
  void acquireLock_whenFree_returnsTrue() {
    assertTrue(syncLockService.acquireLock("op-1"));
  }

  @Test
  void acquireLock_whenFree_setsSyncInProgress() {
    syncLockService.acquireLock("op-1");
    assertTrue(syncLockService.isSyncInProgress());
  }

  @Test
  void acquireLock_whenAlreadyLocked_returnsFalse() {
    syncLockService.acquireLock("op-1");
    assertFalse(syncLockService.acquireLock("op-2"));
  }

  @Test
  void acquireLock_whenAlreadyLocked_doesNotChangeLock() {
    syncLockService.acquireLock("op-1");
    syncLockService.acquireLock("op-2");
    // Lock still held — releasing with op-1 (original holder) should work
    syncLockService.releaseLock("op-1");
    assertFalse(syncLockService.isSyncInProgress());
  }

  @Test
  void releaseLock_withMatchingId_releasesLock() {
    syncLockService.acquireLock("op-1");
    syncLockService.releaseLock("op-1");
    // After release a new acquire should succeed
    assertTrue(syncLockService.acquireLock("op-2"));
  }

  @Test
  void releaseLock_withMatchingId_syncInProgressFalse() {
    syncLockService.acquireLock("op-1");
    syncLockService.releaseLock("op-1");
    assertFalse(syncLockService.isSyncInProgress());
  }

  @Test
  void releaseLock_withMismatchedId_doesNotReleaseLock() {
    syncLockService.acquireLock("op-1");
    syncLockService.releaseLock("op-other");
    assertTrue(syncLockService.isSyncInProgress());
  }

  @Test
  void acquireLock_afterRelease_canAcquireAgain() {
    syncLockService.acquireLock("op-1");
    syncLockService.releaseLock("op-1");
    assertTrue(syncLockService.acquireLock("op-2"));
  }
}
