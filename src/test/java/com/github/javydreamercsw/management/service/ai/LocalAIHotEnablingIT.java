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
package com.github.javydreamercsw.management.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.localai.LocalAIConfigProperties;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles({"test", "local-ai-it"})
class LocalAIHotEnablingIT extends AbstractIntegrationTest {

  @Autowired private SegmentNarrationServiceFactory factory;
  @MockitoBean private LocalAIConfigProperties localAIConfig;
  @MockitoBean private LocalAIStatusService localAIStatusService;

  @Test
  void testLocalAIPriorityWhenEnabled() {
    // Given LocalAI is enabled and ready
    when(localAIConfig.isEnabled()).thenReturn(true);
    when(localAIStatusService.isReady()).thenReturn(true);

        // When getting services in priority order

        List<SegmentNarrationService> services = factory.getAvailableServicesInPriorityOrder();

    

        // Then LocalAI should be first (priority 0)

        assertEquals("LocalAI", services.get(0).getProviderName());
  }

  @Test
  void testLocalAINotInListWhenDisabled() {
    // Given LocalAI is disabled
    when(localAIConfig.isEnabled()).thenReturn(false);
    // Even if status service says ready (e.g. from previous state)
    when(localAIStatusService.isReady()).thenReturn(true);

    // When getting services
    List<SegmentNarrationService> services = factory.getAvailableServicesInPriorityOrder();

    // Then LocalAI should NOT be in the list (or at least not first if other providers are
    // available)
    // Actually, SegmentNarrationService.isAvailable() implementation for LocalAI:
    // return config.isEnabled() && statusService.isReady();
    // So it should be excluded if isEnabled is false.

    services.forEach(
        s -> {
          if (s.getProviderName().equals("LocalAI")) {
            org.junit.jupiter.api.Assertions.fail("LocalAI should not be available when disabled");
          }
        });
  }
}
