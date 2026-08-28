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
package com.github.javydreamercsw.management.service.feud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeudScriptServiceTest {

  @Mock private FeudScriptRepository feudScriptRepository;
  @Mock private FeudScriptBeatRepository feudScriptBeatRepository;
  @Mock private RivalryService rivalryService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private ShowSegmentReservationService reservationService;
  @Mock private GameSettingService gameSettingService;
  @Mock private UniverseContextService universeContextService;

  @InjectMocks private FeudScriptService service;

  // ── findBeatForSegment ────────────────────────────────────────────────────

  @Test
  void findBeatForSegment_nullId_returnsEmpty() {
    Segment segment = new Segment(); // no ID set

    Optional<FeudScriptBeat> result = service.findBeatForSegment(segment);

    assertThat(result).isEmpty();
    verifyNoInteractions(feudScriptBeatRepository);
  }

  @Test
  void findBeatForSegment_withId_delegatesToRepository() {
    Segment segment = new Segment();
    segment.setId(42L);

    FeudScriptBeat beat = new FeudScriptBeat();
    FeudScript script = new FeudScript();
    script.setName("The Bloodline Saga");
    beat.setScript(script);
    beat.setBeatOrder(2);

    when(feudScriptBeatRepository.findByActualSegment(segment)).thenReturn(Optional.of(beat));

    Optional<FeudScriptBeat> result = service.findBeatForSegment(segment);

    assertThat(result).isPresent();
    assertThat(result.get().getScript().getName()).isEqualTo("The Bloodline Saga");
    assertThat(result.get().getBeatOrder()).isEqualTo(2);
    verify(feudScriptBeatRepository).findByActualSegment(segment);
  }

  @Test
  void findBeatForSegment_notLinked_returnsEmpty() {
    Segment segment = new Segment();
    segment.setId(99L);

    when(feudScriptBeatRepository.findByActualSegment(segment)).thenReturn(Optional.empty());

    assertThat(service.findBeatForSegment(segment)).isEmpty();
  }

  // ── createFromWizard validation ───────────────────────────────────────────

  @Test
  void createFromWizard_fewerThanTwoWrestlers_throws() {
    Wrestler solo = new Wrestler();
    solo.setId(1L);

    assertThatThrownBy(() -> service.createFromWizard("Solo Arc", List.of(solo), 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 2");
  }

  // ── getDefaultMaxPleAppearances ───────────────────────────────────────────

  @Test
  void getDefaultMaxPleAppearances_delegatesToGameSettings() {
    when(gameSettingService.getMaxPleFeudAppearances()).thenReturn(2);

    assertThat(service.getDefaultMaxPleAppearances()).isEqualTo(2);
    verify(gameSettingService).getMaxPleFeudAppearances();
  }
}
