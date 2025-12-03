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

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import java.time.Instant;
import lombok.Data;

@Data
public class InjuryResponseDTO {
  private Long id;
  private String name;
  private String description;
  private InjurySeverity severity;
  private Boolean isActive;
  private Instant injuryDate;
  private String injuryNotes;
  private WrestlerResponseDTO wrestler;

  public InjuryResponseDTO(Injury injury) {
    this.id = injury.getId();
    this.name = injury.getName();
    this.description = injury.getDescription();
    this.severity = injury.getSeverity();
    this.isActive = injury.getIsActive();
    this.injuryDate = injury.getInjuryDate();
    this.injuryNotes = injury.getInjuryNotes();

    if (injury.getWrestler() != null) {
      WrestlerResponseDTO wrestlerDTO = new WrestlerResponseDTO();
      wrestlerDTO.setId(injury.getWrestler().getId());
      wrestlerDTO.setName(injury.getWrestler().getName());
      this.wrestler = wrestlerDTO;
    }
  }

  public InjuryResponseDTO() {}
}
