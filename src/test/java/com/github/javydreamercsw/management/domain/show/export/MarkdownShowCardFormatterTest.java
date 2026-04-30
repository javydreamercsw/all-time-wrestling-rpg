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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownShowCardFormatterTest {

  @Test
  void testFormat() {
    MarkdownShowCardFormatter formatter = new MarkdownShowCardFormatter();

    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(LocalDate.of(2026, 4, 29));

    Arena arena = mock(Arena.class);
    when(arena.getName()).thenReturn("Test Arena");
    when(show.getArena()).thenReturn(arena);

    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn("Singles Match");
    when(segment.getSegmentType()).thenReturn(type);

    Wrestler w1 = mock(Wrestler.class);
    when(w1.getName()).thenReturn("Wrestler 1");
    Wrestler w2 = mock(Wrestler.class);
    when(w2.getName()).thenReturn("Wrestler 2");

    when(segment.getWrestlers()).thenReturn(Arrays.asList(w1, w2));
    when(segment.getSegmentRulesAsString()).thenReturn("No DQ");
    when(segment.hasSegmentRules()).thenReturn(true);
    when(segment.getIsTitleSegment()).thenReturn(true);

    List<Segment> segments = Collections.singletonList(segment);

    String result = formatter.format(show, segments, true, true);

    assertTrue(result.contains("# Test Show"));
    assertTrue(result.contains("**Date:** 2026-04-29"));
    assertTrue(result.contains("**Venue:** Test Arena"));
    assertTrue(result.contains("Wrestler 1 vs. Wrestler 2"));
    assertTrue(result.contains("*Rules: No DQ*"));
    assertTrue(result.contains("**CHAMPIONSHIP MATCH**"));
  }
}
