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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Base class for social media formatters. */
public abstract class AbstractSocialMediaFormatter implements ShowCardFormatter {

  @Override
  public String format(
      final Show show,
      final List<Segment> segments,
      final boolean includeSummary,
      final boolean includeResults,
      final boolean includeNarration) {
    StringBuilder sb = new StringBuilder();
    sb.append("📺 ").append(show.getName());
    if (show.getShowDate() != null) {
      sb.append(" (")
          .append(show.getShowDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
          .append(")");
    }
    sb.append("\n\n");

    for (Segment segment : segments) {
      String type = segment.getSegmentType() != null ? segment.getSegmentType().getName() : "";
      if (!type.isEmpty()) {
        sb.append("[").append(type).append("] ");
      }

      String participants =
          segment.getWrestlers().stream()
              .map(Wrestler::getName)
              .collect(Collectors.joining(" vs. "));

      sb.append("🔥 ").append(participants);

      if (Boolean.TRUE.equals(segment.getIsTitleSegment())) {
        sb.append(" 🏆");
        for (Title title : segment.getTitles()) {
          String champions = title.getCurrentChampionsAsString();
          sb.append("\n  📌 ").append(title.getName());
          if (!champions.isBlank()) {
            sb.append(" (champ: ").append(champions).append(")");
          }
        }
      }

      if (segment.isMainEvent()) {
        sb.append(" ⭐ MAIN EVENT ⭐");
      }
      sb.append("\n");

      if (segment.hasSegmentRules()) {
        sb.append("📜 Rules: ").append(segment.getSegmentRulesAsString()).append("\n");
      }

      if (includeResults && !segment.getWinners().isEmpty()) {
        String winners =
            segment.getWinners().stream().map(Wrestler::getName).collect(Collectors.joining(", "));
        sb.append("✅ Winner(s): ").append(winners).append("\n");
      }

      if (includeSummary
          && segment.getSummary() != null
          && !segment.getSummary().trim().isEmpty()) {
        sb.append("📝 ").append(segment.getSummary().trim()).append("\n");
      }

      if (includeNarration
          && segment.getNarration() != null
          && !segment.getNarration().trim().isEmpty()) {
        sb.append("💬 ").append(segment.getNarration().trim()).append("\n");
      }
      sb.append("\n");
    }

    sb.append(getHashtags());

    String result = sb.toString();
    return limitLength(result);
  }

  protected abstract String getHashtags();

  protected abstract String limitLength(String text);
}
