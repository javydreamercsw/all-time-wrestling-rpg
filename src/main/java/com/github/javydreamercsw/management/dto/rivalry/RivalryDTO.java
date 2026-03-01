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
package com.github.javydreamercsw.management.dto.rivalry;

import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import java.io.Serializable;
import java.time.Instant;
import lombok.Data;

@Data
public class RivalryDTO implements Serializable {
  private Long id;
  private WrestlerDTO wrestler1;
  private WrestlerDTO wrestler2;
  private int heat;
  private Boolean isActive;
  private String storylineNotes;
  private Instant startedDate;
  private Instant endedDate;

  /**
   * Helper to get the other participant in the rivalry.
   *
   * @param wrestler The participant to exclude
   * @return The opponent
   */
  public WrestlerDTO getOpponent(WrestlerDTO wrestler) {
    if (wrestler1 != null && wrestler1.getId().equals(wrestler.getId())) {
      return wrestler2;
    } else if (wrestler2 != null && wrestler2.getId().equals(wrestler.getId())) {
      return wrestler1;
    }
    return null;
  }
}
