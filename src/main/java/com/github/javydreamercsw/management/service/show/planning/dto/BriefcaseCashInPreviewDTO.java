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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** A proposed NPC briefcase cash-in (ATW-2b9u) previewed on the show-planning card. */
@Data
public class BriefcaseCashInPreviewDTO {
  private Long opportunityId;
  private Long titleId;

  /**
   * PLANNED / AMBUSH / RUN_IN — see {@link
   * com.github.javydreamercsw.management.domain.title.BriefcaseCashInMode}.
   */
  private String mode;

  private String holderName;
  private String titleName;
  private List<String> championNames = new ArrayList<>();

  /** Display-only team layout: champions first, then the holder. */
  private List<List<String>> teams = new ArrayList<>();

  private String summary;
  private String notes;
}
