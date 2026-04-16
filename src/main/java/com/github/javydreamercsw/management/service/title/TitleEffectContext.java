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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Context object exposed to Groovy scripts in Titles. Provides methods to manipulate segment
 * context and match flow for AI narration.
 */
@Slf4j
@RequiredArgsConstructor
public class TitleEffectContext {

  private final SegmentNarrationContext segmentContext;
  private final WrestlerContext champion;

  public void gainInitiative() {
    String instr = segmentContext.getInstructions();
    if (instr == null) instr = "";
    instr +=
        "\n\nIMPORTANT: The champion, "
            + champion.getName()
            + ", has the initiative at the start of the match.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} gained initiative", champion.getName());
  }

  public void gainHitPoints(int amount) {
    champion.setHealth(champion.getHealth() + amount);

    String instr = segmentContext.getInstructions();
    if (instr == null) instr = "";
    instr +=
        "\n\nNOTE: "
            + champion.getName()
            + " starts the match with extra physical endurance (+ "
            + amount
            + " HP) due to being the champion.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} gained {} HP", champion.getName(), amount);
  }

  public void modifyRoll(int modifier) {
    String instr = segmentContext.getInstructions();
    if (instr == null) instr = "";
    instr +=
        "\n\nNOTE: "
            + champion.getName()
            + " has a championship advantage—they have a one-time bonus (+ "
            + modifier
            + ") to a critical roll during the match.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} granted roll bonus: {}", champion.getName(), modifier);
  }
}
