package com.github.javydreamercsw.management.mapper;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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
