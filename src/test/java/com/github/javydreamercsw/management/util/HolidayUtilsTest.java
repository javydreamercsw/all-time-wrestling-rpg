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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HolidayUtilsTest {

  private Instant toInstant(LocalDate date) {
    return date.atStartOfDay(ZoneId.of("UTC")).toInstant();
  }

  @Test
  void getHolidayTheme_onFixedHoliday() {
    // Given: A date exactly on Christmas Day
    LocalDate christmasDate = LocalDate.of(2024, Month.DECEMBER, 25);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(christmasDate));

    // Then: The theme should be "Christmas Day"
    assertTrue(holidayTheme.isPresent());
    assertEquals("Christmas Day", holidayTheme.get());
  }

  @Test
  void getHolidayTheme_nearFixedHoliday() {
    // Given: A date 2 days before Christmas
    LocalDate nearChristmasDate = LocalDate.of(2024, Month.DECEMBER, 23);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(nearChristmasDate));

    // Then: The theme should still be "Christmas Day"
    assertTrue(holidayTheme.isPresent());
    assertEquals("Christmas Day", holidayTheme.get());
  }

  @Test
  void getHolidayTheme_onFloatingHoliday() {
    // Given: A date exactly on Thanksgiving 2024 (Fourth Thursday in November)
    LocalDate thanksgivingDate = LocalDate.of(2024, Month.NOVEMBER, 28);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(thanksgivingDate));

    // Then: The theme should be "Thanksgiving"
    assertTrue(holidayTheme.isPresent());
    assertEquals("Thanksgiving", holidayTheme.get());
  }

  @Test
  void getHolidayTheme_nearFloatingHoliday() {
    // Given: A date 1 day after Thanksgiving 2024
    LocalDate nearThanksgivingDate = LocalDate.of(2024, Month.NOVEMBER, 29);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(nearThanksgivingDate));

    // Then: The theme should still be "Thanksgiving"
    assertTrue(holidayTheme.isPresent());
    assertEquals("Thanksgiving", holidayTheme.get());
  }

  @Test
  void getHolidayTheme_noHoliday() {
    // Given: A date with no holiday nearby
    LocalDate noHolidayDate = LocalDate.of(2024, Month.AUGUST, 15);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(noHolidayDate));

    // Then: The theme should be empty
    assertFalse(holidayTheme.isPresent());
  }

  @Test
  void getHolidayTheme_differentYear() {
    // Given: A date on New Year's Day in 2025
    LocalDate newYearsDate2025 = LocalDate.of(2025, Month.JANUARY, 1);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(newYearsDate2025));

    // Then: The theme should correctly be "New Year's Day"
    assertTrue(holidayTheme.isPresent());
    assertEquals("New Year's Day", holidayTheme.get());
  }

  @Test
  void getHolidayTheme_memorialDay() {
    // Given: A date exactly on Memorial Day 2024 (Last Monday in May)
    LocalDate memorialDay = LocalDate.of(2024, Month.MAY, 27);

    // When: We check for a holiday theme
    Optional<String> holidayTheme = HolidayUtils.getHolidayTheme(toInstant(memorialDay));

    // Then: The theme should be "Memorial Day"
    assertTrue(holidayTheme.isPresent());
    assertEquals("Memorial Day", holidayTheme.get());
  }
}
