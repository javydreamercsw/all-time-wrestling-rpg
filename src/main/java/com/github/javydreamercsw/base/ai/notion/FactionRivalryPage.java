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

/** Represents a Faction Rivalry page (a row in the Faction Heat database) from Notion. */
@Data
@EqualsAndHashCode(callSuper = true)
public class FactionRivalryPage extends NotionPage {

  /**
   * Inner class representing the parent of a Notion page, which is typically a database. This is
   * required for the generic mapping functionality in NotionHandler.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class NotionParent extends NotionPage.NotionParent {}
}
