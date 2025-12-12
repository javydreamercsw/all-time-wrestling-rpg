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
package com.github.javydreamercsw.management.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Transfer Object for Segment information from Notion. Used for synchronizing segment data
 * between Notion and the local database.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SegmentDTO {

  private String externalId;
  private String name;
  private String showName;
  private String showExternalId;
  private List<String> participantNames;
  private List<String> winnerNames;
  private String segmentTypeName;
  private Instant segmentDate;
  private String narration;
  private List<Long> titleIds; // Added for title association
  private int segmentOrder;
  private boolean isMainEvent;
}
