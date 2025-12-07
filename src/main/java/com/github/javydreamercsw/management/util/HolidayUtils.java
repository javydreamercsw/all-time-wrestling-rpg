/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HolidayUtils {

  private static final Map<MonthDay, String> FIXED_HOLIDAYS = new HashMap<>();
  private static final List<FloatingHolidayRule> FLOATING_HOLIDAYS = new ArrayList<>();
  private static final int PROXIMITY_DAYS = 3;

  static {
    // Fixed Holidays
    FIXED_HOLIDAYS.put(MonthDay.of(Month.JANUARY, 1), "New Year's Day");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.FEBRUARY, 14), "Valentine's Day");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.MARCH, 17), "St. Patrick's Day");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.JULY, 4), "Independence Day");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.OCTOBER, 31), "Halloween");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.NOVEMBER, 11), "Veterans Day");
    FIXED_HOLIDAYS.put(MonthDay.of(Month.DECEMBER, 25), "Christmas Day");

    // Floating Holidays
    FLOATING_HOLIDAYS.add(
        new FloatingHolidayRule(
            "Martin Luther King Jr. Day",
            Month.JANUARY,
            TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)));
    FLOATING_HOLIDAYS.add(
        new FloatingHolidayRule(
            "Presidents' Day",
            Month.FEBRUARY,
            TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY)));
    FLOATING_HOLIDAYS.add(
        new FloatingHolidayRule(
            "Memorial Day", Month.MAY, TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)));
    FLOATING_HOLIDAYS.add(
        new FloatingHolidayRule(
            "Labor Day", Month.SEPTEMBER, TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)));
    FLOATING_HOLIDAYS.add(
        new FloatingHolidayRule(
            "Thanksgiving",
            Month.NOVEMBER,
            TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)));
  }

  public static Optional<String> getHolidayTheme(Instant date) {
    LocalDate localDate = date.atZone(ZoneId.of("UTC")).toLocalDate();
    int year = localDate.getYear();

    // Check fixed holidays
    for (Map.Entry<MonthDay, String> entry : FIXED_HOLIDAYS.entrySet()) {
      LocalDate holidayDate = entry.getKey().atYear(year);
      if (isDateInProximity(localDate, holidayDate)) {
        return Optional.of(entry.getValue());
      }
    }

    // Check floating holidays
    for (FloatingHolidayRule rule : FLOATING_HOLIDAYS) {
      LocalDate holidayDate = rule.calculateDate(year);
      if (isDateInProximity(localDate, holidayDate)) {
        return Optional.of(rule.holidayName());
      }
    }

    return Optional.empty();
  }

  private static boolean isDateInProximity(LocalDate date, LocalDate holidayDate) {
    long daysBetween = ChronoUnit.DAYS.between(holidayDate, date);
    return Math.abs(daysBetween) <= PROXIMITY_DAYS;
  }

  private record FloatingHolidayRule(String holidayName, Month month, TemporalAdjuster adjuster) {
    public LocalDate calculateDate(int year) {
      return LocalDate.of(year, month, 1).with(adjuster);
    }
  }
}
