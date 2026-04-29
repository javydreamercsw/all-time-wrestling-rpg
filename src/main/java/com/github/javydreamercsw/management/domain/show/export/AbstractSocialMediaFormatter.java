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

/** Base class for social media formatters. */
public abstract class AbstractSocialMediaFormatter implements ShowCardFormatter {

  @Override
  public String format(Show show, List<Segment> segments) {
    StringBuilder sb = new StringBuilder();
    sb.append("📺 ").append(show.getName()).append("\n\n");

    for (Segment segment : segments) {
      String participants =
          segment.getWrestlers().stream()
              .map(Wrestler::getName)
              .collect(Collectors.joining(" vs. "));

      sb.append("🔥 ").append(participants);

      if (Boolean.TRUE.equals(segment.getIsTitleSegment())) {
        sb.append(" 🏆");
      }
      sb.append("\n");
    }

    sb.append("\n").append(getHashtags());

    String result = sb.toString();
    return limitLength(result);
  }

  protected abstract String getHashtags();

  protected abstract String limitLength(String text);
}
