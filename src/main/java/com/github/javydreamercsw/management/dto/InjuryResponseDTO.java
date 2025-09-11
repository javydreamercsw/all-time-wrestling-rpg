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
