/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ShowTemplate {
  private String showName;
  private String description;
  private int expectedMatches;
  private int expectedPromos;
  private Gender genderConstraint;

  /**
   * Event-only segment types assigned to this template; AI may use them on its shows (ATW-0331).
   */
  private List<String> eventSegmentTypes = new ArrayList<>();

  /** Assigned rules the AI should prefer when booking this template's shows. */
  private List<String> encouragedRules = new ArrayList<>();

  /** Assigned rules deterministically attached to every approved match segment. */
  private List<String> autoAttachRules = new ArrayList<>();
}
