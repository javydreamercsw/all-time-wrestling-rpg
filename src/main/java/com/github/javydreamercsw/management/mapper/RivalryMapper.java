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
package com.github.javydreamercsw.management.mapper;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.dto.wrestler.WrestlerDTO;
import org.springframework.stereotype.Component;

@Component
public class RivalryMapper {

  public WrestlerDTO toWrestlerDTO(Wrestler wrestler) {
    if (wrestler == null) {
      return null;
    }
    WrestlerDTO dto = new WrestlerDTO();
    dto.setId(wrestler.getId());
    dto.setName(wrestler.getName());
    dto.setFans(wrestler.getFans());
    dto.setTier(wrestler.getTier());
    dto.setExternalId(wrestler.getExternalId());
    // Map other fields as needed
    return dto;
  }

  public RivalryDTO toRivalryDTO(Rivalry rivalry) {
    if (rivalry == null) {
      return null;
    }
    RivalryDTO dto = new RivalryDTO();
    dto.setId(rivalry.getId());
    dto.setHeat(rivalry.getHeat());
    dto.setIsActive(rivalry.getIsActive());
    dto.setStorylineNotes(rivalry.getStorylineNotes());
    dto.setStartedDate(rivalry.getStartedDate());
    dto.setEndedDate(rivalry.getEndedDate());
    dto.setWrestler1(toWrestlerDTO(rivalry.getWrestler1()));
    dto.setWrestler2(toWrestlerDTO(rivalry.getWrestler2()));
    // Map other fields as needed
    return dto;
  }
}
