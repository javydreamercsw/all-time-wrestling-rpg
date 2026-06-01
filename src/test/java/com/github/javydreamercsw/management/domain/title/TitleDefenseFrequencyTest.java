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
package com.github.javydreamercsw.management.domain.title;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import org.junit.jupiter.api.Test;

class TitleDefenseFrequencyTest {

  @Test
  void defenseFrequencyTypeFieldRoundTrips() {
    Title title = new Title();
    title.setDefenseFrequencyType(DefenseFrequencyType.BI_WEEKLY);
    assertEquals(DefenseFrequencyType.BI_WEEKLY, title.getDefenseFrequencyType());
  }

  @Test
  void defenseFrequencyTypeDefaultsToNull() {
    Title title = new Title();
    assertNull(title.getDefenseFrequencyType());
  }

  @Test
  void weeklyIsOverdueAfterSevenDays() {
    assertTrue(DefenseFrequencyType.WEEKLY.isOverdue(7));
    assertFalse(DefenseFrequencyType.WEEKLY.isOverdue(6));
  }

  @Test
  void biWeeklyIsOverdueAfterFourteenDays() {
    assertTrue(DefenseFrequencyType.BI_WEEKLY.isOverdue(14));
    assertFalse(DefenseFrequencyType.BI_WEEKLY.isOverdue(13));
  }

  @Test
  void pleIsOverdueAfterTwentyEightDays() {
    assertTrue(DefenseFrequencyType.PLE.isOverdue(28));
    assertFalse(DefenseFrequencyType.PLE.isOverdue(27));
  }

  @Test
  void effectiveFrequencyUsesExplicitValueWhenSet() {
    Title title = new Title();
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setDefenseFrequencyType(DefenseFrequencyType.WEEKLY);
    assertEquals(DefenseFrequencyType.WEEKLY, title.getEffectiveDefenseFrequencyType());
  }

  @Test
  void effectiveFrequencyDefaultsToPlEForMainEventer() {
    Title title = new Title();
    title.setTier(WrestlerTier.MAIN_EVENTER);
    assertEquals(DefenseFrequencyType.PLE, title.getEffectiveDefenseFrequencyType());
  }

  @Test
  void effectiveFrequencyDefaultsToBiWeeklyForMidcarder() {
    Title title = new Title();
    title.setTier(WrestlerTier.MIDCARDER);
    assertEquals(DefenseFrequencyType.BI_WEEKLY, title.getEffectiveDefenseFrequencyType());
  }

  @Test
  void effectiveFrequencyDefaultsToWeeklyForOtherTiers() {
    Title title = new Title();
    title.setTier(WrestlerTier.CONTENDER);
    assertEquals(DefenseFrequencyType.WEEKLY, title.getEffectiveDefenseFrequencyType());
  }
}
