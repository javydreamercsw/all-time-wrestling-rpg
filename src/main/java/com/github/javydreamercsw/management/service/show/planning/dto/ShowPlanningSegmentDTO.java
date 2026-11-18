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
package com.github.javydreamercsw.management.service.show.planning.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class ShowPlanningSegmentDTO {
  private Long id;
  private String segmentType;
  private String name;
  private String showName;

  /** True when the source show is a premium live event — PLE matches don't block the week. */
  private boolean premiumLiveEvent;

  private Instant showDate;
  private Instant segmentDate;
  private List<String> participants;
  private List<String> winners;
  private String summary;

  /** Rule (stipulation) names attached to the segment; empty for ruleless segments. */
  private List<String> rules = List.of();
}
