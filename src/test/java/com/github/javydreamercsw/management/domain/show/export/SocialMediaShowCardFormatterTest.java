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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocialMediaShowCardFormatterTest {

  private Show show;
  private List<Segment> segments;

  @BeforeEach
  void setUp() {
    show = mock(Show.class);
    when(show.getName()).thenReturn("Friday Night Heat");
    when(show.getShowDate()).thenReturn(LocalDate.of(2026, 4, 29));

    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn("Main Event");
    when(segment.getSegmentType()).thenReturn(type);

    Wrestler w1 = mock(Wrestler.class);
    when(w1.getName()).thenReturn("Stone Cold");
    Wrestler w2 = mock(Wrestler.class);
    when(w2.getName()).thenReturn("The Rock");

    when(segment.getWrestlers()).thenReturn(Arrays.asList(w1, w2));
    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getSummary()).thenReturn("A brutal battle for the gold.");

    Title title = mock(Title.class);
    when(title.getName()).thenReturn("ATW World Title");
    when(segment.getTitles()).thenReturn(new HashSet<>(Collections.singletonList(title)));

    segments = Collections.singletonList(segment);
  }

  @Test
  void testFacebookFormat() {
    FacebookShowCardFormatter formatter = new FacebookShowCardFormatter();
    String result = formatter.format(show, segments);

    assertTrue(result.contains("Friday Night Heat"));
    assertTrue(result.contains("Apr 29, 2026"));
    assertTrue(result.contains("[Main Event]"));
    assertTrue(result.contains("Stone Cold vs. The Rock"));
    assertTrue(result.contains("🏆"));
    assertTrue(result.contains("ATW World Title"));
    assertTrue(result.contains("A brutal battle for the gold."));
    assertTrue(result.contains("#AllTimeWrestling"));
  }

  @Test
  void testXFormat() {
    XShowCardFormatter formatter = new XShowCardFormatter();
    String result = formatter.format(show, segments);

    assertTrue(result.contains("Friday Night Heat"));
    assertTrue(result.contains("Stone Cold vs. The Rock"));
    assertTrue(result.contains("#ATW"));
    assertTrue(result.length() <= 280);
  }

  @Test
  void testBlueskyFormat() {
    BlueskyShowCardFormatter formatter = new BlueskyShowCardFormatter();
    String result = formatter.format(show, segments);

    assertTrue(result.contains("Friday Night Heat"));
    assertTrue(result.contains("Stone Cold vs. The Rock"));
    assertTrue(result.contains("#ATW"));
    assertTrue(result.length() <= 300);
  }
}
