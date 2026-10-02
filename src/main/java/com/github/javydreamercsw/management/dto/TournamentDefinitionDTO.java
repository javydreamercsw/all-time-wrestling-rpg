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
package com.github.javydreamercsw.management.dto;

import java.util.List;

/**
 * One entry of the tournaments.json seed catalog (ATW-vg16) — the JSON shape of a predefined
 * tournament definition. {@code allowedRules} carries segment rule NAMES (SegmentRule is
 * name-keyed; the sync resolves them via SegmentRuleRepository.findByName).
 */
public record TournamentDefinitionDTO(
    String name,
    String code,
    String formatId,
    Integer defaultEntrantCount,
    List<String> allowedRules) {

  public TournamentDefinitionDTO {
    allowedRules = allowedRules == null ? List.of() : List.copyOf(allowedRules);
  }
}
