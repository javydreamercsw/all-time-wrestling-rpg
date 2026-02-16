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
package com.github.javydreamercsw.management.domain.show.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.Month;
import org.junit.jupiter.api.Test;

class ShowTemplateRecurrenceTest {

  @Test
  void testRecurrenceFields() {
    ShowTemplate template = new ShowTemplate();
    template.setDurationDays(2);
    template.setRecurrenceType(RecurrenceType.MONTHLY);
    template.setDayOfWeek(DayOfWeek.SUNDAY);
    template.setWeekOfMonth(-1);
    template.setMonth(Month.FEBRUARY);
    template.setDayOfMonth(15);

    assertEquals(2, template.getDurationDays());
    assertEquals(RecurrenceType.MONTHLY, template.getRecurrenceType());
    assertEquals(DayOfWeek.SUNDAY, template.getDayOfWeek());
    assertEquals(-1, template.getWeekOfMonth());
    assertEquals(Month.FEBRUARY, template.getMonth());
    assertEquals(15, template.getDayOfMonth());
  }

  @Test
  void testDefaultValues() {
    ShowTemplate template = new ShowTemplate();
    assertEquals(1, template.getDurationDays());
    assertEquals(RecurrenceType.NONE, template.getRecurrenceType());
  }
}
