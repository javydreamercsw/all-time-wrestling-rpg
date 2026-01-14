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
package com.github.javydreamercsw.management.service;

import com.github.javydreamercsw.management.domain.Holiday;
import com.github.javydreamercsw.management.domain.HolidayRepository;
import com.github.javydreamercsw.management.domain.HolidayType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class HolidayService {

  private final HolidayRepository repository;
  private static final int PROXIMITY_DAYS = 3;

  public HolidayService(HolidayRepository repository) {
    this.repository = repository;
  }

  public List<Holiday> findAll() {
    return repository.findAll();
  }

  public Holiday save(Holiday holiday) {
    return repository.save(holiday);
  }

  public void delete(Holiday holiday) {
    repository.delete(holiday);
  }

  public Optional<String> getHolidayTheme(Instant date) {
    LocalDate localDate = date.atZone(ZoneId.of("UTC")).toLocalDate();

    for (Holiday holiday : findAll()) {
      for (int yearOffset = -1;
          yearOffset <= 1;
          yearOffset++) { // Check current, previous, and next year
        int targetYear = localDate.getYear() + yearOffset;
        LocalDate holidayDate = null;

        if (holiday.getType() == HolidayType.FIXED) {
          if (holiday.getHolidayMonth() != null && holiday.getDayOfMonth() != null) {
            holidayDate =
                MonthDay.of(holiday.getHolidayMonth(), holiday.getDayOfMonth()).atYear(targetYear);
          }
        } else { // FLOATING
          if (holiday.getWeekOfMonth() != null
              && holiday.getDayOfWeek() != null
              && holiday.getHolidayMonth() != null) {
            if (holiday.getWeekOfMonth() > 0) {
              holidayDate =
                  LocalDate.of(targetYear, holiday.getHolidayMonth(), 1)
                      .with(
                          TemporalAdjusters.dayOfWeekInMonth(
                              holiday.getWeekOfMonth(), holiday.getDayOfWeek()));
            } else if (holiday.getWeekOfMonth() == -1) {
              holidayDate =
                  LocalDate.of(targetYear, holiday.getHolidayMonth(), 1)
                      .with(TemporalAdjusters.lastInMonth(holiday.getDayOfWeek()));
            }
          }
        }

        if (holidayDate != null && isDateInProximity(localDate, holidayDate)) {
          return Optional.of(holiday.getTheme());
        }
      }
    }
    return Optional.empty();
  }

  private boolean isDateInProximity(LocalDate date, LocalDate holidayDate) {
    long daysBetween = ChronoUnit.DAYS.between(holidayDate, date);
    return Math.abs(daysBetween) <= PROXIMITY_DAYS;
  }
}
