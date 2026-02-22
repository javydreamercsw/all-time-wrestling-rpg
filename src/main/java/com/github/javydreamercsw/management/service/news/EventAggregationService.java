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
package com.github.javydreamercsw.management.service.news;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventAggregationService {

  private final SegmentRepository segmentRepository;
  private final TitleReignRepository titleReignRepository;

  @Data
  @Builder
  public static class MonthlySummary {
    private List<Segment> segments;
    private List<TitleReign> titleChanges;
    private Instant startDate;
    private Instant endDate;
  }

  @Transactional(readOnly = true)
  public MonthlySummary getMonthlySummary() {
    Instant now = Instant.now();
    Instant oneMonthAgo = now.minus(30, ChronoUnit.DAYS);

    List<Segment> segments =
        segmentRepository.findAll().stream()
            .filter(
                s -> s.getSegmentDate().isAfter(oneMonthAgo) && s.getSegmentDate().isBefore(now))
            .toList();

    List<TitleReign> titleChanges =
        titleReignRepository.findAll().stream()
            .filter(r -> r.getStartDate().isAfter(oneMonthAgo) && r.getStartDate().isBefore(now))
            .toList();

    return MonthlySummary.builder()
        .segments(segments)
        .titleChanges(titleChanges)
        .startDate(oneMonthAgo)
        .endDate(now)
        .build();
  }

  public String formatMonthlySummary(MonthlySummary summary) {
    StringBuilder sb = new StringBuilder();
    sb.append("PERIOD: ")
        .append(summary.getStartDate())
        .append(" to ")
        .append(summary.getEndDate())
        .append("\n\n");

    if (!summary.getTitleChanges().isEmpty()) {
      sb.append("TITLE CHANGES:\n");
      for (TitleReign reign : summary.getTitleChanges()) {
        String champs =
            reign.getChampions().stream().map(Wrestler::getName).collect(Collectors.joining(", "));
        sb.append("- ")
            .append(reign.getTitle().getName())
            .append(": ")
            .append(champs)
            .append(" crowned champion(s).\n");
      }
      sb.append("\n");
    }

    if (!summary.getSegments().isEmpty()) {
      sb.append("KEY MATCH RESULTS:\n");
      // Filter for important segments only to avoid overwhelming AI
      List<Segment> importantSegments =
          summary.getSegments().stream()
              .filter(s -> s.getIsTitleSegment() || s.isMainEvent())
              .limit(20)
              .toList();

      for (Segment s : importantSegments) {
        String winners =
            s.getWinners().stream().map(Wrestler::getName).collect(Collectors.joining(", "));
        sb.append("- ")
            .append(s.getShow().getName())
            .append(" (")
            .append(s.getSegmentType().getName())
            .append("): ")
            .append(winners)
            .append(" won.\n");
      }
    }

    return sb.toString();
  }
}
