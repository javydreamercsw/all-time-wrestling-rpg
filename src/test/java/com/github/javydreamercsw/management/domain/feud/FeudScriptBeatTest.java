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
package com.github.javydreamercsw.management.domain.feud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.title.Title;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FeudScriptBeat} collection-copy semantics. */
class FeudScriptBeatTest {

  @Test
  void setTitlesCopiesImmutableCallerSet() {
    // Editors assign Vaadin multiselect values (immutable sets) while the feud script service
    // mutates the collection via getTitles(), so the setter must copy instead of aliasing.
    FeudScriptBeat beat = new FeudScriptBeat();
    Title title = new Title();
    Set<Title> immutable = Set.of(title);

    beat.setTitles(immutable);

    assertNotSame(immutable, beat.getTitles(), "setTitles must copy, not alias");
    assertEquals(1, beat.getTitles().size());
    assertTrue(beat.getTitles().contains(title));
    beat.getTitles().add(new Title()); // must not throw
  }
}
