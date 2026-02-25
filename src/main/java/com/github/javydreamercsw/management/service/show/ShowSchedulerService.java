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

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShowSchedulerService {

  private final ShowService showService;
  private final ShowTemplateService showTemplateService;

  /**
   * Generates empty show shells for a season based on available show templates and their recurrence
   * settings.
   *
   * @param season The season to generate shows for.
   */
  public void generateShowsForSeason(@NonNull Season season) {
    if (season.getStartDate() == null) {
      log.warn("Season {} has missing start date. Skipping show generation.", season.getName());
      return;
    }

    LocalDate seasonStart = season.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate seasonEnd;

    if (season.getEndDate() != null) {
      seasonEnd = season.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();
    } else {
      // Default to 1 year from start if open-ended
      seasonEnd = seasonStart.plusYears(1);
      log.info(
          "Season {} has no end date. Generating shows until {} (1 year from start).",
          season.getName(),
          seasonEnd);
    }

    List<ShowTemplate> templates = showTemplateService.findAll();

    // Filter existing shows for this season
    List<Show> seasonShows =
        showService.findAll().stream().filter(s -> season.equals(s.getSeason())).toList();

    for (ShowTemplate template : templates) {
      if (template.getRecurrenceType() == RecurrenceType.NONE) {
        continue;
      }

      Set<LocalDate> targetDates = calculateTargetDates(template, seasonStart, seasonEnd);
      if (targetDates.isEmpty()) {
        log.warn(
            "No target dates calculated for template '{}' (Recurrence: {}, DayOfWeek: {},"
                + " DayOfMonth: {}, WeekOfMonth: {}, Month: {}). Missing configuration?",
            template.getName(),
            template.getRecurrenceType(),
            template.getDayOfWeek(),
            template.getDayOfMonth(),
            template.getWeekOfMonth(),
            template.getMonth());
        continue;
      }
      Set<LocalDate> handledDates = new HashSet<>();

      for (LocalDate targetDate : targetDates) {
        for (int day = 0; day < template.getDurationDays(); day++) {
          LocalDate actualDate = targetDate.plusDays(day);
          handledDates.add(actualDate);

          String showName = template.getName();
          if (template.getDurationDays() > 1) {
            showName += " - Night " + (day + 1);
          }

          if (!showService.existsByNameAndShowDate(showName, actualDate)) {
            log.info("Creating show '{}' for date {}", showName, actualDate);
            showService.createShow(
                showName,
                template.getDescription(),
                template.getShowType().getId(),
                actualDate,
                season.getId(),
                template.getId(),
                null, // League ID - might need to be passed in
                template.getCommentaryTeam() != null ? template.getCommentaryTeam().getId() : null,
                null); // arenaId
          }
        }
      }

      // Cleanup unpopulated shows that are no longer on target dates
      for (Show show : seasonShows) {
        if (template.equals(show.getTemplate())
            && !handledDates.contains(show.getShowDate())
            && showService.getSegments(show).isEmpty()) {
          log.info(
              "Deleting unpopulated show '{}' on {} due to template change.",
              show.getName(),
              show.getShowDate());
          showService.deleteShow(show.getId());
        }
      }
    }
  }

  private Set<LocalDate> calculateTargetDates(
      ShowTemplate template, LocalDate seasonStart, LocalDate seasonEnd) {
    Set<LocalDate> dates = new HashSet<>();

    switch (template.getRecurrenceType()) {
      case WEEKLY:
        if (template.getDayOfWeek() != null) {
          LocalDate current =
              seasonStart.with(TemporalAdjusters.nextOrSame(template.getDayOfWeek()));
          while (!current.isAfter(seasonEnd)) {
            dates.add(current);
            current = current.plusWeeks(1);
          }
        }
        break;

      case MONTHLY:
        LocalDate currentMonth = seasonStart.withDayOfMonth(1);
        while (!currentMonth.isAfter(seasonEnd)) {
          LocalDate target =
              calculateDateInMonth(template, currentMonth.getYear(), currentMonth.getMonthValue());
          if (target != null && !target.isBefore(seasonStart) && !target.isAfter(seasonEnd)) {
            dates.add(target);
          }
          currentMonth = currentMonth.plusMonths(1);
        }
        break;

      case ANNUAL:
        if (template.getMonth() != null) {
          for (int year = seasonStart.getYear(); year <= seasonEnd.getYear(); year++) {
            LocalDate target = calculateDateInMonth(template, year, template.getMonth().getValue());
            if (target != null && !target.isBefore(seasonStart) && !target.isAfter(seasonEnd)) {
              dates.add(target);
            }
          }
        }
        break;
      default:
        break;
    }

    return dates;
  }

  private LocalDate calculateDateInMonth(ShowTemplate template, int year, int month) {
    if (template.getDayOfMonth() != null) {
      try {
        return LocalDate.of(year, month, template.getDayOfMonth());
      } catch (Exception e) {
        return null;
      }
    } else if (template.getWeekOfMonth() != null && template.getDayOfWeek() != null) {
      if (template.getWeekOfMonth() > 0) {
        return LocalDate.of(year, month, 1)
            .with(
                TemporalAdjusters.dayOfWeekInMonth(
                    template.getWeekOfMonth(), template.getDayOfWeek()));
      } else if (template.getWeekOfMonth() == -1) {
        return LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.lastInMonth(template.getDayOfWeek()));
      }
    }
    return null;
  }
}
