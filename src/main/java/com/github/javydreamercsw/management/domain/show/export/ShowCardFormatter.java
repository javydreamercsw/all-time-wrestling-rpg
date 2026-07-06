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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Interface for formatting a show card for export. */
public interface ShowCardFormatter {

  /**
   * Get the display name of the export format.
   *
   * @return display name
   */
  String getFormatName();

  /**
   * Format the show card.
   *
   * @param show the show to format
   * @param segments the segments of the show
   * @param includeSummary whether to include segment summaries
   * @param includeResults whether to include match results
   * @param includeNarration whether to include segment narration
   * @return formatted text
   */
  String format(
      Show show,
      List<Segment> segments,
      boolean includeSummary,
      boolean includeResults,
      boolean includeNarration);

  /**
   * Get the priority for sorting in the UI. Lower values come first.
   *
   * @return priority
   */
  default int getPriority() {
    return 100;
  }

  /**
   * Format the participants of a segment respecting team groupings.
   *
   * <p>Within each team names are joined with ", " and " &amp;" before the last member (e.g.
   * "Alpha, Beta &amp; Gamma"). Teams are separated by " vs. ". A single-member team is rendered as
   * just the wrestler's name, so standard singles and multi-person matches continue to display as
   * "A vs. B vs. C".
   */
  static String formatParticipants(Segment segment) {
    Map<Integer, List<Wrestler>> byTeam = segment.getWrestlersByTeam();
    if (byTeam.isEmpty()) {
      return "";
    }
    return byTeam.values().stream()
        .map(
            wrestlers -> {
              if (wrestlers.size() == 1) {
                return wrestlers.get(0).getName();
              }
              String joined =
                  wrestlers.stream().map(Wrestler::getName).collect(Collectors.joining(", "));
              int lastComma = joined.lastIndexOf(", ");
              return joined.substring(0, lastComma) + " & " + joined.substring(lastComma + 2);
            })
        .collect(Collectors.joining(" vs. "));
  }
}
