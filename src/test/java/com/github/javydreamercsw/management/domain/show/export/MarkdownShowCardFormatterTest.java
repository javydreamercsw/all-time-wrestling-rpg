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
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    when(segment.getTitles()).thenReturn(Collections.emptySet());

    List<Segment> segments = Collections.singletonList(segment);

    String result = formatter.format(show, segments, true, true, false);

    assertTrue(result.contains("# Test Show"));
    assertTrue(result.contains("**Date:** 2026-04-29"));
    assertTrue(result.contains("**Venue:** Test Arena"));
    assertTrue(result.contains("Wrestler 1 vs. Wrestler 2"));
    assertTrue(result.contains("*Rules: No DQ*"));
    assertTrue(result.contains("**CHAMPIONSHIP MATCH**"));
  }

  @Test
  void testTitleAndChampionAppearsInChampionshipMatch() {
    MarkdownShowCardFormatter formatter = new MarkdownShowCardFormatter();

    Show show = mock(Show.class);
    when(show.getName()).thenReturn("PPV");
    when(show.getShowDate()).thenReturn(null);
    when(show.getArena()).thenReturn(null);

    Title title = mock(Title.class);
    when(title.getName()).thenReturn("ATW World Title");
    when(title.getChampionNames()).thenReturn("El Fuego");

    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn("Singles Match");
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.isMainEvent()).thenReturn(false);
    when(segment.hasSegmentRules()).thenReturn(false);
    when(segment.getWinners()).thenReturn(Collections.emptyList());

    Wrestler w1 = mock(Wrestler.class);
    when(w1.getName()).thenReturn("El Fuego");
    Wrestler w2 = mock(Wrestler.class);
    when(w2.getName()).thenReturn("Challenger");
    when(segment.getWrestlers()).thenReturn(List.of(w1, w2));

    String result = formatter.format(show, List.of(segment), false, false, false);

    assertTrue(result.contains("**Title:** ATW World Title"), "title name should appear");
    assertTrue(result.contains("Champion: El Fuego"), "current champion should appear");
  }

  @Test
  void testVacantTitleShowsNoChampionLine() {
    MarkdownShowCardFormatter formatter = new MarkdownShowCardFormatter();

    Show show = mock(Show.class);
    when(show.getName()).thenReturn("PPV");
    when(show.getShowDate()).thenReturn(null);
    when(show.getArena()).thenReturn(null);

    Title title = mock(Title.class);
    when(title.getName()).thenReturn("ATW World Title");
    when(title.getChampionNames()).thenReturn(""); // vacant

    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn("Singles Match");
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.getIsTitleSegment()).thenReturn(true);
    when(segment.getTitles()).thenReturn(Set.of(title));
    when(segment.isMainEvent()).thenReturn(false);
    when(segment.hasSegmentRules()).thenReturn(false);
    when(segment.getWinners()).thenReturn(Collections.emptyList());

    Wrestler w1 = mock(Wrestler.class);
    when(w1.getName()).thenReturn("A");
    when(segment.getWrestlers()).thenReturn(List.of(w1));

    String result = formatter.format(show, List.of(segment), false, false, false);

    assertTrue(result.contains("**Title:** ATW World Title"), "title name should appear");
    assertTrue(!result.contains("Champion:"), "vacant title should not show a champion line");
  }

  @Test
  void testMatchCounterSkipsPromoSegments() {
    MarkdownShowCardFormatter formatter = new MarkdownShowCardFormatter();

    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Test Show");
    when(show.getShowDate()).thenReturn(null);
    when(show.getArena()).thenReturn(null);

    Segment match1 = buildSegment("Singles Match", "Alpha", "Beta");
    Segment promo = buildSegment("Promo", "Champion");
    Segment match2 = buildSegment("Tag Team Match", "Team A", "Team B");

    String result = formatter.format(show, List.of(match1, promo, match2), false, false, false);

    // Match counter resets at promo: Match 1 → Promo → Match 2
    assertTrue(result.contains("### Match 1: Singles Match"), "first match should be Match 1");
    assertTrue(result.contains("### Promo"), "promo should not carry a match number");
    assertTrue(result.contains("### Match 2: Tag Team Match"), "second match should be Match 2");
    assertTrue(!result.contains("### Match 2: Promo"), "promo must not be labelled as a match");
    assertTrue(!result.contains("### Match 3:"), "there should be no Match 3");
  }

  @Test
  void testFormatWithMainEventAndNarration() {
    MarkdownShowCardFormatter formatter = new MarkdownShowCardFormatter();

    Show show = mock(Show.class);
    when(show.getName()).thenReturn("Main Event Show");
    when(show.getShowDate()).thenReturn(LocalDate.of(2026, 5, 1));
    when(show.getArena()).thenReturn(null);

    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn("Main Event Match");
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.isMainEvent()).thenReturn(true);
    when(segment.getIsTitleSegment()).thenReturn(false);
    when(segment.hasSegmentRules()).thenReturn(false);
    when(segment.getNarration()).thenReturn("An intense battle unfolded.");
    when(segment.getSummary()).thenReturn(null);

    Wrestler w1 = mock(Wrestler.class);
    when(w1.getName()).thenReturn("Undertaker");
    Wrestler w2 = mock(Wrestler.class);
    when(w2.getName()).thenReturn("Mankind");
    when(segment.getWrestlers()).thenReturn(Arrays.asList(w1, w2));
    when(segment.getWinners()).thenReturn(Collections.emptyList());

    List<Segment> segments = Collections.singletonList(segment);

    String result = formatter.format(show, segments, true, true, true);

    assertTrue(result.contains("**⭐ MAIN EVENT ⭐**"));
    assertTrue(result.contains("*Narration:* An intense battle unfolded."));
  }

  private Segment buildSegment(final String typeName, final String... wrestlerNames) {
    Segment segment = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getName()).thenReturn(typeName);
    when(segment.getSegmentType()).thenReturn(type);
    when(segment.isMainEvent()).thenReturn(false);
    when(segment.getIsTitleSegment()).thenReturn(false);
    when(segment.hasSegmentRules()).thenReturn(false);
    when(segment.getSummary()).thenReturn(null);
    when(segment.getNarration()).thenReturn(null);
    when(segment.getWinners()).thenReturn(Collections.emptyList());
    List<Wrestler> wrestlers =
        Arrays.stream(wrestlerNames)
            .map(
                name -> {
                  Wrestler w = mock(Wrestler.class);
                  when(w.getName()).thenReturn(name);
                  return w;
                })
            .toList();
    when(segment.getWrestlers()).thenReturn(wrestlers);
    return segment;
  }
}
