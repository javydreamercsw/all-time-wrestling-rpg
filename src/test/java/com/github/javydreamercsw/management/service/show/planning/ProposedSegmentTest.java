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
package com.github.javydreamercsw.management.service.show.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.title.Title;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ProposedSegment} collection-copy semantics. */
class ProposedSegmentTest {

  @Test
  void setTitlesCopiesImmutableCallerSet() {
    // Callers pass immutable sets (Set.of(...)); downstream approval resolves title ids by
    // mutating the collection, so the setter must copy instead of aliasing.
    ProposedSegment segment = new ProposedSegment();
    Title title = new Title();
    Set<Title> immutable = Set.of(title);

    segment.setTitles(immutable);

    assertNotSame(immutable, segment.getTitles(), "setTitles must copy, not alias");
    assertEquals(1, segment.getTitles().size());
    assertTrue(segment.getTitles().contains(title));
    segment.getTitles().add(new Title()); // must not throw
  }
}
