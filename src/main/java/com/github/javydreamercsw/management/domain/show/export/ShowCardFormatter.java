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
import java.util.List;

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
}
