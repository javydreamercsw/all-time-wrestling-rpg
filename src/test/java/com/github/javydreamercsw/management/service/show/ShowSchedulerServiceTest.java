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
package com.github.javydreamercsw.management.service.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowSchedulerServiceTest {

  @Mock private ShowService showService;
  @Mock private ShowTemplateService showTemplateService;

  @InjectMocks private ShowSchedulerService showSchedulerService;

  private Season season;
  private ShowTemplate weeklyTemplate;
  private ShowTemplate pleTemplate;

  @BeforeEach
  void setUp() {
    season = new Season();
    season.setId(1L);
    season.setName("Test Season");
    // Start on a Monday
    season.setStartDate(LocalDate.of(2026, 2, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());
    // End after 4 weeks
    season.setEndDate(LocalDate.of(2026, 3, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());

    ShowType weeklyType = new ShowType();
    weeklyType.setId(1L);
    weeklyType.setName("Weekly");

    weeklyTemplate = new ShowTemplate();
    weeklyTemplate.setId(1L);
    weeklyTemplate.setName("Weekly Show");
    weeklyTemplate.setShowType(weeklyType);
    weeklyTemplate.setRecurrenceType(RecurrenceType.WEEKLY);
    weeklyTemplate.setDayOfWeek(DayOfWeek.MONDAY);
    weeklyTemplate.setDurationDays(1);

    ShowType pleType = new ShowType();
    pleType.setId(2L);
    pleType.setName("PLE");

    pleTemplate = new ShowTemplate();
    pleTemplate.setId(2L);
    pleTemplate.setName("Big Event");
    pleTemplate.setShowType(pleType);
    pleTemplate.setRecurrenceType(RecurrenceType.ANNUAL);
    pleTemplate.setMonth(Month.FEBRUARY);
    pleTemplate.setWeekOfMonth(-1); // Last
    pleTemplate.setDayOfWeek(DayOfWeek.SUNDAY);
    pleTemplate.setDurationDays(2);
  }

  @Test
  void testGenerateWeeklyShows() {
    when(showTemplateService.findAll()).thenReturn(List.of(weeklyTemplate));
    when(showService.existsByNameAndShowDate(any(), any())).thenReturn(false);

    showSchedulerService.generateShowsForSeason(season);

    // Should generate shows for Feb 2, 9, 16, 23, March 2
    verify(showService, atLeastOnce())
        .createShow(
            eq("Weekly Show"),
            any(),
            eq(1L),
            eq(LocalDate.of(2026, 2, 2)),
            eq(1L),
            eq(1L),
            any(),
            any(),
            any()); // Added null for arenaId
    verify(showService, atLeastOnce())
        .createShow(
            eq("Weekly Show"),
            any(),
            eq(1L),
            eq(LocalDate.of(2026, 2, 9)),
            eq(1L),
            eq(1L),
            any(),
            any(),
            any()); // Added null for arenaId
    verify(showService, atLeastOnce())
        .createShow(
            eq("Weekly Show"),
            any(),
            eq(1L),
            eq(LocalDate.of(2026, 2, 16)),
            eq(1L),
            eq(1L),
            any(),
            any(),
            any()); // Added null for arenaId
    verify(showService, atLeastOnce())
        .createShow(
            eq("Weekly Show"),
            any(),
            eq(1L),
            eq(LocalDate.of(2026, 2, 23)),
            eq(1L),
            eq(1L),
            any(),
            any(),
            any()); // Added null for arenaId
    verify(showService, atLeastOnce())
        .createShow(
            eq("Weekly Show"),
            any(),
            eq(1L),
            eq(LocalDate.of(2026, 3, 2)),
            eq(1L),
            eq(1L),
            any(),
            any(),
            any()); // Added null for arenaId
  }

  @Test
  void testGenerateMultiDayPle() {
    when(showTemplateService.findAll()).thenReturn(List.of(pleTemplate));
    when(showService.existsByNameAndShowDate(any(), any())).thenReturn(false);

    showSchedulerService.generateShowsForSeason(season);

    // Last Sunday of Feb 2026 is Feb 22.
    // Duration is 2 days, so Feb 22 and Feb 23.
    verify(showService, atLeastOnce())
        .createShow(
            eq("Big Event - Night 1"),
            any(),
            eq(2L),
            eq(LocalDate.of(2026, 2, 22)),
            eq(1L),
            eq(2L),
            any(),
            any(),
            any()); // Added null for arenaId
    verify(showService, atLeastOnce())
        .createShow(
            eq("Big Event - Night 2"),
            any(),
            eq(2L),
            eq(LocalDate.of(2026, 2, 23)),
            eq(1L),
            eq(2L),
            any(),
            any(),
            any()); // Added null for arenaId
  }

  @Test
  void testGenerateAnnualShowMissingDayOfWeek() {
    ShowType pleType = new ShowType();
    pleType.setId(2L);
    pleType.setName("PLE");

    ShowTemplate invalidTemplate = new ShowTemplate();
    invalidTemplate.setId(3L);
    invalidTemplate.setName("Invalid Annual");
    invalidTemplate.setShowType(pleType);
    invalidTemplate.setRecurrenceType(RecurrenceType.ANNUAL);
    invalidTemplate.setMonth(Month.JANUARY);
    invalidTemplate.setWeekOfMonth(-1); // Last week
    // DayOfWeek is intentionally NULL

    when(showTemplateService.findAll()).thenReturn(List.of(invalidTemplate));

    // The season is in Feb/March, but the template is for January.
    // Even if it were in January, it should be skipped because DayOfWeek is missing.
    showSchedulerService.generateShowsForSeason(season);

    // Verify no shows were created
    verify(showService, org.mockito.Mockito.never())
        .createShow(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
