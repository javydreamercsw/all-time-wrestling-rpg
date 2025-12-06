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
package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SeasonPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Season-specific properties only (common properties inherited from base)
    private Property Description;
    private Property StartDate;
    private Property EndDate;
    private Property IsActive;
    private Property ShowsPerPpv;
    private Property Shows; // Relation to shows in this season
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  static class NotionParent extends NotionPage.NotionParent {
    // Season-specific parent properties (if any)
  }
}
