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
package com.github.javydreamercsw.management.service.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.health.contributor.Health;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemPulseServiceTest {

  @Mock private SegmentNarrationService narrationService;
  @Mock private SyncHealthMonitor syncHealthMonitor;

  @Test
  void getPulse_noServices_noSyncMonitor_returnsEmptyMap() {
    SystemPulseService service = new SystemPulseService(List.of(), Optional.empty());

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).isEmpty();
  }

  @Test
  void getPulse_oneAvailableService_returnsUpStatus() {
    when(narrationService.getProviderName()).thenReturn("OpenAI");
    when(narrationService.isAvailable()).thenReturn(true);

    SystemPulseService service =
        new SystemPulseService(List.of(narrationService), Optional.empty());

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).containsKey("OpenAI");
    assertThat(pulse.get("OpenAI").status()).isEqualTo("UP");
  }

  @Test
  void getPulse_oneUnavailableService_returnsDownStatus() {
    when(narrationService.getProviderName()).thenReturn("Claude");
    when(narrationService.isAvailable()).thenReturn(false);

    SystemPulseService service =
        new SystemPulseService(List.of(narrationService), Optional.empty());

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).containsKey("Claude");
    assertThat(pulse.get("Claude").status()).isEqualTo("DOWN");
  }

  @Test
  void getPulse_multipleServices_returnsAllStatuses() {
    SegmentNarrationService openAiService = org.mockito.Mockito.mock(SegmentNarrationService.class);
    SegmentNarrationService claudeService = org.mockito.Mockito.mock(SegmentNarrationService.class);

    when(openAiService.getProviderName()).thenReturn("OpenAI");
    when(openAiService.isAvailable()).thenReturn(true);
    when(claudeService.getProviderName()).thenReturn("Claude");
    when(claudeService.isAvailable()).thenReturn(false);

    SystemPulseService service =
        new SystemPulseService(List.of(openAiService, claudeService), Optional.empty());

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).hasSize(2);
    assertThat(pulse.get("OpenAI").status()).isEqualTo("UP");
    assertThat(pulse.get("Claude").status()).isEqualTo("DOWN");
  }

  @Test
  void getPulse_withSyncMonitor_includesNotionSyncStatus() {
    Health upHealth = Health.up().withDetail("message", "Sync is healthy").build();
    when(syncHealthMonitor.health()).thenReturn(upHealth);

    SystemPulseService service = new SystemPulseService(List.of(), Optional.of(syncHealthMonitor));

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).containsKey("Notion Sync");
    assertThat(pulse.get("Notion Sync").status()).isEqualTo("UP");
  }

  @Test
  void getPulse_withSyncMonitor_syncUnavailable_includesDownStatus() {
    Health downHealth = Health.down().withDetail("message", "Sync is unavailable").build();
    when(syncHealthMonitor.health()).thenReturn(downHealth);

    SystemPulseService service = new SystemPulseService(List.of(), Optional.of(syncHealthMonitor));

    Map<String, SystemPulseService.ServiceStatus> pulse = service.getPulse();

    assertThat(pulse).containsKey("Notion Sync");
    assertThat(pulse.get("Notion Sync").status()).isEqualTo("DOWN");
  }
}
