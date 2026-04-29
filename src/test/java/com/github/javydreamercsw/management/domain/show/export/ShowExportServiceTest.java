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
package com.github.javydreamercsw.management.domain.show.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowExportServiceTest {

  private ShowExportService service;
  private SegmentRepository segmentRepository;
  private ShowCardFormatter formatter1;
  private ShowCardFormatter formatter2;

  @BeforeEach
  void setUp() {
    segmentRepository = mock(SegmentRepository.class);

    formatter1 = mock(ShowCardFormatter.class);
    when(formatter1.getFormatName()).thenReturn("Format 1");
    when(formatter1.getPriority()).thenReturn(10);

    formatter2 = mock(ShowCardFormatter.class);
    when(formatter2.getFormatName()).thenReturn("Format 2");
    when(formatter2.getPriority()).thenReturn(5);

    service = new ShowExportService(segmentRepository, Arrays.asList(formatter1, formatter2));
  }

  @Test
  void testGetAvailableFormats() {
    List<String> formats = service.getAvailableFormats();
    assertEquals(2, formats.size());
    // Priority 5 (Format 2) should come before Priority 10 (Format 1)
    assertEquals("Format 2", formats.get(0));
    assertEquals("Format 1", formats.get(1));
  }

  @Test
  void testExport() {
    Show show = mock(Show.class);
    List<Segment> segments = Collections.singletonList(mock(Segment.class));
    when(segmentRepository.findByShowOrderBySegmentOrderAsc(show)).thenReturn(segments);

    when(formatter2.format(show, segments, true, true)).thenReturn("Formatted Content");

    String result = service.export(show, "Format 2", true, true);
    assertEquals("Formatted Content", result);
  }
}
