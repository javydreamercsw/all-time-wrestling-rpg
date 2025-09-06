package com.github.javydreamercsw.management.domain.wrestler;

import lombok.Data;

@Data
public class WrestlerDTO {
  private String name;
  private String description;

  public WrestlerDTO(Wrestler wrestler) {
    this.name = wrestler.getName();
    this.description = wrestler.getDescription();
  }
}
