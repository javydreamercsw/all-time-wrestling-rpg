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
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Formatter for Markdown export. */
@Component
public class MarkdownShowCardFormatter implements ShowCardFormatter {

  @Override
  public String getFormatName() {
    return "Markdown";
  }

  @Override
  public String format(Show show, List<Segment> segments) {
    StringBuilder sb = new StringBuilder();
    sb.append("# ").append(show.getName()).append("\n\n");

    if (show.getShowDate() != null) {
      sb.append("**Date:** ").append(show.getShowDate()).append("\n");
    }

    if (show.getArena() != null) {
      sb.append("**Venue:** ").append(show.getArena().getName()).append("\n");
    }

    sb.append("\n## Match Card\n\n");

    for (int i = 0; i < segments.size(); i++) {
      Segment segment = segments.get(i);
      sb.append("### Match ").append(i + 1).append(": ");
      sb.append(
          segment.getSegmentType() != null ? segment.getSegmentType().getName() : "Unknown Match");
      sb.append("\n");

      if (Boolean.TRUE.equals(segment.getIsTitleSegment())) {
        sb.append("**CHAMPIONSHIP MATCH**\n");
      }

      String participants =
          segment.getWrestlers().stream()
              .map(Wrestler::getName)
              .collect(Collectors.joining(" vs. "));

      sb.append(participants).append("\n");

      if (segment.hasSegmentRules()) {
        sb.append("*Rules: ").append(segment.getSegmentRulesAsString()).append("*\n");
      }

      sb.append("\n");
    }

    return sb.toString();
  }

  @Override
  public int getPriority() {
    return 10;
  }
}
