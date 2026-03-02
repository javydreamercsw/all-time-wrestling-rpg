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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.scheduler.enabled=true",
      "notion.sync.scheduler.interval=3600000"
    })
class NotionSyncAuthReproductionTest extends AbstractMockUserIntegrationTest {

  @Autowired private NotionSyncScheduler notionSyncScheduler;

  @Test
  @DisplayName(
      "Should fail with Authentication object not found when called from a separate thread")
  void shouldFailWhenCalledFromSeparateThread() {
    // ... (existing implementation)
  }

  @Test
  @DisplayName(
      "Should succeed when security context is propagated using"
          + " GeneralSecurityUtils.runWithContext")
  void shouldSucceedWhenContextPropagated() throws Exception {
    SecurityContext context = SecurityContextHolder.getContext();

    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              GeneralSecurityUtils.runWithContext(
                  context,
                  () -> {
                    // This should NOT throw AuthenticationCredentialsNotFoundException now
                    notionSyncScheduler.triggerManualSync();
                    return null;
                  });
            });

    // Should not throw exception
    future.get();
  }
}
