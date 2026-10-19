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

import com.github.javydreamercsw.management.domain.team.TeamSize;
import java.util.List;
import lombok.Data;

/**
 * Catalog entry for seeding predefined teams ({@code teams.json}).
 *
 * <p>Back-compat: legacy entries name two wrestlers via {@code wrestler1}/{@code wrestler2}; the
 * ordered {@code members} array is preferred and supports any {@link TeamSize}.
 */
@Data
public class TeamImportDTO {
  private String name;
  private String description;

  /** Ordered member names (slot 0..n-1); preferred over the deprecated duo fields. */
  private List<String> members;

  /** Declared size code ({@link TeamSize} name); defaults to DUO when the catalog omits it. */
  private String teamSize = TeamSize.DUO.name();

  /**
   * @deprecated legacy duo shape; kept so old catalogs still seed.
   */
  @Deprecated private String wrestler1;

  /**
   * @deprecated legacy duo shape; kept so old catalogs still seed.
   */
  @Deprecated private String wrestler2;

  private String manager;

  /**
   * @return true when the entry uses the legacy {@code wrestler1}/{@code wrestler2} shape.
   */
  public boolean hasLegacyWrestlerFields() {
    return (members == null || members.isEmpty()) && wrestler1 != null && wrestler2 != null;
  }
}
