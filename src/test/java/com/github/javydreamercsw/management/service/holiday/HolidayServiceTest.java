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
package com.github.javydreamercsw.management.service.holiday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.Holiday;
import com.github.javydreamercsw.management.domain.HolidayRepository;
import com.github.javydreamercsw.management.domain.HolidayType;
import com.github.javydreamercsw.management.service.HolidayService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HolidayServiceTest {

  @Mock private HolidayRepository holidayRepository;

  @InjectMocks private HolidayService holidayService;

  private Holiday fixedHoliday;
  private Holiday floatingHoliday;

  @BeforeEach
  void setUp() {
    fixedHoliday = new Holiday();
    fixedHoliday.setDescription("New Year's Day");
    fixedHoliday.setTheme("New Year's Day");
    fixedHoliday.setType(HolidayType.FIXED);
    fixedHoliday.setHolidayMonth(Month.JANUARY);
    fixedHoliday.setDayOfMonth(1);

    floatingHoliday = new Holiday();
    floatingHoliday.setDescription("Labor Day");
    floatingHoliday.setTheme("Labor Day");
    floatingHoliday.setType(HolidayType.FLOATING);
    floatingHoliday.setHolidayMonth(Month.SEPTEMBER);
    floatingHoliday.setDayOfWeek(DayOfWeek.MONDAY);
    floatingHoliday.setWeekOfMonth(1);
  }

  @Test
  void testGetHolidayThemeFixed() {
    when(holidayRepository.findAll()).thenReturn(Collections.singletonList(fixedHoliday));

    LocalDate date = LocalDate.of(2025, Month.JANUARY, 1);
    Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);

    Optional<String> theme = holidayService.getHolidayTheme(instant);
    assertTrue(theme.isPresent());
    assertEquals("New Year's Day", theme.get());
  }

  @Test
  void testGetHolidayThemeFixedProximity() {
    when(holidayRepository.findAll()).thenReturn(Collections.singletonList(fixedHoliday));

    // Test one day before
    LocalDate dateBefore = LocalDate.of(2025, Month.DECEMBER, 31);
    Instant instantBefore = dateBefore.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeBefore = holidayService.getHolidayTheme(instantBefore);
    assertTrue(themeBefore.isPresent());
    assertEquals("New Year's Day", themeBefore.get());

    // Test one day after
    LocalDate dateAfter = LocalDate.of(2025, Month.JANUARY, 2);
    Instant instantAfter = dateAfter.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeAfter = holidayService.getHolidayTheme(instantAfter);
    assertTrue(themeAfter.isPresent());
    assertEquals("New Year's Day", themeAfter.get());

    // Test outside proximity
    LocalDate dateFar = LocalDate.of(2025, Month.JANUARY, 5);
    Instant instantFar = dateFar.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeFar = holidayService.getHolidayTheme(instantFar);
    assertFalse(themeFar.isPresent());
  }

  @Test
  void testGetHolidayThemeFloating() {
    when(holidayRepository.findAll()).thenReturn(Collections.singletonList(floatingHoliday));

    // Labor Day is the first Monday in September
    LocalDate date = LocalDate.of(2025, Month.SEPTEMBER, 1); // September 1, 2025 is a Monday
    Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);

    Optional<String> theme = holidayService.getHolidayTheme(instant);
    assertTrue(theme.isPresent());
    assertEquals("Labor Day", theme.get());

    date = LocalDate.of(2026, Month.SEPTEMBER, 7); // September 7, 2026 is a Monday
    instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);

    theme = holidayService.getHolidayTheme(instant);
    assertTrue(theme.isPresent());
    assertEquals("Labor Day", theme.get());
  }

  @Test
  void testGetHolidayThemeFloatingProximity() {
    when(holidayRepository.findAll()).thenReturn(Collections.singletonList(floatingHoliday));

    LocalDate laborDay2025 =
        LocalDate.of(2025, Month.SEPTEMBER, 1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)); // Sept 1, 2025

    // Test one day before
    LocalDate dateBefore = laborDay2025.minusDays(1);
    Instant instantBefore = dateBefore.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeBefore = holidayService.getHolidayTheme(instantBefore);
    assertTrue(themeBefore.isPresent());
    assertEquals("Labor Day", themeBefore.get());

    // Test one day after
    LocalDate dateAfter = laborDay2025.plusDays(1);
    Instant instantAfter = dateAfter.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeAfter = holidayService.getHolidayTheme(instantAfter);
    assertTrue(themeAfter.isPresent());
    assertEquals("Labor Day", themeAfter.get());

    // Test outside proximity
    LocalDate dateFar = laborDay2025.plusDays(4);
    Instant instantFar = dateFar.atStartOfDay().toInstant(ZoneOffset.UTC);
    Optional<String> themeFar = holidayService.getHolidayTheme(instantFar);
    assertFalse(themeFar.isPresent());
  }

  @Test
  void testGetHolidayThemeNoHoliday() {
    when(holidayRepository.findAll()).thenReturn(Arrays.asList(fixedHoliday, floatingHoliday));

    LocalDate date = LocalDate.of(2025, Month.JANUARY, 15); // Not a holiday
    Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);

    Optional<String> theme = holidayService.getHolidayTheme(instant);
    assertFalse(theme.isPresent());
  }
}
