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
package com.github.javydreamercsw.management.service.show.planning.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Planning-card preview of one show-attached tournament slot (ATW-xbn4): what the tournament
 * booking would add on this show at approval. Participants resolve from the bracket at approval —
 * the card carries the shape so the booker sees (and can delete) the slot up front.
 */
@Data
public class TournamentSlotPreviewDTO {

  private String tournamentName;

  /** Payoff/round segment type name, e.g. "Free-for-All" or "One on One". */
  private String typeName;

  /** Optional payoff stipulation name, e.g. "Tables, Ladders and Chairs (TLC)". */
  private String ruleName;

  /** What the slot is: "Payoff final", "Champion showcase", or "N round matches". */
  private String shape;

  /** Championship expected on the line for this slot, or null. */
  private String titleName;

  /**
   * Real match-up (one list per team) when the bracket can supply it — seeded brackets' open
   * matches and champion showcases preview actual participants. Placeholder teams ("Tournament
   * bracket") when the bracket cannot (unseeded, or the final's participants are unknowable before
   * earlier rounds play).
   */
  private List<List<String>> teams = new ArrayList<>();
}
