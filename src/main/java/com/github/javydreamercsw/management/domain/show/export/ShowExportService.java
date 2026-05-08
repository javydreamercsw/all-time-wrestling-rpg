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
package com.github.javydreamercsw.management.domain.show.export;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Service for exporting show cards using discovered formatters. */
@Service
public class ShowExportService {

  private final SegmentRepository segmentRepository;
  private final List<ShowCardFormatter> formatters;

  public ShowExportService(
      SegmentRepository segmentRepository, List<ShowCardFormatter> formatters) {
    this.segmentRepository = segmentRepository;
    this.formatters = formatters;
  }

  /**
   * Get names of all available export formats, sorted by priority.
   *
   * @return list of format names
   */
  public List<String> getAvailableFormats() {
    return formatters.stream()
        .sorted(Comparator.comparingInt(ShowCardFormatter::getPriority))
        .map(ShowCardFormatter::getFormatName)
        .collect(Collectors.toList());
  }

  /**
   * Export a show card in the specified format.
   *
   * @param show the show to export
   * @param formatName the name of the format to use
   * @param includeSummary whether to include segment summaries
   * @param includeResults whether to include match results
   * @param includeNarration whether to include segment narration
   * @return formatted show card text
   * @throws IllegalArgumentException if format is not found
   */
  public String export(
      Show show,
      String formatName,
      boolean includeSummary,
      boolean includeResults,
      boolean includeNarration) {
    ShowCardFormatter formatter =
        formatters.stream()
            .filter(f -> f.getFormatName().equalsIgnoreCase(formatName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown export format: " + formatName));

    List<Segment> segments = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    return formatter.format(show, segments, includeSummary, includeResults, includeNarration);
  }
}
