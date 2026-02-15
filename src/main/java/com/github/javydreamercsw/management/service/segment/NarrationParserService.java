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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.dto.segment.NarrationLineDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for parsing AI-generated narration into structured dialogue lines. */
@Service
@Slf4j
public class NarrationParserService {

  private static final Pattern LINE_PATTERN = Pattern.compile("^(.*?): (.*)$", Pattern.MULTILINE);

  /**
   * Parses a raw narration string into a list of NarrationLineDTO objects.
   *
   * @param rawNarration The raw text from the AI.
   * @return A list of structured dialogue lines.
   */
  public List<NarrationLineDTO> parse(String rawNarration) {
    List<NarrationLineDTO> lines = new ArrayList<>();
    if (rawNarration == null || rawNarration.trim().isEmpty()) {
      return lines;
    }

    Matcher matcher = LINE_PATTERN.matcher(rawNarration);
    while (matcher.find()) {
      String name = matcher.group(1).trim();
      String content = matcher.group(2).trim();
      lines.add(new NarrationLineDTO(name, content));
    }

    // Fallback: if no tags found, treat the whole thing as a single line from "Announcer"
    if (lines.isEmpty()) {
      log.warn("No dialogue tags found in narration, using fallback parsing.");
      lines.add(new NarrationLineDTO("Announcer", rawNarration.trim()));
    }

    return lines;
  }
}
