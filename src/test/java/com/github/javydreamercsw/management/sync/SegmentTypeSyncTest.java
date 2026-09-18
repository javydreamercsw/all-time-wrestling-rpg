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
package com.github.javydreamercsw.management.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentTypeSyncTest {

  @Mock private SegmentTypeService segmentTypeService;

  private SegmentTypeSync sync;

  @BeforeEach
  void setUp() {
    sync = new SegmentTypeSync(false, segmentTypeService, new ObjectMapper());
  }

  @Test
  void sync_passesEventOnlyThroughToService() {
    SegmentType rumble = new SegmentType();
    rumble.setName("Abu Dhabi Rumble");
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), eq(true)))
        .thenReturn(rumble);
    SegmentType regular = new SegmentType();
    regular.setName("One on One");
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), isNull()))
        .thenReturn(regular);
    when(segmentTypeService.count()).thenReturn(0L);

    sync.sync();

    // Abu Dhabi Rumble is the only seeded entry with event_only=true (ATW-0331).
    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("Abu Dhabi Rumble"),
            anyString(),
            eq("RUMBLE"),
            any(),
            eq("abu_dhabi_rumble"),
            eq(true));
  }

  @Test
  void sync_parsesSeedJsonWithoutErrors() {
    // The seed JSON must deserialize into SegmentTypeDTO including the event_only key.
    ObjectMapper mapper = new ObjectMapper();
    try (var is = getClass().getResourceAsStream("/segment_types.json")) {
      List<SegmentTypeDTO> dtos =
          mapper.readValue(
              is,
              mapper.getTypeFactory().constructCollectionType(List.class, SegmentTypeDTO.class));
      assertEquals(6, dtos.size(), "Seed JSON must contain 6 segment types");
      assertTrue(
          dtos.stream().filter(d -> Boolean.TRUE.equals(d.getEventOnly())).count() == 1,
          "Exactly one seeded segment type must be event_only (Abu Dhabi Rumble)");
    } catch (Exception e) {
      throw new IllegalStateException("Seed segment_types.json failed to parse", e);
    }
  }

  @Test
  void sync_skipsWhenNotEmptyAndSkipFlagSet() {
    SegmentTypeSync skipSync = new SegmentTypeSync(true, segmentTypeService, new ObjectMapper());
    when(segmentTypeService.count()).thenReturn(5L);

    skipSync.sync();

    verify(segmentTypeService, org.mockito.Mockito.never())
        .createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), anyBoolean());
  }
}
